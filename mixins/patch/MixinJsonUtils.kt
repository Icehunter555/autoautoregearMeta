package dev.wizard.meta.mixins.patch

import com.google.gson.Gson
import net.minecraft.util.JsonUtils
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.Shadow

@Pseudo
@Mixin(JsonUtils::class)
abstract class MixinJsonUtils {
    companion object {
        @Shadow
        @JvmStatic
        fun <T> func_188176_a(gsonIn: Gson, json: String, adapter: Class<T>, lenient: Boolean): T? = null

        @Overwrite
        @JvmStatic
        fun <T> func_188178_a(gsonIn: Gson, json: String, adapter: Class<T>): T? {
            return func_188176_a(gsonIn, json, adapter, true)
        }
    }
}
