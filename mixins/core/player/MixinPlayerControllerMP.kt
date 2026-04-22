package dev.wizard.meta.mixins.core.player

import dev.wizard.meta.event.events.player.InteractEvent
import dev.wizard.meta.event.events.player.PlayerAttackEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.modules.player.BetterEat
import dev.wizard.meta.module.modules.player.FastBreak
import dev.wizard.meta.module.modules.player.FastUse
import dev.wizard.meta.module.modules.player.TpsSync
import dev.wizard.meta.util.TpsCalculator
import dev.wizard.meta.util.Wrapper
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemStack
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameType
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.callback.LocalCapture

@Mixin(value = [PlayerControllerMP::class])
abstract class MixinPlayerControllerMP {
    @Shadow
    private lateinit var field_78779_k: GameType

    @Shadow
    @Final
    private lateinit var field_78774_b: NetHandlerPlayClient

    @Shadow
    @Final
    private lateinit var field_78776_a: Minecraft

    @Shadow
    private var field_78777_l = 0

    @Shadow
    protected abstract fun func_78750_j()

    @Inject(method = ["attackEntity"], at = [At("HEAD")], cancellable = true)
    fun attackEntity(playerIn: EntityPlayer, targetEntity: Entity?, ci: CallbackInfo) {
        if (targetEntity == null) {
            return
        }
        val event = PlayerAttackEvent(targetEntity)
        event.post()
        if (event.isCancelled) {
            ci.cancel()
        }
    }

    @Inject(method = ["onStoppedUsingItem"], at = [At("HEAD")], cancellable = true)
    fun onStoppedUsingItem$INJECT$HEAD(playerIn: EntityPlayer, ci: CallbackInfo) {
        if (BetterEat.shouldCancelStopUsingItem()) {
            ci.cancel()
        }
    }

