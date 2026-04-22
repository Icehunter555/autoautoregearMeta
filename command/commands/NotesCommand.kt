package dev.wizard.meta.command.commands

import dev.wizard.meta.MetaMod
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.util.text.TextFormatting
import java.io.File

object NotesCommand : ClientCommand("notes", description = "use the notes system") {

    private val notesFolder = File("trollhack/notes")
    private val notesFile = File(notesFolder, "notes.txt")

    init {
        if (!notesFolder.exists()) {
            notesFolder.mkdirs()
            MetaMod.logger.info("Created notes folder!")
        }

        literal("append", "add") {
            greedy("text") { textArg ->
                executeSafe {
                    val text = getValue(textArg)
                    appendLine(text)
                    MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.GREEN}Added note: ${TextFormatting.WHITE}$text")
                }
            }
        }

        literal("delete", "remove") {
            int("line") { lineArg ->
                executeSafe {
                    val lineIndex = getValue(lineArg)
                    if (deleteLine(lineIndex)) {
                        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.RED}Deleted line ${TextFormatting.YELLOW}#$lineIndex")
                    } else {
                        MessageSendUtils.sendErrorMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.RED}Invalid line index: ${TextFormatting.YELLOW}$lineIndex")
                    }
                }
            }
        }

        literal("clear", "wipe") {
            executeSafe {
                clearFile()
                MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.RED}${TextFormatting.BOLD}Cleared all notes")
            }
        }

        literal("save") {
            greedy("filename") { filenameArg ->
                executeSafe {
                    val filename = getValue(filenameArg)
                    if (saveToFile(filename)) {
                        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.GREEN}Saved notes to: ${TextFormatting.AQUA}$filename.txt")
                    } else {
                        MessageSendUtils.sendErrorMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.RED}Failed to save notes (file may be empty)")
                    }
                }
            }
        }

        literal("list", "saved") {
            executeSafe {
                val savedFiles = listSavedFiles()
                if (savedFiles.isEmpty()) {
                    MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.YELLOW}No saved note files found")
                } else {
                    val separator = "${TextFormatting.GRAY}${TextFormatting.STRIKETHROUGH}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    MessageSendUtils.sendChatMessage(separator)
                    MessageSendUtils.sendChatMessage("${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}Saved Notes ${TextFormatting.GRAY}(${savedFiles.size} files)")
                    MessageSendUtils.sendChatMessage(separator)
                    savedFiles.forEachIndexed { index, file ->
                        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}${index + 1}${TextFormatting.DARK_GRAY} │ ${TextFormatting.AQUA}$file")
                    }
                    MessageSendUtils.sendChatMessage(separator)
                }
            }
        }

        literal("send", "get") {
            executeSafe {
                val lines = sendNotesText()
                if (lines.isEmpty()) {
                    MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.YELLOW}Notes file is empty")
                } else {
                    val separator = "${TextFormatting.GRAY}${TextFormatting.STRIKETHROUGH}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    MessageSendUtils.sendChatMessage(separator)
                    MessageSendUtils.sendChatMessage("${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}Notes ${TextFormatting.GRAY}(${lines.size} total)")
                    MessageSendUtils.sendChatMessage(separator)
                    lines.forEachIndexed { index, line ->
                        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}${index + 1}${TextFormatting.DARK_GRAY} │ ${TextFormatting.WHITE}$line")
                    }
                    MessageSendUtils.sendChatMessage(separator)
                }
            }

            int("line") { lineArg ->
                executeSafe {
                    val lineIndex = getValue(lineArg)
                    val text = sendLineAtIndex(lineIndex - 1)
                    if (text != null) {
                        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.YELLOW}Line #$lineIndex: ${TextFormatting.WHITE}$text")
                    } else {
                        MessageSendUtils.sendErrorMessage("${TextFormatting.GRAY}[${TextFormatting.LIGHT_PURPLE}Notes${TextFormatting.GRAY}] ${TextFormatting.RED}Invalid line index: ${TextFormatting.YELLOW}$lineIndex")
                    }
                }
            }
        }
    }

    private fun createNotesFile() {
        if (!notesFile.exists()) {
            notesFile.createNewFile()
            MetaMod.logger.info("Created notes file!")
        }
    }

    private fun sendNotesText(): List<String> {
        if (!notesFile.exists()) {
            createNotesFile()
            return emptyList()
        }
        return notesFile.readLines()
    }

    private fun sendLineAtIndex(index: Int): String? {
        if (!notesFile.exists()) {
            createNotesFile()
            return null
        }
        val lines = notesFile.readLines()
        return if (index in lines.indices) {
            lines[index]
        } else {
            MetaMod.logger.warn("Line index $index is out of bounds!")
            null
        }
    }

    private fun appendLine(text: String) {
        if (!notesFile.exists()) createNotesFile()
        notesFile.appendText("$text\n")
        MetaMod.logger.info("Appended line to notes file!")
    }

    private fun clearFile() {
        if (!notesFile.exists()) {
            createNotesFile()
            return
        }
        notesFile.writeText("")
        MetaMod.logger.info("Cleared notes file!")
    }

    private fun deleteLine(index: Int): Boolean {
        if (!notesFile.exists()) {
            createNotesFile()
            return false
        }
        val lines = notesFile.readLines().toMutableList()
        return if (index in lines.indices) {
            lines.removeAt(index)
            notesFile.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "")
            MetaMod.logger.info("Deleted line $index from notes file!")
            true
        } else {
            MetaMod.logger.warn("Line index $index is out of bounds!")
            false
        }
    }

    private fun saveToFile(filename: String): Boolean {
        return try {
            if (!notesFile.exists()) {
                createNotesFile()
                return false
            }
            val lines = notesFile.readLines()
            if (lines.isEmpty()) return false

            val sanitizedName = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalName = if (sanitizedName.endsWith(".txt")) sanitizedName else "$sanitizedName.txt"
            val targetFile = File(notesFolder, finalName)
            targetFile.writeText(lines.joinToString("\n") + "\n")
            MetaMod.logger.info("Saved notes to $finalName")
            true
        } catch (e: Exception) {
            MetaMod.logger.error("Failed to save notes: ${e.message}")
            false
        }
    }

    private fun listSavedFiles(): List<String> {
        return notesFolder.listFiles()?.filter {
            it.isFile && it.name.endsWith(".txt") && it.name != "notes.txt"
        }?.map { it.name } ?: emptyList()
    }
}
