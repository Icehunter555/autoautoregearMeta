package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.math.RotationUtils
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.ArrayDeque

object ViewLock : Module(
    "ViewLock",
    alias = arrayOf("YawLock", "PitchLock"),
    category = Category.PLAYER,
    description = "Locks your camera view"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.YAW))

    private val yaw by setting(this, BooleanSetting(settingName("Yaw"), true, page.atValue(Page.YAW)))
    private val autoYaw by setting(this, BooleanSetting(settingName("Auto Yaw"), true, page.atValue(Page.YAW) and yaw.atTrue()))
    private val disableMouseYaw by setting(this, BooleanSetting(settingName("Disable Mouse Yaw"), false, page.atValue(Page.YAW) and yaw.atTrue()))
    private val specificYaw by setting(this, FloatSetting(settingName("Specific Yaw"), 180.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.YAW) and yaw.atTrue() and autoYaw.atFalse()))
    private val yawSlice by setting(this, IntegerSetting(settingName("Yaw Slice"), 8, 2..32, 1, page.atValue(Page.YAW) and yaw.atTrue() and autoYaw.atTrue()))

    private val pitch by setting(this, BooleanSetting(settingName("Pitch"), true, page.atValue(Page.PITCH)))
    private val autoPitch by setting(this, BooleanSetting(settingName("Auto Pitch"), true, page.atValue(Page.PITCH) and pitch.atTrue()))
    private val disableMousePitch by setting(this, BooleanSetting(settingName("Disable Mouse Pitch"), false, page.atValue(Page.PITCH) and pitch.atTrue()))
    private val specificPitch by setting(this, FloatSetting(settingName("Specific Pitch"), 0.0f, -90.0f..90.0f, 1.0f, page.atValue(Page.PITCH) and pitch.atTrue() and autoPitch.atFalse()))
    private val pitchSlice by setting(this, IntegerSetting(settingName("Pitch Slice"), 5, 2..32, 1, page.atValue(Page.PITCH) and pitch.atTrue() and autoPitch.atTrue()))

    private var yawSnap = 0
    private var pitchSnap = 0
    private val deltaXQueue = ArrayDeque<Pair<Float, Long>>()
    private val deltaYQueue = ArrayDeque<Pair<Float, Long>>()
    private var pitchSliceAngle = 1.0f
    private var yawSliceAngle = 1.0f

    @JvmStatic
    fun handleTurn(entity: net.minecraft.entity.Entity, deltaX: Float, deltaY: Float, ci: CallbackInfo) {
        if (INSTANCE.isDisabled()) return
        val player = mc.player ?: return
        if (entity != player) return

        val multipliedX = deltaX * 0.15f
        val multipliedY = deltaY * -0.15f

        val yawChange = if (INSTANCE.yaw && INSTANCE.autoYaw) INSTANCE.handleDelta(multipliedX, INSTANCE.deltaXQueue, INSTANCE.yawSliceAngle) else 0
        val pitchChange = if (INSTANCE.pitch && INSTANCE.autoPitch) INSTANCE.handleDelta(multipliedY, INSTANCE.deltaYQueue, INSTANCE.pitchSliceAngle) else 0

        INSTANCE.turn(player, multipliedX, multipliedY)
        INSTANCE.changeDirection(yawChange, pitchChange)
        player.ridingEntity?.updatePassenger(player)
        ci.cancel()
    }

    private fun turn(player: net.minecraft.client.entity.EntityPlayerSP, deltaX: Float, deltaY: Float) {
        if (!yaw || !disableMouseYaw) {
            player.prevRotationYaw += deltaX
            player.rotationYaw += deltaX
        }
        if (!pitch || !disableMousePitch) {
            player.prevRotationPitch += deltaY
            player.rotationPitch += deltaY
            player.rotationPitch = player.rotationPitch.coerceIn(-90.0f, 90.0f)
        }
    }

    private fun handleDelta(delta: Float, queue: ArrayDeque<Pair<Float, Long>>, slice: Float): Int {
        val currentTime = System.currentTimeMillis()
        queue.addLast(delta to currentTime)
        var sum = 0.0f
        queue.forEach { sum += it.first }
        return if (Math.abs(sum) > slice) {
            queue.clear()
            Math.signum(sum).toInt()
        } else {
            while (queue.isNotEmpty() && queue.first.second < currentTime - 500) {
                queue.removeFirst()
            }
            0
        }
    }

    private fun changeDirection(yawChange: Int, pitchChange: Int) {
        yawSnap = Math.floorMod(yawSnap + yawChange, yawSlice)
        pitchSnap = (pitchSnap + pitchChange).coerceIn(0, pitchSlice - 1)
        snapToSlice()
    }

    private fun snapToNext() {
        mc.player?.let {
            yawSnap = Math.round(it.rotationYaw / yawSliceAngle)
            pitchSnap = Math.round((it.rotationPitch + 90.0f) / pitchSliceAngle)
            snapToSlice()
        }
    }

    private fun snapToSlice() {
        mc.player?.let { player ->
            if (yaw && autoYaw) {
                RotationUtils.setYaw(player, yawSnap * yawSliceAngle)
                player.ridingEntity?.rotationYaw = player.rotationYaw
            }
            if (pitch && autoPitch) {
                RotationUtils.setPitch(player, pitchSnap * pitchSliceAngle - 90.0f)
            }
        }
    }

    init {
        onEnable {
            yawSliceAngle = 360.0f / yawSlice
            pitchSliceAngle = 180.0f / (pitchSlice - 1)
            if (autoYaw || autoPitch) snapToNext()
        }

        safeListener<TickEvent.Post> {
            if (autoYaw || autoPitch) snapToSlice()
            if (yaw && !autoYaw) RotationUtils.setYaw(player, specificYaw)
            if (pitch && !autoPitch) RotationUtils.setPitch(player, specificPitch)
        }

        val updateAngles = {
            yawSliceAngle = 360.0f / yawSlice
            pitchSliceAngle = 180.0f / (pitchSlice - 1)
            if (isEnabled && (autoYaw || autoPitch)) snapToNext()
        }

        yawSlice.valueListeners.add { _, _ -> updateAngles() }
        pitchSlice.valueListeners.add { _, _ -> updateAngles() }

        autoYaw.valueListeners.add { _, it -> if (isEnabled && it) snapToNext() }
        autoPitch.valueListeners.add { _, it -> if (isEnabled && it) snapToNext() }
    }

    private enum class Page { YAW, PITCH }
}
