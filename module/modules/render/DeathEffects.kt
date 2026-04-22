package dev.wizard.meta.module.modules.render

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.SoundUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.threads.onMainThreadSafe
import net.minecraft.entity.effect.EntityLightningBolt

object DeathEffects : Module(
    "DeathEffects",
    category = Category.RENDER,
    description = "Death Effects"
) {
    private val lightning by setting(this, BooleanSetting(settingName("Spawn Lightning"), true))
    private val soundMode by setting(this, EnumSetting(settingName("Sound Mode"), SoundMode.NONE))
    private val volume by setting(this, FloatSetting(settingName("Volume"), 1.0f, 0.1f..1.0f, 0.05f, { soundMode != SoundMode.NONE }))

    private val timer = TickTimer()

    init {
        safeListener<TotemPopEvent.Death> {
            if (EntityUtils.isSelf(it.entity)) return@safeListener

            onMainThreadSafe {
                if (timer.tickAndReset(1500L, TimeUnit.MILLISECONDS) && lightning) {
                    val bolt = EntityLightningBolt(world, it.entity.posX, it.entity.posY, it.entity.posZ, true)
                    world.addWeatherEffect(bolt)
                }
            }

            if (soundMode != SoundMode.NONE) {
                SoundUtils.playSound(volume) { soundMode.sound }
            }
        }
    }

    private enum class SoundMode(override val displayName: CharSequence, val sound: String) : DisplayEnum {
        NONE("Off", "none"),
        BONG("Bong", "bong.ogg"),
        FORTNITE("Fortnite", "fortnite.ogg")
    }
}
