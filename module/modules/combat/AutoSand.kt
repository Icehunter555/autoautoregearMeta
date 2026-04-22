package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.gui.hudgui.elements.hud.Notification
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import dev.wizard.meta.util.world.PlacementSearchOption
import net.minecraft.block.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import kotlin.concurrent.thread

object AutoSand : Module(
    "AutoSand",
    category = Category.COMBAT,
    description = "sand on enemy head",
    modulePriority = 100
) {
    private val autoDisable by setting(this, BooleanSetting(settingName("Auto Disable"), false))
    private val requireTarget by setting(this, BooleanSetting(settingName("Require Target"), true, { !autoDisable }))
    private val delayMs by setting(this, IntegerSetting(settingName("Action Delay (ms)"), 75, 25..400, 25))
    private val maxRange by setting(this, DoubleSetting(settingName("Search Range"), 5.0, 3.0..7.0, 0.5))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 0, 0), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = linkedMapOf<BlockPos, Long>()
    private var running = false
    private var worker: Thread? = null
    private var sandPos: BlockPos? = null
    private var lastSandPos: BlockPos? = null
    private var lastNotifyAt = 0L

    init {
        onEnable {
            if (running) return@onEnable
            running = true
            worker = thread(name = "AutoSand-Worker", isDaemon = true) {
                while (running) {
                    try {
                        runSafe {
                            val target = CombatManager.target
                            if (target == null) {
                                if (requireTarget) notifyOncePer(1, "No target found")
                                return@runSafe
                            }

                            val feet = EntityUtils.getBetterPosition(target)
                            val head = feet.up()
                            val top = head.up()
                            sandPos = top

                            val feetBlock = world.getBlockState(feet).block
                            if (feetBlock is BlockBed || feetBlock is BlockButton || feetBlock is BlockLever || feetBlock is BlockTorch || feetBlock is BlockRail || feetBlock is BlockSkull) {
                                notifyOncePer(1, "No valid placement found")
                                return@runSafe
                            }

                            val headBlock = world.getBlockState(head).block
                            if (headBlock is BlockButton || headBlock is BlockLever || headBlock is BlockTorch || headBlock is BlockRail || headBlock is BlockSkull) {
                                notifyOncePer(1, "No valid placement found")
                                return@runSafe
                            }

                            val obbySlot = IterableKt.firstBlock(DefinedKt.getAllSlotsPrioritized(player), Blocks.OBSIDIAN)
                            if (obbySlot == null) {
                                notifyOncePer(2, "No obsidian found")
                                return@runSafe
                            }

                            val sandSlot = IterableKt.firstByStack(DefinedKt.getAllSlotsPrioritized(player)) {
                                val item = it.item
                                item is net.minecraft.item.ItemBlock && item.block is BlockFalling
                            }
                            if (sandSlot == null) {
                                notifyOncePer(2, "No sand or concrete powder found")
                                return@runSafe
                            }

                            val scaffoldSeq = InteractKt.getPlacementSequence(this, top, 5, PlacementSearchOption.range(maxRange.toFloat()), PlacementSearchOption.ENTITY_COLLISION)
                            if (scaffoldSeq != null) {
                                for (info in scaffoldSeq) {
                                    val isFinal = info.placedPos == top
                                    val slot = if (isFinal) sandSlot else obbySlot
                                    HotbarSwitchManager.ghostSwitch(this, slot) {
                                        InteractKt.placeBlock(it, info)
                                    }
                                    placedBlocks[info.placedPos] = System.currentTimeMillis()
                                    Thread.sleep(delayMs.toLong().coerceAtLeast(10L))
                                }

                                val finalSeq = InteractKt.getPlacementSequence(this, top, 3, PlacementSearchOption.range(maxRange.toFloat()))
                                if (finalSeq != null) {
                                    for (info in finalSeq) {
                                        val isFinal = info.placedPos == top
                                        val slot = if (isFinal) sandSlot else obbySlot
                                        HotbarSwitchManager.ghostSwitch(this, slot) {
                                            InteractKt.placeBlock(it, info)
                                        }
                                        placedBlocks[info.placedPos] = System.currentTimeMillis()
                                        Thread.sleep(delayMs.toLong().coerceAtLeast(10L))
                                    }
                                } else {
                                    notifyOncePer(1, "Could not find final sand placement")
                                }

                                if (autoDisable) disable()
                            } else {
                                notifyOncePer(1, "No valid placement found")
                            }
                        }
                    } catch (e: InterruptedException) {
                        return@thread
                    } catch (t: Throwable) {
                        notifyOncePer(1, "Error: ${t.javaClass.simpleName}")
                    } finally {
                        try {
                            Thread.sleep(delayMs.toLong().coerceAtLeast(35L))
                        } catch (e: InterruptedException) {
                            return@thread
                        }
                    }
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
                        renderer.add(box, renderColor.value.alpha(alpha))
                    }
                }
                renderer.render(true)
                placedBlocks.entries.removeIf { System.currentTimeMillis() - it.value >= renderTime }
            }
        }

        onDisable {
            placedBlocks.clear()
            running = false
            worker?.interrupt()
            worker = null
        }
    }

    private fun notifyOncePer(sec: Int, msg: String) {
        val now = System.currentTimeMillis()
        if (now - lastNotifyAt >= sec * 1000) {
            Notification.send(this, msg)
            lastNotifyAt = now
        }
    }

    override fun getHudInfo(): String = if (running) "Active" else "Idle"

    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
}
