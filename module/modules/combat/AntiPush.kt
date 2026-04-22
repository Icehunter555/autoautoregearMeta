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
import dev.wizard.meta.util.world.PlacementSearchOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.block.BlockDirectional
import net.minecraft.block.BlockPistonBase
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.*

object AntiPush : Module(
    "AntiPush",
    category = Category.COMBAT,
    description = "Protects against piston push",
    modulePriority = 90
) {
    private val maxSelfSpeed by setting(this, FloatSetting(settingName("Max Self Speed"), 6.0f, 1.0f..30.0f, 1.0f))
    private val helper by setting(this, BooleanSetting(settingName("Helper"), true))
    private val trap by setting(this, BooleanSetting(settingName("Trap"), true))
    private val onlyBurrow by setting(this, BooleanSetting(settingName("Only Burrow"), false, { trap }))
    private val whenDouble by setting(this, BooleanSetting(settingName("When Double"), false, { onlyBurrow }))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 0, 0), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = linkedMapOf<BlockPos, Long>()
    private var job: Job? = null
    private val speedList = ArrayDeque<Double>()
    private var applyTimer = true
    private const val AVERAGESPEEDTIME = 5

    init {
        onDisable {
            placedBlocks.clear()
            speedList.clear()
        }

        safeListener<TickEvent.Post> {
            updateSpeedList()
            if (!CoroutineUtilsKt.isActiveOrFalse(job) && canRun()) {
                job = runProtection()
            }
            if (CoroutineUtilsKt.isActiveOrFalse(job) && AntiCheat.blockPlaceRotation) {
                PlayerPacketManager.sendPlayerPacket {
                    cancelAll()
                }
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

    override fun isActive(): Boolean = isEnabled && CoroutineUtilsKt.isActiveOrFalse(job)

    override fun getHudInfo(): String {
        return placedBlocks.entries.count { System.currentTimeMillis() - it.value <= 3000L }.toString()
    }

    private fun SafeClientEvent.updateSpeedList() {
        val tps = if (applyTimer) 1000.0 / TimerManager.tickLength else 20.0
        val speed = MovementUtils.getRealSpeed(player) * tps
        if (speed > 0.0 || player.ticksExisted % 4 == 0) {
            speedList.add(speed)
        } else {
            speedList.pollFirst()
        }
        while (speedList.size > 5) {
            speedList.pollFirst()
        }
    }

    private fun getCurrentSpeed(): Double = if (speedList.isEmpty()) 0.0 else speedList.average()

    private fun SafeClientEvent.canRun(): Boolean = player.onGround && CombatManager.isOnTopPriority(this@AntiPush) && getCurrentSpeed() <= maxSelfSpeed

    private fun SafeClientEvent.runProtection(): Job {
        return ConcurrentScope.launch {
            val pos = EntityUtils.getFlooredPosition(player)
            if (world.getBlockState(pos.up(2)).block == Blocks.OBSIDIAN || world.getBlockState(pos.up(2)).block == Blocks.BEDROCK) {
                return@launch
            }

            var doubleCount = 0
            if (whenDouble) {
                for (facing in EnumFacing.HORIZONTALS) {
                    val pistonPos = pos.offset(facing).up()
                    val blockState = world.getBlockState(pistonPos)
                    if (blockState.block is BlockPistonBase && blockState.getValue(BlockDirectional.FACING).opposite == facing) {
                        doubleCount++
                    }
                }
            }

            for (facing in EnumFacing.HORIZONTALS) {
                val pistonPos = pos.offset(facing).up()
                val blockState = world.getBlockState(pistonPos)
                if (blockState.block is BlockPistonBase && blockState.getValue(BlockDirectional.FACING).opposite == facing) {
                    val placePos = pos.up().offset(facing.opposite)
                    placeBlock(placePos)
                    placedBlocks[placePos] = System.currentTimeMillis()

                    if (trap && (world.isAirBlock(pos) || !onlyBurrow || doubleCount >= 2)) {
                        val trapPos = pos.up(2)
                        placeBlock(trapPos)
                        placedBlocks[trapPos] = System.currentTimeMillis()

                        if (!CheckKt.isPlaceable(world, trapPos)) {
                            for (f in EnumFacing.VALUES) {
                                val altPos = pos.offset(f).up(2)
                                if (canPlace(altPos)) {
                                    placeBlock(altPos)
                                    placedBlocks[altPos] = System.currentTimeMillis()
                                    break
                                }
                            }
                        }
                    }

                    if (!CheckKt.isPlaceable(world, placePos) && helper) {
                        val helperPos1 = pos.offset(facing.opposite)
                        if (CheckKt.isPlaceable(world, helperPos1)) {
                            placeBlock(helperPos1)
                            placedBlocks[helperPos1] = System.currentTimeMillis()
                        } else {
                            val helperPos2 = pos.offset(facing.opposite).down()
                            placeBlock(helperPos2)
                            placedBlocks[helperPos2] = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.placeBlock(pos: BlockPos) {
        if (!canPlace(pos)) return
        val slot = getObbySlot() ?: run {
            NoSpamMessage.sendMessage("${getChatName()} No obsidian in hotbar, disabling!")
            disable()
            return
        }
        val info = InteractKt.getPlacement(this, pos, PlacementSearchOption.range(4.25f), PlacementSearchOption.ENTITY_COLLISION) ?: return
        val packet = InteractKt.toPlacePacket(info, EnumHand.MAIN_HAND)

        if (!player.isSneaking) {
            connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
            HotbarSwitchManager.ghostSwitch(this, slot) {
                connection.sendPacket(packet)
            }
            connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
        } else {
            HotbarSwitchManager.ghostSwitch(this, slot) {
                connection.sendPacket(packet)
            }
        }
        player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun SafeClientEvent.getObbySlot(): HotbarSlot? = IterableKt.firstBlock(DefinedKt.getHotbarSlots(player), Blocks.OBSIDIAN)

    private fun SafeClientEvent.canPlace(pos: BlockPos): Boolean = CheckKt.isPlaceable(world, pos) && EntityManager.checkNoEntityCollision(AxisAlignedBB(pos))

    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
}
