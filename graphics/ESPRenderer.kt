package dev.wizard.meta.graphics

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.modules.client.ClickGUI
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*

class ESPRenderer {
    var toRender = ArrayList<Info>()
        private set

    var aFilled: Int = 0
    var aOutline: Int = 0
    var aTracer: Int = 0
    var thickness: Float = 2.0f
    var through: Boolean = true
    var tracerOffset: Int = 50

    val size: Int get() = toRender.size

    fun add(entity: Entity, color: ColorRGB, sides: Int = 63) {
        val partialTicks = 1.0f - RenderUtils3D.getPartialTicks()
        val x = (entity.prevPosX - entity.posX) * partialTicks.toDouble()
        val y = (entity.prevPosY - entity.posY) * partialTicks.toDouble()
        val z = (entity.prevPosZ - entity.posZ) * partialTicks.toDouble()
        val interpolatedBox = entity.entityBoundingBox.offset(x, y, z)
        add(interpolatedBox, color, sides)
    }

    fun add(pos: BlockPos, color: ColorRGB, sides: Int = 63) {
        add(AxisAlignedBB(pos), color, sides)
    }

    fun add(box: AxisAlignedBB, color: ColorRGB, sides: Int = 63) {
        add(Info(box, color, sides))
    }

    fun add(info: Info) {
        toRender.add(info)
    }

    fun replaceAll(list: List<Info>) {
        toRender = ArrayList(list)
    }

    fun addGradient(
        pos: BlockPos,
        colorStart: ColorRGB,
        colorEnd: ColorRGB = ClickGUI.primary,
        sides: Int = 63,
        vertical: Boolean = true
    ) {
        add(Info(AxisAlignedBB(pos), ColorRGB(0), sides, Info.Gradient(colorStart, colorEnd, vertical)))
    }

    fun addGradient(
        box: AxisAlignedBB,
        colorStart: ColorRGB,
        colorEnd: ColorRGB = ClickGUI.primary,
        sides: Int = 63,
        vertical: Boolean = true
    ) {
        add(Info(box, ColorRGB(0), sides, Info.Gradient(colorStart, colorEnd, vertical)))
    }

    fun addTripleGradient(
        pos: BlockPos,
        colorStart: ColorRGB,
        colorMiddle: ColorRGB,
        colorEnd: ColorRGB,
        sides: Int = 63,
        vertical: Boolean = true,
        progress: Float = 0.0f
    ) {
        add(Info(AxisAlignedBB(pos), ColorRGB(0), sides, null, Info.TripleGradient(colorStart, colorMiddle, colorEnd, vertical, progress)))
    }

    fun addTripleGradient(
        box: AxisAlignedBB,
        colorStart: ColorRGB,
        colorMiddle: ColorRGB,
        colorEnd: ColorRGB,
        sides: Int = 63,
        vertical: Boolean = true,
        progress: Float = 0.0f
    ) {
        add(Info(box, ColorRGB(0), sides, null, Info.TripleGradient(colorStart, colorMiddle, colorEnd, vertical, progress)))
    }

    fun addTripleGradient(
        entity: Entity,
        colorStart: ColorRGB,
        colorMiddle: ColorRGB,
        colorEnd: ColorRGB,
        sides: Int = 63,
        vertical: Boolean = true,
        progress: Float = 0.0f
    ) {
        val partialTicks = 1.0f - RenderUtils3D.getPartialTicks()
        val x = (entity.prevPosX - entity.posX) * partialTicks.toDouble()
        val y = (entity.prevPosY - entity.posY) * partialTicks.toDouble()
        val z = (entity.prevPosZ - entity.posZ) * partialTicks.toDouble()
        val interpolatedBox = entity.entityBoundingBox.offset(x, y, z)
        add(Info(interpolatedBox, ColorRGB(0), sides, null, Info.TripleGradient(colorStart, colorMiddle, colorEnd, vertical, progress)))
    }

    fun clear() {
        toRender.clear()
    }

