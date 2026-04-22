package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.gui.AbstractTrollGui
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.util.MovementInputFromOptions
import org.lwjgl.input.Keyboard

object InventoryMove : Module(
    name = "InventoryMove",
    category = Category.MOVEMENT,
    description = "Allows you to walk around with GUIs opened",
    priority = 1010
) {
    private val rotateSpeed by setting("Rotate Speed", 5, 0..20, 1)
    val sneak by setting("Sneak", false)
    private var hasSent = false

    private fun isInvalidGui(guiScreen: GuiScreen?): Boolean {
        return guiScreen == null || guiScreen is GuiChat || guiScreen is GuiEditSign || guiScreen is GuiRepair || (guiScreen is AbstractTrollGui && guiScreen.searching)
    }

    init {
        listener<InputUpdateEvent>(9999) { event ->
            if (it.movementInput !is MovementInputFromOptions || isInvalidGui(mc.currentScreen)) return@listener

            if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                player.rotationYaw -= rotateSpeed.toFloat()
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                player.rotationYaw += rotateSpeed.toFloat()
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                player.rotationPitch = Math.max(player.rotationPitch - rotateSpeed.toFloat(), -90.0f)
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                player.rotationPitch = Math.min(player.rotationPitch + rotateSpeed.toFloat(), 90.0f)
            }

            event.movementInput.moveStrafe = 0.0f
            event.movementInput.moveForward = 0.0f

            try {
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)) {
                    event.movementInput.moveForward += 1.0f
                    event.movementInput.forwardKeyDown = true
                } else {
                    event.movementInput.forwardKeyDown = false
                }
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.keyCode)) {
                    event.movementInput.moveForward -= 1.0f
                    event.movementInput.backKeyDown = true
                } else {
                    event.movementInput.backKeyDown = false
                }
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.keyCode)) {
                    event.movementInput.moveStrafe += 1.0f
                    event.movementInput.leftKeyDown = true
                } else {
                    event.movementInput.leftKeyDown = false
                }
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.keyCode)) {
                    event.movementInput.moveStrafe -= 1.0f
                    event.movementInput.rightKeyDown = true
                } else {
                    event.movementInput.rightKeyDown = false
                }
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.keyCode)) {
                    event.movementInput.jump = true
                }
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode) && sneak) {
                    event.movementInput.sneak = true
                }
            } catch (e: IndexOutOfBoundsException) {
                if (!hasSent) {
                    MetaMod.logger.error("${getChatName()} Error: Key is bound to a mouse button!", e)
                    hasSent = true
                }
            }
        }
    }
}
