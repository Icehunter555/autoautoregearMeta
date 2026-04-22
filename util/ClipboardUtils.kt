package dev.wizard.meta.util

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object ClipboardUtils {
    fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }

    fun pasteFromClipboard(): String {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return (clipboard.getData(DataFlavor.stringFlavor) as? String) ?: ""
    }

    fun getClipboard(): String? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return try {
            clipboard.getData(DataFlavor.stringFlavor)?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
