package dev.wizard.meta.module.modules.beta

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.operation.swapToBlock
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.firstByStack
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.toVec3d
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.pause.MainHandPause
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.world.BlockKt
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import dev.wizard.meta.util.world.PlacementSearchOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.BlockShulkerBox
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketCloseWindow
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketOpenWindow
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

@CombatManager.CombatModule
object ShulkerPush : Module(
    "ShulkerPush",
    category = Category.BETA,
    description = "Push enemy out of hole with shulker",
    modulePriority = 160
) {
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 1, 0..20, 1))
    private val openDelay by setting(this, IntegerSetting(settingName("Open Delay"), 10, 0..20, 1))
    private val closeDelay by setting(this, IntegerSetting(settingName("Close Delay"), 2, 0..20, 1))
    private val range by setting(this, FloatSetting(settingName("Range"), 5.0f, 0.0f..6.0f, 0.25f))

    private val timeoutTimer = TickTimer()
    private val timer = TickTimer()
    private var placeInfo: Pair<BlockPos, EnumFacing>? = null
    private var lastHitVec: Vec3d? = null
    private var windowID = -1
    private var shulkerState = ShulkerState.OBBY

    init {
        onEnable {
            timeoutTimer.reset()
            timer.reset()
            val target = CombatManager.target
            if (target != null) {
                val pair = runSafe {
                    getPlacingPos(target)
                }
                placeInfo = pair
                if (placeInfo == null) disable()
            } else {
                disable()
            }
        }

        onDisable {
            placeInfo = null
            lastHitVec = null
            windowID = -1
            shulkerState = ShulkerState.OBBY
        }

        listener<PacketEvent.Receive> {
            val packet = it.packet
            if (packet is SPacketOpenWindow && shulkerState == ShulkerState.OPEN) {
                it.cancel()
                windowID = packet.windowId
                shulkerState = ShulkerState.OPENING
                timer.reset()
            }
        }

        listener<WorldEvent.ServerBlockUpdate> {
            val info = placeInfo ?: return@listener
            val (pos, side) = info
            when (shulkerState) {
                ShulkerState.OBBY -> {
                    if (it.pos == pos.offset(side.opposite) && it.newState.block != Blocks.AIR) {
                        shulkerState = ShulkerState.SHULKER
                    }
                }
                ShulkerState.SHULKER -> {
                    if (it.pos == pos && it.newState.block is BlockShulkerBox) {
                        shulkerState = ShulkerState.OPEN
                    }
                }
                else -> {}
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (timeoutTimer.tickAndReset(5000L)) {
                disable()
                return@safeListener
            }
            if (timer.tick(0L)) {
                val info = placeInfo
                if (info != null) {
                    val (pos, side) = info
                    MainHandPause.withPause(INSTANCE, 100) {
                        shulkerMode(pos, side)
                    } ?: disable()
                } else {
                    disable()
                }
            }
            lastHitVec?.let { hitVec ->
                PlayerPacketManager.sendPlayerPacket {
                    rotate(RotationUtils.getRotationTo(this@safeListener, hitVec))
                }
            }
        }
    }

    override fun getHudInfo(): String = shulkerState.displayString

    private fun SafeClientEvent.getPlacingPos(target: EntityLivingBase): Pair<BlockPos, EnumFacing>? {
        if (player.distanceSqTo(target) > range * range) return null
        val feetPos = EntityUtils.getFlooredPosition(target)
        val xOffset = target.posX - (feetPos.x + 0.5)
        val zOffset = target.posZ - (feetPos.z + 0.5)
        if (world.isAirBlock(feetPos) && !HoleManager.getHoleInfo(target).isHole) return null

        val headPos = feetPos.up()
        val xDir = if (xOffset >= 0.0) EnumFacing.EAST else EnumFacing.WEST
        val zDir = if (zOffset >= 0.0) EnumFacing.SOUTH else EnumFacing.NORTH

        val xPos = checkAxis(headPos, xDir)
        val zPos = checkAxis(headPos, zDir)

        return if (xPos != null && zPos != null) {
            if (abs(xOffset) < abs(zOffset)) xPos else zPos
        } else xPos ?: zPos
    }

    private fun SafeClientEvent.checkAxis(headPos: BlockPos, sideA: EnumFacing): Pair<BlockPos, EnumFacing>? {
        val sideB = sideA.opposite
        val posA = headPos.offset(sideA)
        val posB = headPos.offset(sideB)
        val stateA = world.getBlockState(posA)
        val stateB = world.getBlockState(posB)
        val airA = stateA.block == Blocks.AIR
        val airB = stateB.block == Blocks.AIR
        val shulkerA = isValidShulker(stateA, sideB)
        val shulkerB = isValidShulker(stateB, sideA)
        val entityA = world.checkNoEntityCollision(AxisAlignedBB(posA))
        val entityB = world.checkNoEntityCollision(AxisAlignedBB(posB))

        return if (airA && airB) {
            if (entityA) posA to sideB
            else if (entityB) posB to sideA
            else null
        } else {
            if (airA && shulkerB) posB to sideA
            else if (airB && shulkerA) posA to sideB
            else null
        }
    }

    private fun isValidShulker(state: net.minecraft.block.state.IBlockState, side: EnumFacing): Boolean {
        return state.block is BlockShulkerBox && state.getValue(BlockShulkerBox.FACING) == side
    }

    private fun SafeClientEvent.shulkerMode(pos: BlockPos, side: EnumFacing) {
        val obbyPos = pos.offset(side.opposite)
        when (shulkerState) {
            ShulkerState.OBBY -> placeObby(obbyPos)
            ShulkerState.SHULKER -> placeShulker(pos, obbyPos, side)
            ShulkerState.OPEN -> openShulker(pos)
            ShulkerState.OPENING -> openingShulker(pos)
            ShulkerState.CLOSE -> closeShulker()
        }
    }

    private fun SafeClientEvent.placeObby(obbyPos: BlockPos) {
        if (!BlockKt.isReplaceable(world.getBlockState(obbyPos))) {
            shulkerState = ShulkerState.SHULKER
            return
        }
        val info = InteractKt.getPlacement(this, obbyPos, PlacementSearchOption.range(6.0), PlacementSearchOption.ENTITY_COLLISION)
        if (info != null) {
            if (!swapToBlock(Blocks.OBSIDIAN)) {
                disable()
                return
            }
            timer.reset(placeDelay.toLong() * 50L)
            lastHitVec = info.hitVec
            DefaultScope.launch {
                delay(50L)
                dev.wizard.meta.util.threads.onMainThreadSafe {
                    InteractKt.placeBlock(it, info)
                }
            }
        } else {
            disable()
        }
    }

    private fun SafeClientEvent.placeShulker(pos: BlockPos, obbyPos: BlockPos, side: EnumFacing) {
        if (world.getBlockState(pos).block is BlockShulkerBox) {
            shulkerState = ShulkerState.OPEN
            return
        }
        val slot = DefinedKt.getHotbarSlots(player).firstByStack {
            val item = it.item
            item is ItemBlock && item.block is BlockShulkerBox
        }
        if (slot == null) {
            disable()
            return
        }
        swapToSlot(slot)
        val info = PlaceInfo.newPlaceInfo(this, obbyPos, side)
        timer.reset(placeDelay.toLong() * 50L)
        lastHitVec = info.hitVec
        DefaultScope.launch {
            delay(50L)
            dev.wizard.meta.util.threads.onMainThreadSafe {
                InteractKt.placeBlock(it, info)
            }
        }
    }

    private fun SafeClientEvent.openShulker(pos: BlockPos) {
        val openSide = InteractKt.getMiningSide(this, pos) ?: EnumFacing.UP
        val offset = InteractKt.getHitVecOffset(openSide)
        timer.reset(openDelay.toLong() * 50L)
        lastHitVec = pos.toVec3d(offset)
        DefaultScope.launch {
            delay(50L)
            dev.wizard.meta.util.threads.onMainThreadSafe {
                it.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, openSide, EnumHand.MAIN_HAND, offset.x, offset.y, offset.z))
            }
        }
    }

    private fun SafeClientEvent.openingShulker(pos: BlockPos) {
        val tile = world.getTileEntity(pos)
        if (tile is TileEntityShulkerBox) {
            if (tile.animationStatus == TileEntityShulkerBox.AnimationStatus.OPENED) {
                shulkerState = ShulkerState.CLOSE
                timer.reset(closeDelay.toLong() * 50L)
            }
        }
    }

    private fun SafeClientEvent.closeShulker() {
        if (windowID != -1) {
            connection.sendPacket(CPacketCloseWindow(windowID))
        }
        disable()
    }

    private enum class ShulkerState(override val displayName: CharSequence) : DisplayEnum {
        OBBY("Obby"), SHULKER("Shulker"), OPEN("Open"), OPENING("Opening"), CLOSE("Close")
    }
}
