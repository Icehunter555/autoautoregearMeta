package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.getItem
import dev.wizard.meta.util.inventory.setMeta
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.*
import java.util.*

object DyeSpoofer : Module(
    "DyeSpoofer",
    category = Category.RENDER,
    description = "changes the colors of shulkers, beds, and dyes"
) {
    val dyeColor by setting(this, EnumSetting(settingName("Dye Color"), DyeColor.DEFAULT))
    val bedColor by setting(this, EnumSetting(settingName("Bed Color"), DyeColor.DEFAULT))
    val shulkerColor by setting(this, EnumSetting(settingName("Shulker Color"), DyeColor.DEFAULT))
    val woolColor by setting(this, EnumSetting(settingName("Wool Color"), DyeColor.DEFAULT))
    val carpet by setting(this, BooleanSetting(settingName("Carpet"), true, { woolColor != DyeColor.DEFAULT }))
    val concreteColor by setting(this, EnumSetting(settingName("Concrete Color"), DyeColor.DEFAULT))
    val terracottaColor by setting(this, EnumSetting(settingName("Terracotta Color"), DyeColor.DEFAULT))
    val glassColor by setting(this, EnumSetting(settingName("Glass Color"), DyeColor.DEFAULT))

    @JvmStatic
    fun getBedBlockColor(): EnumDyeColor {
        return when (bedColor) {
            DyeColor.WHITE -> EnumDyeColor.WHITE
            DyeColor.ORANGE -> EnumDyeColor.ORANGE
            DyeColor.MAGENTA -> EnumDyeColor.MAGENTA
            DyeColor.LIGHTBLUE -> EnumDyeColor.LIGHT_BLUE
            DyeColor.YELLOW -> EnumDyeColor.YELLOW
            DyeColor.LIME -> EnumDyeColor.LIME
            DyeColor.PINK -> EnumDyeColor.PINK
            DyeColor.GRAY -> EnumDyeColor.GRAY
            DyeColor.LIGHTGRAY -> EnumDyeColor.SILVER
            DyeColor.CYAN -> EnumDyeColor.CYAN
            DyeColor.PURPLE -> EnumDyeColor.PURPLE
            DyeColor.BLUE -> EnumDyeColor.BLUE
            DyeColor.BROWN -> EnumDyeColor.BROWN
            DyeColor.GREEN -> EnumDyeColor.GREEN
            DyeColor.RED -> EnumDyeColor.RED
            DyeColor.BLACK -> EnumDyeColor.BLACK
            else -> EnumDyeColor.PURPLE
        }
    }

    @JvmStatic
    fun getShulkerBlockColor(): EnumDyeColor {
        return when (shulkerColor) {
            DyeColor.WHITE -> EnumDyeColor.WHITE
            DyeColor.ORANGE -> EnumDyeColor.ORANGE
            DyeColor.MAGENTA -> EnumDyeColor.MAGENTA
            DyeColor.LIGHTBLUE -> EnumDyeColor.LIGHT_BLUE
            DyeColor.YELLOW -> EnumDyeColor.YELLOW
            DyeColor.LIME -> EnumDyeColor.LIME
            DyeColor.PINK -> EnumDyeColor.PINK
            DyeColor.GRAY -> EnumDyeColor.GRAY
            DyeColor.LIGHTGRAY -> EnumDyeColor.SILVER
            DyeColor.CYAN -> EnumDyeColor.CYAN
            DyeColor.PURPLE -> EnumDyeColor.PURPLE
            DyeColor.BLUE -> EnumDyeColor.BLUE
            DyeColor.BROWN -> EnumDyeColor.BROWN
            DyeColor.GREEN -> EnumDyeColor.GREEN
            DyeColor.RED -> EnumDyeColor.RED
            DyeColor.BLACK -> EnumDyeColor.BLACK
            else -> EnumDyeColor.PURPLE
        }
    }

    @JvmStatic
    fun handleItemStack(stack: ItemStack): IBakedModel {
        val item = stack.item
        if (item == Items.DYE && dyeColor != DyeColor.DEFAULT) {
            val finalStack = ItemStack(item, stack.count, dyeColorToData(dyeColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if (item == Blocks.WOOL.getItem() && woolColor != DyeColor.DEFAULT) {
            val finalStack = ItemStack(item, stack.count, dyeColorToMeta(woolColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if (item == Blocks.CARPET.getItem() && woolColor != DyeColor.DEFAULT && carpet) {
            val finalStack = ItemStack(item, stack.count, dyeColorToMeta(woolColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if ((item == Blocks.CONCRETE.getItem() || item == Blocks.CONCRETE_POWDER.getItem()) && concreteColor != DyeColor.DEFAULT) {
            val finalStack = ItemStack(item, stack.count, dyeColorToMeta(concreteColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if (item == Blocks.HARDENED_CLAY.getItem() && terracottaColor != DyeColor.DEFAULT) {
            val finalStack = ItemStack(item, stack.count, dyeColorToMeta(terracottaColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if ((item == Blocks.STAINED_GLASS.getItem() || item == Blocks.STAINED_GLASS_PANE.getItem()) && glassColor != DyeColor.DEFAULT) {
            val finalStack = ItemStack(item, stack.count, dyeColorToMeta(glassColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if (item is ItemBed && bedColor != DyeColor.DEFAULT) {
            val finalStack = ItemStack(item, stack.count, dyeColorToData(bedColor))
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        if (item is ItemShulkerBox && shulkerColor != DyeColor.DEFAULT && !isInEnderChest()) {
            val finalStack = ItemStack(Item.getItemFromBlock(dyeColorToShulker()), stack.count)
            return mc.renderItem.itemModelMesher.getItemModel(finalStack)
        }

        return mc.renderItem.itemModelMesher.getItemModel(stack)
    }

    fun dyeColorToData(color: DyeColor): Int {
        return when (color) {
            DyeColor.WHITE -> EnumDyeColor.WHITE.dyeDamage
            DyeColor.ORANGE -> EnumDyeColor.ORANGE.dyeDamage
            DyeColor.MAGENTA -> EnumDyeColor.MAGENTA.dyeDamage
            DyeColor.LIGHTBLUE -> EnumDyeColor.LIGHT_BLUE.dyeDamage
            DyeColor.YELLOW -> EnumDyeColor.YELLOW.dyeDamage
            DyeColor.LIME -> EnumDyeColor.LIME.dyeDamage
            DyeColor.PINK -> EnumDyeColor.PINK.dyeDamage
            DyeColor.GRAY -> EnumDyeColor.GRAY.dyeDamage
            DyeColor.LIGHTGRAY -> EnumDyeColor.SILVER.dyeDamage
            DyeColor.CYAN -> EnumDyeColor.CYAN.dyeDamage
            DyeColor.PURPLE -> EnumDyeColor.PURPLE.dyeDamage
            DyeColor.BLUE -> EnumDyeColor.BLUE.dyeDamage
            DyeColor.BROWN -> EnumDyeColor.BROWN.dyeDamage
            DyeColor.GREEN -> EnumDyeColor.GREEN.dyeDamage
            DyeColor.RED -> EnumDyeColor.RED.dyeDamage
            DyeColor.BLACK -> EnumDyeColor.BLACK.dyeDamage
            else -> 0
        }
    }

    fun dyeColorToMeta(color: DyeColor): Int {
        return when (color) {
            DyeColor.WHITE -> EnumDyeColor.WHITE.metadata
            DyeColor.ORANGE -> EnumDyeColor.ORANGE.metadata
            DyeColor.MAGENTA -> EnumDyeColor.MAGENTA.metadata
            DyeColor.LIGHTBLUE -> EnumDyeColor.LIGHT_BLUE.metadata
            DyeColor.YELLOW -> EnumDyeColor.YELLOW.metadata
            DyeColor.LIME -> EnumDyeColor.LIME.metadata
            DyeColor.PINK -> EnumDyeColor.PINK.metadata
            DyeColor.GRAY -> EnumDyeColor.GRAY.metadata
            DyeColor.LIGHTGRAY -> EnumDyeColor.SILVER.metadata
            DyeColor.CYAN -> EnumDyeColor.CYAN.metadata
            DyeColor.PURPLE -> EnumDyeColor.PURPLE.metadata
            DyeColor.BLUE -> EnumDyeColor.BLUE.metadata
            DyeColor.BROWN -> EnumDyeColor.BROWN.metadata
            DyeColor.GREEN -> EnumDyeColor.GREEN.metadata
            DyeColor.RED -> EnumDyeColor.RED.metadata
            DyeColor.BLACK -> EnumDyeColor.BLACK.metadata
            else -> 0
        }
    }

    private fun dyeColorToShulker(): Block {
        return when (shulkerColor) {
            DyeColor.WHITE -> Blocks.WHITE_SHULKER_BOX
            DyeColor.ORANGE -> Blocks.ORANGE_SHULKER_BOX
            DyeColor.MAGENTA -> Blocks.MAGENTA_SHULKER_BOX
            DyeColor.LIGHTBLUE -> Blocks.LIGHT_BLUE_SHULKER_BOX
            DyeColor.YELLOW -> Blocks.YELLOW_SHULKER_BOX
            DyeColor.LIME -> Blocks.LIME_SHULKER_BOX
            DyeColor.PINK -> Blocks.PINK_SHULKER_BOX
            DyeColor.GRAY -> Blocks.GRAY_SHULKER_BOX
            DyeColor.LIGHTGRAY -> Blocks.SILVER_SHULKER_BOX
            DyeColor.CYAN -> Blocks.CYAN_SHULKER_BOX
            DyeColor.PURPLE -> Blocks.PURPLE_SHULKER_BOX
            DyeColor.BLUE -> Blocks.BLUE_SHULKER_BOX
            DyeColor.BROWN -> Blocks.BROWN_SHULKER_BOX
            DyeColor.GREEN -> Blocks.GREEN_SHULKER_BOX
            DyeColor.RED -> Blocks.RED_SHULKER_BOX
            DyeColor.BLACK -> Blocks.BLACK_SHULKER_BOX
            else -> Blocks.PURPLE_SHULKER_BOX
        }
    }

    @JvmStatic
    fun handleBlockState(state: IBlockState): IBlockState {
        val block = state.block
        if (block == Blocks.WOOL && woolColor != DyeColor.DEFAULT) {
            return Blocks.WOOL.setMeta(dyeColorToData(woolColor))
        }
        if (block == Blocks.CARPET && woolColor != DyeColor.DEFAULT && carpet) {
            return Blocks.CARPET.setMeta(dyeColorToData(woolColor))
        }
        if (block == Blocks.CONCRETE_POWDER && concreteColor != DyeColor.DEFAULT) {
            return Blocks.CONCRETE_POWDER.setMeta(dyeColorToData(concreteColor))
        }
        if (block == Blocks.CONCRETE && concreteColor != DyeColor.DEFAULT) {
            return Blocks.CONCRETE.setMeta(dyeColorToData(concreteColor))
        }
        if (block == Blocks.HARDENED_CLAY && terracottaColor != DyeColor.DEFAULT) {
            return Blocks.HARDENED_CLAY.setMeta(dyeColorToData(terracottaColor))
        }
        if (block == Blocks.STAINED_GLASS && glassColor != DyeColor.DEFAULT) {
            return Blocks.STAINED_GLASS.getStateFromMeta(dyeColorToData(glassColor))
        }
        if (block == Blocks.STAINED_GLASS_PANE && glassColor != DyeColor.DEFAULT) {
            return Blocks.STAINED_GLASS_PANE.getStateFromMeta(dyeColorToData(glassColor))
        }
        return state
    }

    fun isInEnderChest(): Boolean {
        val safe = SafeClientEvent.instance ?: return false
        val screen = safe.mc.currentScreen as? GuiContainer ?: return false
        val container = screen.inventorySlots
        return if (container is ContainerChest && container.lowerChestInventory is InventoryBasic) {
            container.lowerChestInventory.displayName.unformattedText.equals("Ender Chest", true)
        } else false
    }

    enum class DyeColor(override val displayName: CharSequence) : DisplayEnum {
        BLACK("Black"), RED("Red"), GREEN("Green"), BROWN("Brown"), BLUE("Blue"), PURPLE("Purple"),
        CYAN("Cyan"), LIGHTGRAY("Light Gray"), GRAY("Gray"), PINK("Pink"), LIME("Lime"),
        YELLOW("Yellow"), LIGHTBLUE("Light Blue"), MAGENTA("Magenta"), ORANGE("Orange"),
        WHITE("White"), DEFAULT("Default")
    }
}
