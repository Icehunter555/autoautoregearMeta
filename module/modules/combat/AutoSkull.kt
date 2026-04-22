package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ModuleToggleEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.HoleManager
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
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.world.BlockKt
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks
import net.minecraft.item.ItemSkull
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import kotlin.math.abs

object AutoSkull : Module(
    "AutoSkull",
    category = Category.COMBAT,
    description = "Auto Skuller",
    modulePriority = 1010
) {
    private val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.NONE))
    private val disableAfterPlace by setting(this, BooleanSetting(settingName("Disable After Place"), true))
    private val placeOnMoving by setting(this, BooleanSetting(settingName("While Moving"), false))
    private val placeIfSurrounded by setting(this, BooleanSetting(settingName("Place In Hole"), false, { !disableAfterPlace }))
    private val placeIfSandAbove by setting(this, BooleanSetting(settingName("Place If Sand Above"), false, { !disableAfterPlace }))
    val placeOnSurround by setting(this, BooleanSetting(settingName("Place On Surround"), false, { !disableAfterPlace }))
    private val placeOnClosePlayer by setting(this, BooleanSetting(settingName("Place On Player"), false, { !disableAfterPlace }))
    private val playerDetectionRange by setting(this, DoubleSetting(settingName("Player Range"), 4.5, 0.5..10.0, 0.5, { placeOnClosePlayer && !disableAfterPlace }))
    private val runDelay by setting(this, IntegerSetting(settingName("Run Delay"), 1, 0..10, 1))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.FADE, { render }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 0, 0), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))

    private val timer = TickTimer()
    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedSkulls = linkedMapOf<BlockPos, Long>()

    init {
        onDisable {
            placedSkulls.clear()
        }

        safeListener<TickEvent.Post> {
            if (timer.tickAndReset(runDelay.toLong(), TimeUnit.SECONDS)) {
                if (disableAfterPlace) {
                    placeSkull(this)
                    disable()
                } else if (shouldPlaceSkull(this)) {
                    placeSkull(this)
                }
            }
        }

        safeListener<ModuleToggleEvent> {
            if (!placeOnSurround) return@safeListener
            if (it.module == Surround && !Surround.isEnabled) {
                placeSkull(this)
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                renderer.clear()
                placedSkulls.forEach { pos, timestamp ->
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
                placedSkulls.entries.removeIf { System.currentTimeMillis() - it.value >= renderTime }
            }
        }
    }

    override fun getHudInfo(): String {
        return placedSkulls.entries.count { System.currentTimeMillis() - it.value <= 3000L }.toString()
    }

    private fun shouldPlaceSkull(event: SafeClientEvent): Boolean {
        val playerPos = EntityUtils.getBetterPosition(event.player)
        if (MovementUtils.isMoving(event.player) && !placeOnMoving) return false
        if (placeIfSurrounded && HoleManager.getHoleInfo(playerPos).isHole) return true
        if (placeIfSandAbove && isSandAbove(event, playerPos)) return true
        return placeOnClosePlayer && isPlayerClose(event)
    }

    private fun isSandAbove(event: SafeClientEvent, playerPos: BlockPos): Boolean {
        return event.world.loadedEntityList.any { entity ->
            if (entity is EntityFallingBlock) {
                abs(entity.posX - playerPos.x) <= 1.5 && abs(entity.posZ - playerPos.z) <= 1.5 &&
                entity.posY >= playerPos.y && entity.posY <= playerPos.y + 6
            } else false
        }
    }

    private fun isPlayerClose(event: SafeClientEvent): Boolean {
        return event.world.playerEntities.any { entity ->
            entity.isEntityAlive && entity != event.player && !FriendManager.isFriend(entity.name) &&
            event.player.getDistance(entity) <= playerDetectionRange
        }
    }

    private fun placeSkull(event: SafeClientEvent) {
        val playerPos = EntityUtils.getBetterPosition(event.player)
        val blockBelow = playerPos.down()
        if (BlockKt.getBlock(event.world, blockBelow) == Blocks.SKULL) return
        if (!event.world.getBlockState(blockBelow).isSideSolid(event.world, blockBelow, EnumFacing.UP)) {
            NoSpamMessage.sendMessage("${getChatName()} Cannot place!")
            return
        }
        val skullSlot = IterableKt.firstByStack(DefinedKt.getAllSlotsPrioritized(event.player)) { it.item is ItemSkull } ?: return
        HotbarSwitchManager.ghostSwitch(event, ghostSwitchBypass, skullSlot) {
            it.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(blockBelow, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 0.5f, 0.5f))
        }
        placedSkulls[blockBelow] = System.currentTimeMillis()
    }

    private enum class RenderMode { FADE, GROW, SHRINK, RISE, STATIC }
}
