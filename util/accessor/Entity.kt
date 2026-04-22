package dev.wizard.meta.util.accessor

import dev.wizard.meta.mixins.accessor.entity.AccessorEntityAreaAffectCloud
import dev.wizard.meta.mixins.accessor.entity.AccessorEntityLivingBase
import net.minecraft.entity.EntityAreaEffectCloud
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.PotionType

fun EntityLivingBase.onItemUseFinish() {
    (this as AccessorEntityLivingBase).trollInvokeOnItemUseFinish()
}

val EntityAreaEffectCloud.effect: PotionType
    get() = (this as AccessorEntityAreaAffectCloud).potion