    fun render(clear: Boolean) {
        val filled = aFilled != 0
        val outline = aOutline != 0
        val tracer = aTracer != 0

        if (toRender.isEmpty() || (!filled && !outline && !tracer)) return

        if (through) {
            GlStateManager.disableDepth()
        }

        GlStateManager.glLineWidth(thickness)

        if (filled) {
            for (info in toRender) {
                val box = info.box
                val color = info.color
                val sides = info.sides
                val gradient = info.gradient
                val tripleGradient = info.tripleGradient

                if (tripleGradient != null) {
                    val aStart = (aFilled * (tripleGradient.start.aFloat)).toInt()
                    val aMiddle = (aFilled * (tripleGradient.middle.aFloat)).toInt()
                    val aEnd = (aFilled * (tripleGradient.end.aFloat)).toInt()
                    if (tripleGradient.vertical) {
                        RenderUtils3D.INSTANCE.drawTripleVerticalGradientBox(
                            box,
                            tripleGradient.start.withAlpha(aStart),
                            tripleGradient.middle.withAlpha(aMiddle),
                            tripleGradient.end.withAlpha(aEnd),
                            sides,
                            tripleGradient.progress
                        )
                    } else {
                        RenderUtils3D.INSTANCE.drawTripleHorizontalGradientBox(
                            box,
                            tripleGradient.start.withAlpha(aStart),
                            tripleGradient.middle.withAlpha(aMiddle),
                            tripleGradient.end.withAlpha(aEnd),
                            sides,
                            tripleGradient.progress
                        )
                    }
                    continue
                }

                if (gradient != null) {
                    val aStart = (aFilled * (gradient.start.aFloat)).toInt()
                    val aEnd = (aFilled * (gradient.end.aFloat)).toInt()
                    if (gradient.vertical) {
                        RenderUtils3D.INSTANCE.drawVerticalGradientBox(
                            box,
                            gradient.start.withAlpha(aStart),
                            gradient.end.withAlpha(aEnd),
                            sides
                        )
                    } else {
                        RenderUtils3D.INSTANCE.drawHorizontalGradientBox(
                            box,
                            gradient.start.withAlpha(aStart),
                            gradient.end.withAlpha(aEnd),
                            sides
                        )
                    }
                    continue
                }

                val a = (aFilled * (color.aFloat)).toInt()
                RenderUtils3D.INSTANCE.drawBox(box, color.withAlpha(a), sides)
            }
            RenderUtils3D.INSTANCE.draw(7) // GL_QUADS
        }

        if (outline || tracer) {
            if (outline) {
                for (info in toRender) {
                    val box = info.box
                    val color = info.color
                    val gradient = info.gradient
                    val tripleGradient = info.tripleGradient

                    if (tripleGradient != null) {
                        val aStart = (aOutline * (tripleGradient.start.aFloat)).toInt()
                        val aMiddle = (aOutline * (tripleGradient.middle.aFloat)).toInt()
                        val aEnd = (aOutline * (tripleGradient.end.aFloat)).toInt()
                        if (tripleGradient.vertical) {
                            RenderUtils3D.INSTANCE.drawTripleVerticalGradientOutline(
                                box,
                                tripleGradient.start.withAlpha(aStart),
                                tripleGradient.middle.withAlpha(aMiddle),
                                tripleGradient.end.withAlpha(aEnd),
                                tripleGradient.progress
                            )
                        } else {
                            RenderUtils3D.INSTANCE.drawTripleHorizontalGradientOutline(
                                box,
                                tripleGradient.start.withAlpha(aStart),
                                tripleGradient.middle.withAlpha(aMiddle),
                                tripleGradient.end.withAlpha(aEnd),
                                tripleGradient.progress
                            )
                        }
                        continue
                    }

                    if (gradient != null) {
                        val aStart = (aOutline * (gradient.start.aFloat)).toInt()
                        val aEnd = (aOutline * (gradient.end.aFloat)).toInt()
                        if (gradient.vertical) {
                            RenderUtils3D.INSTANCE.drawVerticalGradientOutline(
                                box,
                                gradient.start.withAlpha(aStart),
                                gradient.end.withAlpha(aEnd)
                            )
                        } else {
                            RenderUtils3D.INSTANCE.drawHorizontalGradientOutline(
                                box,
                                gradient.start.withAlpha(aStart),
                                gradient.end.withAlpha(aEnd)
                            )
                        }
                        continue
                    }

                    val a = (aOutline * (color.aFloat)).toInt()
                    RenderUtils3D.INSTANCE.drawOutline(box, color.withAlpha(a))
                }
            }

            if (tracer) {
                for (info in toRender) {
                    val box = info.box
                    val color = info.color
                    val a = (aTracer * (color.aFloat)).toInt()
                    val offset = (tracerOffset - 50).toDouble() / 100.0 * (box.maxY - box.minY)
                    val offsetBox = box.center.add(0.0, offset, 0.0)
                    RenderUtils3D.INSTANCE.drawLineTo(offsetBox, color.withAlpha(a))
                }
            }
            RenderUtils3D.INSTANCE.draw(1) // GL_LINES
        }

        if (clear) {
            clear()
        }

        GlStateManager.enableDepth()
    }

    data class Info(
        val box: AxisAlignedBB,
        val color: ColorRGB = ColorRGB(255, 255, 255),
        val sides: Int = 63,
        val gradient: Gradient? = null,
        val tripleGradient: TripleGradient? = null
    ) {
        constructor(pos: BlockPos) : this(AxisAlignedBB(pos))

        data class Gradient(
            val start: ColorRGB,
            val end: ColorRGB,
            val vertical: Boolean = true
        )

        data class TripleGradient(
            val start: ColorRGB,
            val middle: ColorRGB,
            val end: ColorRGB,
            val vertical: Boolean = true,
            val progress: Float = 0.0f
        )
    }

    private enum class Type {
        FILLED, OUTLINE, TRACER
    }
}