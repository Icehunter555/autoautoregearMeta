package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.passive.*
import net.minecraft.util.EnumHand

object AutoRemount : Module(
    "AutoRemount",
    category = Category.PLAYER,
    description = "Automatically remounts your horse"
) {
    private val boat by setting(this, BooleanSetting(settingName("Boats"), true))
    private val horse by setting(this, BooleanSetting(settingName("Horse"), true))
    private val skeletonHorse by setting(this, BooleanSetting(settingName("Skeleton Horse"), true))
    private val donkey by setting(this, BooleanSetting(settingName("Donkey"), true))
    private val mule by setting(this, BooleanSetting(settingName("Mule"), true))
    private val pig by setting(this, BooleanSetting(settingName("Pig"), true))
    private val llama by setting(this, BooleanSetting(settingName("Llama"), true))
    private val range by setting(this, FloatSetting(settingName("Range"), 2.0f, 1.0f..5.0f, 0.5f))
    private val remountDelay by setting(this, IntegerSetting(settingName("Remount Delay"), 5, 0..10, 1))

    private val remountTimer = TickTimer(TimeUnit.TICKS)

    init {
        safeListener<TickEvent.Pre> {
            if (player.isRiding) {
                remountTimer.reset()
                return@safeListener
            }

            if (remountTimer.tickAndReset(remountDelay.toLong())) {
                val entity = EntityManager.entity.asSequence()
                    .filter { isValidEntity(it) }
                    .minByOrNull { player.getDistanceSq(it) }

                entity?.let {
                    if (player.getDistance(it) < range) {
                        mc.playerController.interactWithEntity(player, it, EnumHand.MAIN_HAND)
                    }
                }
            }
        }
    }

    private fun isValidEntity(entity: Entity): Boolean {
        return (boat && entity is EntityBoat) || (entity is EntityAnimal && !entity.isChild && (
            (horse && entity is EntityHorse) ||
            (skeletonHorse && entity is EntitySkeletonHorse) ||
            (donkey && entity is EntityDonkey) ||
            (mule && entity is EntityMule) ||
            (pig && entity is EntityPig && entity.canBeSteered()) ||
            (llama && entity is EntityLlama)
        ))
    }
}
