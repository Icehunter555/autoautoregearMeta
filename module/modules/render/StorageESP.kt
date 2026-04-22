package dev.wizard.meta.module.modules.render

import dev.fastmc.common.getSq
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.esp.*
import dev.wizard.meta.graphics.mask.SideMask
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.math.vector.distanceSqToCenter
import kotlinx.coroutines.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.*
import net.minecraft.tileentity.*
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.util.*

object StorageESP : Module(
    "StorageESP",
    category = Category.RENDER,
    description = "Draws an ESP on top of storage units"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.TYPE))

    private val chest by setting(this, BooleanSetting(settingName("Chest"), true, page.atValue(Page.TYPE)))
    private val shulker by setting(this, BooleanSetting(settingName("Shulker"), true, page.atValue(Page.TYPE)))
    private val enderChest by setting(this, BooleanSetting(settingName("Ender Chest"), true, page.atValue(Page.TYPE)))
    private val frame by setting(this, BooleanSetting(settingName("Item Frame"), true, page.atValue(Page.TYPE)))
    private val withShulkerOnly by setting(this, BooleanSetting(settingName("With Shulker Only"), true, page.atValue(Page.TYPE) and frame.atTrue()))
    private val furnace by setting(this, BooleanSetting(settingName("Furnace"), false, page.atValue(Page.TYPE)))
    private val dispenser by setting(this, BooleanSetting(settingName("Dispenser"), false, page.atValue(Page.TYPE)))
    private val hopper by setting(this, BooleanSetting(settingName("Hopper"), false, page.atValue(Page.TYPE)))
    private val cart by setting(this, BooleanSetting(settingName("Minecart"), false, page.atValue(Page.TYPE)))
    private val range by setting(this, FloatSetting(settingName("Range"), 64.0f, 8.0f..128.0f, 4.0f, page.atValue(Page.TYPE)))

    private val colorChest by setting(this, ColorSetting(settingName("Chest Color"), ColorRGB(255, 132, 32), true, page.atValue(Page.COLOR)))
    private val colorDispenser by setting(this, ColorSetting(settingName("Dispenser Color"), ColorRGB(160, 160, 160), true, page.atValue(Page.COLOR)))
    private val colorShulker by setting(this, ColorSetting(settingName("Shulker Color"), ColorRGB(220, 64, 220), true, page.atValue(Page.COLOR)))
    private val colorEnderChest by setting(this, ColorSetting(settingName("Ender Chest Color"), ColorRGB(137, 50, 184), true, page.atValue(Page.COLOR)))
    private val colorFurnace by setting(this, ColorSetting(settingName("Furnace Color"), ColorRGB(160, 160, 160), true, page.atValue(Page.COLOR)))
    private val colorHopper by setting(this, ColorSetting(settingName("Hopper Color"), ColorRGB(80, 80, 80), true, page.atValue(Page.COLOR)))
    private val colorCart by setting(this, ColorSetting(settingName("Cart Color"), ColorRGB(32, 250, 32), true, page.atValue(Page.COLOR)))
    private val colorFrame by setting(this, ColorSetting(settingName("Frame Color"), ColorRGB(255, 132, 32), true, page.atValue(Page.COLOR)))

    private val filled by setting(this, BooleanSetting(settingName("Filled"), true, page.atValue(Page.RENDER)))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true, page.atValue(Page.RENDER)))
    private val tracer by setting(this, BooleanSetting(settingName("Tracer"), true, page.atValue(Page.RENDER)))
    private val filledAlpha by setting(this, IntegerSetting(settingName("Filled Alpha"), 63, 0..255, 1, page.atValue(Page.RENDER) and filled.atTrue()))
    private val outlineAlpha by setting(this, IntegerSetting(settingName("Outline Alpha"), 200, 0..255, 1, page.atValue(Page.RENDER) and outline.atTrue()))
    private val tracerAlpha by setting(this, IntegerSetting(settingName("Tracer Alpha"), 200, 0..255, 1, page.atValue(Page.RENDER) and tracer.atTrue()))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 2.0f, 0.25f..5.0f, 0.25f, page.atValue(Page.RENDER)))

    private val dynamicBoxRenderer = DynamicBoxRenderer()
    private val staticBoxRenderer = StaticBoxRenderer()
    private val dynamicTracerRenderer = DynamicTracerRenderer()
    private val staticTracerRenderer = StaticTracerRenderer()

    override fun getHudInfo(): String = (dynamicBoxRenderer.size + staticBoxRenderer.size).toString()

    init {
        listener<Render3DEvent> {
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
            GlStateManager.glLineWidth(lineWidth)
            GlStateUtils.depth(false)

            val fAlpha = if (filled) filledAlpha else 0
            val oAlpha = if (outline) outlineAlpha else 0
            val tAlpha = if (tracer) tracerAlpha else 0

            dynamicBoxRenderer.render(fAlpha, oAlpha)
            staticBoxRenderer.render(fAlpha, oAlpha)
            dynamicTracerRenderer.render(tAlpha)
            staticTracerRenderer.render(tAlpha)

            GlStateUtils.depth(true)
            GlStateManager.glLineWidth(1.0f)
        }

        safeParallelListener<TickEvent.Post> {
            coroutineScope {
                launch { updateTileEntities(this@safeParallelListener) }
                launch { updateEntities(this@safeParallelListener) }
            }
        }
    }

    private fun updateTileEntities(event: SafeClientEvent) {
        val eyePos = EntityUtils.getEyePosition(event.player)
        val rangeSq = range.getSq()

        staticTracerRenderer.update {
            staticBoxRenderer.update {
                val list = event.world.loadedTileEntityList.toList()
                for (tileEntity in list) {
                    if (eyePos.distanceSqToCenter(tileEntity.pos) > rangeSq) continue
                    if (!checkTileEntityType(tileEntity)) continue
                    val color = getTileEntityColor(tileEntity)
                    if (color.rgba == 0) continue

                    val box = event.world.getBlockState(tileEntity.pos).getSelectedBoundingBox(event.world, tileEntity.pos) ?: continue
                    var sideMask = SideMask.ALL
                    if (tileEntity is TileEntityChest) {
                        if (tileEntity.adjacentChestZNeg != null) sideMask = sideMask and SideMask.NORTH.inv()
                        if (tileEntity.adjacentChestZPos != null) sideMask = sideMask and SideMask.SOUTH.inv()
                        if (tileEntity.adjacentChestXNeg != null) sideMask = sideMask and SideMask.WEST.inv()
                        if (tileEntity.adjacentChestXPos != null) sideMask = sideMask and SideMask.EAST.inv()
                    }
                    putBox(box, color, sideMask, SideMask.toOutlineMaskInv(sideMask))
                    this@update.putTracer(box, color)
                }
            }
        }
    }

    private fun checkTileEntityType(tileEntity: TileEntity): Boolean {
        return (chest && tileEntity is TileEntityChest) ||
                (dispenser && tileEntity is TileEntityDispenser) ||
                (shulker && tileEntity is TileEntityShulkerBox) ||
                (enderChest && tileEntity is TileEntityEnderChest) ||
                (furnace && tileEntity is TileEntityFurnace) ||
                (hopper && tileEntity is TileEntityHopper)
    }

    private fun getTileEntityColor(tileEntity: TileEntity): ColorRGB {
        return when (tileEntity) {
            is TileEntityChest -> colorChest
            is TileEntityDispenser -> colorDispenser
            is TileEntityShulkerBox -> colorShulker
            is TileEntityEnderChest -> colorEnderChest
            is TileEntityFurnace -> colorFurnace
            is TileEntityHopper -> colorHopper
            else -> ColorRGB(0, 0, 0, 0)
        }
    }

    private fun updateEntities(event: SafeClientEvent) {
        val eyePos = EntityUtils.getEyePosition(event.player)
        val rangeSq = range.getSq()

        dynamicTracerRenderer.update {
            dynamicBoxRenderer.update {
                for (entity in EntityManager.entity) {
                    if (entity.distanceSqTo(eyePos) > rangeSq || !checkEntityType(entity)) continue
                    val box = entity.entityBoundingBox ?: continue
                    val color = getEntityColor(entity)
                    if (color.rgba == 0) continue

                    val xOffset = entity.posX - entity.lastTickPosX
                    val yOffset = entity.posY - entity.lastTickPosY
                    val zOffset = entity.posZ - entity.lastTickPosZ
                    putBox(box, xOffset, yOffset, zOffset, color)
                    this@update.putTracer(entity.posX, entity.posY, entity.posZ, xOffset, yOffset, zOffset, color)
                }
            }
        }
    }

    private fun checkEntityType(entity: Entity): Boolean {
        return (frame && entity is EntityItemFrame && (!withShulkerOnly || entity.displayedItem.item is net.minecraft.item.ItemShulkerBox)) ||
                (cart && (entity is EntityMinecartChest || entity is EntityMinecartHopper || entity is EntityMinecartFurnace))
    }

    private fun getEntityColor(entity: Entity): ColorRGB {
        return when (entity) {
            is EntityMinecartContainer -> colorCart
            is EntityItemFrame -> colorFrame
            else -> ColorRGB(0, 0, 0, 0)
        }
    }

    private enum class Page { TYPE, COLOR, RENDER }
}
