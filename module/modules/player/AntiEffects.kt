package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import net.minecraft.init.MobEffects

object AntiEffects : Module(
    "AntiEffects",
    category = Category.PLAYER,
    description = "remove annoying/bad effects"
) {
    private val levitationMode by setting(this, EnumSetting(settingName("Levitation Mode"), LevitationMode.NONE))
    private val upAmplifier by setting(this, IntegerSetting(settingName("Up Amplifier"), 1, 1..3, 1, { levitationMode == LevitationMode.CONTROL }))
    private val downAmplifier by setting(this, IntegerSetting(settingName("Down Amplifier"), 1, 1..3, 1, { levitationMode == LevitationMode.CONTROL }))
    private val removeJumpBoost by setting(this, BooleanSetting(settingName("Remove Jump-boost"), false))
    private val removeSlowness by setting(this, BooleanSetting(settingName("Remove Slowness"), false))

    init {
        safeListener<TickEvent.Pre> {
            if (removeJumpBoost && player.isPotionActive(MobEffects.JUMP_BOOST)) {
                player.removeActivePotionEffect(MobEffects.JUMP_BOOST)
            }
            if (removeSlowness && player.isPotionActive(MobEffects.SLOWNESS)) {
                player.removeActivePotionEffect(MobEffects.SLOWNESS)
            }
            if (levitationMode == LevitationMode.REMOVE && player.isPotionActive(MobEffects.LEVITATION)) {
                player.removeActivePotionEffect(MobEffects.LEVITATION)
            }
        }

        safeListener<TickEvent.Post> {
            if (levitationMode == LevitationMode.CONTROL && player.isPotionActive(MobEffects.LEVITATION)) {
                val effect = player.getActivePotionEffect(MobEffects.LEVITATION) ?: return@safeListener
                val amplifier = effect.amplifier

                player.motionY = if (mc.gameSettings.keyBindJump.isKeyDown) {
                    (0.05 * (amplifier + 1) - player.motionY) * 0.2 * upAmplifier
                } else if (mc.gameSettings.keyBindSneak.isKeyDown) {
                    -((0.05 * (amplifier + 1) - player.motionY) * 0.2) * downAmplifier
                } else 0.0
            }
        }
    }

    private enum class LevitationMode { NONE, REMOVE, CONTROL }
}
