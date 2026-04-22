package dev.wizard.meta.mixins.core

import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.mixins.accessor.AccessorSlot
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Container::class)
abstract class MixinContainer {
    @Shadow
    lateinit var field_75151_b: List<Slot>

    @Shadow
    private var field_94536_g = 0

    @Shadow
    private var field_94535_f = 0

    @Shadow
    @Final
    lateinit var field_94537_h: MutableSet<Slot>

    @Shadow
    protected abstract fun func_94533_d()

    @Shadow
    abstract fun func_75142_b()

    @Shadow
    abstract fun func_94530_a(var1: ItemStack, var2: Slot): Boolean

    @Shadow
    abstract fun func_82846_b(var1: EntityPlayer, var2: Int): ItemStack

    @Shadow
    abstract fun func_94531_b(var1: Slot): Boolean

    companion object {
        @Shadow
        @JvmStatic
        fun func_94527_a(slotIn: Slot?, stack: ItemStack, stackSizeMatters: Boolean): Boolean = false

        @Shadow
        @JvmStatic
        fun func_94525_a(dragSlotsIn: Set<Slot>, dragModeIn: Int, stack: ItemStack, slotStackSize: Int) {}

        @Shadow
        @JvmStatic
        fun func_94529_b(eventButton: Int): Int = 0

        @Shadow
        @JvmStatic
        fun func_180610_a(dragModeIn: Int, player: EntityPlayer): Boolean = false

        @Shadow
        @JvmStatic
        fun func_94532_c(clickedButton: Int): Int = 0
    }

