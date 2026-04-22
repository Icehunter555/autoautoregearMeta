package dev.wizard.meta.util.text

import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextFormatting

fun formatValue(value: String): String {
    return TextFormatting.GRAY.format("[$value]")
}

fun formatValue(value: Char): String {
    return TextFormatting.GRAY.format("[$value]")
}

fun formatValue(value: Any): String {
    return TextFormatting.GRAY.format("[$value]")
}

fun formatValue(value: Int): String {
    return TextFormatting.GRAY.format("($value)")
}

fun TextFormatting.format(value: Any): String {
    return "$this$value${TextFormatting.RESET}"
}

fun TextFormatting.format(value: Int): String {
    return "$this$value${TextFormatting.RESET}"
}

fun EnumTextColor.format(value: Any): String {
    return "$this$value${TextFormatting.RESET}"
}

val ITextComponent.unformatted: String
    get() = TextFormatting.getTextWithoutFormattingCodes(unformattedText)!!

val ITextComponent.unformattedComponent: String
    get() = TextFormatting.getTextWithoutFormattingCodes(unformattedComponentText)!!
