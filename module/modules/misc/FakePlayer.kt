package dev.wizard.meta.module.modules.misc

import com.mojang.authlib.GameProfile
import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.GuiEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.TextFormattingKt
import dev.wizard.meta.util.threads.onMainThread
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionEffect
import org.lwjgl.input.Keyboard
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

object FakePlayer : Module(
    name = "FakePlayer",
    category = Category.MISC,
    description = "Spawns a client sided fake player"
) {
    private val copyInventory by setting("Copy Inventory", true)
    private val copyPotions by setting("Copy Potions", true)
    private val maxArmor by setting("Max Armor", false)
    private val gappleEffects by setting("Gapple Effects", false)
    private val playerName by setting("Player Name", "Player")
    private val arrowMove by setting("Arrow Move", false)
    private val arrowMoveSpeed by setting("Arrow Move Speed", 0.1f, 0.01f..2.0f, 0.01f)

    private const val ENTITY_ID = -696969420
    private var fakePlayer: EntityOtherPlayerMP? = null

    override fun getHudInfo(): String {
        return playerName
    }

    init {
        onEnable {
            if (mc.player != null) {
                if (playerName == "Player") {
                    NoSpamMessage.sendMessage("You can use ${TextFormattingKt.formatValue("${CommandManager.prefix}set FakePlayer PlayerName <name>")} to set a custom name")
                }
                spawnFakePlayer()
            } else {
                disable()
            }
        }

        onDisable {
            onMainThread {
                fakePlayer?.setDead()
                mc.world?.removeEntityFromWorld(ENTITY_ID)
                fakePlayer = null
            }
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }

        listener<GuiEvent.Displayed> {
            if (it.screen is GuiGameOver) {
                disable()
            }
        }

        parallelListener<TickEvent.Post> {
            runSafeSuspend {
                val player = fakePlayer ?: return@runSafeSuspend
                if (!arrowMove) return@runSafeSuspend

                val movementInput = MovementUtils.calcMovementInput(
                    Keyboard.isKeyDown(Keyboard.KEY_UP),
                    Keyboard.isKeyDown(Keyboard.KEY_DOWN),
                    Keyboard.isKeyDown(Keyboard.KEY_LEFT),
                    Keyboard.isKeyDown(Keyboard.KEY_RIGHT),
                    Keyboard.isKeyDown(Keyboard.KEY_RSHIFT),
                    Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                )

                val moveYaw = MovementUtils.calcMoveYaw(EntityUtils.getViewEntity(this).rotationYaw, movementInput.z, movementInput.x)
                val xzMotion = if (movementInput.x == 0.0f && movementInput.z == 0.0f) 0.0f else arrowMoveSpeed
                val yMotion = if (movementInput.y == 0.0f) 0.0f else arrowMoveSpeed

                player.setPosition(
                    player.posX + -sin(moveYaw) * xzMotion,
                    player.posY + movementInput.y * yMotion,
                    player.posZ + cos(moveYaw) * xzMotion
                )
            }
        }
    }

    private fun spawnFakePlayer() {
        val newPlayer = EntityOtherPlayerMP(mc.world, GameProfile(UUID.randomUUID(), playerName))
        val viewEntity = mc.renderViewEntity ?: mc.player!!
        
        newPlayer.copyLocationAndAnglesFrom(viewEntity)
        newPlayer.rotationYawHead = viewEntity.rotationYawHead

        if (copyInventory) {
            newPlayer.inventory.copyInventory(mc.player.inventory)
        }

        if (copyPotions) {
            copyPotions(newPlayer, mc.player)
        }

        if (maxArmor) {
            addMaxArmor(newPlayer)
        }

        if (gappleEffects) {
            addGappleEffects(newPlayer)
        }

        onMainThread {
            mc.world.addEntityToWorld(ENTITY_ID, newPlayer)
        }
        fakePlayer = newPlayer
    }

    private fun copyPotions(target: EntityPlayer, source: EntityPlayer) {
        for (effect in source.activePotionEffects) {
            target.addPotionEffect(PotionEffect(effect.potion, Int.MAX_VALUE, effect.amplifier))
        }
    }

    private fun addMaxArmor(player: EntityPlayer) {
        val helmet = ItemStack(Items.DIAMOND_HELMET)
        helmet.addEnchantment(Enchantments.PROTECTION, 4)
        helmet.addEnchantment(Enchantments.UNBREAKING, 3)
        helmet.addEnchantment(Enchantments.RESPIRATION, 3)
        helmet.addEnchantment(Enchantments.AQUA_AFFINITY, 1)
        helmet.addEnchantment(Enchantments.MENDING, 1)
        player.inventory.armorInventory[3] = helmet

        val chestplate = ItemStack(Items.DIAMOND_CHESTPLATE)
        chestplate.addEnchantment(Enchantments.PROTECTION, 4)
        chestplate.addEnchantment(Enchantments.UNBREAKING, 3)
        chestplate.addEnchantment(Enchantments.MENDING, 1)
        player.inventory.armorInventory[2] = chestplate

        val leggings = ItemStack(Items.DIAMOND_LEGGINGS)
        leggings.addEnchantment(Enchantments.BLAST_PROTECTION, 4)
        leggings.addEnchantment(Enchantments.UNBREAKING, 3)
        leggings.addEnchantment(Enchantments.MENDING, 1)
        player.inventory.armorInventory[1] = leggings

        val boots = ItemStack(Items.DIAMOND_BOOTS)
        boots.addEnchantment(Enchantments.PROTECTION, 4)
        boots.addEnchantment(Enchantments.FEATHER_FALLING, 4)
        boots.addEnchantment(Enchantments.DEPTH_STRIDER, 3)
        boots.addEnchantment(Enchantments.UNBREAKING, 3)
        boots.addEnchantment(Enchantments.MENDING, 1)
        player.inventory.armorInventory[0] = boots
    }

    private fun addGappleEffects(player: EntityPlayer) {
        player.addPotionEffect(PotionEffect(MobEffects.REGENERATION, Int.MAX_VALUE, 1))
        player.addPotionEffect(PotionEffect(MobEffects.ABSORPTION, Int.MAX_VALUE, 3))
        player.addPotionEffect(PotionEffect(MobEffects.RESISTANCE, Int.MAX_VALUE, 0))
        player.addPotionEffect(PotionEffect(MobEffects.FIRE_RESISTANCE, Int.MAX_VALUE, 0))
    }
}