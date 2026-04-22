package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.FogColorEvent
import dev.wizard.meta.event.events.render.FogDensityEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketTimeUpdate
import net.minecraft.util.ResourceLocation
import java.lang.reflect.Field

object Ambiance : Module(
    "Ambiance",
    alias = arrayOf("FogColor", "shader", "timechanger"),
    category = Category.RENDER,
    description = "ambient surroundings"
) {
    const val tintSky = true

    private val skyTintColor by setting(this, ColorSetting(settingName("Sky Tint Color"), ColorRGB(255, 255, 255), { tintSky }))
    private val tintWorld by setting(this, BooleanSetting(settingName("Tint World"), false))
    private val worldTintColor by setting(this, ColorSetting(settingName("World Tint Color"), ColorRGB(255, 255, 255), { tintWorld }))
    private val useSaturation by setting(this, BooleanSetting(settingName("Use Saturation"), false, { tintWorld }))
    private val saturation by setting(this, FloatSetting(settingName("Saturation"), 0.5f, 0.0f..1.0f, 0.01f, { tintWorld && useSaturation }))

    private val fogEnabled by setting(this, BooleanSetting(settingName("Colored Fog"), false))
    private val fogColorOverworld by setting(this, ColorSetting(settingName("Fog Color OW"), ColorRGB(111, 166, 222), { fogEnabled }))
    private val fogDensityOverworld by setting(this, FloatSetting(settingName("Fog Density OW"), 0.1f, 0.0f..1.0f, 0.01f, { fogEnabled }))
    private val fogColorNether by setting(this, ColorSetting(settingName("Fog Color Nether"), ColorRGB(255, 64, 32), { fogEnabled }))
    private val fogDensityNether by setting(this, FloatSetting(settingName("Fog Density Nether"), 0.2f, 0.0f..1.0f, 0.01f, { fogEnabled }))
    private val fogColorEnd by setting(this, ColorSetting(settingName("Fog Color End"), ColorRGB(150, 0, 150), { fogEnabled }))
    private val fogDensityEnd by setting(this, FloatSetting(settingName("Fog Density End"), 0.15f, 0.0f..1.0f, 0.01f, { fogEnabled }))

    private val timeEnabled by setting(this, BooleanSetting(settingName("Time Enabled"), false))
    private val time by setting(this, IntegerSetting(settingName("Time"), 6000, 0..24000, 20, { timeEnabled }))
    private val weather by setting(this, EnumSetting(settingName("Weather"), WeatherMode.NORMAL))

    val endSky by setting(this, BooleanSetting(settingName("End Sky"), false))
    val biomeColors by setting(this, BooleanSetting(settingName("Biome Colors"), false))
    private val grassColor by setting(this, ColorSetting(settingName("Grass Color"), ColorRGB(145, 189, 89), { biomeColors }))
    private val waterColor by setting(this, ColorSetting(settingName("Water Color"), ColorRGB(63, 118, 228), { biomeColors }))
    private val foliageColor by setting(this, ColorSetting(settingName("Foliage Color"), ColorRGB(119, 171, 47), { biomeColors }))

    private val shader by setting(this, EnumSetting(settingName("Shader"), Shaders.NONE))

    var red = skyTintColor.r
    var green = skyTintColor.g
    var blue = skyTintColor.b

    private var activeShader = "none"
    private var forgeLightPipelineEnabled = false

    init {
        listener<FogColorEvent> {
            if (fogEnabled) {
                when (getCurrentDimension()) {
                    0 -> {
                        it.red = fogColorOverworld.rFloat
                        it.green = fogColorOverworld.gFloat
                        it.blue = fogColorOverworld.bFloat
                    }
                    -1 -> {
                        it.red = fogColorNether.rFloat
                        it.green = fogColorNether.gFloat
                        it.blue = fogColorNether.bFloat
                    }
                    1 -> {
                        it.red = fogColorEnd.rFloat
                        it.green = fogColorEnd.gFloat
                        it.blue = fogColorEnd.bFloat
                    }
                }
            }
        }

        listener<FogDensityEvent> {
            if (fogEnabled) {
                val density = when (getCurrentDimension()) {
                    0 -> fogDensityOverworld
                    -1 -> fogDensityNether
                    1 -> fogDensityEnd
                    else -> fogDensityOverworld
                }
                it.density = density
                it.cancel()
            }
        }

        listener<PacketEvent.Receive> {
            if (timeEnabled && it.packet is SPacketTimeUpdate) {
                it.cancel()
            }
        }

        safeListener<TickEvent.Post> {
            if (timeEnabled) {
                world.worldTime = time.toLong()
            }
        }

        listener<TickEvent.Pre> {
            ensureReload()
            red = skyTintColor.r
            green = skyTintColor.g
            blue = skyTintColor.b
        }

        onEnable {
            if (shader != Shaders.NONE) {
                loadShader()
            }
            if (tintWorld) {
                disableForgeLightPipeline()
                reloadWorldRenderers()
                mc.entityRenderer?.let { renderer ->
                    try {
                        val field = renderer.javaClass.getDeclaredField("lightmapUpdateNeeded")
                        field.isAccessible = true
                        field.setBoolean(renderer, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        onDisable {
            unloadShader()
            if (tintWorld) {
                restoreForgeLightPipeline()
                reloadWorldRenderers()
            }
        }
    }

    private fun loadShader() {
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer) {
            mc.entityRenderer.shaderGroup?.deleteShaderGroup()
            try {
                mc.entityRenderer.loadShader(ResourceLocation("shaders/post/${getShaderLocation()}.json"))
                activeShader = getShaderLocation()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (mc.entityRenderer.shaderGroup != null && mc.currentScreen == null) {
            mc.entityRenderer.shaderGroup.deleteShaderGroup()
        }
    }

    private fun unloadShader() {
        mc.entityRenderer.shaderGroup?.deleteShaderGroup()
    }

    private fun getShaderLocation(): String {
        return when (shader) {
            Shaders.NONE -> "none"
            Shaders.Art -> "art"
            Shaders.Bits -> "bits"
            Shaders.Blobs -> "blobs"
            Shaders.Blur -> "blur"
            Shaders.Bumpy -> "bumpy"
            Shaders.Saturation -> "color_convolve"
            Shaders.Creep -> "creeper"
            Shaders.Deconverge -> "deconverge"
            Shaders.Desaturate -> "desaturate"
            Shaders.Flip -> "flip"
            Shaders.Green -> "green"
            Shaders.Invert -> "invert"
            Shaders.Notch -> "notch"
            Shaders.NTSC -> "ntsc"
            Shaders.Pencil -> "pencil"
            Shaders.Sobel -> "sobel"
            Shaders.Spider -> "spider"
            Shaders.Wobble -> "wobble"
        }
    }

    private fun ensureReload() {
        if (activeShader != getShaderLocation()) {
            unloadShader()
            if (shader != Shaders.NONE) {
                loadShader()
            }
        }
    }

    private fun disableForgeLightPipeline() {
        try {
            val forgeClass = Class.forName("net.minecraftforge.common.ForgeModContainer")
            val field = forgeClass.getDeclaredField("forgeLightPipelineEnabled")
            val accessible = field.isAccessible
            field.isAccessible = true
            forgeLightPipelineEnabled = field.getBoolean(null)
            field.set(null, false)
            field.isAccessible = accessible
        } catch (e: Exception) {
        }
    }

    private fun restoreForgeLightPipeline() {
        try {
            val forgeClass = Class.forName("net.minecraftforge.common.ForgeModContainer")
            val field = forgeClass.getDeclaredField("forgeLightPipelineEnabled")
            val accessible = field.isAccessible
            field.isAccessible = true
            field.set(null, forgeLightPipelineEnabled)
            field.isAccessible = accessible
        } catch (e: Exception) {
        }
    }

    private fun reloadWorldRenderers() {
        if (mc.world != null && mc.renderViewEntity != null && mc.renderGlobal != null && mc.gameSettings != null) {
            mc.renderGlobal.loadRenderers()
        }
    }

    private fun getCurrentDimension(): Int {
        return mc.world?.provider?.dimension ?: 0
    }

    @JvmStatic
    fun getGrass(): Int = grassColor.toArgb()

    @JvmStatic
    fun getFoliage(): Int = foliageColor.toArgb()

    @JvmStatic
    fun getWater(): Int = waterColor.toArgb()

    @JvmStatic
    fun getTheWorldTintColor(): Int {
        return if (tintWorld) worldTintColor.toArgb() else ColorRGB(255, 255, 255).toArgb()
    }

    @JvmStatic
    fun shouldUseSaturation(): Boolean = isEnabled && tintWorld && useSaturation

    @JvmStatic
    fun getSaturationValue(): Float = if (shouldUseSaturation()) saturation else 1.0f

    @JvmStatic
    fun isTintWorldEnabled(): Boolean = isEnabled && tintWorld

    @JvmStatic
    fun toRGBAArray(colorBuffer: Int): IntArray {
        return intArrayOf(colorBuffer shr 16 and 0xFF, colorBuffer shr 8 and 0xFF, colorBuffer and 0xFF)
    }

    @JvmStatic
    fun shouldCancelWeather(): Boolean = isEnabled && weather != WeatherMode.NORMAL

    enum class Shaders {
        NONE, Art, Bits, Blobs, Blur, Bumpy, Saturation, Creep, Deconverge, Desaturate, Flip, Green, Invert, Notch, NTSC, Pencil, Sobel, Spider, Wobble
    }

    private enum class WeatherMode(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"),
        NONE("None")
    }
}
