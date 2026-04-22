package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting

object ItemPhysics : Module(
    "ItemPhysics",
    category = Category.RENDER,
    description = "Physics for items"
) {
    val scale by setting(this, DoubleSetting(settingName("Scale"), 0.34, 0.1..5.0, 0.2))
    val rotateSpeed by setting(this, FloatSetting(settingName("Rotate Speed"), 25.0f, 0.0f..100.0f, 5.0f))
    val oldRotations by setting(this, BooleanSetting(settingName("Old Rotations"), false))

    var tick = 0L

    init {
        listener<TickEvent.Post> {
            tick = System.nanoTime()
        }
    }
}
