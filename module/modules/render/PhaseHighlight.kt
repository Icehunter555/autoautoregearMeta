package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.world.isFullBox
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.*

object PhaseHighlight : Module(
    "PhaseHighlight",
    category = Category.RENDER,
    description = "highlights phased/burrowed/clipped players"
) {
    private val range by setting(this, IntegerSetting(settingName("Range"), 15, 1..30, 1))
    private val friends by setting(this, BooleanSetting(settingName("Friends"), false))

    private val phaseColor by setting(this, ColorSetting(settingName("Phase Color"), ColorRGB(102, 0, 204), { !clipOnly }))
    private val clipColor by setting(this, ColorSetting(settingName("Clip Color"), ColorRGB(255, 255, 153)))
    private val burrowColor by setting(this, ColorSetting(settingName("Burrow Color"), ColorRGB(255, 178, 102), { !clipOnly }))
    private val headTrapColor by setting(this, ColorSetting(settingName("Head Trap Color"), ColorRGB(102, 178, 255), { !clipOnly }))

    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), Mode.TEXT))
    private val clipOnly by setting(this, BooleanSetting(settingName("Clip Only"), false))

    private val renderer = ESPRenderer().apply {
        setAFilled(31)
        setAOutline(255)
    }
    private val moniteredPlayers = ArrayList<EntityPlayer>()

    init {
        safeListener<Render2DEvent.Absolute> {
            moniteredPlayers.clear()
            for (entity in world.loadedEntityList) {
                if (entity !is EntityPlayer || entity == player) continue
                if (!friends && FriendManager.isFriend(entity.name)) continue
                if (player.getDistance(entity) > range) continue
                moniteredPlayers.add(entity)
            }

            if (renderMode != Mode.BOXES) {
                for (person in moniteredPlayers) {
                    val text = getText(this, person)
                    if (text.isEmpty()) continue

                    val center = person.positionVector.add(0.0, 0.5, 0.0)
                    val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                    val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                    val scale = (9.0f / Math.pow(1.5, distFactor).toFloat()).coerceAtLeast(1.0f)

                    val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                    val y = MainFontRenderer.getHeight(scale) * -0.5f
                    MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, getColor(this, person), scale)
                }
            }
        }

        safeListener<Render3DEvent> {
            if (renderMode != Mode.TEXT) {
                for (person in moniteredPlayers) {
                    val clippedBlocks = getClippedBlocks(this, person)
                    val color = getColor(this, person)
                    for (pos in clippedBlocks) {
                        renderer.add(AxisAlignedBB(pos), color)
                    }
                }
                renderer.render(true)
            }
        }
    }

    fun isClipped(event: SafeClientEvent, p: EntityPlayer): Boolean {
        val posX = p.posX - Math.floor(p.posX)
        val posZ = p.posZ - Math.floor(p.posZ)
        val isClippingX = Math.abs(posX - 0.23) < 0.02 || Math.abs(posX - 0.77) < 0.02 || Math.abs(posX - 0.241) < 0.02 || Math.abs(posX - 0.759) < 0.02
        val isClippingZ = Math.abs(posZ - 0.23) < 0.02 || Math.abs(posZ - 0.77) < 0.02 || Math.abs(posZ - 0.241) < 0.02 || Math.abs(posZ - 0.759) < 0.02
        return (isClippingX || isClippingZ) && getClippedBlocks(event, p).isNotEmpty()
    }

    fun getClippedBlocks(event: SafeClientEvent, p: EntityPlayer): List<BlockPos> {
        val clippedBlocks = ArrayList<BlockPos>()
        val bb = p.entityBoundingBox.grow(0.01, 0.0, 0.01)
        for (x in Math.floor(bb.minX).toInt()..Math.floor(bb.maxX).toInt()) {
            for (y in Math.floor(bb.minY).toInt()..Math.floor(bb.maxY).toInt()) {
                for (z in Math.floor(bb.minZ).toInt()..Math.floor(bb.maxZ).toInt()) {
                    val pos = BlockPos(x, y, z)
                    if (event.world.getBlockState(pos).isFullBox) {
                        clippedBlocks.add(pos)
                    }
                }
            }
        }
        return clippedBlocks
    }

    fun isBurrowed(event: SafeClientEvent, p: EntityPlayer): Boolean {
        val state = event.world.getBlockState(EntityUtils.getBetterPosition(p))
        return state.isFullBox && (state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK || state.block == Blocks.ENDER_CHEST)
    }

    fun isHeadBlock(event: SafeClientEvent, p: EntityPlayer): Boolean {
        val state = event.world.getBlockState(EntityUtils.getBetterPosition(p).up())
        return state.isFullBox && (state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK || state.block == Blocks.ENDER_CHEST)
    }

    fun getText(event: SafeClientEvent, p: EntityPlayer): String {
        return if (clipOnly && isClipped(event, p)) "Clipped"
        else if (isHeadBlock(event, p) && isBurrowed(event, p)) "Phased"
        else if (isHeadBlock(event, p) && !isBurrowed(event, p)) "HeadBlock"
        else if (!isHeadBlock(event, p) && isBurrowed(event, p)) "Burrowed"
        else if (isClipped(event, p)) "Clipped"
        else ""
    }

    fun getColor(event: SafeClientEvent, p: EntityPlayer): ColorRGB {
        return if (clipOnly && isClipped(event, p)) clipColor
        else if (isHeadBlock(event, p) && isBurrowed(event, p)) phaseColor
        else if (isHeadBlock(event, p) && !isBurrowed(event, p)) headTrapColor
        else if (!isHeadBlock(event, p) && isBurrowed(event, p)) burrowColor
        else if (isClipped(event, p)) clipColor
        else clipColor
    }

    private enum class Mode { BOXES, TEXT, BOTH }
}