    @Inject(method = ["slotClick"], at = [At("HEAD")], cancellable = true)
    fun `Inject$slotClick$HEAD`(slotId: Int, dragType: Int, clickTypeIn: ClickType, player: EntityPlayer, cir: CallbackInfoReturnable<ItemStack>) {
        cir.cancel()
        val inventoryTaskManager = InventoryTaskManager
        synchronized(inventoryTaskManager) {
            var itemstack = ItemStack.field_190927_a
            val inventoryplayer = player.field_71071_by
            if (clickTypeIn == ClickType.QUICK_CRAFT) {
                val j1 = this.field_94536_g
                this.field_94536_g = func_94532_c(dragType)
                if ((j1 != 1 || this.field_94536_g != 2) && j1 != this.field_94536_g) {
                    this.func_94533_d()
                } else if (inventoryplayer.func_70445_o().func_190926_b()) {
                    this.func_94533_d()
                } else if (this.field_94536_g == 0) {
                    this.field_94535_f = func_94529_b(dragType)
                    if (func_180610_a(this.field_94535_f, player)) {
                        this.field_94536_g = 1
                        this.field_94537_h.clear()
                    } else {
                        this.func_94533_d()
                    }
                } else if (this.field_94536_g == 1) {
                    val slot7 = this.field_75151_b[slotId]
                    val itemstack12 = inventoryplayer.func_70445_o()
                    if (slot7 != null && func_94527_a(slot7, itemstack12, true) && slot7.func_75214_a(itemstack12) && (this.field_94535_f == 2 || itemstack12.func_190916_E() > this.field_94537_h.size) && this.func_94531_b(slot7)) {
                        this.field_94537_h.add(slot7)
                    }
                } else if (this.field_94536_g == 2) {
                    if (this.field_94537_h.isNotEmpty()) {
                        val itemstack9 = inventoryplayer.func_70445_o().func_77946_l()
                        var k1 = inventoryplayer.func_70445_o().func_190916_E()
                        for (slot8 in this.field_94537_h) {
                            val itemstack13 = inventoryplayer.func_70445_o()
                            if (slot8 == null || !func_94527_a(slot8, itemstack13, true) || !slot8.func_75214_a(itemstack13) || (this.field_94535_f != 2 && itemstack13.func_190916_E() < this.field_94537_h.size) || !this.func_94531_b(slot8)) continue
                            val itemstack14 = itemstack9.func_77946_l()
                            val j3 = if (slot8.func_75216_d()) slot8.func_75211_c().func_190916_E() else 0
                            func_94525_a(this.field_94537_h, this.field_94535_f, itemstack14, j3)
                            val k3 = Math.min(itemstack14.func_77976_d(), slot8.func_178170_b(itemstack14))
                            if (itemstack14.func_190916_E() > k3) {
                                itemstack14.func_190920_e(k3)
                            }
                            k1 -= itemstack14.func_190916_E() - j3
                            slot8.func_75215_d(itemstack14)
                        }
                        itemstack9.func_190920_e(k1)
                        inventoryplayer.func_70437_b(itemstack9)
                    }
                    this.func_94533_d()
                } else {
                    this.func_94533_d()
                }
            } else if (this.field_94536_g != 0) {
                this.func_94533_d()
            } else if (!((clickTypeIn != ClickType.PICKUP && clickTypeIn != ClickType.QUICK_MOVE) || (dragType != 0 && dragType != 1))) {
                if (slotId == -999) {
                    if (!inventoryplayer.func_70445_o().func_190926_b()) {
                        if (dragType == 0) {
                            player.func_71019_a(inventoryplayer.func_70445_o(), true)
                            inventoryplayer.func_70437_b(ItemStack.field_190927_a)
                        }
                        if (dragType == 1) {
                            player.func_71019_a(inventoryplayer.func_70445_o().func_77979_a(1), true)
                        }
                    }
                } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                    if (slotId < 0) {
                        cir.returnValue = ItemStack.field_190927_a
                        return
                    }
                    val slot5 = this.field_75151_b[slotId]
                    if (slot5 == null || !slot5.func_82869_a(player)) {
                        cir.returnValue = ItemStack.field_190927_a
                        return
                    }
                    var itemstack7 = this.func_82846_b(player, slotId)
                    while (!itemstack7.func_190926_b() && ItemStack.func_179545_c(slot5.func_75211_c(), itemstack7)) {
                        itemstack = itemstack7.func_77946_l()
                        itemstack7 = this.func_82846_b(player, slotId)
                    }
                } else {
                    if (slotId < 0) {
                        cir.returnValue = ItemStack.field_190927_a
                        return
                    }
                    val slot6 = this.field_75151_b[slotId]
                    if (slot6 != null) {
                        val itemstack8 = slot6.func_75211_c()
                        val itemstack11 = inventoryplayer.func_70445_o()
                        if (!itemstack8.func_190926_b()) {
                            itemstack = itemstack8.func_77946_l()
                        }
                        if (itemstack8.func_190926_b()) {
                            if (!itemstack11.func_190926_b() && slot6.func_75214_a(itemstack11)) {
                                var i3 = if (dragType == 0) itemstack11.func_190916_E() else 1
                                if (i3 > slot6.func_178170_b(itemstack11)) {
                                    i3 = slot6.func_178170_b(itemstack11)
                                }
                                slot6.func_75215_d(itemstack11.func_77979_a(i3))
                            }
                        } else if (slot6.func_82869_a(player)) {
                            if (itemstack11.func_190926_b()) {
                                if (itemstack8.func_190926_b()) {
                                    slot6.func_75215_d(ItemStack.field_190927_a)
                                    inventoryplayer.func_70437_b(ItemStack.field_190927_a)
                                } else {
                                    val l2 = if (dragType == 0) itemstack8.func_190916_E() else (itemstack8.func_190916_E() + 1) / 2
                                    inventoryplayer.func_70437_b(slot6.func_75209_a(l2))
                                    if (itemstack8.func_190926_b()) {
                                        slot6.func_75215_d(ItemStack.field_190927_a)
                                    }
                                    slot6.func_190901_a(player, inventoryplayer.func_70445_o())
                                }
                            } else if (slot6.func_75214_a(itemstack11)) {
                                if (itemstack8.func_77973_b() == itemstack11.func_77973_b() && itemstack8.func_77960_j() == itemstack11.func_77960_j() && ItemStack.func_77970_a(itemstack8, itemstack11)) {
                                    var k2 = if (dragType == 0) itemstack11.func_190916_E() else 1
                                    if (k2 > slot6.func_178170_b(itemstack11) - itemstack8.func_190916_E()) {
                                        k2 = slot6.func_178170_b(itemstack11) - itemstack8.func_190916_E()
                                    }
                                    if (k2 > itemstack11.func_77976_d() - itemstack8.func_190916_E()) {
                                        k2 = itemstack11.func_77976_d() - itemstack8.func_190916_E()
                                    }
                                    itemstack11.func_190918_g(k2)
                                    itemstack8.func_190917_f(k2)
                                } else if (itemstack11.func_190916_E() <= slot6.func_178170_b(itemstack11)) {
                                    slot6.func_75215_d(itemstack11)
                                    inventoryplayer.func_70437_b(itemstack8)
                                }
                            } else if (itemstack8.func_77973_b() == itemstack11.func_77973_b() && itemstack11.func_77976_d() > 1 && !(itemstack8.func_77981_g() && itemstack8.func_77960_j() != itemstack11.func_77960_j()) && ItemStack.func_77970_a(itemstack8, itemstack11) && !itemstack8.func_190926_b()) {
                                val j2 = itemstack8.func_190916_E()
                                if (j2 + itemstack11.func_190916_E() <= itemstack11.func_77976_d()) {
                                    itemstack11.func_190917_f(j2)
                                    val itemstack_temp = slot6.func_75209_a(j2)
                                    if (itemstack_temp.func_190926_b()) {
                                        slot6.func_75215_d(ItemStack.field_190927_a)
                                    }
                                    slot6.func_190901_a(player, inventoryplayer.func_70445_o())
                                }
                            }
                        }
                        slot6.func_75218_e()
                    }
                }
            } else if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
                val slot4 = this.field_75151_b[slotId]
                val itemstack6 = inventoryplayer.func_70301_a(dragType)
                val itemstack10 = slot4.func_75211_c()
                if (!itemstack6.func_190926_b() || !itemstack10.func_190926_b()) {
                    if (itemstack6.func_190926_b()) {
                        if (slot4.func_82869_a(player)) {
                            inventoryplayer.func_70299_a(dragType, itemstack10)
                            (slot4 as AccessorSlot).trollOnSwapCraft(itemstack10.func_190916_E())
                            slot4.func_75215_d(ItemStack.field_190927_a)
                            slot4.func_190901_a(player, itemstack10)
                        }
                    } else if (itemstack10.func_190926_b()) {
                        if (slot4.func_75214_a(itemstack6)) {
                            val l1 = slot4.func_178170_b(itemstack6)
                            if (itemstack6.func_190916_E() > l1) {
                                slot4.func_75215_d(itemstack6.func_77979_a(l1))
                            } else {
                                slot4.func_75215_d(itemstack6)
                                inventoryplayer.func_70299_a(dragType, ItemStack.field_190927_a)
                            }
                        }
                    } else if (slot4.func_82869_a(player) && slot4.func_75214_a(itemstack6)) {
                        val i2 = slot4.func_178170_b(itemstack6)
                        if (itemstack6.func_190916_E() > i2) {
                            slot4.func_75215_d(itemstack6.func_77979_a(i2))
                            slot4.func_190901_a(player, itemstack10)
                            if (!inventoryplayer.func_70441_a(itemstack10)) {
                                player.func_71019_a(itemstack10, true)
                            }
                        } else {
                            slot4.func_75215_d(itemstack6)
                            inventoryplayer.func_70299_a(dragType, itemstack10)
                            slot4.func_190901_a(player, itemstack10)
                        }
                    }
                }
            } else if (clickTypeIn == ClickType.CLONE && player.field_71075_bZ.field_75098_d && inventoryplayer.func_70445_o().func_190926_b() && slotId >= 0) {
                val slot3 = this.field_75151_b[slotId]
                if (slot3 != null && slot3.func_75216_d()) {
                    val itemstack5 = slot3.func_75211_c().func_77946_l()
                    itemstack5.func_190920_e(itemstack5.func_77976_d())
                    inventoryplayer.func_70437_b(itemstack5)
                }
            } else if (clickTypeIn == ClickType.THROW && inventoryplayer.func_70445_o().func_190926_b() && slotId >= 0) {
                val slot2 = this.field_75151_b[slotId]
                if (slot2 != null && slot2.func_75216_d() && slot2.func_82869_a(player)) {
                    val itemstack4 = slot2.func_75209_a(if (dragType == 0) 1 else slot2.func_75211_c().func_190916_E())
                    slot2.func_190901_a(player, itemstack4)
                    player.func_71019_a(itemstack4, true)
                }
            } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0) {
                val slot = this.field_75151_b[slotId]
                val itemstack1 = inventoryplayer.func_70445_o()
                if (!(itemstack1.func_190926_b() || (slot != null && slot.func_75216_d() && slot.func_82869_a(player)))) {
                    val i = if (dragType == 0) 0 else this.field_75151_b.size - 1
                    val j = if (dragType == 0) 1 else -1
                    for (k in 0..1) {
                        var l = i
                        while (l >= 0 && l < this.field_75151_b.size && itemstack1.func_190916_E() < itemstack1.func_77976_d()) {
                            val slot1 = this.field_75151_b[l]
                            if (slot1.func_75216_d() && func_94527_a(slot1, itemstack1, true) && slot1.func_82869_a(player) && this.func_94530_a(itemstack1, slot1)) {
                                val itemstack2 = slot1.func_75211_c()
                                if (!(k == 0 && itemstack2.func_190916_E() == itemstack2.func_77976_d())) {
                                    val i1 = Math.min(itemstack1.func_77976_d() - itemstack1.func_190916_E(), itemstack2.func_190916_E())
                                    val itemstack3 = slot1.func_75209_a(i1)
                                    itemstack1.func_190917_f(i1)
                                    if (itemstack3.func_190926_b()) {
                                        slot1.func_75215_d(ItemStack.field_190927_a)
                                    }
                                    slot1.func_190901_a(player, itemstack3)
                                }
                            }
                            l += j
                        }
                    }
                }
                this.func_75142_b()
            }
            cir.returnValue = itemstack
        }
    }
}
