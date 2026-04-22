package dev.wizard.meta.setting

import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.setting.configs.NameableConfig
import java.io.File

object ModuleConfig : NameableConfig<AbstractModule>("modules", "trollhack/config/modules") {
    override val file: File
        get() = File("$filePath/${Settings.INSTANCE.modulePreset}.json")

    override val backup: File
        get() = File("$filePath/${Settings.INSTANCE.modulePreset}.bak")
}
