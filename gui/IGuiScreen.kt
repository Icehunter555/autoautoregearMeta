package dev.wizard.meta.gui

import dev.fastmc.common.collection.FastObjectArrayList
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.util.threads.runSynchronized
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet

interface IGuiScreen {
    val isVisible: Boolean
    val mouseState: MouseState
    var lastClicked: WindowComponent?
    val hovered: WindowComponent?
    val windows: ObjectLinkedOpenHashSet<WindowComponent>
    val mousePos: Long
    val windowsCachedList: FastObjectArrayList<WindowComponent>

    fun closeWindow(window: WindowComponent): Boolean {
        val closed = windows.runSynchronized {
            remove(window)
        }
        if (closed) {
            window.onClosed()
        }
        if (lastClicked == window) {
            lastClicked = null
        }
        return closed
    }

    fun displayWindow(window: WindowComponent): Boolean {
        val displayed = windows.runSynchronized {
            addAndMoveToLast(window)
        }
        if (displayed) {
            window.onDisplayed()
        }
        return displayed
    }

    companion object {
        inline fun IGuiScreen.forEachWindow(block: (WindowComponent) -> Unit) {
            windows.runSynchronized {
                windowsCachedList.addAll(this)
            }
            for (i in 0 until windowsCachedList.size()) {
                block(windowsCachedList[i])
            }
            windowsCachedList.clear()
        }
    }
}
