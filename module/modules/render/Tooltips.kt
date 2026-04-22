package dev.wizard.meta.module.modules.render

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.Bind
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.inventory.ItemStackHelper
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.NonNullList
import org.lwjgl.opengl.GL20

object Tooltips : Module(
    "Tooltips",
    category = Category.RENDER,
    description = "Enhanced tooltips for various items"
) {
    private val shulkerEnabled by setting(this, BooleanSetting(settingName("Shulker Tooltips"), true))
    val peekBind by setting(this, BindSetting(settingName("Peek Bind"), Bind(), visibility = { shulkerEnabled }))

    private val itemRenderer = mc.renderItem
    private val fontRenderer = mc.fontRenderer

    @JvmStatic
    fun isShulkerEnabled(): Boolean = isEnabled && shulkerEnabled

    @JvmStatic
    fun renderShulkerAndItems(stack: ItemStack, originalX: Int, originalY: Int, tagCompound: NBTTagCompound) {
        val shulkerInventory = NonNullList.withSize(27, ItemStack.EMPTY)
        ItemStackHelper.loadAllItems(tagCompound, shulkerInventory)

        GlStateManager.enableBlend()
        GlStateManager.disableRescaleNormal()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()

        INSTANCE.renderShulker(stack, originalX, originalY)

        GlStateManager.enableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
        RenderHelper.enableGUIStandardItemLighting()

        INSTANCE.renderShulkerItems(shulkerInventory, originalX, originalY)

        RenderHelper.disableStandardItemLighting()
        itemRenderer.zLevel = 0.0f
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
        RenderHelper.enableStandardItemLighting()
        GlStateManager.enableRescaleNormal()
    }

    @JvmStatic
    fun drawPeekGui(stack: ItemStack, name: String?) {
        try {
            val item = stack.item as? ItemShulkerBox ?: return
            val entityBox = TileEntityShulkerBox()
            entityBox.world = mc.world

            stack.tagCompound?.getCompoundTag("BlockEntityTag")?.let { nbt ->
                val itemsField = TileEntityShulkerBox::class.java.getDeclaredField("items")
                itemsField.isAccessible = true
                val items = itemsField.get(entityBox) as NonNullList<ItemStack>
                ItemStackHelper.loadAllItems(nbt, items)
                entityBox.readFromNBT(nbt)
            }

            val customNameField = net.minecraft.tileentity.TileEntityLockableLoot::class.java.getDeclaredField("customName")
            customNameField.isAccessible = true
            customNameField.set(entityBox, name ?: stack.displayName)

            Thread {
                try {
                    Thread.sleep(200L)
                } catch (e: InterruptedException) {
                }
                mc.player.displayGUIChest(entityBox)
            }.start()
        } catch (e: Exception) {
        }
    }

    @JvmStatic
    fun getShulkerData(stack: ItemStack): NBTTagCompound? {
        if (stack.item !is ItemShulkerBox) return null
        val tagCompound = stack.tagCompound ?: return null
        if (tagCompound.hasKey("BlockEntityTag", 10)) {
            val blockEntityTag = tagCompound.getCompoundTag("BlockEntityTag")
            if (blockEntityTag.hasKey("Items", 9)) return blockEntityTag
        }
        return null
    }

    private fun renderShulker(stack: ItemStack, originalX: Int, originalY: Int) {
        val width = Math.max(144, fontRenderer.getStringWidth(stack.displayName) + 3)
        val x = originalX + 12
        val y = originalY - 12
        val height = 57

        itemRenderer.zLevel = 300.0f

        drawGradientRect(x - 3, y - 4, x + width + 3, y - 3, -267386864, -267386864)
        drawGradientRect(x - 3, y + height + 3, x + width + 3, y + height + 4, -267386864, -267386864)
        drawGradientRect(x - 3, y - 3, x + width + 3, y + height + 3, -267386864, -267386864)
        drawGradientRect(x - 4, y - 3, x - 3, y + height + 3, -267386864, -267386864)
        drawGradientRect(x + width + 3, y - 3, x + width + 4, y + height + 3, -267386864, -267386864)

        drawGradientRect(x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, 0x505000FF, 1344798847)
        drawGradientRect(x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, 0x505000FF, 1344798847)
        drawGradientRect(x - 3, y - 3, x + width + 3, y - 3 + 1, 0x505000FF, 0x505000FF)
        drawGradientRect(x - 3, y + height + 2, x + width + 3, y + height + 3, 1344798847, 1344798847)

        fontRenderer.drawStringWithShadow(stack.displayName, x.toFloat(), y.toFloat(), 0xFFFFFF)
    }

    private fun renderShulkerItems(shulkerInventory: NonNullList<ItemStack>, originalX: Int, originalY: Int) {
        GL20.glUseProgram(0)
        for (i in shulkerInventory.indices) {
            val x = originalX + (i % 9) * 16 + 11
            val y = originalY + (i / 9) * 16 - 11 + 8
            val itemStack = shulkerInventory[i]
            itemRenderer.renderItemAndEffectIntoGUI(itemStack, x, y)
            itemRenderer.renderItemOverlayIntoGUI(fontRenderer, itemStack, x, y, null)
        }
    }

    private fun drawGradientRect(left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int) {
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
        GlStateManager.shadeModel(7425)

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR)

        colorVertex(buffer, right, top, startColor)
        colorVertex(buffer, left, top, startColor)
        colorVertex(buffer, left, bottom, endColor)
        colorVertex(buffer, right, bottom, endColor)

        tessellator.draw()

        GlStateManager.shadeModel(7424)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
    }

    private fun colorVertex(buffer: BufferBuilder, x: Int, y: Int, color: Int) {
        buffer.pos(x.toDouble(), y.toDouble(), 300.0)
            .color((color shr 16 and 255) / 255.0f, (color shr 8 and 255) / 255.0f, (color and 255) / 255.0f, (color shr 24 and 255) / 255.0f)
            .endVertex()
    }
}
