package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.BedAura
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.or
import dev.wizard.meta.util.accessor.setSide
import dev.wizard.meta.util.accessor.side
import net.minecraft.block.*
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.EnumAction
import net.minecraft.item.Item
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPickaxe
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard

object Interactions : Module(
    "Interactions",
    alias = arrayOf("LiquidInteract", "NoEntityTrace", "NoMiningTrace"),
    category = Category.PLAYER,
    description = "Modifies block interaction"
) {
    private val prioritizeEating by setting(this, BooleanSetting(settingName("Prioritize Eating"), true))
    private val noAnvilInteract by setting(this, BooleanSetting(settingName("No Anvil Interact"), false))
    private val noEnderChestInteract by setting(this, BooleanSetting(settingName("No EnderChest Interact"), false))
    private val noDragonEggInteract by setting(this, BooleanSetting(settingName("No Dragon Egg"), false))
    private val onlyShulker by setting(this, BooleanSetting(settingName("Only Shulker"), false))
    private val onlyOnBa by setting(this, BooleanSetting(settingName("Only On BedAura"), false, { noAnvilInteract || noEnderChestInteract || onlyShulker }))
    private val onlyOnEat by setting(this, BooleanSetting(settingName("Only When Eating"), false, { noAnvilInteract || noEnderChestInteract || onlyShulker }))
    private val noAirHit by setting(this, BooleanSetting(settingName("No Air Hit"), true))
    private val liquidInteract by setting(this, BooleanSetting(settingName("Liquid Interact"), false, description = "Place block on liquid"))
    private val buildLimitBypass by setting(this, BooleanSetting(settingName("Build Limit AntiCheat"), false))

    private val noEntityTrace by setting(this, BooleanSetting(settingName("No Entity Trace"), true, description = "Interact with blocks through entity"))
    private val checkBlocks by setting(this, BooleanSetting(settingName("Check Blocks"), true, noEntityTrace.atTrue(), description = "Only ignores entity when there is block behind"))
    private val anyItems by setting(this, BooleanSetting(settingName("Any Items"), false, noEntityTrace.atTrue()))
    private val pickaxe by setting(this, BooleanSetting(settingName("Pickaxe"), true, noEntityTrace.atTrue() and anyItems.atFalse(), description = "Ignores entity when holding pickaxe"))
    private val gapple by setting(this, BooleanSetting(settingName("Gapple"), true, noEntityTrace.atTrue() and anyItems.atFalse(), description = "Ignores entity when holding gapple"))
    private val crystal by setting(this, BooleanSetting(settingName("Crystal"), true, noEntityTrace.atTrue() and anyItems.atFalse(), description = "Ignores entity when holding crystal"))
    private val totem by setting(this, BooleanSetting(settingName("Totem"), true, noEntityTrace.atTrue() and anyItems.atFalse(), description = "Ignores entity when holding totem"))
    private val freecam by setting(this, BooleanSetting(settingName("Freecam"), true, noEntityTrace.atTrue() and anyItems.atFalse(), description = "Ignores entity in Freecam"))
    private val sneakOverrides by setting(this, BooleanSetting(settingName("Sneak Override"), true, noEntityTrace.atTrue() and anyItems.atFalse(), description = "Ignores entity when sneaking"))
    private val reverseOverride by setting(this, BooleanSetting(settingName("Reverse Override"), false))

    private val interactableBlocks = listOf(
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.ANVIL, Blocks.DRAGON_EGG,
        Blocks.CRAFTING_TABLE, Blocks.HOPPER, Blocks.FURNACE, Blocks.BURNING_FURNACE, Blocks.DISPENSER,
        Blocks.DROPPER, Blocks.JUKEBOX, Blocks.ENCHANTING_TABLE, Blocks.BREWING_STAND, Blocks.BEACON,
        Blocks.CAULDRON, Blocks.FLOWER_POT, Blocks.STANDING_SIGN, Blocks.WALL_SIGN, Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.SILVER_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.BED,
        Blocks.WOODEN_BUTTON, Blocks.STONE_BUTTON, Blocks.WOODEN_DOOR, Blocks.IRON_DOOR,
        Blocks.WOODEN_TRAPDOOR, Blocks.IRON_TRAPDOOR, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE,
        Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.ACACIA_FENCE_GATE,
        Blocks.LEVER
    )

    @JvmStatic
    fun isNoAirHitEnabled(): Boolean = isEnabled && noAirHit

    @JvmStatic
    fun isLiquidInteractEnabled(): Boolean = isEnabled && liquidInteract

    @JvmStatic
    fun isNoEntityTraceEnabled(): Boolean {
        if (INSTANCE.isDisabled || !INSTANCE.noEntityTrace) return false

        val sneaking = mc.gameSettings.keyBindSneak.isKeyDown
        if (INSTANCE.reverseOverride && sneaking) return false

        if (INSTANCE.checkBlocks) {
            val hit = mc.objectMouseOver
            if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return false
        }

        if (INSTANCE.sneakOverrides && sneaking) return true
        if (INSTANCE.freecam && Freecam.isEnabled) return true

        val item = mc.player?.heldItemMainhand?.item
        return checkItem(item)
    }

    private fun checkItem(item: Item?): Boolean {
        if (anyItems) return true
        return when (item) {
            Items.GOLDEN_APPLE -> gapple
            Items.END_CRYSTAL -> crystal
            Items.TOTEM_OF_UNDYING -> totem
            is ItemPickaxe -> pickaxe
            else -> false
        }
    }

    @JvmStatic
    fun isPrioritizingEating(): Boolean {
        if (INSTANCE.isDisabled || !INSTANCE.prioritizeEating) return false

        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU) ||
            Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return false

        val player = mc.player ?: return false
        if (player.isCreative) return false

        return EnumHand.entries.any { player.getHeldItem(it).item is ItemFood }
    }

    @JvmStatic
    fun shouldCancel(): Boolean {
        if (INSTANCE.isDisabled) return false
        if (!INSTANCE.onlyShulker && !INSTANCE.noAnvilInteract && !INSTANCE.noEnderChestInteract && !INSTANCE.noDragonEggInteract) return false

        if (INSTANCE.onlyOnBa && !BedAura.isEnabled) return false

        if (INSTANCE.onlyOnEat) {
            val player = mc.player ?: return false
            if (!(player.isHandActive && player.activeItemStack.itemUseAction == EnumAction.EAT)) return false
        }

        val hit = mc.objectMouseOver ?: return false
        val pos = hit.blockPos ?: return false
        val block = mc.world?.getBlockState(pos)?.block ?: return false

        if (INSTANCE.noAnvilInteract && block is BlockAnvil) return true
        if (INSTANCE.noEnderChestInteract && block is BlockEnderChest) return true
        if (INSTANCE.noDragonEggInteract && block is BlockDragonEgg) return true

        if (INSTANCE.onlyShulker && interactableBlocks.contains(block)) return true

        return false
    }

    init {
        listener<PacketEvent.Send> {
            val packet = it.packet
            if (buildLimitBypass && packet is CPacketPlayerTryUseItemOnBlock && packet.pos.y == 255 && packet.side == EnumFacing.UP) {
                packet.setSide(EnumFacing.DOWN)
            }
        }
    }
}
