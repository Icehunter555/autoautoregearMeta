package dev.wizard.meta.module.modules.beta

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.toVec3d
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.world.BlockKt
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import dev.wizard.meta.util.world.PlacementSearchOption
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.block.BlockAnvil
import net.minecraft.block.BlockPistonBase
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

@CombatManager.CombatModule
object HoleProtect : Module(
    "HoleProtect",
    category = Category.BETA,
    description = "protect ur hole from rapists",
    modulePriority = 150
) {
    private val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.DEFAULT))
    private val timeMode by setting(this, EnumSetting(settingName("Time Mode"), TimeMode.TICK))
    private val rotate by setting(this, BooleanSetting(settingName("Rotate"), true))
    private val strictDirection by setting(this, BooleanSetting(settingName("Strict Direction"), false))
    private val anvilBlocker by setting(this, BooleanSetting(settingName("Anvil Blocker"), true))
    private val antiPistonCrystal by setting(this, BooleanSetting(settingName("Anti Piston Crystal"), true))
    private val blockPiston by setting(this, BooleanSetting(settingName("Block Pistons"), true))
    private val antiFacePlace by setting(this, BooleanSetting(settingName("AntiFace Place"), true))
    private val blocksPerTick by setting(this, IntegerSetting(settingName("Blocks Per Tick"), 4, 1..10, 1))
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 25, 0..500, 5))
    private val blockType by setting(this, EnumSetting(settingName("Block Type"), BlockType.STRING))
    private val range by setting(this, DoubleSetting(settingName("Range"), 5.0, 0.0..10.0, 0.5))
    private val yRange by setting(this, DoubleSetting(settingName("Y Range"), 5.0, 0.0..10.0, 0.5))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 0, 0), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = LinkedHashMap<BlockPos, Long>()
    private val placeTimer = TickTimer()
    private val pistonList = LongSets.synchronize(LongOpenHashSet())
    private var activatedBefore = false

    init {
        onDisable {
            pistonList.clear()
            activatedBefore = false
            PacketMine.reset(this)
            placedBlocks.clear()
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (timeMode == TimeMode.TICK || timeMode == TimeMode.BOTH) {
                runBlocker()
            }
        }

        safeListener<RunGameLoopEvent.Tick>(alwaysListening = true) {
            if (timeMode == TimeMode.FAST || timeMode == TimeMode.BOTH) {
                runBlocker()
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                renderer.clear()
                placedBlocks.forEach { pos, timestamp ->
                    val timeElapsed = System.currentTimeMillis() - timestamp
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
                        renderer.aFilled = if (renderMode == RenderMode.FADE) (alpha * 0.15f).toInt() else 31
                        renderer.aOutline = if (renderMode == RenderMode.FADE) (alpha * 0.9f).toInt() else 233
                        renderer.add(box, renderColor.alpha(alpha))
                    }
                }
                renderer.render(true)
                placedBlocks.entries.removeIf { System.currentTimeMillis() - it.value >= renderTime }
            }
        }
    }

    override fun getHudInfo(): String {
        return placedBlocks.entries.count { System.currentTimeMillis() - it.value <= 3000L }.toString()
    }

    override fun isActive(): Boolean = isEnabled

    private fun SafeClientEvent.runBlocker() {
        if (!placeTimer.tickAndReset(placeDelay.toLong())) return
        if (anvilBlocker) blockAnvils()
        if (antiPistonCrystal) breakPistonCrystals()
        if (blockPiston) blockPistons()
        if (antiFacePlace && mc.gameSettings.keyBindSneak.isKeyDown) placeAntiFacePlace()
    }

    private fun SafeClientEvent.blockAnvils() {
        val playerPos = EntityUtils.getBetterPosition(player)
        var found = false
        for (entity in EntityManager.entity) {
            if (entity is EntityFallingBlock && entity.block?.block is BlockAnvil) {
                if (entity.posX.toInt() == playerPos.x && entity.posZ.toInt() == playerPos.z) {
                    val protectPos = playerPos.up(2)
                    if (world.getBlockState(protectPos).block == Blocks.AIR) {
                        placeObsidian(protectPos)
                        placedBlocks[protectPos] = System.currentTimeMillis()
                        found = true
                        break
                    }
                }
            }
        }
        activatedBefore = found
    }

    private fun SafeClientEvent.breakPistonCrystals() {
        val playerPos = EntityUtils.getBetterPosition(player)
        for (entity in EntityManager.entity) {
            if (entity is EntityEnderCrystal && entity.distanceSqTo(player) <= range * range) {
                val crystalX = entity.posX
                val crystalZ = entity.posZ
                val crystalY = entity.posY
                if (crystalX >= playerPos.x - 1.5 && crystalX <= playerPos.x + 1.5 &&
                    crystalZ >= playerPos.z - 1.5 && crystalZ <= playerPos.z + 1.5) {
                    for (i in -2..2) {
                        for (j in -2..2) {
                            if (i != 0 && j != 0) continue
                            val checkPos = BlockPos(crystalX + i, crystalY, crystalZ + j)
                            if (world.getBlockState(checkPos).block is BlockPistonBase) {
                                breakCrystal(entity)
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.blockPistons() {
        val playerPos = EntityUtils.getBetterPosition(player)
        val slot = IterableKt.firstBlock(DefinedKt.getAllSlotsPrioritized(player), Blocks.OBSIDIAN) ?: return

        getSphere(playerPos, range, yRange).forEach { pos ->
            if (world.getBlockState(pos).block is BlockPistonBase && !pistonList.contains(pos.toLong())) {
                pistonList.add(pos.toLong())
            }
        }

        pistonList.removeIf { player.distanceSqTo(BlockPos.fromLong(it).toVec3d()) > range * range }

        var placed = 0
        for (l in pistonList) {
            if (placed >= blocksPerTick) return
            val pos = BlockPos.fromLong(l)
            if (world.getBlockState(pos).block == Blocks.OBSIDIAN) continue
            if (!BlockKt.isReplaceable(world.getBlockState(pos))) continue

            val options = arrayOf(PlacementSearchOption.range(6.0), if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null).filterNotNull().toTypedArray()
            val sequence = InteractKt.getPlacementSequence(this, pos, 2, *options)
            val placeInfo = sequence?.firstOrNull() ?: continue

            if (!checkRotation(placeInfo)) continue

            HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, slot) {
                connection.sendPacket(InteractKt.toPlacePacket(placeInfo, EnumHand.MAIN_HAND))
            }
            placedBlocks[placeInfo.placedPos] = System.currentTimeMillis()
            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            placed++
        }

        pistonList.removeIf { world.getBlockState(BlockPos.fromLong(it)).block == Blocks.OBSIDIAN }
    }

    private fun SafeClientEvent.placeAntiFacePlace() {
        val playerPos = EntityUtils.getBetterPosition(player)
        var blocksPlaced = 0
        val offsets = arrayOf(BlockPos(1, 0, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 0, -1))

        val slot = when (blockType) {
            BlockType.PRESSURE -> IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(player), Item.getItemFromBlock(Blocks.WOODEN_PRESSURE_PLATE))
            BlockType.STRING -> IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(player), Items.STRING)
        } ?: return

        for (offset in offsets) {
            if (blocksPlaced >= blocksPerTick) return
            val basePos = playerPos.add(offset)
            val baseState = world.getBlockState(basePos)
            if (baseState.block != Blocks.OBSIDIAN && baseState.block != Blocks.BEDROCK) continue

            val placePos = basePos.up()
            if (!BlockKt.isReplaceable(world.getBlockState(placePos))) continue
            if (!checkColliding(placePos)) continue

            val options = arrayOf(PlacementSearchOption.range(6.0), if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null).filterNotNull().toTypedArray()
            val sequence = InteractKt.getPlacementSequence(this, placePos, 2, *options)
            val placeInfo = sequence?.firstOrNull() ?: continue

            if (!checkRotation(placeInfo)) continue

            HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, slot) {
                connection.sendPacket(InteractKt.toPlacePacket(placeInfo, EnumHand.MAIN_HAND))
            }
            placedBlocks[placeInfo.placedPos] = System.currentTimeMillis()
            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            blocksPlaced++
        }
    }

    private fun SafeClientEvent.placeObsidian(pos: BlockPos) {
        if (!BlockKt.isReplaceable(world.getBlockState(pos))) return
        val slot = IterableKt.firstBlock(DefinedKt.getAllSlotsPrioritized(player), Blocks.OBSIDIAN) ?: run {
            NoSpamMessage.sendMessage("${getChatName()} No obsidian in inventory!")
            return
        }

        val options = arrayOf(PlacementSearchOption.range(6.0), if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null).filterNotNull().toTypedArray()
        val sequence = InteractKt.getPlacementSequence(this, pos, 2, *options)
        val placeInfo = sequence?.firstOrNull() ?: return

        if (!checkRotation(placeInfo)) return

        val sneak = !player.isSneaking
        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

        HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, slot) {
            connection.sendPacket(InteractKt.toPlacePacket(placeInfo, EnumHand.MAIN_HAND))
        }
        placedBlocks[placeInfo.placedPos] = System.currentTimeMillis()
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
    }

    private fun SafeClientEvent.breakCrystal(crystal: Entity) {
        if (rotate) {
            PlayerPacketManager.sendPlayerPacket {
                var eyeHeight = player.eyeHeight
                if (!player.isSneaking) eyeHeight -= 0.08f
                val vec1 = Vec3d(player.posX, player.posY + eyeHeight, player.posZ)
                val vec2 = crystal.positionVector
                rotate(RotationUtils.getRotationTo(vec1, vec2))
            }
        }
        connection.sendPacket(CPacketUseEntity(crystal))
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
    }

    private fun SafeClientEvent.checkRotation(placeInfo: PlaceInfo): Boolean {
        return !AntiCheat.blockPlaceRotation || InteractKt.checkPlaceRotation(this, placeInfo)
    }

    private fun checkColliding(pos: BlockPos): Boolean {
        val box = AxisAlignedBB(pos)
        return EntityManager.entity.none { it.isEntityAlive && it.canBeCollidedWith() && it.entityBoundingBox.intersects(box) }
    }

    private fun getSphere(center: BlockPos, radius: Double, yRadius: Double): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()
        val radiusSq = radius * radius
        val yRadiusSq = yRadius * yRadius
        val r = radius.toInt()
        val yr = yRadius.toInt()
        for (x in -r..r) {
            for (y in -yr..yr) {
                for (z in -r..r) {
                    val distSq = (x * x + z * z).toDouble()
                    val yDistSq = (y * y).toDouble()
                    if (distSq <= radiusSq && yDistSq <= yRadiusSq) {
                        positions.add(center.add(x, y, z))
                    }
                }
            }
        }
        return positions
    }

    private enum class BlockType(override val displayName: CharSequence) : DisplayEnum { PRESSURE("Pressure Plate"), STRING("String") }
    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
    private enum class TimeMode(override val displayName: CharSequence) : DisplayEnum { TICK("Tick"), FAST("Fast"), BOTH("Both") }
}
