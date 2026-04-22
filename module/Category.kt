package dev.wizard.meta.module

import dev.wizard.meta.translation.TranslateType
import dev.wizard.meta.util.interfaces.DisplayEnum

enum class Category(override val displayName: CharSequence) : DisplayEnum {
    COMBAT(TranslateType.COMMON.commonKey("Combat")),
    MISC(TranslateType.COMMON.commonKey("Misc")),
    EXPLOIT(TranslateType.COMMON.commonKey("Exploit")),
    MOVEMENT(TranslateType.COMMON.commonKey("Movement")),
    PLAYER(TranslateType.COMMON.commonKey("Player")),
    RENDER(TranslateType.COMMON.commonKey("Render")),
    BETA(TranslateType.COMMON.commonKey("Beta")),
    CLIENT(TranslateType.COMMON.commonKey("Client"));

    override fun toString(): String {
        return this.displayString
    }
}
