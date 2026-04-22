package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.gui.hudgui.elements.hud.Notification
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.CrystalPlaceBreak
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.PathFinder
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.combat.HoleType
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.isActiveOrFalse
import dev.wizard.meta.util.threads.runSafeSuspend
import dev.wizard.meta.util.world.getGroundPos
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.toList
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.MoverType
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11
import java.util.*

object HolePathFinder : Module(
    name = "HolePathFinder",
    category = Category.MOVEMENT,
    description = "I love hole",
    priority = 1010
) {
    private val moveMode by setting("Move Mode", MoveMode.NORMAL)
    private val bedrockHole by setting("Bedrock Hole", true)
    private val obsidianHole by setting("Obsidian Hole", true)
    private val twoBlocksHole by setting("2 Blocks Hole", true)
    private val fourBlocksHole by setting("4 Blocks Hole", true)
    private val maxMoveTicks by setting("Max Move Ticks", 20, 1..50, 1, { moveMode == MoveMode.TELEPORT })
    private val enableHoleSnap by setting("Enable Hole Snap", true, { moveMode == MoveMode.NORMAL })
    private val bindtargetHole by setting("Bind Target Hole", Bind(), {
        if (it) {
            if (isDisabled) {
                type = TargetHoleType.TARGET
                enable()
            } else {
                disable()
            }
        }
    })
    private val bindNearTarget by setting("Bind Near Target", Bind(), {
        if (it) {
            if (isDisabled) {
                type = TargetHoleType.NEAR_TARGET
                enable()
            } else {
                disable()
            }
        }
    })
    val enableStep by setting("Enable Step", true)
    private val antiPistonTimeout by setting("Anti Piston Timeout", 0, 0..10, 1)
    private val maxTargetHoles by setting("Max target Holes", 5, 1..10, 1)
    private val calcTimeout by setting("Calculation Timeout", 200, 10..1000, 10)
    private val range by setting("Range", 8, 1..16, 1)
    private val scanVRange by setting("Scan V Range", 16, 4..32, 1)
    private val scanHRange by setting("Scan H Range", 16, 4..30, 1)

    private var type = TargetHoleType.NORMAL
    var hole: HoleInfo? = null
        private set
    private var job: Job? = null
    private var path: Deque<PathFinder.PathNode>? = null
    private var targetPos: BlockPos? = null
    private val pistonTimer = TickTimer(TimeUnit.SECONDS)
    private var pistonHolePos: BlockPos? = null

    override fun isActive(): Boolean {
        return isEnabled && hole != null
    }

    private fun SafeClientEvent.check(): Deque<PathFinder.PathNode>? {
        if (!job.isActiveOrFalse() && hole == null) {
            Notification.send(this@HolePathFinder, "Calculation timeout")
            disable()
            return null
        }

        val playerPos = EntityUtils.getBetterPosition(player)
        val playerHole = HoleManager.getHoleInfo(playerPos)

        if (type != TargetHoleType.NORMAL) {
            getTargetPos()?.let { newPos ->
                if (newPos != targetPos) {
                    calculatePath(this)
                    return null
                }
            }
        }

        if (playerHole.origin == hole?.origin) {
            disable()
            return null
        }

        val newHole = hole?.let { oldHole ->
            val info = HoleManager.getHoleInfo(oldHole.origin)
            if (validateHole(info)) info else null
        }

        if (newHole == null) {
            calculatePath(this)
            return null
        }

        val path = path ?: return null
        val goal = path.lastOrNull()

        if (goal == null && !playerHole.isHole) {
            calculatePath(this)
            return null
        }

        if (goal == null || (playerPos.x == goal.x && playerPos.y == goal.y && playerPos.z == goal.z)) {
            disable()
            return null
        }

        if (!clearPreviousNode(this, path)) {
            calculatePath(this)
            return null
        }

        if (moveMode == MoveMode.NORMAL && enableHoleSnap) {
            HoleSnap.enable()
        }

        if (HoleSnap.isActive()) {
            return null
        }

        return path
    }

    private fun SafeClientEvent.moveTeleport(path: Deque<PathFinder.PathNode>) {
        val baseSpeed = MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, 0.2873)
        var countDown = maxMoveTicks
        while (countDown-- > 0 && path.firstOrNull() != null) {
            val node = path.first()
            var motionX = node.x + 0.5 - player.posX
            var motionZ = node.z + 0.5 - player.posZ
            val total = Math.hypot(motionX, motionZ)

            if (total > baseSpeed) {
                val multiplier = baseSpeed / total
                motionX *= multiplier
                motionZ *= multiplier
            } else if (player.posY.toInt() <= node.y) {
                path.pollFirst()
            }

            player.motionX = motionX
            player.motionZ = motionZ
            player.move(MoverType.SELF, motionX, player.motionY, motionZ)
            connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY, player.posZ, player.onGround))
        }
        player.motionX = 0.0
        player.motionZ = 0.0
    }

    private fun SafeClientEvent.moveLegit(event: PlayerMoveEvent.Pre, path: Deque<PathFinder.PathNode>) {
        val baseSpeed = MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, 0.2873)
        var countDown = 20
        var motionX = 0.0
        var motionZ = 0.0
        while (countDown-- > 0 && path.firstOrNull() != null) {
            val node = path.first()
            motionX = node.x + 0.5 - player.posX
            motionZ = node.z + 0.5 - player.posZ
            val total = Math.hypot(motionX, motionZ)

            if (total > baseSpeed) {
                val multiplier = baseSpeed / total
                motionX *= multiplier
                motionZ *= multiplier
                break
            }

            if (player.posY.toInt() > node.y) break
            path.pollFirst()
        }
        player.motionX = 0.0
        player.motionZ = 0.0
        event.x = motionX
        event.z = motionZ
        TimerManager.modifyTimer(this@HolePathFinder, 45.87156f)
    }

    private fun clearPreviousNode(event: SafeClientEvent, path: Deque<PathFinder.PathNode>): Boolean {
        while (path.firstOrNull() != null) {
            val nextNode = path.first()
            val lastNode = nextNode.parent
            if (lastNode != null) {
                val minX = Math.min(lastNode.x, nextNode.x)
                val maxX = Math.max(lastNode.x, nextNode.x)
                val minY = Math.min(lastNode.y, nextNode.y)
                val maxY = Math.max(lastNode.y, nextNode.y)
                val minZ = Math.min(lastNode.z, nextNode.z)
                val maxZ = Math.max(lastNode.z, nextNode.z)

                if (event.player.posX >= minX - 0.5 && event.player.posX <= maxX + 1.5 &&
                    event.player.posY >= minY - 0.5 && event.player.posY <= maxY + 1.5 &&
                    event.player.posZ >= minZ - 0.5 && event.player.posZ <= maxZ + 1.5
                ) {
                    return true
                }
            } else if (MathUtils.approxEq(event.player.posX, nextNode.x + 0.5, 0.2) &&
                MathUtils.approxEq(event.player.posZ, nextNode.z + 0.5, 0.2) &&
                MathUtils.approxEq(event.player.posY, nextNode.y.toDouble(), 0.5)
            ) {
                return true
            }

            if (path.pollFirst() == null || path.isEmpty()) break
        }
        return false
    }

    private fun calculatePath(event: SafeClientEvent) {
        if (job.isActiveOrFalse()) return

        job = ConcurrentScope.launch {
            val playerPos = EntityUtils.getBetterPosition(event.player)
            val rangeSq = MathUtilKt.getSq(range)
            val currentTargetPos = getTargetPos()

            val sequence = HoleManager.holeInfos.asSequence()
                .filterNot { it.isFullyTrapped }
                .filter { validateHole(it) }

            val sortedHoles = when (type) {
                TargetHoleType.NORMAL -> {
                    sequence.filter { playerPos.distanceSq(it.origin) <= rangeSq }
                        .sortedBy { playerPos.distanceSqTo(it.origin) }
                }
                TargetHoleType.TARGET -> {
                    sequence.filter { (currentTargetPos ?: playerPos).distanceSq(it.origin) <= rangeSq }
                        .sortedBy { (currentTargetPos ?: playerPos).distanceSqTo(it.origin) }
                }
                TargetHoleType.NEAR_TARGET -> {
                    val targetHole = currentTargetPos?.let { HoleManager.getHoleInfo(it).origin }
                    sequence.filter { (currentTargetPos ?: playerPos).distanceSq(it.origin) <= rangeSq }
                        .filter { it.origin != targetHole }
                        .sortedBy { (currentTargetPos ?: playerPos).distanceSqTo(it.origin) }
                }
            }

            val holes = sortedHoles.take(maxTargetHoles).toList()

            if (holes.isEmpty()) {
                Notification.send(this@HolePathFinder, "No Holes")
                disable()
                return@launch
            }

            val actor = parallelPathFindingActor(this, currentTargetPos)
            coroutineScope {
                val set = dumpWorld(event)
                val pathFinder = PathFinder(set, MathUtilKt.floorToInt(Step.maxHeight), 0, 4)
                val startPos = event.world.getGroundPos(event.player).up()
                val startNode = toNode(startPos)

                holes.forEachIndexed { i, holeInfo ->
                    launch {
                        val result = runCatching {
                            pathFinder.calculatePath(startNode, toNode(holeInfo.origin), calcTimeout)
                        }
                        val path = result.getOrNull()
                        if (path != null) {
                            actor.send(IndexedValue(i, holeInfo to path))
                        }
                    }
                }
            }
            actor.close()
        }
    }

    private fun validateHole(it: HoleInfo): Boolean {
        return when (it.type) {
            HoleType.NONE -> false
            HoleType.OBBY -> obsidianHole
            HoleType.BEDROCK -> bedrockHole
            HoleType.TWO -> twoBlocksHole
            HoleType.FOUR -> fourBlocksHole
        }
    }

    private fun CoroutineScope.parallelPathFindingActor(scope: CoroutineScope, target: BlockPos?) = scope.actor<IndexedValue<Pair<HoleInfo, Deque<PathFinder.PathNode>>>>(capacity = 15) {
        val results = channel.toList()
        val bestResult = if (type == TargetHoleType.NORMAL) {
            results.minByOrNull { it.value.second.lastOrNull()?.g ?: -1 }
        } else {
            results.minByOrNull { it.index }
        }

        val finalResult = bestResult?.value
        if (finalResult == null) {
            hole = null
            path = null
            if (type != TargetHoleType.NORMAL) {
                targetPos = target
            }
        } else {
            hole = finalResult.first
            path = finalResult.second
        }
    }

    private fun getTargetPos(): BlockPos? {
        val target = if (CrystalPlaceBreak.isEnabled) CrystalPlaceBreak.target else null
        return (target ?: CombatManager.target)?.let { EntityUtils.getBetterPosition(it) }
    }

    private fun dumpWorld(event: SafeClientEvent): LongSet {
        val set = LongOpenHashSet()
        val playerPos = EntityUtils.getBetterPosition(event.player)
        val mutablePos = BlockPos.MutableBlockPos()
        val hRange = scanHRange
        val vRange = scanVRange

        for (x in -hRange..hRange) {
            for (z in -hRange..hRange) {
                for (y in -vRange..vRange) {
                    VectorUtils.setAndAdd(mutablePos, playerPos, x, y, z)
                    if (!event.world.isBlockLoaded(mutablePos) && event.world.worldBorder.contains(mutablePos)) {
                        val blockState = event.world.getBlockState(mutablePos)
                        if (blockState.getCollisionBoundingBox(event.world, mutablePos) != null) {
                            set.add(mutablePos.toLong())
                        }
                    }
                }
            }
        }
        return set
    }

    private fun toNode(pos: BlockPos) = PathFinder.Node(pos.x, pos.y, pos.z)

    init {
        onDisable {
            type = TargetHoleType.NORMAL
            job?.cancel()
            job = null
            hole = null
            path = null
            targetPos = null
        }

        onEnable {
            SafeClientEvent.instance?.let {
                if (type == TargetHoleType.NORMAL && validateHole(HoleManager.getHoleInfo(it.player))) {
                    Notification.send(this, "Already in hole")
                    disable()
                    return@onEnable
                }
                if (it.world.collidesWithAnyBlock(it.player.entityBoundingBox)) {
                    Notification.send(this, "Player in block")
                    disable()
                    return@onEnable
                }
                calculatePath(it)
            } ?: disable()
        }

        listener<InputUpdateEvent>(-68) {
            if (it.movementInput is MovementInputFromOptions && isActive()) {
                MovementUtils.resetMove(it.movementInput)
            }
        }

        listener<WorldEvent.ServerBlockUpdate> {
            if (antiPistonTimeout == 0) return@listener
            val block = it.newState.block
            if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON || block == Blocks.PISTON_HEAD || block == Blocks.PISTON_EXTENSION) {
                val holeInfo = HoleManager.getHoleInfo(player)
                if (validateHole(holeInfo)) {
                    pistonTimer.reset()
                    pistonHolePos = holeInfo.origin
                }
            }
        }

        parallelListener<TickEvent.Post>(true) {
            if (antiPistonTimeout == 0) {
                pistonTimer.setTime(0L)
                return@parallelListener
            }
            if (pistonTimer.tick(antiPistonTimeout)) {
                return@parallelListener
            }
            if (MovementUtils.getRealMotionY(player) > 0.1) {
                pistonTimer.setTime(0L)
                return@parallelListener
            }
            val holeInfo = HoleManager.getHoleInfo(player)
            if (validateHole(holeInfo) || holeInfo.origin == pistonHolePos || world.collidesWithAnyBlock(player.entityBoundingBox)) {
                return@parallelListener
            }
            enable()
        }

        listener<PlayerMoveEvent.Pre> { event ->
            if (moveMode == MoveMode.NORMAL) {
                check()?.let { moveLegit(event, it) }
            }
        }

        listener<TickEvent.Post> {
            if (moveMode == MoveMode.TELEPORT) {
                check()?.let { moveTeleport(it) }
            }
        }

        listener<Render3DEvent> {
            path?.let {
                val color = ColorRGB(32, 255, 32, 200)
                it.forEach { cell ->
                    RenderUtils3D.putVertex(cell.x + 0.5, cell.y + 0.5, cell.z + 0.5, color)
                }
                GlStateManager.glLineWidth(2.0f)
                GL11.glDisable(GL11.GL_DEPTH_TEST)
                RenderUtils3D.draw(GL11.GL_LINE_STRIP)
                GlStateManager.glLineWidth(1.0f)
                GL11.glEnable(GL11.GL_DEPTH_TEST)
            }
        }
    }

    private enum class MoveMode {
        NORMAL, TELEPORT
    }

    enum class TargetHoleType {
        NORMAL, TARGET, NEAR_TARGET
    }
}
