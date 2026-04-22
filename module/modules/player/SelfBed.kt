package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.managers.*
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.AutoSplashPotion
import dev.wizard.meta.setting.settings.impl.number.*
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.operation.swapWith
import dev.wizard.meta.util.inventory.slot.*
import dev.wizard.meta.util.math.vector.toVec3d
import dev.wizard.meta.util.math.vector.toVec3dCenter
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.onMainThreadSafe
import dev.wizard.meta.util.world.BlockKt
import dev.wizard.meta.util.world.InteractKt
import kotlinx.coroutines.launch
import net.minecraft.block.BlockBed
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object SelfBed : Module(
    "SelfBed",
    category = Category.PLAYER,
    description = "Places bed behind you for velocity boost",
    modulePriority = 64
) {
    private val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.NONE))
    private val bedSlot by setting(this, IntegerSetting(settingName("Bed Slot"), 3, 1..9, 1))
    private val range by setting(this, IntegerSetting(settingName("Range"), 5, 2..6, 1))
    private val yRange by setting(this, IntegerSetting(settingName("Y Range"), 2, 0..4, 1))
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 75, 0..1000, 1))
    private val directionBias by setting(this, BooleanSetting(settingName("Directional Bias"), false))
    private val reverseBias by setting(this, BooleanSetting(settingName("Reverse Bias"), false, { directionBias }))
    private val safety by setting(this, BooleanSetting(settingName("Safety"), true))
    private val safetyHealth by setting(this, FloatSetting(settingName("Safety Health"), 4.0f, 1.0f..20.0f, 1.0f, { safety }))
    private val onlyWhenPot by setting(this, BooleanSetting(settingName("Only When Pot"), false))
    private val stopOnPop by setting(this, BooleanSetting(settingName("Disable On Pop"), false))
    private val stopOnEnemy by setting(this, BooleanSetting(settingName("Disable On Enemy"), false))
    private val enemyRange by setting(this, IntegerSetting(settingName("Enemy Range"), 15, 2..35, 1, { stopOnEnemy }))
    private val minVelocity by setting(this, FloatSetting(settingName("Min Velocity"), 0.1f, 0.0f..1.0f, 0.01f))
    private val maxVelocity by setting(this, FloatSetting(settingName("Max Velocity"), 0.0f, 0.0f..4.0f, 0.01f))

    private val updateTimer = TickTimer(TimeUnit.MILLISECONDS)
    private val timer = TickTimer(TimeUnit.MILLISECONDS)
    private var placeInfo: PlaceInfo? = null
    private var lastTask: InventoryTask? = null
    private var inactiveTicks = 10

    override fun getHudInfo(): String = "$range, $delay, $minVelocity"
    override fun isActive(): Boolean = isEnabled && inactiveTicks < 10

    init {
        onEnable {
            reset()
        }

        onDisable {
            reset()
        }

        safeListener<TickEvent.Post> {
            inactiveTicks++
            update(this)
            runLoop(this)
        }

        safeListener<TotemPopEvent.Pop> {
            if (it.entity == player && stopOnPop) {
                NoSpamMessage.sendWarning("${getChatName()} disabled due to totem pop!")
                disable()
            }
        }

        safeConcurrentListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketSoundEffect && packet.category == SoundCategory.BLOCKS && packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                placeInfo?.let { info ->
                    val center = info.bedFootPos.toVec3dCenter()
                    if (center.squareDistanceTo(packet.x, packet.y, packet.z) <= 4.0) {
                        inactiveTicks = 0
                    }
                }
            }
        }
    }

    private fun update(event: SafeClientEvent) {
        if (event.player.dimension == 0) return
        if (!event.player.allSlotsPrioritized.hasItem(Items.BED)) {
            reset()
            return
        }

        val velocity = event.player.motionX * event.player.motionX + event.player.motionZ * event.player.motionZ
        if (minVelocity != 0.0f && velocity < minVelocity * minVelocity) {
            placeInfo = null
            return
        }
        if (maxVelocity != 0.0f && velocity > maxVelocity * maxVelocity) {
            placeInfo = null
            return
        }

        if (updateTimer.tickAndReset(50L)) {
            ConcurrentScope.launch {
                placeInfo = calcPlaceInfo(event)
            }
        }
    }

    private fun runLoop(event: SafeClientEvent) {
        val info = placeInfo ?: return
        if (safety && event.player.health <= safetyHealth) return
        if (onlyWhenPot && (!AutoSplashPotion.isEnabled || AutoSplashPotion.countInstantHealthPotions() == 0)) return
        if (lastTask?.isExecuted == false) return
        if (stopOnEnemy && isEnemyClose(event)) return

        val hotbarSlot = event.player.hotbarSlots[bedSlot - 1]
        if (hotbarSlot.stack.item != Items.BED) {
            refillBed(event, hotbarSlot)
            return
        }

        if (timer.tick(delay.toLong())) {
            placeBed(event, info)
            breakBed(event, info)
        }
    }

    private fun isEnemyClose(event: SafeClientEvent): Boolean {
        return event.world.loadedEntityList.any {
            it is EntityPlayer && it.isEntityAlive && !FriendManager.isFriend(it.name) && it != event.player && it.getDistance(event.player) <= enemyRange.toDouble()
        }
    }

    private fun refillBed(event: SafeClientEvent, hotbarSlot: HotbarSlot) {
        val bedSlot = event.player.storageSlots.firstItem(Items.BED) ?: event.player.craftingSlots.firstItem(Items.BED) ?: return
        lastTask = InventoryTask.Builder().priority(getModulePriority()).build().apply {
            swapWith(bedSlot, hotbarSlot)
            InventoryTaskManager.addTask(this)
        }
    }

    private fun placeBed(event: SafeClientEvent, info: PlaceInfo) {
        val packet = CPacketPlayerTryUseItemOnBlock(info.solidPos, info.placementFacing, EnumHand.MAIN_HAND, 0.5f, 0.5f, 0.5f)
        val hotbarSlot = event.player.hotbarSlots[bedSlot - 1]
        HotbarSwitchManager.ghostSwitch(event, ghostSwitchBypass, hotbarSlot) {
            connection.sendPacket(packet)
        }
        event.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        timer.reset()
        inactiveTicks = 0
    }

    private fun breakBed(event: SafeClientEvent, info: PlaceInfo) {
        val side = InteractKt.getMiningSide(event, info.bedFootPos) ?: EnumFacing.UP
        val hitVecOffset = InteractKt.getHitVecOffset(side)

        if (event.player.isSneaking) {
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.STOP_SNEAKING))
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(info.bedFootPos, side, EnumHand.MAIN_HAND, hitVecOffset.x, hitVecOffset.y, hitVecOffset.z))
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.START_SNEAKING))
        } else {
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(info.bedFootPos, side, EnumHand.MAIN_HAND, hitVecOffset.x, hitVecOffset.y, hitVecOffset.z))
        }
        event.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        timer.reset()
        inactiveTicks = 0
    }

    private fun calcPlaceInfo(event: SafeClientEvent): PlaceInfo? {
        val playerPos = BlockPos(event.player)
        val motionFacing = getMovementDirection(event)
        val facing = if (directionBias) motionFacing else event.player.horizontalFacing
        val biasDir = if (reverseBias) facing.opposite else facing

        val directions = listOf(biasDir, biasDir.rotateY(), biasDir.rotateYCCW(), biasDir.opposite, biasDir.opposite.rotateY(), biasDir.opposite.rotateYCCW())

        for (direction in directions) {
            for (distance in 1..range) {
                val targetPos = playerPos.offset(direction, distance)
                for (yOffset in -yRange..yRange) {
                    val bedFootPos = targetPos.add(0, yOffset, 0)
                    for (bedFacing in EnumFacing.HORIZONTALS) {
                        val bedHeadPos = bedFootPos.offset(bedFacing)
                        val solidPos = bedFootPos.down()
                        if (isValidBedPlacement(event, solidPos, bedFootPos, bedHeadPos)) {
                            val hitVec = solidPos.toVec3d(0.5, 1.0, 0.5)
                            return PlaceInfo(solidPos, bedFootPos, bedHeadPos, hitVec, EnumFacing.UP, bedFacing)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun getMovementDirection(event: SafeClientEvent): EnumFacing {
        val mx = event.player.motionX
        val mz = event.player.motionZ
        if (Math.abs(mx) < 0.01 && Math.abs(mz) < 0.01) return event.player.horizontalFacing
        return if (Math.abs(mx) > Math.abs(mz)) {
            if (mx > 0.0) EnumFacing.EAST else EnumFacing.WEST
        } else {
            if (mz > 0.0) EnumFacing.SOUTH else EnumFacing.NORTH
        }
    }

    private fun isValidBedPlacement(event: SafeClientEvent, solidPos: BlockPos, bedFootPos: BlockPos, bedHeadPos: BlockPos): Boolean {
        val solidState = event.world.getBlockState(solidPos)
        if (!solidState.block.isFullBlock(solidState) || !solidState.isOpaqueCube) return false
        if (!isBedPositionValid(event, bedFootPos) || !isBedPositionValid(event, bedHeadPos)) return false

        val eyePos = EntityUtils.getEyePosition(event.player)
        if (eyePos.distanceTo(bedFootPos.toVec3dCenter()) > 6.0 || eyePos.distanceTo(bedHeadPos.toVec3dCenter()) > 6.0) return false

        val playerBox = event.player.entityBoundingBox
        return !playerBox.intersects(getBedBoundingBox(bedFootPos)) && !playerBox.intersects(getBedBoundingBox(bedHeadPos))
    }

    private fun isBedPositionValid(event: SafeClientEvent, pos: BlockPos): Boolean {
        val state = event.world.getBlockState(pos)
        val block = state.block
        return block == Blocks.AIR || block is BlockBed || BlockKt.isReplaceable(state) || block == Blocks.TALLGRASS || block == Blocks.YELLOW_FLOWER || block == Blocks.RED_FLOWER
    }

    private fun getBedBoundingBox(pos: BlockPos): AxisAlignedBB {
        return AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), (pos.x + 1).toDouble(), pos.y + 0.5625, (pos.z + 1).toDouble())
    }

    private fun reset() {
        timer.reset(-69420L)
        updateTimer.reset(-69420L)
        placeInfo = null
        inactiveTicks = 10
        lastTask = null
    }

    private data class PlaceInfo(val solidPos: BlockPos, val bedFootPos: BlockPos, val bedHeadPos: BlockPos, val hitVec: Vec3d, val placementFacing: EnumFacing, val bedFacing: EnumFacing)
}
