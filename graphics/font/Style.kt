package dev.wizard.meta.graphics.font

enum class Style(val code: String, val codeChar: Char, val styleConst: Int) {
    REGULAR("§r", 'r', 0),
    BOLD("§l", 'l', 1),
    ITALIC("§o", 'o', 2);
}
