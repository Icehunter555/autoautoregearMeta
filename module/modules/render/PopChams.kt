package dev.wizard.meta.module.modules.render

import com.mojang.authlib.GameProfile
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.TotemPopChams
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.SPacketEntityStatus
import net.minecraft.util.math.MathHelper
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color

object PopChams : Module(
    "PopChams",
    category = Category.RENDER,
    description = "Show popped player",
    modulePriority = 498
) {
    private var alphaFill = 255.0
    private var alphaLine = 255.0

    private val self by setting(this, BooleanSetting(settingName("Render Own Pops"), true))
    private val friends by setting(this, BooleanSetting(settingName("Render Friends"), true))
    private val outlineColor by setting(this, ColorSetting(settingName("Outline Color"), ColorRGB(30, 255, 167, 255)))
    private val fillColor by setting(this, ColorSetting(settingName("Fill Color"), ColorRGB(30, 255, 167, 140)))
    private val selfOutlineColor by setting(this, ColorSetting(settingName("Self Outline Color"), ColorRGB(255, 30, 30, 255)))
    private val selfFillColor by setting(this, ColorSetting(settingName("Self Fill Color"), ColorRGB(255, 30, 30, 140)))
    private val friendOutlineColor by setting(this, ColorSetting(settingName("Friend Outline Color"), ColorRGB(30, 167, 255, 255)))
    private val friendFillColor by setting(this, ColorSetting(settingName("Friend Fill Color"), ColorRGB(30, 167, 255, 140)))
    private val initialFillAlpha by setting(this, IntegerSetting(settingName("Initial Fill Alpha"), 255, 0..255, 1))
    private val initialOutlineAlpha by setting(this, IntegerSetting(settingName("Initial Outline Alpha"), 255, 0..255, 1))
    private val fadeStart by setting(this, IntegerSetting(settingName("Fade Start"), 0, 0..5000, 50))
    private val fadeTime by setting(this, DoubleSetting(settingName("Fade Time"), 1.0, 0.0..5.0, 0.1))
    private val onlyOneEsp by setting(this, BooleanSetting(settingName("Only Render One"), true))
    private val elevatorMode by setting(this, EnumSetting(settingName("Elevator"), ElevatorMode.UP))
    private val spin by setting(this, IntegerSetting(settingName("Spin"), 0, -100..100, 1))
    private val shrink by setting(this, BooleanSetting(settingName("Shrink"), false))
    private val shrinkStart by setting(this, IntegerSetting(settingName("Shrink Start"), 500, 0..5000, 50))
    private val shrinkTime by setting(this, DoubleSetting(settingName("Shrink Time"), 1.0, 0.0..5.0, 0.1))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 2.0f, 0.5f..6.0f, 0.1f))
    private val throughWalls by setting(this, BooleanSetting(settingName("Through Walls"), true))

    private var targetPlayer: EntityOtherPlayerMP? = null
    private var playerModel: ModelPlayer? = null
    private var startTime: Long? = null
    private var registered = false
    private var isSelfPop = false
    private var isFriendPop = false
    private var entityName: String? = null

    init {
        onEnable {
            alphaFill = initialFillAlpha.toDouble()
            alphaLine = initialOutlineAlpha.toDouble()
        }

        safeListener<TickEvent.Post> {
            if (!registered) {
                MinecraftForge.EVENT_BUS.register(this@PopChams)
                registered = true
            }
        }

        onDisable {
            if (registered) {
                MinecraftForge.EVENT_BUS.unregister(this@PopChams)
                registered = false
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketEntityStatus && packet.opCode.toInt() == 35) {
                val entity = packet.getEntity(world) ?: return@safeListener
                isSelfPop = entity.uniqueID == player.uniqueID
                isFriendPop = FriendManager.isFriend(entity.name)
                entityName = entity.name

                if (!self && isSelfPop) return@safeListener
                if (!friends && isFriendPop) return@safeListener

                targetPlayer = EntityOtherPlayerMP(world, GameProfile(entity.uniqueID, entity.name)).apply {
                    copyLocationAndAnglesFrom(entity)
                }
                playerModel = ModelPlayer(0.0f, false).apply {
                    isChild = false
                    bipedHead.isChild = false
                    bipedBody.isChild = false
                    bipedRightArm.isChild = false
                    bipedLeftArm.isChild = false
                    bipedRightLeg.isChild = false
                    bipedLeftLeg.isChild = false
                }
                startTime = System.currentTimeMillis()
                alphaFill = initialFillAlpha.toDouble()
                alphaLine = initialOutlineAlpha.toDouble()

                if (!onlyOneEsp) {
                    MinecraftForge.EVENT_BUS.register(TotemPopChams(targetPlayer, playerModel, startTime, alphaFill, alphaLine, isSelfPop, isFriendPop))
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!onlyOneEsp) return
        val player = targetPlayer ?: return
        val model = playerModel ?: return
        val start = startTime ?: return
        if (mc.world == null || mc.player == null) return

        when (elevatorMode) {
            ElevatorMode.UP -> player.posY += 0.05 * event.partialTicks
            ElevatorMode.DOWN -> player.posY -= 0.05 * event.partialTicks
            else -> {}
        }

        if (spin != 0) {
            player.renderYawOffset += spin * event.partialTicks
            player.rotationYaw = player.renderYawOffset
        }

        GL11.glLineWidth(1.0f)

        val colors = when {
            isSelfPop -> selfOutlineColor to selfFillColor
            isFriendPop -> friendOutlineColor to friendFillColor
            else -> outlineColor to fillColor
        }

        val lineC = Color(colors.first.r, colors.first.g, colors.first.b, colors.first.a)
        val fillC = Color(colors.second.r, colors.second.g, colors.second.b, colors.second.a)

        var lineA = lineC.alpha
        var fillA = fillC.alpha

        val elapsed = System.currentTimeMillis() - start
        if (elapsed > fadeStart) {
            val fadeProgress = (elapsed - fadeStart).toDouble() / (fadeTime * 1000.0)
            val fadeFactor = 1.0 - MathHelper.clamp(fadeProgress, 0.0, 1.0)
            lineA = (lineA * fadeFactor).toInt()
            fillA = (fillA * fadeFactor).toInt()
        }

        val lineFinal = newAlpha(lineC, lineA)
        val fillFinal = newAlpha(fillC, fillA)

        var scale = 1.0f
        if (shrink && elapsed > shrinkStart) {
            val shrinkProgress = (elapsed - shrinkStart).toDouble() / (shrinkTime * 1000.0)
            scale = (1.0 - MathHelper.clamp(shrinkProgress, 0.0, 1.0)).toFloat()
        }

        prepareGL()
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        glColor(fillFinal)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        renderEntity(player, model, player.limbSwing, player.limbSwingAmount, player.ticksExisted.toFloat(), player.renderYawOffset, player.rotationPitch, scale)

        glColor(lineFinal)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
        renderEntity(player, model, player.limbSwing, player.limbSwingAmount, player.ticksExisted.toFloat(), player.renderYawOffset, player.rotationPitch, scale)

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        GL11.glPopAttrib()
        releaseGL()
    }

    private fun renderEntity(entity: EntityLivingBase, model: ModelBase, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scale: Float) {
        val renderManager = mc.renderManager
        val partialTicks = mc.renderPartialTicks
        val x = entity.posX - renderManager.viewerPosX
        val y = entity.posY - renderManager.viewerPosY
        val z = entity.posZ - renderManager.viewerPosZ

        GlStateManager.pushMatrix()
        var renderY = y
        if (entity.isSneaking) renderY -= 0.125

        GlStateManager.translate(x, renderY, z)
        GlStateManager.rotate(180.0f - entity.rotationYaw, 0.0f, 1.0f, 0.0f)

        GlStateManager.enableRescaleNormal()
        GlStateManager.scale(-1.0f, -1.0f, 1.0f)
        val bb = entity.entityBoundingBox
        GlStateManager.scale((scale + (bb.maxX - bb.minX)) * scale, (scale * entity.height) * scale, (scale + (bb.maxZ - bb.minZ)) * scale)
        GlStateManager.translate(0.0f, -1.501f, 0.0f)

        model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks)
        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, entity.rotationYaw, entity.rotationPitch, 0.0625f, entity)
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, entity.rotationYaw, entity.rotationPitch, 0.0625f)

        GlStateManager.popMatrix()
    }

    private fun newAlpha(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))
    }

    private fun glColor(color: Color) {
        GL11.glColor4f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, color.alpha / 255.0f)
    }

    private fun prepareGL() {
        GL11.glBlendFunc(770, 771)
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
        GlStateManager.glLineWidth(1.5f)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)
        GlStateManager.enableBlend()
        GlStateManager.disableDepth()
        GlStateManager.disableLighting()
        GlStateManager.disableCull()
        GlStateManager.enableAlpha()
        GlStateManager.color(1.0f, 1.0f, 1.0f)
    }

    private fun releaseGL() {
        GlStateManager.enableCull()
        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.enableDepth()
    }

    enum class ElevatorMode { UP, DOWN, OFF }
}
