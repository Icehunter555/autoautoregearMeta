package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.RenderOverlayEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import net.minecraftforge.client.event.RenderGameOverlayEvent

object F3Spoof : Module(
    "F3Spoof",
    category = Category.RENDER,
    description = "Modifies your Debug Menu"
) {
    private val useCustomText by setting(this, BooleanSetting(settingName("Use Custom Text"), false))
    private val customText by setting(this, StringSetting(settingName("Custom Text"), "TrollHack", { useCustomText }))
    private val hideCoords by setting(this, BooleanSetting(settingName("Hide Coordinates"), true))
    private val hideBiome by setting(this, BooleanSetting(settingName("Hide Biome"), false))
    private val hideDirection by setting(this, BooleanSetting(settingName("Hide Direction"), false))
    private val hideBlock by setting(this, BooleanSetting(settingName("Hide Block"), false))
    private val fpsSpoof by setting(this, IntegerSetting(settingName("Spoof Fps"), 0, -600..600, 100))

    init {
        listener<RenderOverlayEvent.Pre> { event ->
            if (event.type == RenderGameOverlayEvent.ElementType.TEXT && event.event is RenderGameOverlayEvent.Text) {
                val textEvent = event.event as RenderGameOverlayEvent.Text
                val replaceText = if (useCustomText) customText else "[Hidden]"
                val hideFps = fpsSpoof == 0

                for (i in textEvent.left.indices) {
                    val line = textEvent.left[i] ?: continue
                    if (hideCoords) {
                        if (line.contains("XYZ")) {
                            textEvent.left[i] = "XYZ: $replaceText"
                        } else if (line.contains("Block:")) {
                            textEvent.left[i] = "Block: $replaceText"
                        } else if (line.contains("Chunk:")) {
                            textEvent.left[i] = "Chunk: $replaceText"
                        }
                    }
                    if (hideDirection) {
                        if (line.contains("Facing:")) {
                            textEvent.left[i] = "Facing: $replaceText"
                        }
                    }
                    if (hideBlock) {
                        if (line.contains("Looking")) {
                            textEvent.left[i] = "Looking at $replaceText"
                        }
                    }
                    if (hideFps) {
                        if (line.contains("fps")) {
                            textEvent.left[i] = "$replaceText fps"
                        }
                    }
                    if (hideBiome) {
                        if (line.contains("Biome:")) {
                            textEvent.left[i] = "Biome: $replaceText"
                        }
                    }
                }

                for (i in textEvent.right.indices) {
                    val line = textEvent.right[i] ?: continue
                    if (hideFps) {
                        if (line.contains("fps")) {
                            textEvent.right[i] = "$replaceText fps"
                        }
                    }
                }
            }
        }
    }
}
