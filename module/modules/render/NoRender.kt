package dev.wizard.meta.module.modules.render

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.floorToInt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.RenderBlockOverlayEvent
import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.event.events.render.RenderOverlayEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.accessor.*
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.math.vector.distanceSqToCenter
import net.minecraft.client.gui.BossInfoClient
import net.minecraft.client.gui.GuiBossOverlay
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleFirework
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.tutorial.TutorialSteps
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityAreaEffectCloud
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.IAnimals
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.*
import net.minecraft.init.Blocks
import net.minecraft.init.MobEffects
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.network.play.server.*
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityEnchantmentTable
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.world.BossInfo
import net.minecraft.world.EnumSkyBlock
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.registry.EntityEntry
import net.minecraftforge.registries.GameData
import org.lwjgl.opengl.GL11
import java.util.*
import kotlin.math.roundToInt

object NoRender : Module(
    "NoRender",
    category = Category.RENDER,
    description = "Clean up ur hud and world to remove lag"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.WORLD))

    // Object
    private val droppedItem by setting(this, EnumSetting(settingName("Dropped Item"), Mode.OFF, { page == Page.OBJECT }))
    private val effectCloud by setting(this, EnumSetting(settingName("Effect Cloud"), Mode.OFF, { page == Page.OBJECT }))
    private val tnt by setting(this, EnumSetting(settingName("TNT"), Mode.OFF, { page == Page.OBJECT }))
    private val crystal by setting(this, EnumSetting(settingName("Crystal"), Mode.OFF, { page == Page.OBJECT }))
    private val arrow by setting(this, EnumSetting(settingName("Arrow"), Mode.OFF, { page == Page.OBJECT }))
    private val itemFrame by setting(this, EnumSetting(settingName("Item Frame"), Mode.OFF, { page == Page.OBJECT }))
    private val painting by setting(this, EnumSetting(settingName("Painting"), Mode.OFF, { page == Page.OBJECT }))
    private val potion by setting(this, EnumSetting(settingName("Potion"), Mode.OFF, { page == Page.OBJECT }))
    private val xpBottle by setting(this, EnumSetting(settingName("XP Bottle"), Mode.REMOVE, { page == Page.OBJECT }))
    private val xpOrb by setting(this, EnumSetting(settingName("XP Orb"), Mode.REMOVE, { page == Page.OBJECT }))
    private val fallingBlock by setting(this, EnumSetting(settingName("Falling Block"), Mode.REMOVE, { page == Page.OBJECT }))
    private val firework by setting(this, EnumSetting(settingName("Firework"), Mode.REMOVE, { page == Page.OBJECT }))
    private val armorStand by setting(this, EnumSetting(settingName("Armor Stand"), Mode.OFF, { page == Page.OBJECT }))
    private val projectile by setting(this, EnumSetting(settingName("Projectile"), Mode.OFF, { page == Page.OBJECT }))

    // Entity
    private val player by setting(this, EnumSetting(settingName("Player"), Mode.OFF, { page == Page.ENTITY }))
    private val noWalkingAnimation by setting(this, BooleanSetting(settingName("No Walking Animation"), false, { page == Page.ENTITY }))
    val noEntityFire by setting(this, BooleanSetting(settingName("No Entity Fire"), false, { page == Page.ENTITY }))
    private val mob by setting(this, EnumSetting(settingName("Mobs"), Mode.OFF, { page == Page.ENTITY }))
    private val animal by setting(this, EnumSetting(settingName("Animal"), Mode.OFF, { page == Page.ENTITY }))
    val noEntityShadow by setting(this, BooleanSetting(settingName("No Entity Shadow"), false, { page == Page.ENTITY }))
    val noEntityHurtOverlay by setting(this, BooleanSetting(settingName("No Entity Hurt Overlay"), false, { page == Page.ENTITY }))

    // Tile Entity
    private val tileEntityRange by setting(this, BooleanSetting(settingName("Tile Entity Range"), true, { page == Page.TILE_ENTITY }))
    private val range by setting(this, IntegerSetting(settingName("Range"), 16, 0..128, 2, { page == Page.TILE_ENTITY && tileEntityRange }))
    val beaconBeams by setting(this, BooleanSetting(settingName("Beacon Beams"), true, { page == Page.TILE_ENTITY }))
    private val enchantingTableSnow by setting(this, BooleanSetting(settingName("Enchanting Table Snow"), false, { page == Page.TILE_ENTITY }))

    // World
    val map by setting(this, BooleanSetting(settingName("Maps"), false, { page == Page.WORLD }))
    private val explosion by setting(this, BooleanSetting(settingName("Explosions"), true, { page == Page.WORLD }))
    val signText by setting(this, BooleanSetting(settingName("Sign Text"), false, { page == Page.WORLD }))
    private val particle by setting(this, EnumSetting(settingName("Particle"), Mode.REMOVE, { page == Page.WORLD }))
    private val allLightingUpdate by setting(this, BooleanSetting(settingName("All Lighting Update"), true, { page == Page.WORLD }))
    val skylightUpdate by setting(this, BooleanSetting(settingName("SkyLight Update"), true, { page == Page.WORLD }))
    val antiFog by setting(this, BooleanSetting(settingName("AntiFog"), false, { page == Page.WORLD }))
    val noWeather by setting(this, BooleanSetting(settingName("Anti Weather"), false, { page == Page.WORLD }))
    val noClouds by setting(this, BooleanSetting(settingName("No Clouds"), false, { page == Page.WORLD }))
    val showBarriers by setting(this, BooleanSetting(settingName("Show Barriers"), false, { page == Page.WORLD }))

    // Overlay
    val hurtCamera by setting(this, BooleanSetting(settingName("Hurt Camera"), true, { page == Page.OVERLAY }))
    val fire by setting(this, BooleanSetting(settingName("Fire"), true, { page == Page.OVERLAY }))
    val water by setting(this, BooleanSetting(settingName("Water"), true, { page == Page.OVERLAY }))
    val blocks by setting(this, BooleanSetting(settingName("Blocks"), true, { page == Page.OVERLAY }))
    val portals by setting(this, BooleanSetting(settingName("Portals"), true, { page == Page.OVERLAY }))
    val blindness by setting(this, BooleanSetting(settingName("Blindness"), true, { page == Page.OVERLAY }))
    val nausea by setting(this, BooleanSetting(settingName("Nausea"), true, { page == Page.OVERLAY }))
    val totems by setting(this, BooleanSetting(settingName("Totems"), true, { page == Page.OVERLAY }))
    val vignette by setting(this, BooleanSetting(settingName("Vignette"), false, { page == Page.OVERLAY }))
    val helmet by setting(this, BooleanSetting(settingName("Helmet"), true, { page == Page.OVERLAY }))
    val tutorial by setting(this, BooleanSetting(settingName("Tutorial"), true, { page == Page.OVERLAY }))
    val potionIcons by setting(this, BooleanSetting(settingName("Potion Icons"), false, { page == Page.OVERLAY }))
    val toasts by setting(this, BooleanSetting(settingName("Toasts"), false, { page == Page.OVERLAY }))
    val hideExperienceBar by setting(this, BooleanSetting(settingName("Hide Experience Bar"), false, { page == Page.OVERLAY }))
    val hideSelectedItemName by setting(this, BooleanSetting(settingName("Hide Selected Item Name"), true, { page == Page.OVERLAY }))
    val noGuiBackground by setting(this, BooleanSetting(settingName("No Gui Background"), false, { page == Page.OVERLAY }))
    val noBobbing by setting(this, BooleanSetting(settingName("No Bobbing"), false, { page == Page.OVERLAY }))

    // Armor
    private val allArmor by setting(this, BooleanSetting(settingName("All"), true, { page == Page.ARMOR }))
    private val hat by setting(this, BooleanSetting(settingName("Helmet"), false, { page == Page.ARMOR }))
    private val chestplate by setting(this, BooleanSetting(settingName("Chestplate"), false, { page == Page.ARMOR }))
    private val leggings by setting(this, BooleanSetting(settingName("Leggings"), false, { page == Page.ARMOR }))
    private val boots by setting(this, BooleanSetting(settingName("Boots"), false, { page == Page.ARMOR }))

    // BossStack
    private val bossStackMode by setting(this, EnumSetting(settingName("Boss Stack Mode"), BossStackMode.REMOVE, { page == Page.BOSSSTACK }))
    private val bossStackScale by setting(this, FloatSetting(settingName("Boss Stack Scale"), 1.0f, 0.1f..5.0f, 0.25f, { page == Page.BOSSSTACK }))
    private val bossStackXOffset by setting(this, IntegerSetting(settingName("Boss Stack X Offset"), 0, -500..500, 1, { page == Page.BOSSSTACK }))
    private val bossStackYOffset by setting(this, IntegerSetting(settingName("Boss Stack Y Offset"), 0, 0..1000, 1, { page == Page.BOSSSTACK }))
    private val bossStackCensor by setting(this, BooleanSetting(settingName("Boss Stack Censor"), false, { page == Page.BOSSSTACK }))

    private val bossStackTexture = ResourceLocation("textures/gui/bars.png")
    private var bossInfoList: List<Pair<BossInfoClient, Int>> = emptyList()

    init {
        safeListener<TickEvent.Pre> {
            if (blindness) player.removeActivePotionEffect(MobEffects.BLINDNESS)
            if (nausea) player.removeActivePotionEffect(MobEffects.NAUSEA)
            if (tutorial) mc.gameSettings.tutorialStep = TutorialSteps.NONE
            if (toasts) mc.toastGui.clear()
        }

        listener<PacketEvent.Receive> {
            val packet = it.packet
            when (packet) {
                is SPacketExplosion -> if (explosion) it.cancel()
                is SPacketSpawnExperienceOrb -> if (xpOrb.remove) it.cancel()
                is SPacketSpawnObject -> {
                    val hide = when (packet.type) {
                        2 -> droppedItem.remove
                        3 -> effectCloud.remove
                        50 -> tnt.remove
                        51 -> crystal.remove
                        60, 91 -> arrow.remove
                        70 -> fallingBlock.remove
                        71 -> itemFrame.remove
                        73 -> potion.remove
                        75 -> xpBottle.remove
                        78 -> armorStand.remove
                        76 -> firework.remove
                        61, 62, 63, 64, 66, 67, 68, 93 -> projectile.remove
                        else -> false
                    }
                    if (hide) it.cancel()
                }
                is SPacketSpawnMob -> {
                    val entry = GameData.getEntityRegistry().getValue(packet.entityTypeId)
                    val clazz = entry?.entityClass
                    if (clazz != null) {
                        if (EntityMob::class.java.isAssignableFrom(clazz) && mob.remove) it.cancel()
                        else if (IAnimals::class.java.isAssignableFrom(clazz) && animal.remove) it.cancel()
                    }
                }
                is SPacketSpawnPainting -> if (painting.remove) it.cancel()
                is SPacketParticles -> if (particle.remove) it.cancel()
            }
        }

        listener<RenderBlockOverlayEvent> {
            it.cancelled = when (it.type) {
                RenderBlockOverlayEvent.OverlayType.FIRE -> fire
                RenderBlockOverlayEvent.OverlayType.WATER -> water
                RenderBlockOverlayEvent.OverlayType.BLOCK -> blocks
                else -> false
            }
        }

        listener<RenderOverlayEvent.Pre> {
            when (it.type) {
                RenderGameOverlayEvent.ElementType.VIGNETTE -> it.cancelled = vignette
                RenderGameOverlayEvent.ElementType.PORTAL -> it.cancelled = portals
                RenderGameOverlayEvent.ElementType.HELMET -> it.cancelled = helmet
                RenderGameOverlayEvent.ElementType.POTION_ICONS -> it.cancelled = potionIcons
                RenderGameOverlayEvent.ElementType.BOSSHEALTH -> {
                    it.cancel()
                    drawBossHealthBar()
                }
                RenderGameOverlayEvent.ElementType.EXPERIENCE -> it.cancelled = hideExperienceBar
            }
        }

        listener<RenderEntityEvent.Model.Pre> {
            if (noWalkingAnimation) {
                mc.world?.loadedEntityList?.filterIsInstance<EntityPlayer>()?.forEach { p ->
                    p.prevLimbSwingAmount = 0.0f
                    p.limbSwingAmount = 0.0f
                    p.limbSwing = 0.0f
                }
            }
        }

        listener<RenderEntityEvent.All.Pre>(Int.MAX_VALUE) {
            val entity = it.entity
            when (entity) {
                is EntityXPOrb -> xpOrb.handleRenderEvent(it)
                is EntityItem -> droppedItem.handleRenderEvent(it)
                is EntityAreaEffectCloud -> effectCloud.handleRenderEvent(it)
                is EntityTNTPrimed -> tnt.handleRenderEvent(it)
                is EntityEnderCrystal -> crystal.handleRenderEvent(it)
                is EntityArrow -> arrow.handleRenderEvent(it)
                is EntityFallingBlock -> fallingBlock.handleRenderEvent(it)
                is EntityItemFrame -> itemFrame.handleRenderEvent(it)
                is EntityPotion -> potion.handleRenderEvent(it)
                is EntityExpBottle -> xpBottle.handleRenderEvent(it)
                is EntityArmorStand -> armorStand.handleRenderEvent(it)
                is EntityFireworkRocket -> firework.handleRenderEvent(it)
                is EntitySnowball, is EntityEgg, is EntitySmallFireball, is EntityFireball, is EntityShulkerBullet, is EntityLlamaSpit -> projectile.handleRenderEvent(it)
                is EntityMob -> mob.handleRenderEvent(it)
                is IAnimals -> animal.handleRenderEvent(it)
                is EntityPlayer -> if (entity != mc.player) player.handleRenderEvent(it)
            }
        }

        safeConcurrentListener<TickEvent.Post> {
            updateBossInfoMap(this)
        }
    }

    @JvmStatic
    fun handleTileEntity(tileEntity: TileEntity, ci: CallbackInfo) {
        if (enchantingTableSnow && tileEntity is TileEntityEnchantmentTable) {
            SafeClientEvent.instance?.let {
                val state = Blocks.SNOW_LAYER.defaultState.withProperty(BlockSnow.LAYERS, 7)
                it.world.setBlockState(tileEntity.pos, state)
                it.world.markBlockRangeForRenderUpdate(tileEntity.pos, tileEntity.pos)
            }
            ci.cancel()
        } else if (tileEntityRange) {
            val viewEntity = EntityUtils.getViewEntity() ?: return
            if (viewEntity.getDistanceSq(tileEntity.pos) > range * range) {
                ci.cancel()
            }
        }
    }

    @JvmStatic
    fun shouldHideParticles(effect: Particle): Boolean {
        if (!isEnabled) return false
        if (particle.hide) return true
        if (firework.hide && (effect is ParticleFirework.Overlay || effect is ParticleFirework.Spark || effect is ParticleFirework.Starter)) return true
        return false
    }

    @JvmStatic
    fun handleLighting(lightType: EnumSkyBlock): Boolean {
        return isEnabled && (allLightingUpdate || (skylightUpdate && lightType == EnumSkyBlock.SKY))
    }

    @JvmStatic
    fun shouldHide(entity: EntityLivingBase, slotIn: EntityEquipmentSlot): Boolean {
        return isEnabled && allArmor && when (slotIn) {
            EntityEquipmentSlot.HEAD -> hat
            EntityEquipmentSlot.CHEST -> chestplate
            EntityEquipmentSlot.LEGS -> leggings
            EntityEquipmentSlot.FEET -> boots
            else -> false
        }
    }

    private fun updateBossInfoMap(event: SafeClientEvent) {
        val newList = ArrayList<Pair<BossInfoClient, Int>>()
        val bossOverlay = event.mc.ingameGUI.bossOverlay
        val map = bossOverlay.getMapBossInfos() ?: return
        val infos = map.values

        when (bossStackMode) {
            BossStackMode.MINIMIZE -> {
                val closest = getMatchBoss(event, infos) ?: return
                newList.add(closest to -1)
            }
            BossStackMode.STACK -> {
                val cacheMap = HashMap<String, ArrayList<BossInfoClient>>()
                infos.forEach {
                    val name = if (bossStackCensor) "Boss" else it.name.formattedText
                    cacheMap.getOrPut(name) { ArrayList() }.add(it)
                }
                cacheMap.forEach { (name, list) ->
                    val closest = getMatchBoss(event, list, name) ?: return@forEach
                    newList.add(closest to list.size)
                }
            }
            else -> {}
        }
        bossInfoList = newList
    }

    private fun getMatchBoss(event: SafeClientEvent, list: Collection<BossInfoClient>, name: String? = null): BossInfoClient? {
        val closestBoss = getClosestBoss(event, name) ?: return null
        val health = closestBoss.health / closestBoss.maxHealth
        return list.minByOrNull { Math.abs(it.percent - health) }
    }

    private fun getClosestBoss(event: SafeClientEvent, name: String?): EntityLivingBase? {
        var bosses = EntityManager.entity.asSequence().filterIsInstance<EntityLivingBase>().filter { !it.isNonBoss }
        if (name != null) {
            bosses = bosses.filter { it.displayName.formattedText == name }
        }
        return bosses.minByOrNull { it.getDistanceSq(event.player) }
    }

    private fun drawBossHealthBar() {
        mc.profiler.startSection("bossHealth")
        val resolution = ScaledResolution(mc)
        val width = resolution.scaledWidth
        var posY = 12 + bossStackYOffset

        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        for ((bossInfo, count) in bossInfoList) {
            val posX = (width / bossStackScale / 2.0f - 91.0f).roundToInt() + bossStackXOffset
            val name = if (bossStackCensor) "Boss" else bossInfo.name.formattedText
            val text = name + (if (count != -1) " x$count" else "")
            val textPosX = width / bossStackScale / 2.0f - mc.fontRenderer.getStringWidth(text) / 2.0f
            val textPosY = posY - 9.0f

            GlStateManager.pushMatrix()
            GlStateManager.scale(bossStackScale, bossStackScale, 1.0f)
            mc.textureManager.bindTexture(bossStackTexture)
            mc.ingameGUI.bossOverlay.render(posX, posY, bossInfo as BossInfo)
            mc.fontRenderer.drawStringWithShadow(text, textPosX, textPosY, 0xFFFFFF)
            GlStateManager.popMatrix()

            posY += 10 + mc.fontRenderer.FONT_HEIGHT
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        mc.profiler.endSection()
    }

    private enum class Mode(val hide: Boolean, val remove: Boolean) {
        OFF(false, false),
        HIDE(true, false),
        REMOVE(true, true);

        fun handleRenderEvent(event: RenderEntityEvent.All) {
            event.setCancelled(hide)
            if (remove) event.entity.setDead()
        }
    }

    private enum class Page { OBJECT, ENTITY, TILE_ENTITY, WORLD, OVERLAY, ARMOR, BOSSSTACK }
    private enum class BossStackMode { REMOVE, MINIMIZE, STACK }
}
