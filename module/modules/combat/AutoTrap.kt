package dev.wizard.meta.module.modules.combat

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.HotbarSlot
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.CoroutineUtilsKt
import dev.wizard.meta.util.world.CheckKt
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import dev.wizard.meta.util.world.PlacementSearchOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.*

object AutoTrap : Module(
    "AutoTrap",
    category = Category.COMBAT,
    description = "Automatically traps enemies",
    modulePriority = 300
) {
    private val targetMode by setting(this, EnumSetting(settingName("Target Mode"), TargetMode.SINGLE))
    private val mode by setting(this, EnumSetting(settingName("Trap Mode"), TrapMode.TRAP))
    private val range by setting(this, FloatSetting(settingName("Target Range"), 5.0f, 1.0f..8.0f, 0.5f))
    private val multiPlace by setting(this, IntegerSetting(settingName("Multi Place"), 1, 1..12, 1))
    private val placeRange by setting(this, FloatSetting(settingName("Place Range"), 5.2f, 1.0f..6.0f, 0.25f))
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 50, 0..300, 10))
    private val maxTargetSpeed by setting(this, FloatSetting(settingName("Max Target Speed"), 4.0f, 1.0f..30.0f, 1.0f))
    private val selfGround by setting(this, BooleanSetting(settingName("Self Ground"), true))
    private val maxSelfSpeed by setting(this, FloatSetting(settingName("Max Self Speed"), 6.0f, 1.0f..30.0f, 1.0f))
    private val antiStep by setting(this, BooleanSetting(settingName("AntiStep"), false, { mode == TrapMode.TRAP }))
    private val extend by setting(this, BooleanSetting(settingName("Extend"), true, { mode == TrapMode.TRAP }))
    private val head by setting(this, BooleanSetting(settingName("Trap Head"), true, { mode == TrapMode.TRAP }))
    private val chest by setting(this, BooleanSetting(settingName("Trap Chest"), true, { mode == TrapMode.TRAP }))
    private val legs by setting(this, BooleanSetting(settingName("Trap Legs"), false, { mode == TrapMode.TRAP }))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 0, 0), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))
    private val autoDisable by setting(this, BooleanSetting(settingName("Auto Disable"), true))

    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val placedBlocks = linkedMapOf<BlockPos, Long>()
    private var job: Job? = null
    private var target: EntityPlayer? = null
    private var lastPlaceTime = 0L
    private var progress = 0

    init {
        onDisable {
            placedBlocks.clear()
            target = null
            job = null
        }

        safeListener<TickEvent.Post> {
            if (!canRun(this)) {
                target = null
                if (autoDisable) disable()
                return@safeListener
            }

            if (System.currentTimeMillis() - lastPlaceTime < delay) return@safeListener

            if (!CoroutineUtilsKt.isActiveOrFalse(job)) {
                findTarget(this)
                if (target != null) {
                    job = runTrap(this)
                }
            }

            if (CoroutineUtilsKt.isActiveOrFalse(job) && AntiCheat.blockPlaceRotation) {
                PlayerPacketManager.sendPlayerPacket { cancelAll() }
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                renderer.clear()
                placedBlocks.forEach { pos, timestamp ->
                    val age = System.currentTimeMillis() - timestamp
                    if (age < renderTime) {
                        val progress = age.toFloat() / renderTime.toFloat()
                        val box = when (renderMode) {
                            RenderMode.FADE -> AxisAlignedBB(pos)
                            RenderMode.GROW -> BoxRenderUtils.calcGrowBox(pos, progress.toDouble())
                            RenderMode.SHRINK -> BoxRenderUtils.calcGrowBox(pos, 1.0 - progress.toDouble())
                            RenderMode.RISE -> BoxRenderUtils.calcRiseBox(pos, progress.toDouble())
                            RenderMode.STATIC -> AxisAlignedBB(pos)
                        }
                        val alpha = if (renderMode == RenderMode.FADE) (255 * (1.0f - progress)).toInt().coerceIn(0, 255) else if (renderMode == RenderMode.STATIC) 255 else 200
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

    private fun canRun(event: SafeClientEvent): Boolean {
        if (selfGround && !event.player.onGround) return false
        val tps = 1000.0 / TimerManager.tickLength
        val speed = MovementUtils.getRealSpeed(event.player) * tps
        return speed <= maxSelfSpeed && CombatManager.isOnTopPriority(this)
    }

    private fun findTarget(event: SafeClientEvent) {
        val candidates = event.world.playerEntities.filter { isValidTarget(event, it) }
        target = when (targetMode) {
            TargetMode.SINGLE -> candidates.minByOrNull { event.player.getDistance(it) }
            TargetMode.MULTI -> candidates.firstOrNull()
        }
    }

    private fun isValidTarget(event: SafeClientEvent, entity: EntityPlayer): Boolean {
        if (entity == event.player || entity.isDead || entity.health <= 0.0f || event.player.getDistance(entity) > range || EntityUtils.isFriend(entity)) return false
        val tps = 1000.0 / TimerManager.tickLength
        val speed = MovementUtils.getRealSpeed(entity) * tps
        return speed <= maxTargetSpeed
    }

    private fun runTrap(event: SafeClientEvent): Job {
        return ConcurrentScope.launch {
            progress = 0
            val currentTarget = target ?: return@launch
            val pos = EntityUtils.getFlooredPosition(currentTarget)

            when (mode) {
                TrapMode.TRAP -> doTrap(event, pos)
                TrapMode.PISTON -> doPiston(event, pos.up())
                TrapMode.AUTO -> doAuto(event, pos)
            }

            if (targetMode == TargetMode.MULTI) findTarget(event)
        }
    }

    private fun doTrap(event: SafeClientEvent, pos: BlockPos) {
        if (antiStep && event.world.isAirBlock(pos.up(3))) {
            placeBlock(event, pos.up(3))
        }
        if (extend) {
            val offsets = listOf(pos.east(), pos.west(), pos.south(), pos.north())
            for (offsetPos in offsets) {
                if (checkEntity(event, offsetPos) != null) {
                    placeBlock(event, offsetPos.up(2))
                }
            }
        }
        var trapChest = false
        if (head && event.world.isAirBlock(pos.up(2))) {
            if (!placeBlock(event, pos.up(2))) trapChest = true
        }
        if (chest || trapChest) {
            for (facing in EnumFacing.HORIZONTALS) {
                val offsetPos = pos.offset(facing).up()
                placeBlock(event, offsetPos)
                if (event.world.isAirBlock(pos.up(2))) placeBlock(event, offsetPos.up())
                if (event.world.isAirBlock(offsetPos)) placeBlock(event, offsetPos.down())
            }
        }
        if (legs) {
            for (facing in EnumFacing.HORIZONTALS) {
                val offsetPos = pos.offset(facing)
                if (event.world.isAirBlock(offsetPos.up())) {
                    placeBlock(event, offsetPos)
                    if (event.world.isAirBlock(offsetPos)) placeBlock(event, offsetPos.down())
                }
            }
        }
    }

    private fun doPiston(event: SafeClientEvent, pos: BlockPos) {
        for (facing in EnumFacing.HORIZONTALS) {
            val pistonPos = pos.offset(facing)
            val block = event.world.getBlockState(pistonPos).block
            if (block != Blocks.PISTON && block != Blocks.STICKY_PISTON) {
                placeBlock(event, pistonPos)
            }
        }
    }

    private fun doAuto(event: SafeClientEvent, pos: BlockPos) {
        val hasPiston = DefinedKt.getHotbarSlots(event.player).any { it.stack.item == Item.getItemFromBlock(Blocks.PISTON) || it.stack.item == Item.getItemFromBlock(Blocks.STICKY_PISTON) }
        if (!hasPiston) {
            doTrap(event, pos)
            return
        }
        val headPos = pos.up()
        if (EnumFacing.HORIZONTALS.any { val p = headPos.offset(it); event.world.getBlockState(p).block == Blocks.PISTON || event.world.getBlockState(p).block == Blocks.STICKY_PISTON }) return

        for (facing in EnumFacing.HORIZONTALS) {
            val pistonPos = headPos.offset(facing)
            if (placeBlock(event, pistonPos)) return
        }
        doTrap(event, pos)
    }

    private fun placeBlock(event: SafeClientEvent, pos: BlockPos): Boolean {
        if (progress >= multiPlace || !canPlace(event, pos) || event.player.getDistanceSqToCenter(pos) > placeRange * placeRange) return false

        val slot = getObbySlot(event) ?: run {
            NoSpamMessage.sendMessage("${getChatName()} No obsidian in hotbar, disabling!")
            disable()
            return false
        }

        val info = InteractKt.getPlacement(event, pos, PlacementSearchOption.range(placeRange.toFloat()), PlacementSearchOption.ENTITY_COLLISION) ?: return false
        val packet = InteractKt.toPlacePacket(info, EnumHand.MAIN_HAND)

        if (!event.player.isSneaking) {
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.START_SNEAKING))
            HotbarSwitchManager.ghostSwitch(event, slot) { event.connection.sendPacket(packet) }
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.STOP_SNEAKING))
        } else {
            HotbarSwitchManager.ghostSwitch(event, slot) { event.connection.sendPacket(packet) }
        }
        event.player.swingArm(EnumHand.MAIN_HAND)
        placedBlocks[pos] = System.currentTimeMillis()
        lastPlaceTime = System.currentTimeMillis()
        progress++
        return true
    }

    private fun getObbySlot(event: SafeClientEvent): HotbarSlot? = IterableKt.firstBlock(DefinedKt.getHotbarSlots(event.player), Blocks.OBSIDIAN)

    private fun canPlace(event: SafeClientEvent, pos: BlockPos): Boolean = CheckKt.isPlaceable(event.world, pos) && EntityManager.checkNoEntityCollision(AxisAlignedBB(pos))

    private fun checkEntity(event: SafeClientEvent, pos: BlockPos): EntityPlayer? = event.world.getEntitiesWithinAABB(EntityPlayer::class.java, AxisAlignedBB(pos)).firstOrNull { it != event.player }

    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
    private enum class TargetMode { SINGLE, MULTI }
    private enum class TrapMode { TRAP, PISTON, AUTO }
}
