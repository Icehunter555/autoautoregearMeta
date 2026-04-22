package dev.wizard.meta.mixins.patch.chat

import com.google.common.collect.Iterators
import dev.wizard.meta.mixins.PatchedITextComponent
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentBase
import net.minecraft.util.text.TextComponentTranslation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow

@Mixin(TextComponentTranslation::class)
abstract class MixinTextComponentTranslation : TextComponentBase(), PatchedITextComponent {
    @Shadow
    lateinit var field_150278_b: MutableList<ITextComponent>

    @Shadow
    protected abstract fun func_150270_g()

    override fun inplaceIterator(): Iterator<ITextComponent> {
        this.func_150270_g()
        return Iterators.concat(this.field_150278_b.iterator(), this.siblings.iterator())
    }
}
