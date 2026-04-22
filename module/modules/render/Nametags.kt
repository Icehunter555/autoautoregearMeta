package dev.wizard.meta.module.modules.render

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.floorToInt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorGradient
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.Style
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.TotemPopManager
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EnchantmentUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.getOriginalName
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.text.getUnformatted
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.util.*
import kotlin.math.roundToInt

object Nametags : Module(
    "Nametags",
    category = Category.RENDER,
    description = "Draws descriptive nametags above entities"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.ENTITY_TYPE))
    private val self by setting(this, BooleanSetting(settingName("Self"), false, { page == Page.ENTITY_TYPE }))
    private val experience by setting(this, BooleanSetting(settingName("Experience"), false, { page == Page.ENTITY_TYPE }))
    private val items by setting(this, BooleanSetting(settingName("Items"), true, { page == Page.ENTITY_TYPE }))
    private val players by setting(this, BooleanSetting(settingName("Players"), true, { page == Page.ENTITY_TYPE }))
    private val mobs by setting(this, BooleanSetting(settingName("Mobs"), true, { page == Page.ENTITY_TYPE }))
    private val passive by setting(this, BooleanSetting(settingName("Passive Mobs"), false, { page == Page.ENTITY_TYPE && mobs }))
    private val neutral by setting(this, BooleanSetting(settingName("Neutral Mobs"), true, { page == Page.ENTITY_TYPE && mobs }))
    private val hostile by setting(this, BooleanSetting(settingName("Hostile Mobs"), true, { page == Page.ENTITY_TYPE && mobs }))
    private val invisible by setting(this, BooleanSetting(settingName("Invisible"), true, { page == Page.ENTITY_TYPE }))
    private val range by setting(this, IntegerSetting(settingName("Range"), 64, 0..512, 4, { page == Page.ENTITY_TYPE }))

    private val line1left by setting(this, EnumSetting(settingName("Line 1 Left"), ContentType.NONE, { page == Page.CONTENT }))
    private val line1center by setting(this, EnumSetting(settingName("Line 1 Center"), ContentType.NONE, { page == Page.CONTENT }))
    private val line1right by setting(this, EnumSetting(settingName("Line 1 Right"), ContentType.NONE, { page == Page.CONTENT }))
    private val line2left by setting(this, EnumSetting(settingName("Line 2 Left"), ContentType.NAME, { page == Page.CONTENT }))
    private val line2center by setting(this, EnumSetting(settingName("Line 2 Center"), ContentType.PING, { page == Page.CONTENT }))
    private val line2right by setting(this, EnumSetting(settingName("Line 2 Right"), ContentType.TOTAL_HP, { page == Page.CONTENT }))

    private val healthBar by setting(this, BooleanSetting(settingName("Health Bar"), true, { page == Page.CONTENT }))
    private val dropItemCount by setting(this, BooleanSetting(settingName("Drop Item Count"), true, { page == Page.CONTENT && items }))
    private val maxDropItems by setting(this, IntegerSetting(settingName("Max Drop Items"), 5, 2..16, 1, { page == Page.CONTENT && items }))

    private val healthColor1 by setting(this, ColorSetting(settingName("Health Low"), ColorRGB(180, 20, 20), { page == Page.COLORS && healthBar }))
    private val healthColor2 by setting(this, ColorSetting(settingName("Health Mid"), ColorRGB(240, 220, 20), { page == Page.COLORS && healthBar }))
    private val healthColor3 by setting(this, ColorSetting(settingName("Health High"), ColorRGB(20, 232, 20), { page == Page.COLORS && healthBar }))
    private val absorptionColor by setting(this, ColorSetting(settingName("Absorption"), ColorRGB(234, 204, 32), { page == Page.COLORS && healthBar }))
    private var friendcolor by setting(this, ColorSetting(settingName("FriendColor"), ColorRGB(0, 232, 20), { page == Page.COLORS }))

    private val pingColor1 by setting(this, ColorSetting(settingName("Ping Excellent"), ColorRGB(20, 232, 20), { page == Page.COLORS }))
    private val pingColor2 by setting(this, ColorSetting(settingName("Ping Good"), ColorRGB(20, 232, 20), { page == Page.COLORS }))
    private val pingColor3 by setting(this, ColorSetting(settingName("Ping Medium"), ColorRGB(20, 232, 20), { page == Page.COLORS }))
    private val pingColor4 by setting(this, ColorSetting(settingName("Ping Bad"), ColorRGB(150, 0, 0), { page == Page.COLORS }))
    private val pingColor5 by setting(this, ColorSetting(settingName("Ping Terrible"), ColorRGB(101, 101, 101), { page == Page.COLORS }))

    private val pingThreshold1 by setting(this, FloatSetting(settingName("Ping Threshold 1"), 0.0f, 0.0f..1000.0f, 1.0f, { page == Page.COLORS }))
    private val pingThreshold2 by setting(this, FloatSetting(settingName("Ping Threshold 2"), 20.0f, 0.0f..1000.0f, 1.0f, { page == Page.COLORS }))
    private val pingThreshold3 by setting(this, FloatSetting(settingName("Ping Threshold 3"), 150.0f, 0.0f..1000.0f, 1.0f, { page == Page.COLORS }))
    private val pingThreshold4 by setting(this, FloatSetting(settingName("Ping Threshold 4"), 300.0f, 0.0f..1000.0f, 1.0f, { page == Page.COLORS }))

    private val mainHand by setting(this, BooleanSetting(settingName("Main Hand"), true, { page == Page.ITEM }))
    private val offhand by setting(this, BooleanSetting(settingName("Off Hand"), true, { page == Page.ITEM }))
    private val invertHand by setting(this, BooleanSetting(settingName("Invert Hand"), false, { page == Page.ITEM && (mainHand || offhand) }))
    private val armor by setting(this, BooleanSetting(settingName("Armor"), true, { page == Page.ITEM }))
    private val count by setting(this, BooleanSetting(settingName("Count"), true, { page == Page.ITEM && (mainHand || offhand || armor) }))
    private val dura by setting(this, BooleanSetting(settingName("Dura"), true, { page == Page.ITEM && (mainHand || offhand || armor) }))
    private val enchantment by setting(this, BooleanSetting(settingName("Enchantment"), true, { page == Page.ITEM && (mainHand || offhand || armor) }))
    private val blast by setting(this, BooleanSetting(settingName("Blast Enchantment"), false, { page == Page.ITEM && armor && !enchantment }))
    private val itemScale by setting(this, FloatSetting(settingName("Item Scale"), 0.8f, 0.1f..2.0f, 0.1f, { page == Page.ITEM }))

    private val background by setting(this, BooleanSetting(settingName("Background"), true, { page == Page.RENDERING }))
    private val margins by setting(this, FloatSetting(settingName("Margins"), 1.0f, 0.0f..4.0f, 0.1f, { page == Page.RENDERING }))
    private val rText by setting(this, IntegerSetting(settingName("Text Red"), 232, 0..255, 1, { page == Page.RENDERING }))
    private val gText by setting(this, IntegerSetting(settingName("Text Green"), 229, 0..255, 1, { page == Page.RENDERING }))
    private val bText by setting(this, IntegerSetting(settingName("Text Blue"), 255, 0..255, 1, { page == Page.RENDERING }))
    private val aText by setting(this, IntegerSetting(settingName("Text Alpha"), 255, 0..255, 1, { page == Page.RENDERING }))
    private val scale by setting(this, FloatSetting(settingName("Scale"), 1.3f, 0.1f..5.0f, 0.1f, { page == Page.RENDERING }))
    private val distScaleFactor by setting(this, FloatSetting(settingName("Distance Scale Factor"), 0.35f, 0.0f..1.0f, 0.05f, { page == Page.RENDERING }))
    private val minDistScale by setting(this, FloatSetting(settingName("Min Distance Scale"), 0.35f, 0.0f..1.0f, 0.05f, { page == Page.RENDERING }))

    private val line1Settings by lazy { arrayOf(line1left, line1center, line1right) }
    private val line2Settings by lazy { arrayOf(line2left, line2center, line2right) }
    private var renderInfos: List<IRenderInfo> = emptyList()

    private val healthColorGradient: ColorGradient
        get() = ColorGradient(
            ColorGradient.Stop(0.0f, healthColor1.rgba),
            ColorGradient.Stop(50.0f, healthColor2.rgba),
            ColorGradient.Stop(100.0f, healthColor3.rgba)
        )

    private val pingColorGradient: ColorGradient
        get() = ColorGradient(
            ColorGradient.Stop(pingThreshold1, pingColor5.rgba),
            ColorGradient.Stop(pingThreshold2, pingColor1.rgba),
            ColorGradient.Stop(pingThreshold3, pingColor2.rgba),
            ColorGradient.Stop(pingThreshold4, pingColor3.rgba),
            ColorGradient.Stop(1000.0f, pingColor4.rgba)
        )

    init {
        onDisable {
            renderInfos = emptyList()
        }

        listener<Render2DEvent.Absolute> {
            val camPos = RenderUtils3D.camPos
            val partialTicks = RenderUtils3D.partialTicks
            for (info in renderInfos) {
                info.render(camPos, partialTicks)
            }
        }

        safeParallelListener<TickEvent.Post> {
            val rangeSq = range * range
            val newList = ArrayList<IRenderInfo>()
            val itemList = ArrayList<ItemGroup>()

            for (entity in EntityManager.entity) {
                if (!checkEntityType(entity) || player.distanceSqTo(entity) > rangeSq) continue

                when (entity) {
                    is EntityLivingBase -> newList.add(LivingRenderInfo(this, entity))
                    is EntityXPOrb -> newList.add(XpOrbRenderInfo(this, entity))
                    is EntityItem -> {
                        var added = false
                        for (group in itemList) {
                            if (group.add(entity)) {
                                added = true
                                break
                            }
                        }
                        if (!added) {
                            val group = ItemGroup()
                            group.add(entity)
                            itemList.add(group)
                        }
                    }
                }
            }

            for (group1 in itemList) {
                for (group2 in itemList) {
                    if (group1 != group2) group1.merge(group2)
                }
            }

            itemList.filter { !it.isEmpty() }.forEach {
                it.updateText()
                newList.add(ItemGroupRenderInfo(this, it))
            }

            renderInfos = newList.sortedByDescending { it.distanceSq }
        }
    }

    private fun checkEntityType(entity: Entity): Boolean {
        if (!self && entity == mc.player) return false
        if (entity.isInvisible && !invisible) return false

        return when {
            experience && entity is EntityXPOrb -> true
            items && entity is EntityItem -> true
            players && entity is EntityPlayer && EntityUtils.playerTypeCheck(entity, true, true) -> true
            else -> EntityUtils.mobTypeSettings(entity, mobs, passive, neutral, hostile)
        }
    }

    private fun calcScale(camPos: Vec3d, pos: Vec3d): Float {
        val dist = (camPos.distanceTo(pos).toFloat() - 1.5f).coerceAtLeast(0.0f) * 0.25f * distScaleFactor
        val distFactor = if (distScaleFactor == 0.0f) 1.0f else (1.0f / Math.pow(2.0, dist.toDouble()).toFloat()).coerceAtLeast(minDistScale)
        return scale * 2.0f * distFactor
    }

    private fun drawNametag(screenPos: Vec3d, scale: Float, textComponent: TextComponent) {
        val halfWidth = textComponent.getWidth() / 2.0f + margins + 2.0f
        val halfHeight = textComponent.getHeight(2, true) / 2.0f + margins + 1.0f

        val scaledHalfWidth = halfWidth * scale
        val scaledHalfHeight = halfHeight * scale

        if (screenPos.x - scaledHalfWidth < 0 || screenPos.x + scaledHalfWidth > mc.displayWidth ||
            screenPos.y - scaledHalfHeight < 0 || screenPos.y + scaledHalfHeight > mc.displayHeight) return

        RenderUtils2D.prepareGL()
        if (background) {
            RenderUtils2D.putVertex(halfWidth, -halfHeight, ClickGUI.backGround)
            RenderUtils2D.putVertex(-halfWidth, -halfHeight, ClickGUI.backGround)
        }
        RenderUtils2D.putVertex(-halfWidth, halfHeight + (if (healthBar) 3.0f else 1.0f), ClickGUI.backGround)
        RenderUtils2D.putVertex(halfWidth, halfHeight + (if (healthBar) 3.0f else 1.0f), ClickGUI.backGround)

        RenderUtils2D.putVertex(-halfWidth, halfHeight - 1.0f, ClickGUI.primary)
        RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, ClickGUI.primary)
        RenderUtils2D.putVertex(halfWidth, halfHeight + 1.0f, ClickGUI.primary)
        RenderUtils2D.putVertex(halfWidth, halfHeight - 1.0f, ClickGUI.primary)

        RenderUtils2D.draw(7)
        RenderUtils2D.releaseGL()
        textComponent.draw(null, 0, 0.0f, 0.0f, true, HAlign.CENTER, VAlign.CENTER)
    }

    private fun drawNametagLiving(screenPos: Vec3d, scale: Float, entity: EntityLivingBase, textComponent: TextComponent): Boolean {
        val halfWidth = textComponent.getWidth() / 2.0f + margins + 2.0f
        val halfHeight = textComponent.getHeight(2, true) / 2.0f + margins + 1.0f

        val scaledHalfWidth = halfWidth * scale
        val scaledHalfHeight = halfHeight * scale

        if (screenPos.x - scaledHalfWidth < 0 || screenPos.x + scaledHalfWidth > mc.displayWidth ||
            screenPos.y - scaledHalfHeight < 0 || screenPos.y + scaledHalfHeight > mc.displayHeight) return false

        val lineColor = getHpColor(entity)
        val lineProgress = entity.health / entity.maxHealth
        val absorption = entity.absorptionAmount / entity.maxHealth
        val width = halfWidth * 2.0f

        RenderUtils2D.prepareGL()
        if (background) {
            RenderUtils2D.putVertex(halfWidth, -halfHeight, ClickGUI.backGround)
            RenderUtils2D.putVertex(-halfWidth, -halfHeight, ClickGUI.backGround)
        }
        RenderUtils2D.putVertex(-halfWidth, halfHeight + (if (healthBar) 3.0f else 1.0f), ClickGUI.backGround)
        RenderUtils2D.putVertex(halfWidth, halfHeight + (if (healthBar) 3.0f else 1.0f), ClickGUI.backGround)

        if (healthBar) {
            if (absorption > 0.0f) {
                val aCol = absorptionColor.rgba
                RenderUtils2D.putVertex(-halfWidth, halfHeight - 1.0f, aCol)
                RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, aCol)
                RenderUtils2D.putVertex(-halfWidth + width * absorption, halfHeight + 1.0f, aCol)
                RenderUtils2D.putVertex(-halfWidth + width * absorption, halfHeight - 1.0f, aCol)

                RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, lineColor.rgba)
                RenderUtils2D.putVertex(-halfWidth, halfHeight + 3.0f, lineColor.rgba)
                RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight + 3.0f, lineColor.rgba)
                RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight + 1.0f, lineColor.rgba)
            } else {
                RenderUtils2D.putVertex(-halfWidth, halfHeight - 1.0f, lineColor.rgba)
                RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, lineColor.rgba)
                RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight + 1.0f, lineColor.rgba)
                RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight - 1.0f, lineColor.rgba)
            }
        }

        RenderUtils2D.draw(7)
        RenderUtils2D.releaseGL()
        textComponent.draw(null, 0, 0.0f, 0.0f, true, HAlign.CENTER, VAlign.CENTER)
        return true
    }

    private fun drawItem(itemStack: ItemStack, enchantmentText: TextComponent, drawDura: Boolean) {
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        mc.renderItem.zLevel = -100.0f
        RenderHelper.enableGUIStandardItemLighting()
        GL20.glUseProgram(0)
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
        RenderHelper.disableStandardItemLighting()
        mc.renderItem.zLevel = 0.0f
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        if (drawDura && itemStack.isItemDamaged) {
            val duraPercentage = 100.0f - (itemStack.itemDamage.toFloat() / itemStack.maxDamage.toFloat() * 100.0f)
            val color = ColorRGB(healthColorGradient.get(duraPercentage))
            val text = duraPercentage.roundToInt().toString()
            val textWidth = MainFontRenderer.getWidth(text)
            MainFontRenderer.drawString(text, 8.0f - textWidth / 2.0f, 17.0f, color)
        }

        if (count && itemStack.count > 1) {
            val itemCount = itemStack.count.toString()
            GlStateManager.translate(0.0f, 0.0f, 60.0f)
            val stringWidth = 17.0f - MainFontRenderer.getWidth(itemCount)
            MainFontRenderer.drawString(itemCount, stringWidth, 9.0f, ColorRGB(255, 255, 255))
            GlStateManager.translate(0.0f, 0.0f, -60.0f)
        }

        GlStateManager.translate(0.0f, -2.0f, 0.0f)
        if (enchantment) {
            enchantmentText.draw(null, 2, 0.0f, 0.6f, false, null, VAlign.BOTTOM)
        }
        GlStateManager.translate(28.0f, 2.0f, 0.0f)
    }

    private fun getHpColor(entity: EntityLivingBase): ColorRGB {
        return ColorRGB(healthColorGradient.get(entity.health / entity.maxHealth * 100.0f)).withAlpha(aText)
    }

    private fun getTextColor(): ColorRGB {
        return ColorRGB(rText, gText, bText, aText)
    }

    private fun getContent(event: SafeClientEvent, contentType: ContentType, entity: Entity): TextComponent.TextElement? {
        return when (contentType) {
            ContentType.NONE -> null
            ContentType.NAME -> {
                val name = entity.displayName.getUnformatted()
                val col = if (FriendManager.isFriend(name)) ColorRGB(friendcolor) else getTextColor()
                TextComponent.TextElement(name, col.rgba)
            }
            ContentType.TYPE -> TextComponent.TextElement(getEntityType(entity), getTextColor().rgba)
            ContentType.TOTAL_HP -> {
                if (entity !is EntityLivingBase) return null
                val totalHp = MathUtils.round(entity.health + entity.absorptionAmount, 1).toString()
                TextComponent.TextElement(totalHp, getHpColor(entity).rgba)
            }
            ContentType.HP -> {
                if (entity !is EntityLivingBase) return null
                val hp = MathUtils.round(entity.health, 1).toString()
                TextComponent.TextElement(hp, getHpColor(entity).rgba)
            }
            ContentType.ABSORPTION -> {
                if (entity !is EntityLivingBase || entity.absorptionAmount == 0.0f) return null
                val absorption = MathUtils.round(entity.absorptionAmount, 1).toString()
                TextComponent.TextElement(absorption, absorptionColor.withAlpha(aText).rgba)
            }
            ContentType.PING -> {
                if (entity !is EntityOtherPlayerMP) return null
                val info = event.connection.getPlayerInfo(entity.uniqueID)
                val ping = info?.responseTime ?: 0
                TextComponent.TextElement("${ping}ms", ColorRGB(pingColorGradient.get(ping.toFloat())).withAlpha(aText).rgba)
            }
            ContentType.DISTANCE -> {
                val dist = MathUtils.round(event.player.getDistance(entity), 1).toString()
                TextComponent.TextElement("${dist}m", getTextColor().rgba)
            }
            ContentType.TOTEM_POPS -> {
                TextComponent.TextElement(TotemPopManager.getPopCount(entity).toString(), getTextColor().rgba)
            }
        }
    }

    private fun getEntityType(entity: Entity): String {
        var name = entity::class.java.simpleName
        arrayOf("Entity", "MP", "Other", "SP").forEach { name = name.replace(it, "") }
        return name.replace(" ", "")
    }

    private fun getEnchantmentText(itemStack: ItemStack): TextComponent {
        val component = TextComponent()
        EnchantmentUtils.getAllEnchantments(itemStack).forEach {
            component.add(it.alias, ColorRGB(255, 255, 255, aText).rgba, Style.BOLD)
            component.addLine(it.levelText, ColorRGB(150, 240, 250, aText).rgba, Style.BOLD)
        }
        return component
    }

    private interface IRenderInfo : Comparable<IRenderInfo> {
        val distanceSq: Float
        fun render(camPos: Vec3d, partialTicks: Float)
        override fun compareTo(other: IRenderInfo): Int = distanceSq.compareTo(other.distanceSq)
    }

    private class ItemGroup {
        private val items = ArrayList<EntityItem>()
        val textComponent = TextComponent()

        fun merge(other: ItemGroup) {
            val thisCenter = getCenter()
            val otherCenter = other.getCenter()
            if (thisCenter.squareDistanceTo(otherCenter) < 20.0) {
                val it = other.items.iterator()
                while (it.hasNext()) {
                    val item = it.next()
                    if ((items.size < other.items.size && item.getDistanceSq(thisCenter) < item.getDistanceSq(otherCenter)) || add(item)) {
                        it.remove()
                    }
                }
            }
        }

        fun add(item: EntityItem): Boolean {
            for (other in items) {
                if (other.getDistanceSq(item) > 10.0) return false
            }
            return items.add(item)
        }

        fun isEmpty() = items.isEmpty()

        fun getCenter(): Vec3d {
            if (isEmpty()) return Vec3d.ZERO
            var x = 0.0
            var y = 0.0
            var z = 0.0
            items.forEach { x += it.posX; y += it.posY; z += it.posZ }
            return Vec3d(x / items.size, y / items.size, z / items.size)
        }

        fun getCenterPrev(): Vec3d {
            if (isEmpty()) return Vec3d.ZERO
            var x = 0.0
            var y = 0.0
            var z = 0.0
            items.forEach { x += it.prevPosX; y += it.prevPosY; z += it.prevPosZ }
            return Vec3d(x / items.size, y / items.size, z / items.size)
        }

        fun updateText() {
            val map = TreeMap<String, Int>()
            items.forEach {
                val stack = it.item
                val name = if (stack.displayName == stack.getOriginalName()) stack.getOriginalName() else "${stack.displayName} (${stack.getOriginalName()})"
                map[name] = (map[name] ?: 0) + stack.count
            }
            textComponent.clear()
            var i = 0
            for (entry in map.entries.sortedByDescending { it.value }) {
                val text = if (dropItemCount) "${entry.key} x${entry.value}" else entry.key
                textComponent.addLine(text, getTextColor().rgba)
                if (++i >= maxDropItems) {
                    if (map.size > maxDropItems) {
                        textComponent.addLine("...and ${map.size - maxDropItems} more", getTextColor().rgba)
                    }
                    break
                }
            }
        }
    }

    private class ItemGroupRenderInfo(event: SafeClientEvent, group: ItemGroup) : IRenderInfo {
        override val distanceSq = event.player.getDistanceSq(group.getCenter()).toFloat()
        val textComponent = group.textComponent
        val posPrev = group.getCenterPrev()
        val pos = group.getCenter()

        override fun render(camPos: Vec3d, partialTicks: Float) {
            val p = MathUtils.lerp(posPrev, pos, partialTicks.toDouble())
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(p)
            val scale = calcScale(camPos, p)
            GlStateManager.pushMatrix()
            GlStateManager.translate(screenPos.x.toFloat(), screenPos.y.toFloat(), 0.0f)
            GlStateManager.scale(scale, scale, 1.0f)
            drawNametag(screenPos, scale, textComponent)
            GlStateManager.popMatrix()
        }
    }

    private class LivingRenderInfo(event: SafeClientEvent, val entity: EntityLivingBase) : IRenderInfo {
        override val distanceSq = event.player.getDistanceSq(entity).toFloat()
        val textComponent = TextComponent()
        val itemList = ArrayList<Pair<ItemStack, TextComponent>>()
        var empty = true
        var halfHeight = 0.0f
        var halfWidth = 0.0f
        var drawDura = false

        init {
            var isLine1Empty = true
            line1Settings.forEach {
                getContent(event, it.value, entity)?.let {
                    textComponent.add(it)
                    isLine1Empty = false
                }
            }
            if (!isLine1Empty) textComponent.currentLine++
            line2Settings.forEach {
                getContent(event, it.value, entity)?.let { textComponent.add(it) }
            }

            val mainSide = if (invertHand) EnumHandSide.RIGHT else EnumHandSide.LEFT
            val offSide = if (invertHand) EnumHandSide.LEFT else EnumHandSide.RIGHT

            fun addHand(side: EnumHandSide) {
                val hand = if (mc.gameSettings.mainHand == side && mainHand) EnumHand.MAIN_HAND
                else if (mc.gameSettings.mainHand != side && offhand) EnumHand.OFF_HAND
                else return
                val stack = entity.getHeldItem(hand)
                if (!stack.isEmpty) itemList.add(stack to getEnchantmentText(stack))
            }

            addHand(mainSide)
            if (armor) {
                entity.armorInventoryList.reversed().forEach {
                    if (!it.isEmpty) itemList.add(it to getEnchantmentText(it))
                }
            }
            addHand(offSide)

            empty = itemList.all { it.first.isEmpty }
            halfHeight = textComponent.getHeight(2, true) / 2.0f + margins + 2.0f
            halfWidth = (itemList.count { !it.first.isEmpty } * 28) / 2.0f
            drawDura = dura && itemList.any { it.first.isItemDamaged }
        }

        override fun render(camPos: Vec3d, partialTicks: Float) {
            val p = EntityUtils.getInterpolatedPos(entity, partialTicks).add(0.0, entity.height.toDouble() + 0.5, 0.0)
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(p)
            val scale = calcScale(camPos, p)
            GlStateManager.pushMatrix()
            GlStateManager.translate(screenPos.x.toFloat(), screenPos.y.toFloat(), 0.0f)
            GlStateManager.scale(scale, scale, 1.0f)
            if (drawNametagLiving(screenPos, scale, entity, textComponent) && !empty) {
                GlStateManager.pushMatrix()
                GlStateManager.translate(0.0f, -halfHeight, 0.0f)
                GlStateManager.scale(itemScale, itemScale, 1.0f)
                GlStateManager.translate(0.0f, -margins - 2.0f, 0.0f)
                GlStateManager.translate(-halfWidth + 4.0f, -16.0f, 0.0f)
                if (drawDura) GlStateManager.translate(0.0f, -MainFontRenderer.getHeight() - 2.0f, 0.0f)
                itemList.forEach { (stack, enchants) ->
                    if (!stack.isEmpty) drawItem(stack, enchants, drawDura)
                }
                GlStateManager.popMatrix()
            }
            GlStateManager.popMatrix()
        }
    }

    private class XpOrbRenderInfo(event: SafeClientEvent, val entity: EntityXPOrb) : IRenderInfo {
        override val distanceSq = event.player.getDistanceSq(entity).toFloat()
        val textComponent = TextComponent().apply { add("${entity.name} x${entity.xpValue}") }

        override fun render(camPos: Vec3d, partialTicks: Float) {
            val p = EntityUtils.getInterpolatedPos(entity, partialTicks).add(0.0, 1.0, 0.0)
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(p)
            val scale = calcScale(camPos, p)
            GlStateManager.pushMatrix()
            GlStateManager.translate(screenPos.x.toFloat(), screenPos.y.toFloat(), 0.0f)
            GlStateManager.scale(scale, scale, 1.0f)
            drawNametag(screenPos, scale, textComponent)
            GlStateManager.popMatrix()
        }
    }

    private enum class ContentType { NONE, NAME, TYPE, TOTAL_HP, HP, ABSORPTION, PING, DISTANCE, TOTEM_POPS }
    private enum class Page { ENTITY_TYPE, CONTENT, ITEM, RENDERING, COLORS }
}
