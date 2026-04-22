package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.execute.ExecuteOption
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.ClientExecuteEvent
import dev.wizard.meta.event.SafeExecuteEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.toSafe
import kotlinx.coroutines.launch
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos
import java.io.File

abstract class ClientCommand @JvmOverloads constructor(
    name: String,
    alias: Array<String> = emptyArray(),
    description: String = "No description"
) : CommandBuilder<ClientExecuteEvent>(name, alias, description), AlwaysListening {

    val prefixName: String get() = Companion.prefix + name
    val chatName: String get() = "[$name]"

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.module(name: String, block: BuilderBlock<AbstractModule>) {
        val arg = ModuleArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.hudElement(name: String, block: BuilderBlock<AbstractHudElement>) {
        val arg = HudElementArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.block(name: String, block: BuilderBlock<Block>) {
        val arg = BlockArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.item(name: String, block: BuilderBlock<Item>) {
        val arg = ItemArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.player(name: String, block: BuilderBlock<PlayerProfile>) {
        val arg = PlayerArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.blockPos(name: String, block: BuilderBlock<BlockPos>) {
        val arg = BlockPosArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.baritoneBlock(name: String, block: BuilderBlock<Block>) {
        val arg = BaritoneBlockArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.schematic(name: String, block: BuilderBlock<File>) {
        val arg = SchematicArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.executeAsync(description: String = "No description", block: ExecuteBlock<ClientExecuteEvent>) {
        execute(description, emptyArray()) {
            ConcurrentScope.launch {
                block()
            }
        }
    }

    @CommandBuilder.CommandBuilder
    protected fun AbstractArg<*>.executeSafe(description: String = "No description", block: ExecuteBlock<SafeExecuteEvent>) {
        execute(description, emptyArray()) {
            val safeEvent = this.toSafe() ?: return@execute
            block(safeEvent)
        }
    }

    protected companion object {
        val mc: Minecraft = Wrapper.minecraft
        val prefix: String get() = Settings.prefix
    }
}
