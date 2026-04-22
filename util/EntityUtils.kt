package dev.wizard.meta.util

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.util.inventory.getId
import dev.wizard.meta.util.inventory.slot.getArmorSlots
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.math.vector.toBlockPos
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityAgeable
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EnumCreatureType
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityPigZombie
import net.minecraft.entity.passive.AbstractHorse
import net.minecraft.entity.passive.EntityAmbientCreature
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityTameable
import net.minecraft.entity.passive.EntityWolf
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object EntityUtils {
    private val mc = Minecraft.getMinecraft()

    fun SafeClientEvent.getViewEntity(): Entity {
        return mc.renderViewEntity ?: player
    }

    fun getViewEntity(): Entity? {
        return mc.renderViewEntity ?: mc.player
    }

    fun Entity.getEyePosition(): Vec3d {
        return Vec3d(posX, posY + eyeHeight.toDouble(), posZ)
    }

    fun Entity.getLastTickPos(): Vec3d {
        return Vec3d(lastTickPosX, lastTickPosY, lastTickPosZ)
    }

    fun Entity.getFlooredPosition(): BlockPos {
        return BlockPos(MathUtilKt.floorToInt(posX), MathUtilKt.floorToInt(posY), MathUtilKt.floorToInt(posZ))
    }

    fun Entity.getBetterPosition(): BlockPos {
        return BlockPos(MathUtilKt.floorToInt(posX), MathUtilKt.floorToInt(posY + 0.25), MathUtilKt.floorToInt(posZ))
    }

    fun Entity.isPassive(): Boolean {
        return this is EntityAgeable || this is EntityAmbientCreature || this is EntitySquid
    }

    fun Entity.isNeutral(): Boolean {
        return isNeutralMob(this) && !isMobAggressive(this)
    }

    fun Entity.isHostile(): Boolean {
        return isMobAggressive(this)
    }

    fun Entity.isTamed(): Boolean {
        return (this is EntityTameable && isTamed) || (this is AbstractHorse && isTame)
    }

    fun Entity.isInOrAboveLiquid(): Boolean {
        return isInWater || isInLava || world.checkBlockCollision(entityBoundingBox.grow(0.0, -1.0, 0.0))
    }

    fun EntityPlayer.isFriend(): Boolean {
        return FriendManager.isFriend(name)
    }

    fun EntityPlayer.isNaked(): Boolean {
        return getArmorSlots().all { it.stack.isEmpty }
    }

    fun EntityPlayer.isSelf(): Boolean {
        return this === mc.player || this === mc.renderViewEntity
    }

    fun EntityPlayer.isFakeOrSelf(): Boolean {
        return this === mc.player || this === mc.renderViewEntity || entityId < 0
    }

    fun EntityPlayer.isFlying(): Boolean {
        return isElytraFlying || capabilities.isFlying
    }

    fun EntityPlayer.getHelmetCount(): Int = getArmorSlots()[0].stack.count
    fun EntityPlayer.getChestPlateCount(): Int = getArmorSlots()[1].stack.count
    fun EntityPlayer.getLeggingsCount(): Int = getArmorSlots()[2].stack.count
    fun EntityPlayer.getBootsCount(): Int = getArmorSlots()[3].stack.count

    fun EntityPlayer.getArmorCounts(): Map<EntityEquipmentSlot, Int> {
        return mapOf(
            EntityEquipmentSlot.HEAD to getHelmetCount(),
            EntityEquipmentSlot.CHEST to getChestPlateCount(),
            EntityEquipmentSlot.LEGS to getLeggingsCount(),
            EntityEquipmentSlot.FEET to getBootsCount()
        )
    }

    private fun isNeutralMob(entity: Entity): Boolean {
        return entity is EntityPigZombie || entity is EntityWolf || entity is EntityEnderman || entity is EntityIronGolem
    }

    private fun isMobAggressive(entity: Entity): Boolean {
        return when (entity) {
            is EntityPigZombie -> entity.isArmsRaised || entity.isAngry
            is EntityWolf -> entity.isAngry && mc.player != entity.owner
            is EntityEnderman -> entity.isScreaming
            is EntityIronGolem -> entity.revengeTarget != null
            else -> entity.isCreatureType(EnumCreatureType.MONSTER, false)
        }
    }

    fun mobTypeSettings(entity: Entity, mobs: Boolean, passive: Boolean, neutral: Boolean, hostile: Boolean): Boolean {
        return mobs && ((passive && entity.isPassive()) || (neutral && entity.isNeutral()) || (hostile && entity.isHostile()))
    }

    fun getInterpolatedPos(entity: Entity, ticks: Float): Vec3d {
        return entity.getLastTickPos().add(getInterpolatedAmount(entity, ticks))
    }

    fun getInterpolatedAmount(entity: Entity, ticks: Float): Vec3d {
        return entity.positionVector.subtract(entity.getLastTickPos()).scale(ticks.toDouble())
    }

    fun getTargetList(player: Array<Boolean>, mobs: Array<Boolean>, invisible: Boolean, range: Float, ignoreSelf: Boolean = true): ArrayList<EntityLivingBase> {
        val entityList = ArrayList<EntityLivingBase>()
        for (entity in EntityManager.livingBase) {
            if (ignoreSelf && (entity === mc.player || entity === mc.renderViewEntity)) continue
            if (entity is EntityPlayer) {
                if (!player[0] || !playerTypeCheck(entity, player[1], player[2])) continue
            } else {
                if (!mobTypeSettings(entity, mobs[0], mobs[1], mobs[2], mobs[3])) continue
            }
            if (mc.player.isRiding && entity === mc.player.ridingEntity) continue
            if (mc.player.distanceTo(entity) > range || entity.health <= 0f || (!invisible && entity.isInvisible)) continue
            entityList.add(entity)
        }
        return entityList
    }

    fun playerTypeCheck(player: EntityPlayer, friend: Boolean, sleeping: Boolean): Boolean {
        if (!friend && FriendManager.isFriend(player.name)) return false
        if (!sleeping && player.isPlayerSleeping) return false
        return true
    }

    fun SafeClientEvent.getDroppedItems(itemId: Int, range: Float): ArrayList<EntityItem> {
        val entityList = ArrayList<EntityItem>()
        for (entity in EntityManager.entity) {
            if (entity is EntityItem) {
                if (entity.item.item.getId() == itemId && player.distanceTo(entity) <= range) {
                    entityList.add(entity)
                }
            }
        }
        return entityList
    }

    fun SafeClientEvent.getDroppedItem(itemId: Int, range: Float): BlockPos? {
        return getDroppedItems(itemId, range).minByOrNull { player.distanceTo(it) }?.positionVector?.toBlockPos()
    }

    fun EntityPlayerSP.spoofSneak(block: () -> Unit) {
        if (!isSneaking) {
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING))
            block()
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING))
        } else {
            block()
        }
    }

    fun EntityPlayerSP.spoofUnSneak(block: () -> Unit) {
        if (isSneaking) {
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING))
            block()
            connection.sendPacket(CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING))
        } else {
            block()
        }
    }
}
