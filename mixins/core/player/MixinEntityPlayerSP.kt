package dev.wizard.meta.mixins.core.player

import com.mojang.authlib.GameProfile
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.manager.managers.MessageManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.modules.exploit.AntiSand
import dev.wizard.meta.module.modules.misc.PortalTweaks
import dev.wizard.meta.module.modules.movement.Velocity
import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.entity.Entity
import net.minecraft.entity.MoverType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(value = [EntityPlayerSP::class], priority = 0x7FFFFFFF)
abstract class MixinEntityPlayerSP(worldIn: World, gameProfileIn: GameProfile) : EntityPlayer(worldIn, gameProfileIn) {
    @Shadow
    @Final
    lateinit var field_71174_a: NetHandlerPlayClient

    @Shadow
    protected lateinit var field_71159_c: Minecraft

    @Shadow
    private var field_175172_bI = 0.0

    @Shadow
    private var field_175166_bJ = 0.0

    @Shadow
    private var field_175167_bK = 0.0

    @Shadow
    private var field_175164_bL = 0f

    @Shadow
    private var field_175168_bP = 0

    @Shadow
    private var field_175165_bM = 0f

    @Shadow
    private var field_175171_bO = false

    @Shadow
    private var field_175170_bN = false

    @Shadow
    private var field_184841_cd = false

    @Shadow
    private var field_189811_cr = false

    @Shadow
    protected abstract fun func_175160_A(): Boolean

    @Shadow
    protected abstract fun func_189810_i(var1: Float, var2: Float)

    @Redirect(method = ["onLivingUpdate"], at = At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;closeScreen()V"))
    fun closeScreen(player: EntityPlayerSP) {
        if (!PortalTweaks.portalChat()) {
            player.closeScreen()
        }
    }

    @Redirect(method = ["onLivingUpdate"], at = At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    fun closeScreen(minecraft: Minecraft, screen: GuiScreen?) {
        if (!PortalTweaks.portalChat()) {
            Wrapper.getMinecraft().displayGuiScreen(screen)
        }
    }

    @Inject(method = ["move"], at = [At("HEAD")], cancellable = true)
    fun move$Inject$HEAD(type2: MoverType, x: Double, y: Double, z: Double, ci: CallbackInfo) {
        when (type2) {
            MoverType.SELF -> {
                val player = Wrapper.getPlayer() ?: return
                val event = PlayerMoveEvent.Pre(player)
                event.post()
                if (event.isModified) {
                    val prevX = this.posX
                    val prevZ = this.posZ
                    super.move(type2, event.x, event.y, event.z)
                    this.func_189810_i((this.posX - prevX).toFloat(), (this.posZ - prevZ).toFloat())
                    PlayerMoveEvent.Post.post()
                    ci.cancel()
                }
            }
            MoverType.PLAYER -> {}
            else -> {
                if (AntiSand.isEnabled || Velocity.shouldCancelMove()) {
                    ci.cancel()
                }
            }
        }
    }

    @Inject(method = ["move"], at = [At("RETURN")])
    fun move$Inject$RETURN(type2: MoverType, x: Double, y: Double, z: Double, ci: CallbackInfo) {
        PlayerMoveEvent.Post.post()
    }

    @Inject(method = ["isCurrentViewEntity"], at = [At("HEAD")], cancellable = true)
    protected fun mixinIsCurrentViewEntity(cir: CallbackInfoReturnable<Boolean>) {
        if (Freecam.isEnabled && Freecam.cameraGuy != null) {
            cir.returnValue = this.field_71159_c.renderViewEntity === Freecam.cameraGuy
        }
    }

    @Inject(method = ["sendChatMessage"], at = [At("HEAD")])
    fun sendChatMessage(message2: String, ci: CallbackInfo) {
        MessageManager.lastPlayerMessage = message2
    }

    @Inject(method = ["swingArm"], at = [At("HEAD")], cancellable = true)
    fun swingArm$Inject$HEAD(hand2: EnumHand, ci: CallbackInfo) {
    }

    @Inject(method = ["onUpdate"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V", shift = At.Shift.AFTER)])
    private fun onUpdateInvokeOnUpdateWalkingPlayer(ci: CallbackInfo) {
        val serverSidePos = PlayerPacketManager.position
        val serverSideRotationX = PlayerPacketManager.rotationX
        val serverSideRotationY = PlayerPacketManager.rotationY
        this.field_175172_bI = serverSidePos.xCoord
        this.field_175166_bJ = serverSidePos.yCoord
        this.field_175167_bK = serverSidePos.zCoord
        this.field_175164_bL = serverSideRotationX
        this.field_175165_bM = serverSideRotationY
        this.rotationYawHead = serverSideRotationX
    }

    @Inject(method = ["onUpdateWalkingPlayer"], at = [At("HEAD")], cancellable = true)
    private fun onUpdateWalkingPlayerHead(ci: CallbackInfo) {
        var position = Vec3d(this.posX, this.entityBoundingBox.minY, this.posZ)
        var rotationX = this.rotationYaw
        var rotationY = this.rotationPitch
        var onGround = this.onGround
        val eventPre = OnUpdateWalkingPlayerEvent.Pre(position, rotationX, rotationY, onGround)
        eventPre.post()
        PlayerPacketManager.applyPacket(eventPre)
        if (eventPre.isCancelled) {
            ci.cancel()
            if (!eventPre.cancelAll) {
                position = eventPre.position
                rotationX = eventPre.rotationX
                rotationY = eventPre.rotationY
                onGround = eventPre.isOnGround
                val moving = !eventPre.cancelMove && this.isMoving(position)
                val rotating = !eventPre.cancelRotate && this.isRotating(rotationX, rotationY)
                this.sendSprintPacket()
                this.sendSneakPacket()
                this.sendPlayerPacket(moving, rotating, position, rotationX, rotationY, onGround)
                this.field_184841_cd = onGround
            }
            ++this.field_175168_bP
            this.field_189811_cr = this.field_71159_c.gameSettings.keyBindSprint.isKeyDown
        }
        val eventPos = OnUpdateWalkingPlayerEvent.Post(position, rotationX, rotationY, onGround)
        eventPos.post()
    }

    private fun sendSprintPacket() {
        val sprinting = this.isSprinting
        if (sprinting != this.field_175171_bO) {
            if (sprinting) {
                this.field_71174_a.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.START_SPRINTING))
            } else {
                this.field_71174_a.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SPRINTING))
            }
            this.field_175171_bO = sprinting
        }
    }

