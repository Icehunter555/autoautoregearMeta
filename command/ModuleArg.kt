package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.DynamicPrefixMatch
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.util.delegate.CachedValueN

class ModuleArg(override val name: String) : AbstractArg<AbstractModule>(), AutoComplete by DynamicPrefixMatch(Companion::allAlias) {

    override suspend fun convertToType(string: String?): AbstractModule? {
        val module = ModuleManager.getModuleOrNull(string)
        return if (module?.isDevOnly == true) null else module
    }

    companion object {
        val allAlias: List<String> by CachedValueN(5000L) {
            ModuleManager.modules.asSequence()
                .filter { !it.isDevOnly }
                .flatMap { module ->
                    sequence {
                        yield(module.internalName)
                        module.alias.forEach { yield(it) }
                    }
                }
                .sorted()
                .toList()
        }
    }
}
