package dev.wizard.meta.setting.settings.impl.other

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.setting.settings.ImmutableSetting
import dev.wizard.meta.setting.settings.NonPrimitive
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.KeyboardUtils
import dev.wizard.meta.util.interfaces.Helper
import dev.wizard.meta.util.interfaces.Nameable
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.lwjgl.input.Keyboard
import java.util.concurrent.CopyOnWriteArrayList

class BindSetting(
    name: CharSequence,
    value: Bind,
    visibility: (() -> Boolean)? = null,
    private val action: ((Boolean) -> Unit)? = null,
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : ImmutableSetting<Bind>(name, value, visibility, { _, input -> input }, description), NonPrimitive<Bind> {

    override val defaultValue: Bind = Bind(IntAVLTreeSet(value.modifierKeys), value.key)

    init {
        binds.add(this)
    }

    override fun resetValue() {
        value.setBind(defaultValue.modifierKeys, defaultValue.key)
    }

    override fun setValue(valueIn: String) {
        if (valueIn.equals("None", ignoreCase = true)) {
            value.clear()
            return
        }
        val splitNames = valueIn.split('+')
        val lastName = splitNames.last()
        val lastKey = if (!lastName.startsWith("Mouse ")) {
            KeyboardUtils.getKey(lastName)
        } else {
            lastName.last().digitToIntOrNull()?.let { -it - 1 } ?: 0
        }

        if (lastKey !in 1..255 && lastKey !in -16..-1) return

        val modifierKeys = IntArrayList(0)
        for (i in 0 until splitNames.size - 1) {
            val key = KeyboardUtils.getKey(splitNames[i])
            if (key in 1..255) {
                modifierKeys.add(key)
            }
        }
        value.setBind(modifierKeys, lastKey)
    }

    override fun write(): JsonPrimitive {
        return JsonPrimitive(value.toString())
    }

    override fun read(jsonElement: JsonElement) {
        val string = jsonElement.asString ?: "None"
        setValue(string)
    }

    companion object : AlwaysListening, Helper, Nameable {
        override val name = "BindSetting"
        private val binds = CopyOnWriteArrayList<BindSetting>()

        init {
            listener<InputEvent.Keyboard>(alwaysListening = true) {
                if (mc.currentScreen == null && it.key != 0 && !Keyboard.isKeyDown(61)) {
                    binds.forEach { bind ->
                        if (bind.value.isDown(it.key)) {
                            bind.action?.invoke(it.state)
                        }
                    }
                }
            }
            listener<InputEvent.Mouse>(alwaysListening = true) {
                if (mc.currentScreen == null) {
                    binds.forEach { bind ->
                        if (bind.value.isDown(-it.button - 1)) {
                            bind.action?.invoke(it.state)
                        }
                    }
                }
            }
        }
    }
}
