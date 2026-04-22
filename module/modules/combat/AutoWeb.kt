package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.isInWeb
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.world.BlockKt
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.*

object AutoWeb : Module(
    "AutoWeb",
    category = Category.COMBAT,
    description = "Places webs on nearby players",
    modulePriority = 130
) {
    private val bypassMode by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.NONE))
    private val autoDisable by setting(this, BooleanSetting(settingName("Auto Disable"), false))
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 50, 0..1000, 1))
    private val multiPlace by setting(this, IntegerSetting(settingName("Multi-Place"), 2, 1..12, 1))
    private val placeFeet by setting(this, BooleanSetting(settingName("Place On Feet"), true))
    private val placeHead by setting(this, BooleanSetting(settingName("Place On Head"), false))
    private val singleTarget by setting(this, BooleanSetting(settingName("Single Target"), false))
    private val targetRange by setting(this, FloatSetting(settingName("Range"), 5.0f, 1.0f..6.0f, 1.0f, { !singleTarget }))
    private val targetOnGround by setting(this, BooleanSetting(settingName("Target On Ground"), false))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 0, 0), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))

    private val placeTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val placedBlocks = linkedMapOf<BlockPos, Long>()

    init {
        onDisable {
            placeTimer.reset(-114514L)
            placedBlocks.clear()
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            if (placeTimer.tick(placeDelay.toLong())) {
                runAutoWeb(this)
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

    override fun getHudInfo(): String {
        return placedBlocks.entries.count { System.currentTimeMillis() - it.value <= 3000L }.toString()
    }

    private fun runAutoWeb(event: SafeClientEvent) {
        val targets = getTargets(event).entries.sortedBy { it.value }.map { it.key }
        if (targets.isEmpty()) return

        val primaryTarget = if (singleTarget) targets.firstOrNull() else null
        var placedCount = 0

        for (target in targets) {
            if (primaryTarget != null && target != primaryTarget) continue
            if (placedCount >= multiPlace) break
            if (target.isInWeb) continue

            val basePos = BlockPos(target.posX, target.posY, target.posZ)
            val positions = mutableListOf<BlockPos>()
            if (placeFeet) positions.add(basePos)
            if (placeHead) positions.add(basePos.up())

            for (pos in positions) {
                if (placedCount >= multiPlace) break
                if (!event.world.getBlockState(pos).material.isReplaceable || pos.y >= 256) continue
                placeWeb(event, pos)
                placedBlocks[pos] = System.currentTimeMillis()
                placedCount++
            }
        }

        if (autoDisable && placedCount == 0) disable()
    }

    private fun getTargets(event: SafeClientEvent): Map<EntityPlayer, Double> {
        val targetMap = mutableMapOf<EntityPlayer, Double>()
        for (target in event.world.playerEntities) {
            if (EntityUtils.isFriend(target) || target.isCreative || EntityUtils.isSelf(target) || !target.isEntityAlive || (targetOnGround && !target.onGround)) continue
            val dist = event.player.getDistance(target).toDouble()
            if (dist <= targetRange) {
                targetMap[target] = dist
            }
        }
        return targetMap
    }

    private fun placeWeb(event: SafeClientEvent, pos: BlockPos) {
        val slot = IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(event.player), Item.getItemFromBlock(Blocks.WEB)) ?: run {
            NoSpamMessage.sendError("${getChatName()} No webs found in inventory!")
            if (autoDisable) disable()
            return
        }
        HotbarSwitchManager.ghostSwitch(event, bypassMode, slot) {
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos.down(), EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f))
        }
        event.player.swingArm(EnumHand.MAIN_HAND)
    }

    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
}
