package dev.wizard.meta.module

import dev.wizard.meta.setting.ModuleConfig

abstract class Module(
    name: String,
    alias: Array<String> = emptyArray(),
    category: Category,
    description: String,
    modulePriority: Int = -1,
    alwaysListening: Boolean = false,
    visible: Boolean = true,
    devOnly: Boolean = false,
    alwaysEnabled: Boolean = false,
    enabledByDefault: Boolean = false
) : AbstractModule(
    name,
    alias,
    category,
    description,
    modulePriority,
    alwaysListening,
    visible,
    devOnly,
    alwaysEnabled,
    enabledByDefault,
    ModuleConfig
)
