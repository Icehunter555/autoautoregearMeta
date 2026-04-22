package dev.wizard.meta.gui.hudgui

import dev.wizard.meta.event.IListenerOwner
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.Resolution
import dev.wizard.meta.gui.AbstractTrollGui
import dev.wizard.meta.gui.IGuiScreen.Companion.forEachWindow
import dev.wizard.meta.gui.hudgui.component.HudButton
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.gui.rgui.windows.ListWindow
import dev.wizard.meta.module.modules.client.HudEditor
import dev.wizard.meta.util.extension.getRootName
import dev.wizard.meta.util.extension.remove
import dev.wizard.meta.util.threads.runSynchronized
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import java.util.*

object TrollHudGui : AbstractTrollGui() {
    override val alwaysTicking = true
    private val hudWindows = EnumMap<AbstractHudElement.Category, ListWindow>(AbstractHudElement.Category::class.java)

    override var searchString: String
        get() = super.searchString
        set(value) {
            super.searchString = value
            val string = value.remove(" ")
            if (string.isNotEmpty()) {
                setHudButtonVisibility { button ->
                    button.hudElement.allNames.any { it.contains(string, ignoreCase = true) }
                }
            } else {
                setHudButtonVisibility { true }
            }
        }

    init {
        var posX = 0.0f
        var posY = 0.0f
        for (category in AbstractHudElement.Category.entries) {
            val window = ListWindow(this, category.displayName, Component.UiSettingGroup.HUD_GUI)
            window.forcePosX = posX
            window.forcePosY = posY
            window.forceWidth = 80.0f
            window.forceHeight = 400.0f
            hudWindows[category] = window
            posX += 90.0f
            if (posX > Resolution.trollWidthF) {
                posX = 0.0f
                posY += 100.0f
            }
        }

        windows.runSynchronized {
            addAll(hudWindows.values)
        }

        listener<InputEvent.Keyboard> {
            if (!it.state || it.key == 0 || Keyboard.isKeyDown(61)) return@listener
            forEachWindow { child ->
                if (child is AbstractHudElement && child.bind.isDown(it.key)) {
                    child.visible = !child.visible
                }
            }
        }

        listener<Render2DEvent.Troll>(-1000) {
            if (mc.world == null || mc.player == null || mc.currentScreen == this || mc.gameSettings.hideGUI) return@listener
            if (HudEditor.enableHud) {
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1)
                forEachWindow { window ->
                    if (window is AbstractHudElement && window.visible) {
                        mc.mcProfiler.startSection(window.getRootName())
                        renderHudElement(window)
                        mc.mcProfiler.endSection()
                    }
                }
                GlStateUtils.depth(true)
            }
        }
    }

    fun register(hudElement: AbstractHudElement) {
        val button = HudButton(this, hudElement)
        hudWindows[hudElement.category]?.children?.add(button)
        windows.runSynchronized {
            addAndMoveToLast(hudElement)
        }
    }

    fun unregister(hudElement: AbstractHudElement) {
        hudWindows[hudElement.category]?.children?.removeIf { it is HudButton && it.hudElement == hudElement }
        windows.runSynchronized {
            remove(hudElement)
        }
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        setHudButtonVisibility { true }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1 || (HudEditor.bind.value.isDown(keyCode) && !searching)) {
            val lastClicked = lastClicked
            if (lastClicked?.keybordListening == null) {
                HudEditor.disable()
                return
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    private inline fun setHudButtonVisibility(function: (HudButton) -> Boolean) {
        hudWindows.values.asSequence()
            .flatMap { it.children.asSequence() }
            .filterIsInstance<HudButton>()
            .forEach {
                it.visible = function(it)
            }
    }

    private fun renderHudElement(window: AbstractHudElement) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(window.renderPosX, window.renderPosY, 0.0f)
        GlStateManager.scale(window.scale, window.scale, window.scale)
        window.renderHud()
        GlStateManager.popMatrix()
    }
}
