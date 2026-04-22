package dev.wizard.meta.module.modules.render

import com.google.gson.reflect.TypeToken
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.collection.CollectionSetting
import dev.wizard.meta.util.threads.onMainThread
import net.minecraft.block.state.IBlockState
import java.util.*

object XRay : Module(
    "XRay",
    category = Category.RENDER,
    description = "Lets you see through blocks"
) {
    private val defaultVisibleList = linkedSetOf("minecraft:diamond_ore", "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:portal", "minecraft:cobblestone")

    val blockList = setting(this, CollectionSetting(settingName("Visible List"), defaultVisibleList, object : TypeToken<LinkedHashSet<String>>() {}.type))

    @JvmStatic
    fun shouldReplace(state: IBlockState): Boolean {
        if (!INSTANCE.isEnabled) return false
        return !blockList.contains(state.block.registryName.toString())
    }

    init {
        onToggle {
            onMainThread {
                mc.renderGlobal?.loadRenderers()
            }
        }

        blockList.editListeners.add {
            onMainThread {
                mc.renderGlobal?.loadRenderers()
            }
        }
    }
}
