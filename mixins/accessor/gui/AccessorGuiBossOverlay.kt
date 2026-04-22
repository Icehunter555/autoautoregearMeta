package dev.wizard.meta.mixins.accessor.gui

import java.util.Map
import java.util.UUID
import net.minecraft.client.gui.BossInfoClient
import net.minecraft.client.gui.GuiBossOverlay
import net.minecraft.world.BossInfo
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(GuiBossOverlay::class)
interface AccessorGuiBossOverlay {
    @Accessor("mapBossInfos")
    fun getMapBossInfos(): Map<UUID, BossInfoClient>

    @Invoker("render")
    fun invokeRender(var1: Int, var2: Int, var3: BossInfo)
}
