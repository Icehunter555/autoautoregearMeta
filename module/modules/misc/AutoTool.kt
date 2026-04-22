package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.player.InteractEvent
import dev.wizard.meta.event.events.player.PlayerAttackEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.inventory.equipBestTool
import net.minecraft.entity.EntityLivingBase

object AutoTool : Module(
    name = "AutoTool",
    category = Category.MISC,
    description = "Automatically switch to the best tools when mining or attacking"
) {
    private val swapWeapon by setting("Switch Weapon", false)
    private val preferWeapon by setting("Prefer", CombatUtils.PreferWeapon.SWORD)

    init {
        listener<InteractEvent.Block.LeftClick> {
            if (!player.isCreative && world.getBlockState(it.pos).getBlockHardness(world, it.pos) != -1.0f) {
                equipBestTool(world.getBlockState(it.pos))
            }
        }

        listener<PlayerAttackEvent> {
            if (swapWeapon && it.entity is EntityLivingBase) {
                CombatUtils.equipBestWeapon(preferWeapon)
            }
        }
    }
}
