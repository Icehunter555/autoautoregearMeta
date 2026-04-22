package dev.wizard.meta.setting.groups

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import dev.wizard.meta.MetaMod
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.util.extension.getRootName
import dev.wizard.meta.util.interfaces.Nameable
import java.util.*

open class SettingGroup(override val name: CharSequence) : Nameable {
    protected val subSetting = LinkedHashMap<String, AbstractSetting<*>>()

    val settings: List<AbstractSetting<*>>
        get() = subSetting.values.toList()

    open fun <S : AbstractSetting<*>> addSetting(setting: S): S {
        subSetting[setting.getRootName().lowercase(Locale.ROOT)] = setting
        return setting
    }

    open fun write(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.add("name", JsonPrimitive(this.getRootName()))
        if (subSetting.isNotEmpty()) {
            val settingsObject = JsonObject()
            subSetting.values.forEach { setting ->
                if (!setting.isTransient) {
                    settingsObject.add(setting.getRootName().toJsonName(), setting.write())
                }
            }
            jsonObject.add("settings", settingsObject)
        }
        return jsonObject
    }

    open fun read(jsonObject: JsonObject) {
        if (subSetting.isNotEmpty()) {
            jsonObject.get("settings")?.asJsonObject?.let { settings ->
                subSetting.values.forEach { setting ->
                    if (!setting.isTransient) {
                        try {
                            settings.get(setting.getRootName().toJsonName())?.let {
                                setting.read(it)
                            }
                        } catch (e: Exception) {
                            MetaMod.logger.warn("Failed loading setting ${setting.name} at $name", e)
                        }
                    }
                }
            }
        }
    }

    private fun String.toJsonName(): String = replace(' ', '_').lowercase(Locale.ROOT)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SettingGroup
        return name == other.name && subSetting == other.subSetting
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + subSetting.hashCode()
        return result
    }
}
