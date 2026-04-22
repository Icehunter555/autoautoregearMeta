package dev.wizard.meta.gui.hudgui.elements.hud

import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.ModuleToggleEvent
import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.gui.hudgui.TrollHudGui
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.delegate.FrameFloat
import it.unimi.dsi.fastutil.HashCommon
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.text.TextFormatting
import java.util.concurrent.CopyOnWriteArrayList

object Notification : HudElement("Notification", category = Category.HUD, description = "Client notifications") {

    private val moduleToggle by setting(this, "Module Toggle", true)
    private val moduleToggleMessageTimeout by setting(this, "Module Toggle Message Timeout", 3000, 0..10000, 100, visibility = { moduleToggle })
    private val defaultTimeout by setting(this, "Default Timeout", 5000, 0..10000, 100)
    val nvidia by setting(this, "Nvidia Theme", false)
    val backgroundAlpha by setting(this, "Background Alpha", 180, 0..255, 1, visibility = { nvidia })

    private val notifications = CopyOnWriteArrayList<Message>()
    private val map: Long2ObjectMap<Message> = Long2ObjectMaps.synchronize(Long2ObjectOpenHashMap())

    override val hudWidth get() = Message.minWidth
    override val hudHeight get() = Message.height

    init {
        safeListener<ModuleToggleEvent> {
            if (moduleToggle) {
                val status = if (it.module.isEnabled) "${TextFormatting.GREEN} enabled" else "${TextFormatting.RED} disabled"
                val message = "${it.module.nameAsString}$status"
                send((hashCode() * 31 + it.module.hashCode()).toLong(), message, moduleToggleMessageTimeout.toLong())
            }
        }
    }

    override fun renderHud() {
        if (mc.currentScreen == TrollHudGui && notifications.isEmpty()) {
            val posX = if (dockingH == HAlign.LEFT) 0.0f else Message.minWidth - Message.padding
            if (dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(Message.minWidth - Message.padding, 0.0f, Message.minWidth, Message.height, Message.color)
            } else {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, Message.padding, Message.height, Message.color)
            }
            MainFontRenderer.drawString("Example Notification", Message.stringPosX, Message.stringPosY, 0, 0.0f, false)
        }
    }

    fun render() {
        if (!visible) return
        GlStateUtils.pushMatrixAll()
        GlStateUtils.rescaleTroll()
        GlStateManager.translate(renderPosX, renderPosY, 0.0f)
        GlStateManager.scale(scale, scale, 0.0f)
        notifications.removeIf {
            GlStateManager.pushMatrix()
            val y = it.render()
            GlStateManager.popMatrix()
            if (y == -1.0f) {
                synchronized(map) {
                    if (map[it.id] == it) map.remove(it.id)
                }
                true
            } else {
                GlStateManager.translate(0.0f, y, 0.0f)
                false
            }
        }
        GlStateUtils.popMatrixAll()
    }

    fun send(message: String, length: Long = defaultTimeout.toLong()) {
        send(message.hashCode().toLong(), message, length)
    }

    fun send(identifier: Any, message: String, length: Long = defaultTimeout.toLong()) {
        send(identifier.hashCode().toLong(), message, length)
    }

    fun send(id: Long, message: String, length: Long = defaultTimeout.toLong()) {
        synchronized(map) {
            val existing = map[id]
            if (existing != null && !existing.isTimeout) {
                existing.update(message, length)
            } else {
                val newMessage = Message(message, length, id)
                map[id] = newMessage
                notifications.add(newMessage)
            }
        }
    }

    private class Message(var message: String, var length: Long, val id: Long) {
        val startTime by lazy { System.currentTimeMillis() }
        val width0 by FrameFloat {
            Math.max(Companion.minWidth, padding * 3 + MainFontRenderer.getWidth(message))
        }
        val width get() = width0

        val isTimeout get() = System.currentTimeMillis() - startTime > length

        fun update(message: String, length: Long) {
            this.message = message
            this.length = length + (System.currentTimeMillis() - startTime)
            width0.updateLazy()
        }

        fun render(): Float {
            if (INSTANCE.dockingH != HAlign.LEFT && width > INSTANCE.hudWidth) {
                GlStateManager.translate(INSTANCE.hudWidth - width, 0.0f, 0.0f)
            }
            val deltaTotal = Easing.toDelta(startTime)
            return when {
                deltaTotal in 0 until 300 -> {
                    val progress = Easing.OUT_CUBIC.inc(deltaTotal.toFloat() / 300.0f)
                    renderStage1(progress)
                }
                deltaTotal in 300 until 501 -> {
                    val progress = Easing.OUT_CUBIC.inc((deltaTotal - 300).toFloat() / 200.0f)
                    renderStage2(progress)
                }
                deltaTotal < length -> renderStage3()
                else -> {
                    val endDelta = deltaTotal - length
                    when {
                        endDelta in 0 until 200 -> {
                            val progress = Easing.OUT_CUBIC.dec(endDelta.toFloat() / 200.0f)
                            renderStage2(progress)
                        }
                        endDelta in 200 until 501 -> {
                            val progress = Easing.OUT_CUBIC.dec((endDelta - 200).toFloat() / 300.0f)
                            renderStage1(progress)
                        }
                        else -> -1.0f
                    }
                }
            }
        }

        private fun renderStage1(progress: Float): Float {
            if (INSTANCE.dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, width * progress, height, color)
            } else {
                RenderUtils2D.drawRectFilled(minWidth * (1.0f - progress), 0.0f, width, height, color)
            }
            return (height + space) * progress
        }

        private fun renderStage2(progress: Float): Float {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, width, height, backGroundColor)
            val textColor = ColorRGB(255, 255, 255, (255.0f * progress).toInt()).unbox()
            MainFontRenderer.drawString(message, stringPosX, stringPosY, textColor, 0.0f, false)
            if (INSTANCE.dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled((width - padding) * progress, 0.0f, width, height, color)
            } else {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, padding + (width - padding) * (1.0f - progress), height, color)
            }
            return height + space
        }

        private fun renderStage3(): Float {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, width, height, backGroundColor)
            if (INSTANCE.dockingH == HAlign.LEFT) {
                RenderUtils2D.drawRectFilled(width - padding, 0.0f, width, height, color)
            } else {
                RenderUtils2D.drawRectFilled(0.0f, 0.0f, padding, height, color)
            }
            MainFontRenderer.drawString(message, stringPosX, stringPosY, 0, 0.0f, false)
            return height + space
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Message) return false
            return id == other.id
        }

        override fun hashCode(): Int = HashCommon.long2int(id)

        companion object {
            val color get() = if (INSTANCE.nvidia) ColorRGB(118, 185, 0).unbox() else ColorRGB.alpha(ClickGUI.primary, 255)
            val backGroundColor get() = if (INSTANCE.nvidia) ColorRGB(0, 0, 0, INSTANCE.backgroundAlpha).unbox() else ClickGUI.backGround
            const val minWidth = 150.0f
            val height get() = MainFontRenderer.height * 4.0f
            const val space = 4.0f
            const val padding = 4.0f
            val stringPosX get() = if (INSTANCE.dockingH == HAlign.LEFT) padding else padding * 2
            val stringPosY get() = height * 0.5f - 1.0f - MainFontRenderer.height * 0.5f
        }
    }
}
