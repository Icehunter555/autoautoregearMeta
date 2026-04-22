package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.gui.IGuiScreen

open class BooleanSlider(
    screen: IGuiScreen,
    name: CharSequence,
    description: CharSequence = "",
    visibility: (() -> Boolean)? = null
) : Slider(screen, name, description, visibility)
