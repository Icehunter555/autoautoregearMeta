package dev.wizard.meta.setting.groups

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.util.extension.getRootName
import java.util.*

open class SettingMultiGroup(name: String) : SettingGroup(name) {
    protected val subGroup = LinkedHashMap<String, SettingMultiGroup>()

    fun getSetting(settingName: String): AbstractSetting<*>? {
        return subSetting[settingName.lowercase(Locale.ROOT)]
    }

    val groups: List<SettingMultiGroup>
        get() = subGroup.values.toList()

    fun getGroupOrPut(groupName: String): SettingMultiGroup {
        return subGroup.getOrPut(groupName.lowercase(Locale.ROOT)) {
            SettingMultiGroup(groupName)
        }
    }

    fun getGroup(groupName: String): SettingMultiGroup? {
        return subGroup[groupName.lowercase(Locale.ROOT)]
    }

    fun addGroup(settingGroup: SettingMultiGroup) {
        subGroup[settingGroup.getRootName().lowercase(Locale.ROOT)] = settingGroup
    }

    override fun write(): JsonObject {
        val jsonObject = super.write()
        if (subGroup.isNotEmpty()) {
            val jsonArray = JsonArray()
            subGroup.values.forEach {
                jsonArray.add(it.write())
            }
            jsonObject.add("groups", jsonArray)
        }
        return jsonObject
    }

    override fun read(jsonObject: JsonObject) {
        super.read(jsonObject)
        if (subGroup.isNotEmpty()) {
            jsonObject.get("groups")?.asJsonArray?.forEach { element ->
                element.asJsonObject?.let { groupObj ->
                    val name = groupObj.get("name")?.asString
                    if (name != null) {
                        getGroup(name)?.read(groupObj)
                    }
                }
            }
        }
    }
}
