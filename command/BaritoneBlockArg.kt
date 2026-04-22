package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.StaticPrefixMatch
import dev.wizard.meta.util.BaritoneUtils
import net.minecraft.block.Block

class BaritoneBlockArg(override val name: String) : AbstractArg<Block>(), AutoComplete {
    private val delegate = StaticPrefixMatch(baritoneBlockNames)

    override fun completeForInput(string: String): String? {
        return delegate.completeForInput(string)
    }

    override suspend fun convertToType(string: String?): Block? {
        return string?.let { Block.func_149684_b(it) }
    }

    companion object {
        val baritoneBlockNames: ArrayList<String> = ArrayList<String>().apply {
            BaritoneUtils.baritoneCachedBlocks.forEach { block ->
                block.registryName?.let {
                    add(it.toString())
                    add(it.resourcePath)
                }
            }
            sort()
        }
    }
}