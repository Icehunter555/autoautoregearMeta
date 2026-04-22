package dev.wizard.meta.module.modules.beta

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.HotbarSlot
import dev.wizard.meta.util.inventory.slot.firstByStack
import dev.wizard.meta.util.math.RotationUtils
import net.minecraft.block.Block
import net.minecraft.block.BlockButton
import net.minecraft.block.BlockPistonBase
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArrow
import net.minecraft.entity.item.EntityExpBottle
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemPiston
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

@CombatManager.CombatModule
object AutoPiston : Module(
    "AutoPiston",
    arrayOf("PistonKick", "PistonPush", "AntiHoleCamper", "HolePusher"),
    Category.BETA,
    "Automatically pushes players out of holes with pistons",
    5000
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.GENERAL))
    private val pushDelay by setting(this, IntegerSetting(settingName("Push Delay"), 0, 0..20, 1, { page == Page.GENERAL }))
    private val pauseOnMove by setting(this, BooleanSetting(settingName("Movement Pause"), true, { page == Page.GENERAL }))
    private val powerPriority by setting(this, EnumSetting(settingName("Power"), PowerMode.BLOCK, { page == Page.GENERAL }))
    private val placeBlock by setting(this, BooleanSetting(settingName("Place Obsidian"), true, { page == Page.GENERAL }))
    private val blockDelay by setting(this, IntegerSetting(settingName("Obsidian Delay"), 500, 0..2000, 50, { page == Page.GENERAL && placeBlock }))
    private val breakRedstone by setting(this, BooleanSetting(settingName("Break Power"), false, { page == Page.GENERAL }))
    private val packetPlace by setting(this, BooleanSetting(settingName("PacketPlace"), true, { page == Page.GENERAL }))
    private val rotate by setting(this, BooleanSetting(settingName("Rotate"), false, { page == Page.GENERAL }))
    private val activateButton by setting(this, BooleanSetting(settingName("Activate Button"), true, { page == Page.GENERAL && powerPriority == PowerMode.BUTTON }))

    private val targetRange by setting(this, DoubleSetting(settingName("Target Range"), 6.0, 0.0..10.0, 0.5, { page == Page.TARGETING }))
    private val yRange by setting(this, DoubleSetting(settingName("Y Range"), 5.0, 0.0..10.0, 0.5, { page == Page.TARGETING }))
    private val lookingTarget by setting(this, BooleanSetting(settingName("Looking Check"), false, { page == Page.TARGETING }))
    private val groundCheck by setting(this, BooleanSetting(settingName("Ground Check"), true, { page == Page.TARGETING }))
    private val entityBox by setting(this, BooleanSetting(settingName("Hitbox Check"), true, { page == Page.TARGETING }))
    private val noFriends by setting(this, BooleanSetting(settingName("Friend Check"), true, { page == Page.TARGETING }))
    private val maxSpeed by setting(this, DoubleSetting(settingName("Max Target Speed"), 10.0, 1.0..30.0, 0.5, { page == Page.TARGETING }))
    private val doubleHoleCheck by setting(this, BooleanSetting(settingName("Hole Check"), false, { page == Page.TARGETING }))
    private val pushCheck by setting(this, BooleanSetting(settingName("Push Check"), false, { page == Page.TARGETING }))

    private val render by setting(this, BooleanSetting(settingName("Render"), true, { page == Page.RENDER }))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render && page == Page.RENDER }))
    private val pistonColor by setting(this, ColorSetting(settingName("Piston Color"), ColorRGB(255, 165, 0), { render && page == Page.RENDER }))
    private val redstoneColor by setting(this, ColorSetting(settingName("Redstone Color"), ColorRGB(255, 0, 0), { render && page == Page.RENDER }))
    private val blockColor by setting(this, ColorSetting(settingName("Block Color"), ColorRGB(64, 64, 64), { render && page == Page.RENDER }))

    private val timer = TickTimer(TimeUnit.MILLISECONDS)
    private val blockTimer = TickTimer(TimeUnit.MILLISECONDS)
    private var currentTarget: EntityPlayer? = null
    private var pistonPos: BlockPos? = null
    private var redstonePos: BlockPos? = null
    private var targetPos: BlockPos? = null
    private var targetDirection: EnumFacing? = null
    private var useBlock = false
    private var useButton = false
    private var lastTargetCoords: IntArray? = null
    private var lastMinedPos: BlockPos? = null
    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = mutableMapOf<BlockPos, Pair<Long, BlockType>>()

    private val horizontalSides = arrayOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
    private val torchOffsets = arrayOf(BlockPos(0, -1, 0), BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))
    private val blockOffsets = arrayOf(BlockPos(0, -1, 0), BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1), BlockPos(0, 1, 0))
    private val buttonOffsets = arrayOf(BlockPos(0, 1, 0), BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))

    init {
        onDisable { reset() }
        onEnable { reset() }

        safeListener<TickEvent.Post> {
            if (player.isDead) {
                disable()
                return@safeListener
            }
            if (!timer.tickAndReset(pushDelay.toLong() * 50L)) return@safeListener
            if (pauseOnMove && MovementUtils.getSpeed(player) > 0.1) return@safeListener

            val pistonSlot = DefinedKt.getHotbarSlots(player).firstByStack { it.item is ItemPiston }
            val redstoneSlot = getRedstoneSlot()
            val obbySlot = DefinedKt.getHotbarSlots(player).firstByStack { it.item == Item.getItemFromBlock(Blocks.OBSIDIAN) }

            if (pistonSlot == null || redstoneSlot == null) return@safeListener
            if (powerPriority != PowerMode.BLOCK && powerPriority != PowerMode.BUTTON && obbySlot == null && placeBlock) return@safeListener

            val stack = redstoneSlot.stack
            useBlock = Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) == stack.item
            useButton = stack.item is ItemBlock && (stack.item as ItemBlock).block is BlockButton

            val target = findTarget() ?: return@safeListener
            currentTarget = target
            targetPos = BlockPos(target.posX, target.posY, target.posZ)
            calculatePositions(EntityUtils.getBetterPosition(target))

            if (lastTargetCoords == null) {
                lastTargetCoords = intArrayOf(target.posX.toInt(), target.posZ.toInt())
            }
            val hasMoved = lastTargetCoords?.let { it[0] != target.posX.toInt() || it[1] != target.posZ.toInt() } ?: false

            if (pistonPos != null && redstonePos != null) {
                val pPos = pistonPos!!
                val rPos = redstonePos!!
                if (abs(pPos.y.toDouble() - player.posY) > yRange) {
                    reset()
                    return@safeListener
                }

                if (rPos == pPos.down()) {
                    placeRedstone(redstoneSlot, obbySlot)
                    placePiston(pistonSlot)
                } else {
                    placePiston(pistonSlot)
                    placeRedstone(redstoneSlot, obbySlot)
                }

                if (useButton && activateButton) {
                    redstonePos?.let { activateButtonAt(it) }
                }

                if (placeBlock && hasMoved && blockTimer.tickAndReset(blockDelay.toLong())) {
                    obbySlot?.let { slot ->
                        HotbarSwitchManager.ghostSwitch(this, HotbarSwitchManager.Override.NONE, slot) {
                            targetPos?.let { placeBlockAt(it) }
                        }
                    }
                }

                if (breakRedstone && redstonePos != null) {
                    val blockAtRedstone = getBlock(rPos)
                    if (blockAtRedstone == Blocks.REDSTONE_BLOCK || blockAtRedstone == Blocks.REDSTONE_TORCH || blockAtRedstone is BlockButton) {
                        breakBlock(rPos)
                    }
                }
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                placedBlocks.forEach { (pos, data) ->
                    val (placeTime, blockType) = data
                    val timeElapsed = System.currentTimeMillis() - placeTime
                    val timeLeft = renderTime - timeElapsed
                    val progress = timeElapsed.toFloat() / renderTime.toFloat()
                    if (timeLeft > 0) {
                        val box = when (renderMode) {
                            RenderMode.FADE -> AxisAlignedBB(pos)
                            RenderMode.GROW -> BoxRenderUtils.calcGrowBox(pos, progress.toDouble())
                            RenderMode.SHRINK -> BoxRenderUtils.calcGrowBox(pos, 1.0 - progress.toDouble())
                            RenderMode.RISE -> BoxRenderUtils.calcRiseBox(pos, progress.toDouble())
                            RenderMode.STATIC -> AxisAlignedBB(pos)
                        }
                        val alpha = when (renderMode) {
                            RenderMode.FADE -> (timeLeft.toFloat() / renderTime.toFloat() * 255f).toInt().coerceIn(0, 255)
                            RenderMode.STATIC -> 255
                            else -> 200
                        }
                        val color = when (blockType) {
                            BlockType.PISTON -> pistonColor
                            BlockType.REDSTONE -> redstoneColor
                            BlockType.OBSIDIAN -> blockColor
                        }
                        renderer.aFilled = if (renderMode == RenderMode.FADE) (alpha * 0.15f).toInt() else 31
                        renderer.aOutline = if (renderMode == RenderMode.FADE) (alpha * 0.9f).toInt() else 233
                        renderer.add(box, color.alpha(alpha))
                    }
                }
                renderer.render(true)
                placedBlocks.entries.removeIf { System.currentTimeMillis() - it.value.first >= renderTime }
            }
        }
    }

    override fun getHudInfo(): String = currentTarget?.name ?: "None"

    private fun reset() {
        currentTarget = null
        pistonPos = null
        redstonePos = null
        targetPos = null
        targetDirection = null
        lastTargetCoords = null
        lastMinedPos = null
        useButton = false
        placedBlocks.clear()
        timer.reset()
        blockTimer.reset()
    }

    private fun SafeClientEvent.getRedstoneSlot(): HotbarSlot? {
        return when (powerPriority) {
            PowerMode.BLOCK -> DefinedKt.getHotbarSlots(player).firstByStack { it.item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) }
            PowerMode.TORCH -> DefinedKt.getHotbarSlots(player).firstByStack { it.item == Item.getItemFromBlock(Blocks.REDSTONE_TORCH) }
            PowerMode.BUTTON -> DefinedKt.getHotbarSlots(player).firstByStack { val item = it.item; item is ItemBlock && item.block is BlockButton }
            PowerMode.ANY -> {
                DefinedKt.getHotbarSlots(player).firstByStack { it.item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) }
                    ?: DefinedKt.getHotbarSlots(player).firstByStack { it.item == Item.getItemFromBlock(Blocks.REDSTONE_TORCH) }
                    ?: DefinedKt.getHotbarSlots(player).firstByStack { val item = it.item; item is ItemBlock && item.block is BlockButton }
            }
        }
    }

    private fun SafeClientEvent.findTarget(): EntityPlayer? {
        val candidates = world.playerEntities.filter { p ->
            p != player && !p.isDead && p.health > 0.0f &&
                player.getDistance(p) <= targetRange &&
                (!groundCheck || p.onGround) &&
                MovementUtils.getSpeed(p) <= maxSpeed &&
                (!noFriends || !FriendManager.isFriend(p.name))
        }

        return if (lookingTarget) {
            candidates.minByOrNull { p ->
                val vec = Vec3d(p.posX, p.posY + p.eyeHeight, p.posZ)
                val rot = RotationUtils.getRotationTo(this, vec)
                abs(rot.x - player.rotationYaw) + abs(rot.y - player.rotationPitch)
            }
        } else {
            candidates.minByOrNull { player.getDistance(it) }
        }
    }

    private fun SafeClientEvent.calculatePositions(pos: BlockPos): Boolean {
        if (entityBox) {
            currentTarget?.let { target ->
                for (side in horizontalSides) {
                    val checkPos = pos.add(side)
                    if (AxisAlignedBB(checkPos).intersects(target.entityBoundingBox)) {
                        if (tryPosition(checkPos)) return true
                        for (side2 in horizontalSides) {
                            val doublePos = checkPos.add(side2)
                            if (AxisAlignedBB(doublePos).intersects(target.entityBoundingBox)) {
                                if (tryPosition(doublePos)) return true
                            }
                        }
                    }
                }
            }
        }
        return tryPosition(pos)
    }

    private fun SafeClientEvent.tryPosition(pos: BlockPos): Boolean {
        if (canPlacePiston(pos.east(), pos.west(), EnumFacing.WEST)) {
            pistonPos = pos.east().up()
            redstonePos = getRedstonePosition(pistonPos!!, pos)
            if (redstonePos != null) {
                targetDirection = EnumFacing.WEST
                return true
            }
        }
        if (canPlacePiston(pos.west(), pos.east(), EnumFacing.EAST)) {
            pistonPos = pos.west().up()
            redstonePos = getRedstonePosition(pistonPos!!, pos)
            if (redstonePos != null) {
                targetDirection = EnumFacing.EAST
                return true
            }
        }
        if (canPlacePiston(pos.north(), pos.south(), EnumFacing.SOUTH)) {
            pistonPos = pos.north().up()
            redstonePos = getRedstonePosition(pistonPos!!, pos)
            if (redstonePos != null) {
                targetDirection = EnumFacing.SOUTH
                return true
            }
        }
        if (canPlacePiston(pos.south(), pos.north(), EnumFacing.NORTH)) {
            pistonPos = pos.south().up()
            redstonePos = getRedstonePosition(pistonPos!!, pos)
            if (redstonePos != null) {
                targetDirection = EnumFacing.NORTH
                return true
            }
        }
        return false
    }

    private fun SafeClientEvent.canPlacePiston(pPos: BlockPos, pushPos: BlockPos, facing: EnumFacing): Boolean {
        val pistonPosUp = pPos.up()
        if (!world.isAirBlock(pistonPosUp)) {
            if (!isPistonFacing(pistonPosUp, facing)) return false
        }
        if (intersectsWithEntity(pistonPosUp)) return false
        if (doubleHoleCheck && world.isAirBlock(pushPos)) return false
        return !pushCheck || (world.isAirBlock(pushPos.up()) && world.isAirBlock(pushPos.up(2)))
    }

    private fun SafeClientEvent.isPistonFacing(pos: BlockPos, facing: EnumFacing): Boolean {
        val state = world.getBlockState(pos)
        val block = state.block
        if (block !is BlockPistonBase) return false
        return try {
            state.getValue(BlockPistonBase.FACING) == facing
        } catch (e: Exception) {
            false
        }
    }

    private fun SafeClientEvent.getRedstonePosition(pPos: BlockPos, tPos: BlockPos): BlockPos? {
        val offsets = if (useButton) buttonOffsets else if (useBlock) blockOffsets else torchOffsets
        for (offset in offsets) {
            val pos = pPos.add(offset)
            if (intersectsWithEntity(pos) || pos == tPos) continue
            val block = getBlock(pos)
            if (!world.isAirBlock(pos) && block != Blocks.REDSTONE_BLOCK && block != Blocks.REDSTONE_TORCH && block !is BlockButton) continue
            if (useButton) {
                val hasAdjacentSolid = EnumFacing.values().any { facing ->
                    val neighbor = pos.offset(facing)
                    val neighborState = world.getBlockState(neighbor)
                    neighborState.isSideSolid(world, neighbor, facing.opposite) && neighbor != pPos
                }
                if (!hasAdjacentSolid) continue
            }
            return pos
        }
        return null
    }

    private fun SafeClientEvent.placePiston(pistonSlot: HotbarSlot) {
        val pos = pistonPos ?: return
        if (!useBlock && !useButton) {
            val rPos = redstonePos!!
            if (getBlock(rPos.down()) == Blocks.AIR) {
                val obbySlot = DefinedKt.getAllSlotsPrioritized(player).firstByStack { it.item == Item.getItemFromBlock(Blocks.OBSIDIAN) }
                obbySlot?.let { slot ->
                    HotbarSwitchManager.ghostSwitch(this, HotbarSwitchManager.Override.NONE, slot) {
                        placeBlockAt(rPos.down())
                        if (render) placedBlocks[rPos.down()] = System.currentTimeMillis() to BlockType.OBSIDIAN
                    }
                }
            }
        }
        targetDirection?.let { facing ->
            val yaw = when (facing) {
                EnumFacing.WEST -> -90.0f
                EnumFacing.EAST -> 90.0f
                EnumFacing.SOUTH -> 0.0f
                EnumFacing.NORTH -> 180.0f
                else -> player.rotationYaw
            }
            sendRotation(yaw, 0.0f)
        }
        HotbarSwitchManager.ghostSwitch(this, HotbarSwitchManager.Override.NONE, pistonSlot) {
            placeBlockAt(pos)
            if (render) placedBlocks[pos] = System.currentTimeMillis() to BlockType.PISTON
        }
    }

    private fun SafeClientEvent.placeRedstone(redstoneSlot: HotbarSlot, obbySlot: HotbarSlot?) {
        val pos = redstonePos ?: return
        HotbarSwitchManager.ghostSwitch(this, HotbarSwitchManager.Override.NONE, redstoneSlot) {
            placeBlockAt(pos)
            if (render) placedBlocks[pos] = System.currentTimeMillis() to BlockType.REDSTONE
        }
    }

    private fun SafeClientEvent.activateButtonAt(pos: BlockPos) {
        if (getBlock(pos) !is BlockButton) return
        for (facing in EnumFacing.values()) {
            val neighbor = pos.offset(facing)
            val neighborState = world.getBlockState(neighbor)
            if (neighborState.isSideSolid(world, neighbor, facing.opposite)) {
                val hitVec = Vec3d(neighbor).addVector(0.5, 0.5, 0.5).add(Vec3d(facing.opposite.directionVec).scale(0.5))
                if (rotate) {
                    PlayerPacketManager.sendPlayerPacket {
                        rotate(RotationUtils.getRotationTo(this@activateButtonAt, hitVec))
                    }
                }
                player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, facing.opposite, EnumHand.MAIN_HAND,
                    (hitVec.x - pos.x).toFloat(), (hitVec.y - pos.y).toFloat(), (hitVec.z - pos.z).toFloat()))
                break
            }
        }
    }

    private fun SafeClientEvent.placeBlockAt(pos: BlockPos) {
        val side = getPlaceSide(pos) ?: return
        val neighbour = pos.offset(side)
        val opposite = side.opposite
        val hitVec = Vec3d(neighbour).addVector(0.5, 0.5, 0.5).add(Vec3d(opposite.directionVec).scale(0.5))
        if (rotate) {
            PlayerPacketManager.sendPlayerPacket {
                rotate(RotationUtils.getRotationTo(this@placeBlockAt, hitVec))
            }
        }
        if (packetPlace) {
            player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(neighbour, opposite, EnumHand.MAIN_HAND,
                (hitVec.x - neighbour.x).toFloat(), (hitVec.y - neighbour.y).toFloat(), (hitVec.z - neighbour.z).toFloat()))
        } else {
            mc.playerController.processRightClickBlock(player, world, neighbour, opposite, hitVec, EnumHand.MAIN_HAND)
        }
    }

    private fun SafeClientEvent.breakBlock(pos: BlockPos) {
        if (pos == lastMinedPos) return
        PacketMine.mineBlock(INSTANCE, pos, modulePriority)
        lastMinedPos = pos
    }

    private fun SafeClientEvent.getPlaceSide(pos: BlockPos): EnumFacing? {
        return EnumFacing.values().firstOrNull { side ->
            val neighbour = pos.offset(side)
            val state = world.getBlockState(neighbour)
            state.block.canCollideCheck(state, false) && !state.material.isReplaceable
        }
    }

    private fun SafeClientEvent.intersectsWithEntity(pos: BlockPos): Boolean {
        return world.getEntitiesWithinAABB(Entity::class.java, AxisAlignedBB(pos)).any { entity ->
            !entity.isDead && entity !is EntityItem && entity !is EntityXPOrb && entity !is EntityExpBottle && entity !is EntityArrow
        }
    }

    private fun SafeClientEvent.getBlock(pos: BlockPos): Block = world.getBlockState(pos).block

    private fun SafeClientEvent.sendRotation(yaw: Float, pitch: Float) {
        player.connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, player.onGround))
    }

    private enum class BlockType { PISTON, REDSTONE, OBSIDIAN }
    private enum class Page(override val displayName: CharSequence) : DisplayEnum { GENERAL("General"), TARGETING("Targeting"), RENDER("Render") }
    private enum class PowerMode(override val displayName: CharSequence) : DisplayEnum { BLOCK("Block"), TORCH("Torch"), BUTTON("Button"), ANY("Any") }
    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
}
