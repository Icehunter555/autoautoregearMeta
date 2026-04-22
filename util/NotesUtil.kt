package dev.wizard.meta.util

import dev.wizard.meta.MetaMod
import java.io.File

object NotesUtil {
    private val notesFile = File("trollhack/notes.txt")

    fun createNotesFile() {
        if (!notesFile.exists()) {
            notesFile.createNewFile()
            MetaMod.logger.info("Created notes file!")
        }
    }

    fun sendNotesText(): List<String> {
        if (!notesFile.exists()) {
            createNotesFile()
            return emptyList()
        }
        return notesFile.readLines()
    }

    fun sendLineAtIndex(index: Int): String? {
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

    fun appendLine(text: String) {
        if (!notesFile.exists()) {
            createNotesFile()
        }
        notesFile.appendText(text + '\n')
        MetaMod.logger.info("Appended line to notes file!")
    }

    fun clearFile() {
        if (!notesFile.exists()) {
            createNotesFile()
            return
        }
        notesFile.writeText("")
        MetaMod.logger.info("Cleared notes file!")
    }

    fun deleteLine(index: Int): Boolean {
        if (!notesFile.exists()) {
            createNotesFile()
            return false
        }
        val lines = notesFile.readLines().toMutableList()
        return if (index in lines.indices) {
            lines.removeAt(index)
            notesFile.writeText(lines.joinToString("\n"))
            MetaMod.logger.info("Deleted line $index from notes file!")
            true
        } else {
            MetaMod.logger.warn("Line index $index is out of bounds!")
            false
        }
    }
}
