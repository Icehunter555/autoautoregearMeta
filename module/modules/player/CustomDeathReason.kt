package dev.wizard.meta.module.modules.player

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString

object CustomDeathReason : Module(
    "CustomDeathReason",
    category = Category.PLAYER,
    description = "set a custom reason for why u died"
) {
    private val reason by setting(this, StringSetting(settingName("Reason"), "Skill Issue"))

    @JvmStatic
    fun getReason(): ITextComponent = TextComponentString(reason)
}
