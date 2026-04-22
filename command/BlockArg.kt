package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.StaticPrefixMatch
import net.minecraft.block.Block

class BlockArg(override val name: String) : AbstractArg<Block>(), AutoComplete {
    private val delegate = StaticPrefixMatch(allBlockNames)

    override fun completeForInput(string: String): String? {
        return delegate.completeForInput(string)
    }

    override suspend fun convertToType(string: String?): Block? {
        return string?.let { Block.func_149684_b(it) }
    }

    companion object {
        val allBlockNames: ArrayList<String> = ArrayList<String>().apply {
            Block.field_149771_c.keys.forEach {
                add(it.toString())
                add(it.resourcePath)
            }
            sort()
        }
    }
}