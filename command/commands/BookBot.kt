package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.inventory.itemPayload
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.item.ItemWritableBook
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import java.util.*
import java.util.stream.Collectors

object BookBot : ClientCommand("bookbot", description = "makes dupe books") {

    private fun SafeClientEvent.createBook(sign: Boolean) {
        val heldItem = player.inventory.getCurrentItem()
        if (heldItem.item is ItemWritableBook) {
            val characterGenerator = Random().ints(128, 1112063).map {
                if (it < 55296) it else it + 2048
            }
            val joinedPages = characterGenerator.limit(10500).mapToObj { it.toChar().toString() }.collect(Collectors.joining())
            val pages = NBTTagList()
            val title = if (sign) UUID.randomUUID().toString().substring(0, 5) else ""

            for (page in 0 until 50) {
                pages.appendTag(NBTTagString(joinedPages.substring(page * 210, (page + 1) * 210)))
            }

            if (heldItem.hasTagCompound()) {
                heldItem.tagCompound?.apply {
                    setTag("pages", pages)
                    setTag("title", NBTTagString(title))
                    setTag("author", NBTTagString(player.name))
                }
            } else {
                heldItem.setTagInfo("pages", pages)
                heldItem.setTagInfo("title", NBTTagString(title))
                heldItem.setTagInfo("author", NBTTagString(player.name))
            }

            itemPayload(heldItem, "MC|BEdit")
            if (sign) {
                itemPayload(heldItem, "MC|BSign")
            }
            NoSpamMessage.sendMessage("Dupe book generated.")
        } else {
            NoSpamMessage.sendError("You must be holding a writable book.")
        }
    }

    init {
        literal("sign") {
            executeSafe {
                createBook(true)
            }
        }

        executeSafe {
            createBook(false)
        }
    }
}
