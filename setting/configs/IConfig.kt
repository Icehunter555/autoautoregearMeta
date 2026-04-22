package dev.wizard.meta.setting.configs

import dev.wizard.meta.util.interfaces.Nameable
import java.io.File

interface IConfig : Nameable {
    val file: File
    val backup: File

    fun save()
    fun load()
}