    @Inject(method = ["clickBlock"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", ordinal = 2)], cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    fun clickBlock$Inject$INVOKE$getBlockState(pos: BlockPos, side: EnumFacing, cir: CallbackInfoReturnable<Boolean>, forgeEvent: PlayerInteractEvent.LeftClickBlock) {
        val event = InteractEvent.Block.LeftClick(pos, side)
        event.post()
        if (event.isCancelled) {
            cir.returnValue = true
        }
    }

    @Inject(method = ["clickBlock"], at = [At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;blockHitDelay:I", opcode = 181, shift = At.Shift.AFTER)])
    fun clickBlock$Inject$FIELD$blockHitDelay$PUTFIELD(pos: BlockPos, side: EnumFacing, cir: CallbackInfoReturnable<Boolean>) {
        FastBreak.updateBreakDelay()
    }

    @Inject(method = ["onPlayerDamageBlock"], at = [At("HEAD")], cancellable = true)
    fun onPlayerDamageBlock$Inject$HEAD(pos: BlockPos, side: EnumFacing, cir: CallbackInfoReturnable<Boolean>) {
        val event = InteractEvent.Block.Damage(pos, side)
        event.post()
        if (event.isCancelled) {
            cir.returnValue = false
        }
    }

    @Inject(method = ["onPlayerDamageBlock"], at = [At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;blockHitDelay:I", opcode = 181, ordinal = 1, shift = At.Shift.AFTER)])
    fun onPlayerDamageBlock$Inject$FIELD$blockHitDelay$PUTFIELD$1(pos: BlockPos, side: EnumFacing, cir: CallbackInfoReturnable<Boolean>) {
        FastBreak.updateBreakDelay()
    }

    @Inject(method = ["onPlayerDamageBlock"], at = [At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;blockHitDelay:I", opcode = 181, ordinal = 2, shift = At.Shift.AFTER)])
    fun onPlayerDamageBlock$Inject$FIELD$blockHitDelay$PUTFIELD$2(pos: BlockPos, side: EnumFacing, cir: CallbackInfoReturnable<Boolean>) {
        FastBreak.updateBreakDelay()
    }

    @Inject(method = ["processRightClickBlock"], at = [At("HEAD")], cancellable = true)
    fun processRightClickBlock$Inject$HEAD(player: EntityPlayerSP, worldIn: WorldClient, pos: BlockPos, direction: EnumFacing, vec: Vec3d, hand2: EnumHand, cir: CallbackInfoReturnable<EnumActionResult>) {
        val event = InteractEvent.Block.RightClick(pos, direction)
        event.post()
        if (event.isCancelled) {
            cir.returnValue = EnumActionResult.PASS
        }
    }

    @Inject(method = ["processRightClick"], at = [At("HEAD")], cancellable = true)
    fun processRightClick$Inject$HEAD(player: EntityPlayer, worldIn: World, hand2: EnumHand, cir: CallbackInfoReturnable<EnumActionResult>) {
        if (FastUse.isDisabled || FastUse.multiUse == 1 || !FastUse.shouldApplyFastUse()) {
            return
        }
        if (this.field_78779_k == GameType.SPECTATOR) {
            cir.returnValue = EnumActionResult.PASS
        } else {
            for (use in 1 until FastUse.multiUse) {
                this.func_78750_j()
                this.field_78774_b.sendPacket(CPacketPlayerTryUseItem(hand2))
                val itemstack = player.getHeldItem(hand2)
                if (player.cooldownTracker.hasCooldown(itemstack.item)) {
                    cir.returnValue = EnumActionResult.PASS
                    return
                }
                val cancelResult = ForgeHooks.onItemRightClick(player, hand2)
                if (cancelResult != null) {
                    cir.returnValue = cancelResult
                    return
                }
                val i = itemstack.stackSize
                val actionResult = itemstack.useItemRightClick(worldIn, player, hand2)
                val itemStack1 = actionResult.result
                if (itemStack1 !== itemstack || itemStack1.stackSize != i) {
                    player.setHeldItem(hand2, itemStack1)
                    if (itemStack1.isEmpty) {
                        ForgeEventFactory.onPlayerDestroyItem(player, itemstack, hand2)
                    }
                }
                if (actionResult.type != EnumActionResult.SUCCESS) {
                    cir.returnValue = actionResult.type
                    return
                }
            }
        }
    }

    @Inject(method = ["windowClick"], at = [At("HEAD")], cancellable = true)
    private fun `Inject$windowClick$HEAD`(windowId: Int, slotId: Int, mouseButton: Int, type2: ClickType, player: EntityPlayer, cir: CallbackInfoReturnable<ItemStack>) {
        cir.cancel()
        val inventoryTaskManager = InventoryTaskManager
        synchronized(inventoryTaskManager) {
            val short1 = player.openContainer.getNextTransactionID(player.inventory)
            val itemstack = player.openContainer.slotClick(slotId, mouseButton, type2, player)
            this.field_78774_b.sendPacket(CPacketClickWindow(windowId, slotId, mouseButton, type2, itemstack, short1))
            cir.returnValue = itemstack
        }
    }

    @Inject(method = ["syncCurrentPlayItem"], at = [At("HEAD")], cancellable = true)
    private fun `Inject$syncCurrentPlayItem$HEAD`(ci: CallbackInfo) {
        ci.cancel()
        if (Wrapper.getPlayer() == null) {
            return
        }
        val inventoryTaskManager = InventoryTaskManager
        synchronized(inventoryTaskManager) {
            val i = this.field_78776_a.player.inventory.currentItem
            if (i != this.field_78777_l) {
                this.field_78777_l = i
                this.field_78774_b.sendPacket(CPacketHeldItemChange(this.field_78777_l))
            }
        }
    }

    @Redirect(method = ["onPlayerDamageBlock"], at = At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getPlayerRelativeBlockHardness(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)F"))
    fun getPlayerBlockHardness(instance: IBlockState, entityPlayer: EntityPlayer, world: World, blockPos: BlockPos): Float {
        return instance.getPlayerRelativeBlockHardness(entityPlayer, world, blockPos) * (if (TpsSync.isEnabled) TpsCalculator.tickRate / 20.0f else 1.0f)
    }
}
