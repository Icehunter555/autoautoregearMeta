package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.onMainThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiShulkerBox
import net.minecraft.item.ItemShulkerBox
import net.minecraft.tileentity.TileEntityShulkerBox

object PeekCommand : ClientCommand("peek", arrayOf("shulkerpeek"), "Look inside the contents of a shulker box without opening it.") {

    init {
        executeSafe {
            val itemStack = player.inventory.getCurrentItem()
            val item = itemStack.item
            if (item is ItemShulkerBox) {
                val entityBox = TileEntityShulkerBox()
                entityBox.world = world
                val nbt = itemStack.tagCompound ?: return@executeSafe
                entityBox.readFromNBT(nbt.getCompoundTag("BlockEntityTag"))

                val scaledResolution = ScaledResolution(mc)
                val gui = GuiShulkerBox(player.inventory, entityBox)
                gui.setWorldAndResolution(mc, scaledResolution.scaledWidth, scaledResolution.scaledHeight)

                ConcurrentScope.launch {
                    delay(50L)
                    onMainThread {
                        mc.displayGuiScreen(gui)
                    }
                }
            } else {
                NoSpamMessage.sendError("You aren't holding a shulker box.")
            }
        }
    }
}
