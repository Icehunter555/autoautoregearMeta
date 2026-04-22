package dev.wizard.meta.mixins.core

import dev.wizard.meta.event.events.GuiEvent
import dev.wizard.meta.event.events.ProcessKeyBindEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.mixins.accessor.player.AccessorEntityPlayerSP
import dev.wizard.meta.mixins.accessor.player.AccessorPlayerControllerMP
import dev.wizard.meta.module.modules.player.FastUse
import dev.wizard.meta.module.modules.player.Interactions
import dev.wizard.meta.module.modules.player.MultiTask
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.module.modules.player.UnfocusedFps
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.settings.GameSettings
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.LWJGLException
import org.lwjgl.opengl.ContextAttribs
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.PixelFormat
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.io.IOException

@Mixin(Minecraft::class)
abstract class MixinMinecraft {
    @Shadow
    lateinit var field_71441_e: WorldClient

    @Shadow
    lateinit var field_71439_g: EntityPlayerSP

    @Shadow
    var field_71462_r: GuiScreen? = null

    @Shadow
    lateinit var field_71474_y: GameSettings

    @Shadow
    lateinit var field_71442_b: PlayerControllerMP

    @Shadow
    var field_71476_x: RayTraceResult? = null

    private var type: RayTraceResult.Type? = null
    private var handActive = false
    private var isHittingBlock = false

    @Shadow
    protected abstract fun func_147116_af()

    @Shadow
    @Throws(LWJGLException::class, IOException::class)
    protected abstract fun func_71384_a()

    @Redirect(method = ["createDisplay"], at = At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create(Lorg/lwjgl/opengl/PixelFormat;)V", remap = false))
    @Throws(LWJGLException::class)
    fun `Redirect$createDisplay$INVOKE$Display$create`(pixel_format: PixelFormat) {
        Display.create(pixel_format, ContextAttribs(4, 5).withProfileCompatibility(true))
    }

    @ModifyVariable(method = ["displayGuiScreen"], at = At(value = "HEAD"), ordinal = 0, argsOnly = true)
    fun `displayGuiScreen$ModifyVariable$HEAD`(value: GuiScreen?): GuiScreen? {
        val current = this.field_71462_r
        if (current != null) {
            val closed = GuiEvent.Closed(current)
            closed.post()
        }
        val displayed = GuiEvent.Displayed(value)
        displayed.post()
        return displayed.screen
    }

    @ModifyVariable(method = ["displayGuiScreen"], at = At(value = "STORE", ordinal = 0), ordinal = 0, argsOnly = true)
    fun `displayGuiScreen$ModifyVariable$STORE`(value: GuiScreen?): GuiScreen? {
        return value
    }

    @Inject(method = ["runGameLoop"], at = [At(value = "INVOKE", target = "Lnet/minecraft/util/Timer;updateTimer()V", shift = At.Shift.BEFORE)])
    fun `runGameLoop$Inject$INVOKE$updateTimer`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76320_a("trollRunGameLoop")
        RunGameLoopEvent.Start.post()
        Wrapper.getMinecraft().field_71424_I.func_76319_b()
    }

