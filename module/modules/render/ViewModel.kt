package dev.wizard.meta.module.modules.render

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide

object ViewModel : Module(
    "ViewModel",
    alias = arrayOf("ItemModel", "SmallShield", "LowerOffhand"),
    category = Category.RENDER,
    description = "change how ur hands are rendered"
) {
    private val switchHands by setting(this, BooleanSetting(settingName("Switch Hands"), false))
    private val separatedHand by setting(this, BooleanSetting(settingName("Separated Hand"), false))
    private val totem by setting(this, BooleanSetting(settingName("Totem"), false))
    val noEatAnimation by setting(this, BooleanSetting(settingName("No Eat Animation"), false))
    val eatX by setting(this, DoubleSetting(settingName("Eat X"), 1.4, -5.0..15.0, 0.1, { !noEatAnimation }))
    val eatY by setting(this, DoubleSetting(settingName("Eat Y"), 1.3, -5.0..15.0, 0.1, { !noEatAnimation }))

    private val page by setting(this, EnumSetting(settingName("Page"), Page.POSITION))
    private val animationMode by setting(this, EnumSetting(settingName("Animation Mode"), AnimationMode.Default, page.atValue(Page.ANIMATION)))
    private val static0 by setting(this, BooleanSetting(settingName("Static"), false, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))

    private val rotateXPre by setting(this, DoubleSetting(settingName("Rotate X Pre"), 0.0, -180.0..180.0, 2.0, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))
    private val rotateYPre by setting(this, DoubleSetting(settingName("Rotate Y Pre"), 0.0, -180.0..180.0, 2.0, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))
    private val rotateZPre by setting(this, DoubleSetting(settingName("Rotate Z Pre"), 0.0, -180.0..180.0, 2.0, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))
    private val rotateXPost by setting(this, DoubleSetting(settingName("Rotate X Post"), 0.0, -180.0..180.0, 2.0, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))
    private val rotateYPost by setting(this, DoubleSetting(settingName("Rotate Y Post"), 0.0, -180.0..180.0, 2.0, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))
    private val rotateZPost by setting(this, DoubleSetting(settingName("Rotate Z Post"), 0.0, -180.0..180.0, 2.0, page.atValue(Page.ANIMATION) and { animationMode == AnimationMode.Custom }))

    val posX by setting(this, FloatSetting(settingName("Pos X"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION)))
    val posY by setting(this, FloatSetting(settingName("Pos Y"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION)))
    val posZ by setting(this, FloatSetting(settingName("Pos Z"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION)))
    val posXR by setting(this, FloatSetting(settingName("Pos X Right"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and separatedHand.atTrue()))
    val posYR by setting(this, FloatSetting(settingName("Pos Y Right"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and separatedHand.atTrue()))
    val posZR by setting(this, FloatSetting(settingName("Pos Z Right"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and separatedHand.atTrue()))
    val posXT by setting(this, FloatSetting(settingName("Pos X Totem"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and totem.atTrue()))
    val posYT by setting(this, FloatSetting(settingName("Pos Y Totem"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and totem.atTrue()))
    val posZT by setting(this, FloatSetting(settingName("Pos Z Totem"), 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and totem.atTrue()))

    val rotateX by setting(this, FloatSetting(settingName("Rotate X"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION)))
    val rotateY by setting(this, FloatSetting(settingName("Rotate Y"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION)))
    val rotateZ by setting(this, FloatSetting(settingName("Rotate Z"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION)))
    val rotateXR by setting(this, FloatSetting(settingName("Rotate X Right"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and separatedHand.atTrue()))
    val rotateYR by setting(this, FloatSetting(settingName("Rotate Y Right"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and separatedHand.atTrue()))
    val rotateZR by setting(this, FloatSetting(settingName("Rotate Z Right"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and separatedHand.atTrue()))
    val rotateXT by setting(this, FloatSetting(settingName("Rotate X Totem"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and totem.atTrue()))
    val rotateYT by setting(this, FloatSetting(settingName("Rotate Y Totem"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and totem.atTrue()))
    val rotateZT by setting(this, FloatSetting(settingName("Rotate Z Totem"), 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and totem.atTrue()))

    val scale by setting(this, FloatSetting(settingName("Scale"), 1.0f, 0.1f..3.0f, 0.025f, page.atValue(Page.SCALE)))
    val scaleR by setting(this, FloatSetting(settingName("Scale Right"), 1.0f, 0.1f..3.0f, 0.025f, page.atValue(Page.SCALE) and separatedHand.atTrue()))
    val scaleT by setting(this, FloatSetting(settingName("Scale Totem"), 1.0f, 0.1f..3.0f, 0.025f, page.atValue(Page.SCALE) and totem.atTrue()))

    val modifyHand by setting(this, BooleanSetting(settingName("Modify Hand"), false))

    init {
        switchHands.valueListeners.add { prev, it ->
            if (prev != it) {
                mc.gameSettings.setOptionValue(GameSettings.Options.MAIN_HAND, if (it) 1 else 0)
            }
        }
    }

    @JvmStatic
    fun translate(stack: ItemStack, hand: EnumHand, player: AbstractClientPlayer) {
        if (isDisabled || (!modifyHand && stack.isEmpty)) return

        val side = getEnumHandSide(player, hand)
        if (totem && player.getHeldItem(hand).item == Items.TOTEM_OF_UNDYING) {
            translate(posXT, posYT, posZT, getSideMultiplier(side))
        } else if (separatedHand) {
            if (side == EnumHandSide.LEFT) translate(posX, posY, posZ, -1.0f)
            else translate(posXR, posYR, posZR, 1.0f)
        } else {
            translate(posX, posY, posZ, getSideMultiplier(side))
        }
    }

    private fun translate(x: Float, y: Float, z: Float, sideMultiplier: Float) {
        GlStateManager.translate(x * sideMultiplier, y, -z)
    }

    @JvmStatic
    fun rotateAndScale(stack: ItemStack, hand: EnumHand, player: AbstractClientPlayer, swingProgress: Float) {
        if (isDisabled || (!modifyHand && stack.isEmpty)) return

        val side = getEnumHandSide(player, hand)
        if (side == EnumHandSide.RIGHT && animationMode != AnimationMode.Default) {
            if (animationMode != AnimationMode.Classic1) applyVanillaAnimation(0.0f)
            applyAnimation(swingProgress.toDouble())
        } else if (side == EnumHandSide.RIGHT) {
            applyVanillaAnimation(swingProgress)
        }

        if (totem && player.getHeldItem(hand).item == Items.TOTEM_OF_UNDYING) {
            rotate(rotateXT, rotateYT, rotateZT, getSideMultiplier(side))
            GlStateManager.scale(scaleT, scaleT, scaleT)
        } else if (separatedHand) {
            if (side == EnumHandSide.LEFT) {
                rotate(rotateX, rotateY, rotateZ, -1.0f)
                GlStateManager.scale(scale, scale, scale)
            } else {
                rotate(rotateXR, rotateYR, rotateZR, 1.0f)
                GlStateManager.scale(scaleR, scaleR, scaleR)
            }
        } else {
            rotate(rotateX, rotateY, rotateZ, getSideMultiplier(side))
            GlStateManager.scale(scale, scale, scale)
        }
    }

    private fun applyAnimation(progress: Double) {
        when (animationMode) {
            AnimationMode.Glide -> {
                val p = Math.abs(Math.abs(progress * 2.0 - 1.0) - 1.0)
                GlStateManager.translate(0.0, (1.0 - p) * 0.15, 0.0)
                rotateAnimation(lerp(-86.0, -102.0, p), lerp(32.0, -44.0, p), lerp(56.0, 82.0, p))
            }
            AnimationMode.Slide1 -> {
                val p = Math.abs(Math.abs(progress * 2.0 - 1.0) - 1.0)
                rotateAnimation(lerp(-176.0, -180.0, p), lerp(26.0, -70.0, p), lerp(82.0, 90.0, p))
            }
            AnimationMode.Slide2 -> {
                val p = Math.sin(Math.sqrt(progress) * Math.PI)
                rotateAnimation(6.0, lerp(-62.0, 20.0, p), lerp(96.0, 92.0, p))
            }
            AnimationMode.Slide3 -> {
                val p = Math.sin(Math.sqrt(progress) * Math.PI)
                rotateAnimation(36.0, lerp(-70.0, 18.0, p), 104.0)
            }
            AnimationMode.Classic1 -> {
                val f = Math.sin(progress * progress * Math.PI).toFloat()
                val f1 = Math.sin(Math.sqrt(progress) * Math.PI).toFloat()
                GlStateManager.rotate(45.0f + f * -20.0f, 0.0f, 1.0f, 0.0f)
                GlStateManager.rotate(f1 * -20.0f, 0.0f, 0.0f, 1.0f)
                GlStateManager.rotate(f1 * -80.0f, 1.0f, 0.0f, 0.0f)
                GlStateManager.rotate(-45.0f, 0.0f, 1.0f, 0.0f)
            }
            AnimationMode.Classic2 -> {
                val p = Math.sin(Math.sqrt(progress) * Math.PI)
                rotateAnimation(lerp(0.0, -100.0, p), lerp(0.0, -36.0, p), lerp(0.0, 36.0, p))
            }
            AnimationMode.Classic3 -> {
                val p = Math.sin(Math.sqrt(progress) * Math.PI)
                rotateAnimation(lerp(0.0, -90.0, p), lerp(0.0, -30.0, p), lerp(0.0, 18.0, p))
            }
            AnimationMode.Custom -> {
                val p = ease(progress.toFloat())
                rotateAnimation(lerp(rotateXPre, rotateXPost, p), lerp(rotateYPre, rotateYPost, p), lerp(rotateZPre, rotateZPost, p))
            }
            else -> {}
        }
    }

    private fun ease(x: Float): Float {
        return if (static0) Math.abs(Math.abs(x * 2.0 - 1.0) - 1.0).toFloat()
        else Math.sin(Math.sqrt(x.toDouble()) * Math.PI).toFloat()
    }

    private fun applyVanillaAnimation(swingProgress: Float) {
        val v = -0.4f * Math.sin(Math.sqrt(swingProgress.toDouble()) * Math.PI).toFloat()
        val v1 = 0.2f * Math.sin(Math.sqrt(swingProgress.toDouble()) * Math.PI * 2).toFloat()
        val v2 = -0.2f * Math.sin(swingProgress.toDouble() * Math.PI).toFloat()
        GlStateManager.translate(v, v1, v2)
        val f = Math.sin(swingProgress.toDouble() * swingProgress.toDouble() * Math.PI).toFloat()
        val f1 = Math.sin(Math.sqrt(swingProgress.toDouble()) * Math.PI).toFloat()
        GlStateManager.rotate(45.0f + f * -20.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(f1 * -20.0f, 0.0f, 0.0f, 1.0f)
        GlStateManager.rotate(f1 * -80.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(-45.0f, 0.0f, 1.0f, 0.0f)
    }

    private fun rotateAnimation(x: Double, y: Double, z: Double) {
        GlStateManager.rotate(x.toFloat(), 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(y.toFloat(), 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(z.toFloat(), 0.0f, 0.0f, 1.0f)
    }

    private fun lerp(start: Double, end: Double, progress: Double): Double = start + (end - start) * progress
    private fun lerp(start: Double, end: Double, progress: Float): Double = start + (end - start) * progress

    private fun rotate(x: Float, y: Float, z: Float, sideMultiplier: Float) {
        GlStateManager.rotate(x, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(y * sideMultiplier, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(z * sideMultiplier, 0.0f, 0.0f, 1.0f)
    }

    private fun getEnumHandSide(player: AbstractClientPlayer, hand: EnumHand): EnumHandSide {
        return if (hand == EnumHand.MAIN_HAND) player.primaryHand else player.primaryHand.opposite()
    }

    private fun getSideMultiplier(side: EnumHandSide): Float = if (side == EnumHandSide.LEFT) -1.0f else 1.0f

    private enum class Page { ANIMATION, POSITION, ROTATION, SCALE }
    private enum class AnimationMode { Default, Slide1, Slide2, Slide3, Classic1, Classic2, Classic3, Glide, Custom }
}
