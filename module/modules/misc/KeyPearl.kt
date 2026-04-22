package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.firstItem
import dev.wizard.meta.util.inventory.slot.inventorySlots
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard
import kotlin.math.floor

object KeyPearl : Module(
    name = "KeyPearl",
    alias = arrayOf("MiddleClickPearl", "MCP"),
    category = Category.MISC,
    description = "auto pearl",
    modulePriority = 9999
) {
    private val resetDelay by setting("Reset Delay", 2000, 0..10000, 100)
    private val normalThrowMode by setting("Normal Throw Mode", ActionMode.OFF)
    private val normalThrowBind by setting("Normal Throw Key", Bind()) { normalThrowMode == ActionMode.KEY }
    private val normalThrowMouse by setting("Normal Throw Mouse Button", MouseButton.BTN3) { normalThrowMode == ActionMode.MOUSE }
    private val normalThrowModifier by setting("Normal Throw Modifier", ActionModifier.NONE) { normalThrowMode != ActionMode.OFF }

    private val enemyThrowMode by setting("Enemy Throw Mode", ActionMode.OFF)
    private val enemyThrowBind by setting("Enemy Throw Key", Bind()) { enemyThrowMode == ActionMode.KEY }
    private val enemyThrowMouse by setting("Enemy Throw Mouse Button", MouseButton.BTN4) { enemyThrowMode == ActionMode.MOUSE }
    private val enemyThrowModifier by setting("Enemy Throw Modifier", ActionModifier.NONE) { enemyThrowMode != ActionMode.OFF }
    private val enemyThrowRange by setting("Enemy Throw Range", 40.0f, 4.0f..100.0f, 5.0f) { enemyThrowMode != ActionMode.OFF }

    private val pearlPhaseMode by setting("Pearl Phase Mode", ActionMode.OFF)
    private val pearlPhaseBind by setting("Pearl Phase Key", Bind()) { pearlPhaseMode == ActionMode.KEY }
    private val pearlPhaseMouse by setting("Pearl Phase Mouse Button", MouseButton.BTN4) { pearlPhaseMode == ActionMode.MOUSE }
    private val pearlPhaseModifier by setting("Pearl Phase Modifier", ActionModifier.NONE) { pearlPhaseMode != ActionMode.OFF }
    private val pearlPhaseBypass by setting("Pearl Phase Bypass", true) { pearlPhaseMode != ActionMode.OFF }
    private val pearlPhaseRotation by setting("Pearl Phase Rotation Pitch", 81, 75..90, 1) { pearlPhaseMode != ActionMode.OFF }

    private var shouldThrowNormal = false
    private var shouldThrowEnemy = false
    private var shouldPearlPhase = false
    private val resetTimer = TickTimer(TimeUnit.MILLISECONDS)

    init {
        listener<InputEvent.Mouse> {
            if (it.state) return@listener
            val shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
            val controlHeld = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)

            if (normalThrowMode == ActionMode.MOUSE && it.button == normalThrowMouse.buttonId && checkModifier(normalThrowModifier, shiftHeld, controlHeld)) {
                shouldThrowNormal = true
            }
            if (enemyThrowMode == ActionMode.MOUSE && it.button == enemyThrowMouse.buttonId && checkModifier(enemyThrowModifier, shiftHeld, controlHeld)) {
                shouldThrowEnemy = true
            }
            if (pearlPhaseMode == ActionMode.MOUSE && it.button == pearlPhaseMouse.buttonId && checkModifier(pearlPhaseModifier, shiftHeld, controlHeld)) {
                shouldPearlPhase = true
            }
        }

        listener<InputEvent.Keyboard> {
            if (!it.state) return@listener
            val shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
            val controlHeld = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)

            if (normalThrowMode == ActionMode.KEY && normalThrowBind.isDown(it.key) && checkModifier(normalThrowModifier, shiftHeld, controlHeld)) {
                shouldThrowNormal = true
            }
            if (enemyThrowMode == ActionMode.KEY && enemyThrowBind.isDown(it.key) && checkModifier(enemyThrowModifier, shiftHeld, controlHeld)) {
                shouldThrowEnemy = true
            }
            if (pearlPhaseMode == ActionMode.KEY && pearlPhaseBind.isDown(it.key) && checkModifier(pearlPhaseModifier, shiftHeld, controlHeld)) {
                shouldPearlPhase = true
            }
        }

        listener<TickEvent.Post> {
            if (!resetTimer.tickAndReset(resetDelay.toLong())) return@listener

            if (shouldThrowNormal) {
                throwPearlNormal()
                shouldThrowNormal = false
            }
            if (shouldThrowEnemy) {
                throwPearlEnemy()
                shouldThrowEnemy = false
            }
            if (shouldPearlPhase) {
                pearlPhase()
                shouldPearlPhase = false
            }
        }

        onToggle {
            shouldThrowNormal = false
            shouldThrowEnemy = false
            shouldPearlPhase = false
        }
    }

    private fun checkModifier(required: ActionModifier, shiftHeld: Boolean, controlHeld: Boolean): Boolean {
        return when (required) {
            ActionModifier.NONE -> !shiftHeld && !controlHeld
            ActionModifier.SHIFT -> shiftHeld && !controlHeld
            ActionModifier.CONTROL -> controlHeld && !shiftHeld
        }
    }

    private fun throwPearlNormal() {
        val objectMouseOver = mc.objectMouseOver
        if (objectMouseOver == null || objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) {
            val pearlSlot = player.inventorySlots.firstItem(Items.ENDER_PEARL)
            if (pearlSlot == null) {
                NoSpamMessage.sendError("No Ender Pearl was Found!")
                return
            }
            HotbarSwitchManager.ghostSwitch(this, HotbarSwitchManager.Override.NONE, pearlSlot) {
                player.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
            }
        }
    }

    private fun getTarget(): EntityPlayer? {
        val possiblePlayers = world.playerEntities.filter {
            player.distanceTo(it) <= enemyThrowRange && !EntityUtils.isFriend(it) && !EntityUtils.isSelf(it)
        }
        
        return if (possiblePlayers.isEmpty()) null
        else possiblePlayers.minByOrNull { player.distanceTo(it) }
    }

    private fun throwPearlEnemy() {
        val target = getTarget() ?: return
        val pearlSlot = player.inventorySlots.firstItem(Items.ENDER_PEARL)
        if (pearlSlot == null) {
            NoSpamMessage.sendMessage("No Ender Pearl was found!")
            return
        }

        val rotation = RotationUtils.getRotationToEntity(target)
        HotbarSwitchManager.ghostSwitch(this, HotbarSwitchManager.Override.NONE, pearlSlot) {
            player.connection.sendPacket(CPacketPlayer.Rotation(rotation.x, rotation.y, player.onGround))
            player.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
        }
    }

    private fun pearlPhase() {
        val pearlSlot = player.inventorySlots.firstItem(Items.ENDER_PEARL)
        if (pearlSlot == null) {
            NoSpamMessage.sendMessage("No Ender Pearl was found!")
            return
        }

        val aimYaw = player.rotationYaw
        val aimPitch = pearlPhaseRotation
        val originalPitch = player.rotationPitch

        val obbySlot = player.inventorySlots.firstItem(Item.getItemFromBlock(Blocks.OBSIDIAN))
        if (pearlPhaseBypass && obbySlot != null) {
            val x = floor(player.posX).toInt()
            val z = floor(player.posZ).toInt()
            HotbarSwitchManager.ghostSwitch(this, obbySlot) {
                player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(BlockPos(x, 0, z), EnumFacing.DOWN, EnumHand.MAIN_HAND, x.toFloat(), -1.0f, z.toFloat()))
            }
        }

        HotbarSwitchManager.ghostSwitch(this, pearlSlot) {
            player.connection.sendPacket(CPacketPlayer.Rotation(aimYaw, aimPitch.toFloat(), player.onGround))
            playerController.processRightClick(player, world, EnumHand.MAIN_HAND)
            player.rotationPitch = originalPitch
        }
    }

    private enum class ActionMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off"),
        KEY("Keybind"),
        MOUSE("Mouse Button")
    }

    private enum class ActionModifier(override val displayName: CharSequence) : DisplayEnum {
        NONE("None"),
        SHIFT("Shift"),
        CONTROL("Control")
    }

    private enum class MouseButton(override val displayName: CharSequence, val buttonId: Int) : DisplayEnum {
        BTN1("Click", 0),
        BTN2("Right Click", 1),
        BTN3("Middle Click", 2),
        BTN4("Button 4", 3)
    }
}
