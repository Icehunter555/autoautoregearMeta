package dev.wizard.meta.mixins.patch.chat

import com.google.common.collect.Iterators
import dev.wizard.meta.mixins.PatchedITextComponent
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow

@Mixin(ITextComponent::class)
interface MixinITextComponent : PatchedITextComponent, Iterable<ITextComponent> {
    @Shadow
    fun func_150253_a(): MutableList<ITextComponent>

    override fun inplaceIterator(): Iterator<ITextComponent> {
        return Iterators.concat(Iterators.forArray(this as ITextComponent), this.func_150253_a().iterator())
    }
}
