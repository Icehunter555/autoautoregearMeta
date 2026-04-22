package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.world.isAir
import dev.wizard.meta.util.world.isBlacklisted
import dev.wizard.meta.util.world.isReplaceable
import net.minecraft.block.BlockDeadBush
import net.minecraft.block.BlockSoulSand
import net.minecraft.block.BlockTallGrass
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object AutoWither : Module(
    name = "AutoWither",
    category = Category.MISC,
    description = "Automatically spawns Withers"
) {
    private val useMode by setting("Use Mode", UseMode.SPAM)
    private val party by setting("Party", false)
    private val placeRange by setting("Place Range", 3.5f, 2.0f..10.0f, 0.5f)
    private val delay by setting("Delay", 20, 10..300, 5) { useMode == UseMode.SPAM }

    private val timer = TickTimer(TimeUnit.TICKS)
    private var placeTarget: BlockPos? = null
    private var rotationPlaceableX = false
    private var rotationPlaceableZ = false
    private var bodySlot = -1
    private var headSlot = -1
    private var isSneaking = false
    private var buildStage = Stage.PRE

    init {
        onEnable {
            if (mc.player == null) disable()
        }

        onDisable {
            placeTarget = null
            rotationPlaceableX = false
            rotationPlaceableZ = false
            bodySlot = -1
            isSneaking = false
            buildStage = Stage.PRE
        }

        listener<TickEvent.Pre> {
            when (buildStage) {
                Stage.PRE -> {
                    isSneaking = false
                    rotationPlaceableX = false
                    rotationPlaceableZ = false

                    if (!checkBlocksInHotbar()) {
                        if (!party) {
                            NoSpamMessage.sendError("$chatName Missing blocks!")
                            disable()
                        }
                        return@listener
                    }

                    val blockPosList = VectorUtils.getBlockPosInSphere(player.positionVector, placeRange)
                    var noPositionInArea = true

                    for (pos in blockPosList) {
                        placeTarget = pos
                        if (testStructure()) {
                            noPositionInArea = false
                            break
                        }
                    }

                    if (noPositionInArea) {
                        if (useMode == UseMode.SINGLE) {
                            NoSpamMessage.sendError("$chatName No valid position, disabling.")
                            disable()
                        }
                        return@listener
                    }

                    buildStage = Stage.BODY
                }
                Stage.BODY -> {
                    swapToSlot(bodySlot)
                    BodyParts.bodyBase.forEach { placeBlock(placeTarget!!.add(it)) }

                    if (rotationPlaceableX) {
                        BodyParts.ArmsX.forEach { placeBlock(placeTarget!!.add(it)) }
                    } else if (rotationPlaceableZ) {
                        BodyParts.ArmsZ.forEach { placeBlock(placeTarget!!.add(it)) }
                    }

                    buildStage = Stage.HEAD
                }
                Stage.HEAD -> {
                    swapToSlot(headSlot)

                    if (rotationPlaceableX) {
                        BodyParts.headsX.forEach { placeBlock(placeTarget!!.add(it)) }
                    } else if (rotationPlaceableZ) {
                        BodyParts.headsZ.forEach { placeBlock(placeTarget!!.add(it)) }
                    }

                    if (isSneaking) {
                        player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
                        isSneaking = false
                    }

                    if (useMode == UseMode.SINGLE) disable()
                    buildStage = Stage.DELAY
                    timer.reset()
                }
                Stage.DELAY -> {
                    if (timer.tick(delay.toLong())) buildStage = Stage.PRE
                }
            }
        }
    }

    private fun checkBlocksInHotbar(): Boolean {
        headSlot = -1
        bodySlot = -1

        for (i in 0..8) {
            val stack = player.inventory.getStackInSlot(i)
            if (stack.isEmpty) continue

            if (stack.item == Items.SKULL && stack.itemDamage == 1) {
                if (checkItemStackSize(stack, 3)) headSlot = i
            } else if (stack.item is ItemBlock) {
                val block = (stack.item as ItemBlock).block
                if (block is BlockSoulSand && checkItemStackSize(stack, 4)) bodySlot = i
            }
        }
        return bodySlot != -1 && headSlot != -1
    }

    private fun checkItemStackSize(stack: ItemStack, target: Int): Boolean {
        return (player.isCreative && stack.count >= 1) || stack.count >= target
    }

    private fun testStructure(): Boolean {
        val center = placeTarget ?: return false
        rotationPlaceableX = true
        rotationPlaceableZ = true

        val block = world.getBlockState(center).block
        if (block is BlockTallGrass || block is BlockDeadBush) return false
        if (getPlaceableSide(center) == null) return false

        for (pos in BodyParts.bodyBase) {
            if (placingIsBlocked(center.add(pos))) return false
        }

        for (pos in BodyParts.ArmsX) {
            if (!world.getBlockState(center.add(pos).down()).isAir) {
                if (placingIsBlocked(center.add(pos))) rotationPlaceableX = false
            }
        }

        for (pos in BodyParts.headsX) {
            if (placingIsBlocked(center.add(pos))) rotationPlaceableX = false
        }

        if (rotationPlaceableX) {
            for (pos in BodyParts.spawnBlockersX) {
                if (!world.isAirBlock(center.add(pos))) {
                    rotationPlaceableX = false
                    break
                }
            }
        }

        for (pos in BodyParts.ArmsZ) {
            if (!world.getBlockState(center.add(pos).down()).isAir) {
                if (placingIsBlocked(center.add(pos))) rotationPlaceableZ = false
            }
        }

        for (pos in BodyParts.headsZ) {
            if (placingIsBlocked(center.add(pos))) rotationPlaceableZ = false
        }

        if (rotationPlaceableZ) {
            for (pos in BodyParts.spawnBlockersZ) {
                if (!world.isAirBlock(center.add(pos))) {
                    rotationPlaceableZ = false
                    break
                }
            }
        }

        return rotationPlaceableX || rotationPlaceableZ
    }

    private fun placingIsBlocked(pos: BlockPos): Boolean {
        return !world.isAirBlock(pos) || !world.checkNoEntityCollision(AxisAlignedBB(pos))
    }

    private fun placeBlock(pos: BlockPos) {
        val side = getPlaceableSide(pos) ?: return
        val neighbour = pos.offset(side)
        val opposite = side.opposite
        val hitVec = Vec3d(neighbour).add(0.5, 0.5, 0.5).add(Vec3d(opposite.directionVec).scale(0.5))

        if (!isSneaking && world.getBlockState(neighbour).isBlacklisted) {
            player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
            isSneaking = true
        }

        playerController.processRightClickBlock(player, world, neighbour, opposite, hitVec, EnumHand.MAIN_HAND)
        player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun getPlaceableSide(pos: BlockPos): EnumFacing? {
        for (side in EnumFacing.values()) {
            val neighbour = pos.offset(side)
            if (!world.getBlockState(neighbour).block.canCollideCheck(world.getBlockState(neighbour), false)) continue
            val blockState = world.getBlockState(neighbour)
            if (!blockState.material.isReplaceable || blockState.block is BlockTallGrass || blockState.block is BlockDeadBush) return side
        }
        return null
    }

    private object BodyParts {
        val bodyBase = arrayOf(BlockPos(0, 1, 0), BlockPos(0, 2, 0))
        val ArmsX = arrayOf(BlockPos(-1, 2, 0), BlockPos(1, 2, 0))
        val ArmsZ = arrayOf(BlockPos(0, 2, -1), BlockPos(0, 2, 1))
        val headsX = arrayOf(BlockPos(0, 3, 0), BlockPos(-1, 3, 0), BlockPos(1, 3, 0))
        val headsZ = arrayOf(BlockPos(0, 3, 0), BlockPos(0, 3, -1), BlockPos(0, 3, 1))
        val spawnBlockersX = arrayOf(BlockPos(-1, 1, 0), BlockPos(1, 1, 0))
        val spawnBlockersZ = arrayOf(BlockPos(0, 1, -1), BlockPos(0, 1, 1))
    }

    private enum class Stage { PRE, BODY, HEAD, DELAY }
    private enum class UseMode { SINGLE, SPAM }
}
