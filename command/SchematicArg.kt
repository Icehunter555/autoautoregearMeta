package dev.wizard.meta.command

import dev.fastmc.common.TimeUnit
import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.DynamicPrefixMatch
import dev.wizard.meta.util.delegate.AsyncCachedValue
import kotlinx.coroutines.Dispatchers
import java.io.File

class SchematicArg(override val name: String) : AbstractArg<File>(), AutoComplete by DynamicPrefixMatch(Companion::schematicFiles) {

    override suspend fun convertToType(string: String?): File? {
        if (string == null) return null
        val nameWithoutExt = string.removeSuffix(".schematic")
        return schematicFolder.listFiles()?.firstOrNull {
            it.exists() && it.isFile && it.name.equals("$nameWithoutExt.schematic", ignoreCase = true)
        }
    }

    companion object {
        val schematicFolder = File("schematics")
        val schematicFiles: List<String> by AsyncCachedValue(5L, TimeUnit.SECONDS, Dispatchers.IO) {
            schematicFolder.listFiles()?.map { it.name } ?: emptyList()
        }
    }
}
