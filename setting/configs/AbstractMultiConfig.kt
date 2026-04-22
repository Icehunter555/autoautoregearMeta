package dev.wizard.meta.setting.configs

import dev.wizard.meta.MetaMod
import dev.wizard.meta.setting.groups.SettingMultiGroup
import java.io.File

abstract class AbstractMultiConfig<T : Any>(
    name: String,
    val directoryPath: String,
    vararg groupNames: String
) : AbstractConfig<T>(name, directoryPath) {

    init {
        groupNames.forEach {
            addGroup(SettingMultiGroup(it))
        }
    }

    override val file: File
        get() = File(directoryPath + name)

    override fun save() {
        if (!file.exists()) {
            file.mkdirs()
        }
        subGroup.values.forEach { group ->
            val (f, b) = getFiles(group)
            saveToFile(group, f, b)
        }
    }

    override fun load() {
        if (!file.exists()) {
            file.mkdirs()
            return
        }
        subGroup.values.forEach { group ->
            val (f, b) = getFiles(group)
            try {
                loadFromFile(group, f)
            } catch (e: Exception) {
                MetaMod.logger.warn("Failed to load latest, loading backup.")
                loadFromFile(group, b)
            }
        }
    }

    private fun getFiles(group: SettingMultiGroup): Pair<File, File> {
        return File(file, "${group.name}.json") to File(file, "${group.name}.bak")
    }
}
