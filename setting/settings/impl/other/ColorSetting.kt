package dev.wizard.meta.setting.settings.impl.other

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.setting.settings.MutableNonPrimitive
import dev.wizard.meta.setting.settings.MutableSetting
import dev.wizard.meta.util.JsonUtilsKt
import java.util.*
import kotlin.reflect.KProperty

class ColorSetting(
    name: CharSequence,
    value: ColorRGB,
    val hasAlpha: Boolean = true,
    visibility: (() -> Boolean)? = null,
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : MutableSetting<ColorRGB>(name, value, visibility, { _, input ->
    if (!hasAlpha) input.alpha(255) else input
}, description), MutableNonPrimitive<ColorRGB> {

    constructor(
        name: CharSequence,
        value: Int,
        hasAlpha: Boolean = true,
        visibility: (() -> Boolean)? = null,
        description: CharSequence = "",
        isTransient: Boolean = false
    ) : this(name, ColorRGB(value), hasAlpha, visibility, description, isTransient)

    override fun setValue(valueIn: String) {
        val anti0x = valueIn.removePrefix("0x")
        val split = anti0x.split(',')
        if (split.size in 3..4) {
            val r = split[0].toIntOrNull() ?: return
            val g = split[1].toIntOrNull() ?: return
            val b = split[2].toIntOrNull() ?: return
            val a = split.getOrNull(3)?.toIntOrNull() ?: 255
            this.value = ColorRGB(r, g, b, a)
        } else {
            anti0x.toUIntOrNull(16)?.let {
                this.value = ColorRGB(it.toInt())
            }
        }
    }

    override fun write(): JsonPrimitive {
        @OptIn(ExperimentalUnsignedTypes::class)
        return JsonPrimitive(value.rgba.toUInt().toString(16).uppercase(Locale.ROOT))
    }

    override fun read(jsonElement: JsonElement) {
        JsonUtilsKt.getAsStringOrNull(jsonElement)?.let {
            setValue(it.lowercase(Locale.ROOT))
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ColorRGB {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: ColorRGB) {
        this.value = value
    }
}
