package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.DynamicPrefixMatch
import dev.wizard.meta.util.Wrapper
import net.minecraft.util.math.BlockPos

class BlockPosArg(override val name: String) : AbstractArg<BlockPos>(), AutoComplete {
    private val delegate = DynamicPrefixMatch { getPlayerPosString() }

    override fun completeForInput(string: String): String? {
        return delegate.completeForInput(string)
    }

    override suspend fun convertToType(string: String?): BlockPos? {
        if (string == null) return null
        val splitInts = string.split(',').mapNotNull { it.toIntOrNull() }
        if (splitInts.size != 3) return null
        return BlockPos(splitInts[0], splitInts[1], splitInts[2])
    }

    companion object {
        fun getPlayerPosString(): List<String>? {
            return Wrapper.player?.position?.let {
                listOf("${it.x},${it.y},${it.z}")
            }
        }
    }
}