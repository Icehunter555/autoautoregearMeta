package dev.wizard.meta.module.modules.render

import dev.fastmc.common.getSq
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.graphics.GLObjectKt
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.buffer.PersistentMappedVBO
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.esp.DynamicBoxRenderer
import dev.wizard.meta.graphics.shaders.DrawShader
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.accessor.getEntityOutlineFramebuffer
import dev.wizard.meta.util.accessor.getEntityOutlineShader
import dev.wizard.meta.util.accessor.getListShaders
import dev.wizard.meta.util.accessor.getRenderOutlines
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.onMainThreadSafe
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.ShaderGroup
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

object ESP : Module(
    "ESP",
    category = Category.RENDER,
    description = "Highlights entities"
) {
    private val mode by setting(this, EnumSetting(settingName("Mode"), Mode.SHADER))
    private val page by setting(this, EnumSetting(settingName("Page"), Page.ENTITY_TYPE))

    private val all by setting(this, BooleanSetting(settingName("All"), false, page.atValue(Page.ENTITY_TYPE)))
    private val item by setting(this, BooleanSetting(settingName("Item"), true, page.atValue(Page.ENTITY_TYPE) and all.atFalse()))
    private val player by setting(this, BooleanSetting(settingName("Player"), true, page.atValue(Page.ENTITY_TYPE) and all.atFalse()))
    private val friend by setting(this, BooleanSetting(settingName("Friend"), true, page.atValue(Page.ENTITY_TYPE) and all.atFalse() and player.atTrue()))
    private val mob by setting(this, BooleanSetting(settingName("Mob"), true, page.atValue(Page.ENTITY_TYPE) and all.atFalse()))
    private val passive by setting(this, BooleanSetting(settingName("Passive"), false, page.atValue(Page.ENTITY_TYPE) and all.atFalse() and mob.atTrue()))
    private val neutral by setting(this, BooleanSetting(settingName("Neutral"), true, page.atValue(Page.ENTITY_TYPE) and all.atFalse() and mob.atTrue()))
    private val hostile by setting(this, BooleanSetting(settingName("Hostile"), true, page.atValue(Page.ENTITY_TYPE) and all.atFalse() and mob.atTrue()))

    private val range by setting(this, FloatSetting(settingName("Range"), 32.0f, 8.0f..64.0f, 0.5f, page.atValue(Page.ENTITY_TYPE)))

    private val targetColor by setting(this, ColorSetting(settingName("Target Color"), ColorRGB(255, 32, 255), page.atValue(Page.COLOR)))
    private val playerColor by setting(this, ColorSetting(settingName("Player Color"), ColorRGB(150, 180, 255), page.atValue(Page.COLOR) and player.atTrue()))
    private val friendColor by setting(this, ColorSetting(settingName("Friend Color"), ColorRGB(150, 255, 180), page.atValue(Page.COLOR) and player.atTrue() and friend.atTrue()))
    private val passiveColor by setting(this, ColorSetting(settingName("Passive Color"), ColorRGB(32, 255, 32), page.atValue(Page.COLOR) and mob.atTrue() and passive.atTrue()))
    private val neutralColor by setting(this, ColorSetting(settingName("Neutral Color"), ColorRGB(255, 255, 32), page.atValue(Page.COLOR) and mob.atTrue() and neutral.atTrue()))
    private val hostileColor by setting(this, ColorSetting(settingName("Hostile Color"), ColorRGB(255, 32, 32), page.atValue(Page.COLOR) and mob.atTrue() and hostile.atTrue()))
    private val itemColor by setting(this, ColorSetting(settingName("Item Color"), ColorRGB(255, 160, 32), page.atValue(Page.COLOR) and item.atTrue()))
    private val otherColor by setting(this, ColorSetting(settingName("Other Color"), ColorRGB(255, 255, 255), page.atValue(Page.COLOR)))

    private val hideOriginal by setting(this, BooleanSetting(settingName("Hide Original"), false, page.atValue(Page.RENDERING) and mode.atValue(Mode.SHADER)))
    private val filled by setting(this, BooleanSetting(settingName("Filled"), false, page.atValue(Page.RENDERING) and mode.atValue(Mode.BOX, Mode.SHADER)))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true, page.atValue(Page.RENDERING) and mode.atValue(Mode.BOX, Mode.SHADER)))
    private val aFilled by setting(this, IntegerSetting(settingName("Filled Alpha"), 63, 0..255, 1, page.atValue(Page.RENDERING) and mode.atValue(Mode.BOX, Mode.SHADER)))
    private val aOutline by setting(this, IntegerSetting(settingName("Outline Alpha"), 255, 0..255, 1, page.atValue(Page.RENDERING) and mode.atValue(Mode.BOX, Mode.SHADER)))
    private val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 1.0f..8.0f, 0.25f, page.atValue(Page.RENDERING)))

    private val boxRenderer = DynamicBoxRenderer()
    private var dirty = false

    val outlineESP: Boolean
        get() = isEnabled && mode == Mode.SHADER

    init {
        onDisable {
            boxRenderer.clear()
            if (mode == Mode.GLOW || mode == Mode.SHADER) {
                resetGlow()
            }
        }

        mode.valueListeners.add { prev, _ ->
            if (isEnabled && (prev == Mode.GLOW || prev == Mode.SHADER)) {
                resetGlow()
            }
        }

        safeListener<WorldEvent.Entity.Add> {
            synchronized(this@ESP) {
                dirty = true
            }
        }

        safeListener<RenderEntityEvent.All.Pre> {
            if (mode == Mode.SHADER) {
                if (!mc.renderManager.getRenderOutlines() && hideOriginal && player.distanceSqTo(it.entity) <= range.getSq() && checkEntityType(it.entity)) {
                    it.cancel()
                }
            }
        }

        safeListener<Render3DEvent>(69420) {
            when (mode) {
                Mode.BOX -> {
                    if (dirty) {
                        ConcurrentScope.launch {
                            updateBoxESP(this@safeListener)
                        }
                    }
                    renderBoxESP()
                }
                Mode.SHADER -> {
                    drawShader()
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            when (mode) {
                Mode.GLOW, Mode.SHADER -> {
                    boxRenderer.clear()
                    val rangeSq = range.getSq()
                    for (entity in EntityManager.entity) {
                        entity.isGlowing = player.distanceSqTo(entity) <= rangeSq && checkEntityType(entity)
                    }
                }
                Mode.BOX -> {
                    updateBoxESP(this)
                }
            }
        }

        safeListener<TickEvent.Post> {
            if (mode == Mode.GLOW) {
                val renderGlobal = mc.renderGlobal
                for (shader in renderGlobal.getEntityOutlineShader().getListShaders()) {
                    val uniform = shader.shaderManager.getShaderUniform("Radius")
                    uniform?.set(width)
                }
                val rangeSq = range.getSq()
                for (entity in EntityManager.entity) {
                    entity.isGlowing = player.distanceSqTo(entity) <= rangeSq && checkEntityType(entity)
                }
            }
        }
    }

    private fun updateBoxESP(event: SafeClientEvent) {
        val rangeSq = range.getSq()
        synchronized(this) {
            boxRenderer.update {
                for (entity in EntityManager.entity) {
                    if (event.player.distanceSqTo(entity) > rangeSq || !checkEntityType(entity)) continue
                    val xOffset = entity.posX - entity.lastTickPosX
                    val yOffset = entity.posY - entity.lastTickPosY
                    val zOffset = entity.posZ - entity.lastTickPosZ
                    putBox(entity.renderBoundingBox, xOffset, yOffset, zOffset, getEntityColor(entity))
                }
            }
            dirty = false
        }
    }

    private fun renderBoxESP() {
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GlStateManager.glLineWidth(width)
        GlStateUtils.depth(false)
        boxRenderer.render(aFilled, aOutline)
        GlStateUtils.depth(true)
        GlStateManager.glLineWidth(1.0f)
    }

    private fun drawShader() {
        val renderGlobal = mc.renderGlobal
        val framebufferIn = renderGlobal.getEntityOutlineFramebuffer()
        framebufferIn.setFramebufferFilter(9728)
        framebufferIn.bindFramebuffer(true)
        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1)
        GlStateUtils.depth(false)
        GLObjectKt.use(Shader) {
            it.updateUniforms()
            RenderUtils2D.putVertex(-1.0f, 1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(-1.0f, -1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(1.0f, 1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(1.0f, -1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(1.0f, 1.0f, ColorRGB(0))
            RenderUtils2D.putVertex(-1.0f, -1.0f, ColorRGB(0))
            GL30.glBindVertexArray(PersistentMappedVBO.POS2_COLOR)
            GL11.glDrawArrays(GL11.GL_TRIANGLES, PersistentMappedVBO.drawOffset, RenderUtils2D.vertexSize)
            PersistentMappedVBO.end()
            GL30.glBindVertexArray(0)
            RenderUtils2D.vertexSize = 0
        }
        GlStateUtils.depth(true)
        framebufferIn.unbindFramebuffer()
        framebufferIn.framebufferColor[0] = 0.0f
        framebufferIn.framebufferColor[1] = 0.0f
        framebufferIn.framebufferColor[2] = 0.0f
        framebufferIn.framebufferColor[3] = 0.0f
    }

    private fun checkEntityType(entity: Entity): Boolean {
        if (entity == mc.player) return false
        if (all) return true
        if (item && entity is EntityItem) return true
        if (player && entity is EntityPlayer) {
            if (friend || FriendManager.isFriend(entity.name)) return true
        }
        return EntityUtils.mobTypeSettings(entity, mob, passive, neutral, hostile)
    }

    private fun getEntityColor(entity: Entity): ColorRGB {
        return when {
            entity == CombatManager.target -> targetColor
            entity is EntityItem -> itemColor
            entity is EntityPlayer -> if (FriendManager.isFriend(entity.name)) friendColor else playerColor
            else -> when {
                EntityUtils.isPassive(entity) -> passiveColor
                EntityUtils.isNeutral(entity) -> neutralColor
                EntityUtils.isHostile(entity) -> hostileColor
                else -> otherColor
            }
        }
    }

    @JvmStatic
    fun getEspColor(entity: Entity): Int? {
        if (isDisabled || mode == Mode.BOX || !checkEntityType(entity)) return null
        val color = getEntityColor(entity)
        return (color.a shl 24) or (color.r shl 16) or (color.g shl 8) or color.b
    }

    private fun resetGlow() {
        onMainThreadSafe {
            val renderGlobal = mc.renderGlobal
            for (shader in renderGlobal.getEntityOutlineShader().getListShaders()) {
                val uniform = shader.shaderManager.getShaderUniform("Radius")
                uniform?.set(2.0f)
            }
            for (entity in EntityManager.entity) {
                entity.isGlowing = false
            }
        }
    }

    private enum class Mode { BOX, GLOW, SHADER }
    private enum class Page { ENTITY_TYPE, COLOR, RENDERING }

    object NoOpShaderGroup : ShaderGroup(
        mc.textureManager, mc.resourceManager, mc.framebuffer, ResourceLocation("shaders/post/noop.json")
    ) {
        override fun render(partialTicks: Float) {}
    }

    private object Shader : DrawShader("/assets/meta/shaders/OutlineESP.vert.glsl", "/assets/meta/shaders/OutlineESP.frag.glsl") {
        override fun updateUniforms() {
            GL20.glUniform2f(0, 1.0f / (mc.displayWidth * AntiAlias.sampleLevel), 1.0f / (mc.displayHeight * AntiAlias.sampleLevel))
            GL20.glUniform1f(1, if (!outline) 0.0f else aOutline / 255.0f)
            GL20.glUniform1f(2, if (!filled) 0.0f else aFilled / 255.0f)
            GL20.glUniform1f(3, width / 2.0f)
        }
    }
}