    @Inject(method = ["runGameLoop"], at = [At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 0, shift = At.Shift.AFTER)])
    fun `runGameLoop$INVOKE$endSection`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76320_a("trollRunGameLoop")
        RunGameLoopEvent.Tick.post()
        Wrapper.getMinecraft().field_71424_I.func_76319_b()
    }

    @Inject(method = ["runGameLoop"], at = [At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE)])
    fun `runGameLoop$Inject$INVOKE$endStartSection`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76318_c("trollRunGameLoop")
        RunGameLoopEvent.Render.post()
    }

    @Inject(method = ["runGameLoop"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isFramerateLimitBelowMax()Z", shift = At.Shift.BEFORE)])
    fun `runGameLoop$Inject$INVOKE$isFramerateLimitBelowMax`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76320_a("trollRunGameLoop")
        RunGameLoopEvent.End.post()
        Wrapper.getMinecraft().field_71424_I.func_76319_b()
    }

    @Redirect(method = ["runGameLoop"], at = At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false))
    fun `runGameLoop$Redirect$INVOKE$yield`() {
    }

    @Inject(method = ["runTick"], at = [At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", ordinal = 0)])
    fun `runTick$Inject$FIELD$currentScreen$0`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76318_c("gui")
    }

    @Inject(method = ["runTick"], at = [At(value = "HEAD")])
    fun `runTick$Inject$HEAD`(ci: CallbackInfo) {
        TickEvent.Pre.post()
    }

    @Inject(method = ["runTick"], at = [At(value = "RETURN")])
    fun `runTick$Inject$RETURN`(ci: CallbackInfo) {
        TickEvent.Post.post()
    }

    @Inject(method = ["processKeyBinds"], at = [At(value = "HEAD")])
    fun `processKeyBinds$Inject$HEAD`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76320_a("trollProcessKeyBind")
        ProcessKeyBindEvent.Pre.post()
        Wrapper.getMinecraft().field_71424_I.func_76319_b()
    }

    @Inject(method = ["processKeyBinds"], at = [At(value = "RETURN")])
    fun `processKeyBinds$Inject$RETURN`(ci: CallbackInfo) {
        Wrapper.getMinecraft().field_71424_I.func_76320_a("trollProcessKeyBind")
        ProcessKeyBindEvent.Post.post()
        Wrapper.getMinecraft().field_71424_I.func_76319_b()
    }

    @Inject(method = ["processKeyBinds"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z", shift = At.Shift.BEFORE, ordinal = 2)])
    fun `processKeyBinds$Inject$INVOKE$isKeyDown`(ci: CallbackInfo) {
        if (MultiTask.isEnabled) {
            while (this.field_71474_y.field_74312_F.func_151468_f()) {
                this.func_147116_af()
            }
        }
    }

    @Inject(method = ["clickMouse"], at = [At(value = "HEAD")], cancellable = true)
    fun `clickMouse$Inject$HEAD`(ci: CallbackInfo) {
        val result = this.field_71476_x
        if (Interactions.isNoAirHitEnabled && result != null && result.typeOfHit == RayTraceResult.Type.MISS) {
            ci.cancel()
        }
    }

    @Inject(method = ["clickMouse"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingArm(Lnet/minecraft/util/EnumHand;)V")], cancellable = true)
    fun `clickMouse$Inject$INVOKE$swingArm`(ci: CallbackInfo) {
        val rayTraceResult = this.field_71476_x
        if (PacketMine.isEnabled && PacketMine.noSwing && rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            val miningInfo = PacketMine.miningInfo
            if (miningInfo != null && rayTraceResult.blockPos == miningInfo.pos) {
                ci.cancel()
            }
        }
    }

    @Inject(method = ["rightClickMouse"], at = [At(value = "HEAD")])
    fun `rightClickMouse$Inject$HEAD`(ci: CallbackInfo) {
        val rayTraceResult = this.field_71476_x
        if (MultiTask.isEnabled) {
            this.isHittingBlock = this.field_71442_b.isHittingBlock
            (this.field_71442_b as AccessorPlayerControllerMP).trollSetIsHittingBlock(false)
        }
        if (Interactions.shouldCancel() && rayTraceResult != null) {
            this.type = rayTraceResult.typeOfHit
            rayTraceResult.typeOfHit = RayTraceResult.Type.MISS
        }
        if (Interactions.isPrioritizingEating && rayTraceResult != null) {
            this.type = rayTraceResult.typeOfHit
            rayTraceResult.typeOfHit = RayTraceResult.Type.MISS
        }
    }

    @Inject(method = ["rightClickMouse"], at = [At(value = "RETURN")])
    fun `rightClickMouse$Inject$RETURN`(ci: CallbackInfo) {
        if (MultiTask.isEnabled && !this.field_71442_b.isHittingBlock) {
            (this.field_71442_b as AccessorPlayerControllerMP).trollSetIsHittingBlock(this.isHittingBlock)
        }
        val cache2 = this.type
        this.type = null
        val rayTraceResult = this.field_71476_x
        if (cache2 != null && rayTraceResult != null) {
            rayTraceResult.typeOfHit = cache2
        }
    }

    @Inject(method = ["sendClickBlockToController"], at = [At(value = "HEAD")])
    fun `sendClickBlockToController$Inject$HEAD`(leftClick: Boolean, ci: CallbackInfo) {
        if (MultiTask.isEnabled) {
            this.handActive = this.field_71439_g.isHandActive
            (this.field_71439_g as AccessorEntityPlayerSP).trollSetHandActive(false)
        }
    }

    @Inject(method = ["sendClickBlockToController"], at = [At(value = "RETURN")])
    fun `sendClickBlockToController$Inject$RETURN`(leftClick: Boolean, ci: CallbackInfo) {
        if (MultiTask.isEnabled && !this.field_71439_g.isHandActive) {
            (this.field_71439_g as AccessorEntityPlayerSP).trollSetHandActive(this.handActive)
        }
    }

    @Inject(method = ["getLimitFramerate"], at = [At(value = "HEAD")], cancellable = true)
    fun `getLimitFramerate$Inject$HEAD`(cir: CallbackInfoReturnable<Int>) {
        UnfocusedFps.handleGetLimitFramerate(cir)
    }

    @Inject(method = ["rightClickMouse"], at = [At(value = "RETURN", ordinal = 0)])
    fun `rightClickMouseBlock$Inject$RETURN$0`(ci: CallbackInfo) {
        FastUse.updateRightClickDelay()
    }

    @Inject(method = ["rightClickMouse"], at = [At(value = "RETURN", ordinal = 1)])
    fun `rightClickMouseBlock$Inject$RETURN$1`(ci: CallbackInfo) {
        FastUse.updateRightClickDelay()
    }

    @Inject(method = ["rightClickMouse"], at = [At(value = "RETURN", ordinal = 2)])
    fun `rightClickMouseBlock$Inject$RETURN$2`(ci: CallbackInfo) {
        FastUse.updateRightClickDelay()
    }

    @Inject(method = ["rightClickMouse"], at = [At(value = "RETURN", ordinal = 3)])
    fun `rightClickMouseBlock$Inject$RETURN$3`(ci: CallbackInfo) {
        FastUse.updateRightClickDelay()
    }
}
