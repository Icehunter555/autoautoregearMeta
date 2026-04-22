package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.StaticPrefixMatch
import net.minecraft.item.Item

class ItemArg(override val name: String) : AbstractArg<Item>(), AutoComplete by StaticPrefixMatch(allItemNames) {

    override suspend fun convertToType(string: String?): Item? {
        return string?.let { Item.getByNameOrId(it) }
    }

    companion object {
        private val allItemNames = ArrayList<String>().apply {
            Item.REGISTRY.keys.forEach {
                add(it.toString())
                add(it.path)
            }
        }.sorted()
    }
}
