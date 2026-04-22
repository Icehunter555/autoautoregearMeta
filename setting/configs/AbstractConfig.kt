package dev.wizard.meta.setting.configs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.wizard.meta.MetaMod
import dev.wizard.meta.setting.groups.SettingGroup
import dev.wizard.meta.setting.groups.SettingMultiGroup
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.setting.settings.SettingRegister
import dev.wizard.meta.util.ConfigUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

abstract class AbstractConfig<T : Any>(
    override val name: String,
    val filePath: String
) : SettingMultiGroup(name), IConfig, SettingRegister<T> {

    override val file: File
        get() = File("$filePath$name.json")

    override val backup: File
        get() = File("$filePath$name.bak")

    override fun <S : AbstractSetting<*>> setting(owner: T, setting: S): S {
        addSettingToConfig(owner, setting)
        return setting
    }

    abstract fun addSettingToConfig(owner: T, setting: AbstractSetting<*>)

    override fun save() {
        val directory = File(filePath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        saveToFile(this, file, backup)
    }

    override fun load() {
        val directory = File(filePath)
        if (!directory.exists()) {
            directory.mkdirs()
            return
        }
        try {
            loadFromFile(this, file)
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed to load latest, loading backup.")
            loadFromFile(this, backup)
        }
    }

    protected fun saveToFile(group: SettingGroup, file: File, backup: File) {
        ConfigUtils.fixEmptyJson(file)
        ConfigUtils.fixEmptyJson(backup)
        if (file.exists()) {
            file.copyTo(backup, true)
        }
        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).buffered().use {
            gson.toJson(group.write(), it)
        }
    }

    protected fun loadFromFile(group: SettingGroup, file: File) {
        ConfigUtils.fixEmptyJson(file)
        val jsonObject = parser.parse(file.readText(Charsets.UTF_8)).asJsonObject
        if (jsonObject != null) {
            group.read(jsonObject)
        }
    }

    protected companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val parser = JsonParser()
    }
}
