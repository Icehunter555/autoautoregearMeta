package dev.wizard.meta.module.modules.render

import dev.fastmc.common.toRadians
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.mask.EnumFacingMask
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.player.FastUse
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.ActiveRenderInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import java.util.*

object Trajectories : Module(
    "Trajectories",
    category = Category.RENDER,
    description = "Draws lines to where trajectories are going to fall"
) {
    private val showEntity by setting(this, BooleanSetting(settingName("Show Entity"), true))
    private val showBlock by setting(this, BooleanSetting(settingName("Show Block"), true))
    private val r by setting(this, IntegerSetting(settingName("Red"), 255, 0..255, 1))
    private val g by setting(this, IntegerSetting(settingName("Green"), 255, 0..255, 1))
    private val b by setting(this, IntegerSetting(settingName("Blue"), 255, 0..255, 1))
    private val aFilled by setting(this, IntegerSetting(settingName("Filled Alpha"), 127, 0..255, 1))
    private val aOutline by setting(this, IntegerSetting(settingName("Outline Alpha"), 255, 0..255, 1))
    private val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 0.25f..5.0f, 0.25f))

    private var prevItemUseCount = 0

    init {
        safeListener<TickEvent.Pre> {
            prevItemUseCount = player.itemInUseCount
        }

        safeListener<Render3DEvent> {
            val itemStack = player.heldItemMainhand
            var type = getThrowingType(itemStack)
            if (type == null) {
                val offhandStack = player.heldItemOffhand
                type = getThrowingType(offhandStack)
                if (type == null) return@safeListener
            }

            val path = ArrayList<Vec3d>()
            val flightPath = FlightPath(type)
            path.add(flightPath.position)
            while (flightPath.collision == null && path.size < 500) {
                flightPath.simulateTick()
                path.add(flightPath.position)
            }

            val offset = getPathOffset(this)
            val color = ColorRGB(r, g, b, aOutline)

            GlStateManager.glLineWidth(width)
            GlStateUtils.depth(false)

            for (i in path.indices) {
                val scale = (path.size - 1 - i).toDouble() * (1.0 / (path.size - 1))
                val offsetPos = path[i].add(offset.scale(scale))
                RenderUtils3D.putVertex(offsetPos.x, offsetPos.y, offsetPos.z, color)
            }
            RenderUtils3D.draw(3)

            flightPath.collision?.let { result ->
                val axis = result.sideHit.axis
                val boxBase = when (axis) {
                    EnumFacing.Axis.X -> AxisAlignedBB(0.0, -0.25, -0.25, 0.0, 0.25, 0.25)
                    EnumFacing.Axis.Y -> AxisAlignedBB(-0.25, 0.0, -0.25, 0.25, 0.0, 0.25)
                    else -> AxisAlignedBB(-0.25, -0.25, 0.0, 0.25, 0.25, 0.0)
                }
                val box = boxBase.offset(result.hitVec)

                val colorBox = ColorRGB(r, g, b)
                val quadSide = EnumFacingMask.getMaskForSide(result.sideHit)
                val renderer = ESPRenderer().apply {
                    setAFilled(aFilled)
                    setAOutline(aOutline)
                    setThickness(width)
                }
                renderer.add(box, colorBox, quadSide)
                renderer.render(true)

                renderer.setAFilled(0)
                if (showEntity && result.entityHit != null) {
                    renderer.add(result.entityHit!!, colorBox)
                } else if (showBlock) {
                    renderer.add(result.blockPos, colorBox)
                }
                renderer.render(true)
            }

            GlStateManager.glLineWidth(1.0f)
            GlStateUtils.depth(true)
        }
    }

    private fun getPathOffset(event: SafeClientEvent): Vec3d {
        if (mc.gameSettings.thirdPersonView != 0) return Vec3d.ZERO

        val offhandType = getThrowingType(event.player.heldItemOffhand)
        var multiplier = if (offhandType != null) -1.0 else 1.0
        if (mc.gameSettings.mainHand != EnumHandSide.RIGHT) multiplier *= -1.0

        val partialTicks = RenderUtils3D.partialTicks
        val eyePos = event.player.getPositionEyes(partialTicks)
        val camPos = EntityUtils.getInterpolatedPos(event.player as Entity, partialTicks).add(ActiveRenderInfo.getCameraPosition())

        val yawRad = event.player.rotationYaw.toRadians()
        val pitchRad = event.player.rotationPitch.toRadians()

        val offset = Vec3d(
            Math.cos(yawRad.toDouble()) * 0.2 + Math.sin(pitchRad.toDouble()) * -Math.sin(yawRad.toDouble()) * 0.15,
            0.0,
            Math.sin(yawRad.toDouble()) * 0.2 + Math.sin(pitchRad.toDouble()) * Math.cos(yawRad.toDouble()) * 0.15
        )

        return camPos.subtract(offset.scale(multiplier).add(0.0, Math.cos(pitchRad.toDouble()) * 0.1, 0.0)).subtract(eyePos)
    }

    private fun getThrowingType(stack: ItemStack): ThrowingType? {
        val item = stack.item
        return when {
            item == Items.BOW -> ThrowingType.BOW
            item == Items.EXPERIENCE_BOTTLE -> ThrowingType.EXPERIENCE
            item == Items.SPLASH_POTION || item == Items.LINGERING_POTION -> ThrowingType.POTION
            item == Items.SNOWBALL || item == Items.EGG || item == Items.ENDER_PEARL -> ThrowingType.OTHER
            else -> null
        }
    }

    private class FlightPath(val throwingType: ThrowingType) {
        val halfSize = if (throwingType == ThrowingType.BOW) 0.25 else 0.125
        var position: Vec3d
        var motion: Vec3d
        var boundingBox: AxisAlignedBB
        var collision: RayTraceResult? = null

        init {
            val partialTicks = RenderUtils3D.partialTicks
            position = mc.player!!.getPositionEyes(partialTicks)
            boundingBox = AxisAlignedBB(position.x - halfSize, position.y - halfSize, position.z - halfSize, position.x + halfSize, position.y + halfSize, position.z + halfSize)

            var pitch = mc.player!!.rotationPitch.toDouble()
            if (throwingType == ThrowingType.EXPERIENCE || throwingType == ThrowingType.POTION) pitch -= 20.0

            val yawRad = Math.toRadians(mc.player!!.rotationYaw.toDouble())
            val pitchRad = Math.toRadians(pitch)
            val cosPitch = Math.cos(pitchRad)

            val initVelocity = if (throwingType == ThrowingType.BOW) {
                val charge = FastUse.getBowCharge() ?: if (mc.player!!.isHandActive) getInterpolatedCharge() else 0.0
                val duration = (72000.0 - charge) / 20.0
                val velocity = (duration * duration + duration * 2.0) / 3.0
                Math.min(velocity, 1.0) * throwingType.velocity
            } else {
                throwingType.velocity
            }

            motion = Vec3d(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch).scale(initVelocity)
        }

        fun simulateTick() {
            if (position.y <= -9.11) {
                collision = RayTraceResult(position, EnumFacing.UP)
                return
            }

            val nextPos = position.add(motion)
            collision = mc.world!!.rayTraceBlocks(position, nextPos, false, true, false)

            if (collision == null) {
                val results = ArrayList<RayTraceResult>()
                for (entity in EntityManager.entity) {
                    if (!entity.canBeCollidedWith() || entity == mc.player) continue
                    val box = entity.entityBoundingBox.grow(0.3)
                    val result = box.calculateIntercept(position, nextPos)
                    if (result != null) {
                        result.typeOfHit = RayTraceResult.Type.ENTITY
                        result.entityHit = entity
                        results.add(result)
                    }
                }
                collision = results.minByOrNull { it.hitVec.distanceTo(position) }
            }

            collision?.let {
                setPosition(it.hitVec)
                return
            }

            val motionModifier = if (mc.world!!.isMaterialInBB(boundingBox, Material.WATER)) {
                if (throwingType == ThrowingType.BOW) 0.6 else 0.8
            } else 0.99

            setPosition(nextPos)
            motion = motion.scale(motionModifier).subtract(0.0, throwingType.gravity, 0.0)
        }

        private fun setPosition(pos: Vec3d) {
            boundingBox = boundingBox.offset(pos.subtract(position))
            position = pos
        }

        private fun getInterpolatedCharge(): Double {
            return prevItemUseCount + (mc.player!!.itemInUseCount - prevItemUseCount) * RenderUtils3D.partialTicks.toDouble()
        }
    }

    private enum class ThrowingType(val gravity: Double, val velocity: Double) {
        BOW(0.05, 3.0),
        EXPERIENCE(0.07, 0.7),
        POTION(0.05, 0.5),
        OTHER(0.03, 1.5)
    }
}