    private fun sendSneakPacket() {
        val sneaking = this.isSneaking
        if (sneaking != this.field_175170_bN) {
            if (sneaking) {
                this.field_71174_a.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING))
            } else {
                this.field_71174_a.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING))
            }
            this.field_175170_bN = sneaking
        }
    }

    private fun sendPlayerPacket(moving: Boolean, rotating: Boolean, position: Vec3d, rotationX: Float, rotationY: Float, onGround: Boolean) {
        var movingVar = moving
        if (!this.func_175160_A()) {
            return
        }
        if (this.isRiding) {
            this.field_71174_a.sendPacket(CPacketPlayer.PositionRotation(this.motionX, -999.0, this.motionZ, rotationX, rotationY, onGround))
            movingVar = false
        } else if (movingVar && rotating) {
            this.field_71174_a.sendPacket(CPacketPlayer.PositionRotation(position.xCoord, position.yCoord, position.zCoord, rotationX, rotationY, onGround))
        } else if (movingVar) {
            this.field_71174_a.sendPacket(CPacketPlayer.Position(position.xCoord, position.yCoord, position.zCoord, onGround))
        } else if (rotating) {
            this.field_71174_a.sendPacket(CPacketPlayer.Rotation(rotationX, rotationY, onGround))
        } else if (this.field_184841_cd != onGround) {
            this.field_71174_a.sendPacket(CPacketPlayer(onGround))
        }
        if (movingVar) {
            this.field_175168_bP = 0
        }
    }

    private fun isMoving(position: Vec3d): Boolean {
        val xDiff = position.xCoord - this.field_175172_bI
        val yDiff = position.yCoord - this.field_175166_bJ
        val zDiff = position.zCoord - this.field_175167_bK
        return this.field_175168_bP >= 20 || xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4
    }

    private fun isRotating(rotationX: Float, rotationY: Float): Boolean {
        return (rotationX - this.field_175164_bL).toDouble() != 0.0 || (rotationY - this.field_175165_bM).toDouble() != 0.0
    }
}
