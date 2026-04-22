package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.mask.EnumFacingMask
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.world.getSelectedBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d

object HighLight : Module(
    "Highlight",
    alias = arrayOf("Selection"),
    category = Category.RENDER,
    description = "Highlights object you are looking at"
) {
    private val expand by setting(this, FloatSetting(settingName("Expand"), 0.002f, 0.0f..1.0f, 0.001f))
    private val target by setting(this, EnumSetting(settingName("Target"), Target.BOTH))
    private val mode by setting(this, EnumSetting(settingName("Mode"), Mode.NORMAL))
    private val gradientDirection by setting(this, EnumSetting(settingName("Gradient Direction"), GradientDir.VERTICAL, { mode == Mode.GRADIENT }))
    private val hitSideOnly by setting(this, BooleanSetting(settingName("Hit Side Only"), false))
    private val depth by setting(this, BooleanSetting(settingName("Depth"), false))
    private val filled by setting(this, BooleanSetting(settingName("Filled"), true))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true))
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255), { mode == Mode.NORMAL }))
    private val gradientColor1 by setting(this, ColorSetting(settingName("Gradient Color 1"), ColorRGB(255, 255, 255), { mode == Mode.GRADIENT }))
    private val gradientColor2 by setting(this, ColorSetting(settingName("Gradient Color 2"), ColorRGB(119, 237, 255), { mode == Mode.GRADIENT }))
    private val aFilled by setting(this, IntegerSetting(settingName("Filled Alpha"), 63, 0..255, 1, filled.atTrue()))
    private val aOutline by setting(this, IntegerSetting(settingName("Outline Alpha"), 200, 0..255, 1, outline.atTrue()))
    private val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 0.25f..5.0f, 0.25f))
    private val animate by setting(this, BooleanSetting(settingName("Animate"), false))
    private val movingLength by setting(this, IntegerSetting(settingName("Moving Length"), 200, 0..1000, 50, { animate }))
    private val fadeLength by setting(this, IntegerSetting(settingName("Fade Length"), 200, 0..1000, 50, { animate }))

    private val renderer = ESPRenderer()
    private var lastBlockPos: BlockPos? = null
    private var lastEntityId: Int? = null
    private var prevBox: AxisAlignedBB? = null
    private var currentBox: AxisAlignedBB? = null
    private var lastRenderBox: AxisAlignedBB? = null
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f
    private var lastSide = 63
    private var isFadingOut = false

    init {
        onDisable {
            resetAnimation()
        }

        safeListener<Render3DEvent> {
            val hitObject = mc.objectMouseOver
            if (hitObject == null) {
                if (animate) {
                    updateAnimation(null, null, 63)
                    renderAnimated()
                }
                return@safeListener
            }

            when (hitObject.typeOfHit) {
                RayTraceResult.Type.ENTITY -> {
                    if (target == Target.ENTITY || target == Target.BOTH) {
                        val viewEntity = mc.renderViewEntity ?: player
                        val eyePos = viewEntity.getPositionEyes(RenderUtils3D.partialTicks)
                        val targetEntity = hitObject.entityHit ?: return@safeListener
                        val lookVec = viewEntity.lookVec
                        val sightEnd = eyePos.add(lookVec.scale(6.0))
                        val rayTraceResult = targetEntity.entityBoundingBox.calculateIntercept(eyePos, sightEnd)
                        val hitSide = rayTraceResult?.sideHit ?: return@safeListener
                        val side = if (hitSideOnly) EnumFacingMask.getMaskForSide(hitSide) else 63
                        val box = targetEntity.entityBoundingBox.grow(expand.toDouble())

                        if (animate) {
                            updateAnimation(targetEntity.entityId, box, side)
                            renderAnimated()
                        } else {
                            renderDirect(box, side)
                        }
                    } else if (animate) {
                        updateAnimation(null, null, 63)
                        renderAnimated()
                    }
                }
                RayTraceResult.Type.BLOCK -> {
                    if (target == Target.BLOCK || target == Target.BOTH) {
                        val blockPos = hitObject.blockPos ?: return@safeListener
                        val box = world.getSelectedBox(blockPos).grow(expand.toDouble())
                        val side = if (hitSideOnly) EnumFacingMask.getMaskForSide(hitObject.sideHit) else 63

                        if (animate) {
                            updateAnimation(blockPos, box, side)
                            renderAnimated()
                        } else {
                            renderDirect(box, side)
                        }
                    } else if (animate) {
                        updateAnimation(null, null, 63)
                        renderAnimated()
                    }
                }
                else -> {
                    if (animate) {
                        updateAnimation(null, null, 63)
                        renderAnimated()
                    }
                }
            }
        }
    }

    private fun resetAnimation() {
        lastBlockPos = null
        lastEntityId = null
        prevBox = null
        currentBox = null
        lastRenderBox = null
        lastUpdateTime = 0L
        startTime = 0L
        scale = 0.0f
        lastSide = 63
        isFadingOut = false
    }

    private fun updateAnimation(target: Any?, box: AxisAlignedBB?, side: Int) {
        val targetChanged = when (target) {
            is BlockPos -> target != lastBlockPos
            is Int -> target != lastEntityId
            null -> lastBlockPos != null || lastEntityId != null
            else -> false
        }

        if (targetChanged) {
            when (target) {
                is BlockPos -> {
                    lastBlockPos = target
                    lastEntityId = null
                    isFadingOut = false
                }
                is Int -> {
                    lastEntityId = target
                    lastBlockPos = null
                    isFadingOut = false
                }
                null -> {
                    lastBlockPos = null
                    lastEntityId = null
                    isFadingOut = true
                }
            }
            if (box != null) {
                currentBox = box
                prevBox = lastRenderBox ?: box
                lastUpdateTime = System.currentTimeMillis()
                if (lastRenderBox == null) startTime = System.currentTimeMillis()
            } else {
                lastUpdateTime = System.currentTimeMillis()
                startTime = System.currentTimeMillis()
            }
            lastSide = side
        } else if (box != null) {
            currentBox = box
            lastSide = side
        }
    }

    private fun renderAnimated() {
        if (isFadingOut) {
            lastRenderBox?.let { last ->
                scale = Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength.toLong()))
                if (scale > 0.0f) {
                    renderWithAlpha(last, lastSide, scale)
                } else {
                    prevBox = null
                    currentBox = null
                    lastRenderBox = null
                }
            }
            return
        }

        prevBox?.let { prev ->
            currentBox?.let { curr ->
                val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, movingLength.toLong()))
                val renderBox = interpolateBox(prev, curr, multiplier.toDouble())
                scale = Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.toLong()))
                if (scale > 0.0f) {
                    renderWithAlpha(renderBox, lastSide, scale)
                }
                lastRenderBox = renderBox
                return
            }
        }

        currentBox?.let { curr ->
            currentBox = curr
            prevBox = curr
            lastRenderBox = curr
            lastUpdateTime = System.currentTimeMillis()
            startTime = System.currentTimeMillis()
        }
    }

    private fun interpolateBox(from: AxisAlignedBB, to: AxisAlignedBB, progress: Double): AxisAlignedBB {
        return AxisAlignedBB(
            from.minX + (to.minX - from.minX) * progress,
            from.minY + (to.minY - from.minY) * progress,
            from.minZ + (to.minZ - from.minZ) * progress,
            from.maxX + (to.maxX - from.maxX) * progress,
            from.maxY + (to.maxY - from.maxY) * progress,
            from.maxZ + (to.maxZ - from.maxZ) * progress
        )
    }

    private fun renderDirect(box: AxisAlignedBB, side: Int) {
        when (mode) {
            Mode.NORMAL -> renderer.add(box, color, side)
            Mode.GRADIENT -> renderer.addGradient(box, gradientColor1, gradientColor2, side, gradientDirection == GradientDir.VERTICAL)
        }
        renderer.setAFilled(if (filled) aFilled else 0)
        renderer.setAOutline(if (outline) aOutline else 0)
        renderer.setThrough(!depth)
        renderer.setThickness(width)
        renderer.render(true)
    }

    private fun renderWithAlpha(box: AxisAlignedBB, side: Int, alphaScale: Float) {
        when (mode) {
            Mode.NORMAL -> renderer.add(box, color, side)
            Mode.GRADIENT -> renderer.addGradient(box, gradientColor1, gradientColor2, side, gradientDirection == GradientDir.VERTICAL)
        }
        renderer.setAFilled(if (filled) (aFilled * alphaScale).toInt() else 0)
        renderer.setAOutline(if (outline) (aOutline * alphaScale).toInt() else 0)
        renderer.setThrough(!depth)
        renderer.setThickness(width)
        renderer.render(true)
    }

    private enum class GradientDir { HORIZONTAL, VERTICAL }
    private enum class Mode { NORMAL, GRADIENT }
    private enum class Target { ENTITY, BLOCK, BOTH }
}
