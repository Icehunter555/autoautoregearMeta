package dev.wizard.meta.module.modules.player

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module

object NoRotate : Module(
    "NoRotate",
    alias = arrayOf("AntiForceLook"),
    category = Category.PLAYER,
    description = "Stops server packets from turning your head"
)
