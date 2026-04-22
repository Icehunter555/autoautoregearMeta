package dev.wizard.meta.mixins

import net.minecraft.util.text.ITextComponent

interface PatchedITextComponent {
    fun inplaceIterator(): Iterator<ITextComponent>
}
