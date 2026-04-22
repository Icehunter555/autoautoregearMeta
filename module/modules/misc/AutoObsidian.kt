package dev.wizard.meta.module.modules.misc

import baritone.api.pathing.goals.Goal
import baritone.api.pathing.goals.GoalNear
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.events.BlockBreakEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.process.AutoObsidianProcess
import dev.wizard.meta.process.PauseProcess
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.operation.swapToItem
import dev.wizard.meta.util.inventory.operation.swapToItemOrMove
import dev.wizard.meta.util.inventory.slot.*
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.distanceSqToCenter
import dev.wizard.meta.util.math.vector.toVec3dCenter
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.defaultScope
import dev.wizard.meta.util.threads.onMainThread
import dev.wizard.meta.util.world.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.BlockEnderChest
import net.minecraft.block.BlockShulkerBox
import net.minecraft.block.state.IBlockState
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.inventory.GuiShulkerBox
import net.minecraft.enchantment.Enchantments
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.EnumDifficulty
import kotlin.math.ceil

object AutoObsidian : Module(
    name = "AutoObsidian",
    category = Category.MISC,
    description = "Breaks down Ender Chests to restock obsidian",
    modulePriority = 15
) {
    private val fillMode by setting("Fill Mode", FillMode.TARGET_STACKS)
    private val searchShulker by setting("Search Shulker", false)
    private val leaveEmptyShulkers by setting("Leave Empty Shulkers", true) { searchShulker }
    private val autoRefill by setting("Auto Refill", false) { fillMode != FillMode.INFINITE }
    private val instantMining by setting("Instant Mining", true)
    private val instantMiningDelay by setting("Instant Mining Delay", 10, 1..20, 1) { instantMining }
    private val threshold by setting("Refill Threshold", 32, 1..64, 1) { autoRefill && fillMode != FillMode.INFINITE }
    private val targetStacks by setting("Target Stacks", 1, 1..20, 1) { fillMode == FillMode.TARGET_STACKS }
    private val delayTicks by setting("Delay Ticks", 4, 1..10, 1)
    private val rotationMode by setting("Rotation Mode", RotationMode.SPOOF)
    private val maxReach by setting("Max Reach", 4.9f, 2.0f..6.0f, 0.1f)

    var goal: Goal? = null
        private set
    var state = State.SEARCHING
        private set
    private var searchingState = SearchingState.PLACING
    override var isActive = false
        private set
    private var placingPos = BlockPos(0, -1, 0)
    private var shulkerID = 0
    private var lastHitVec: Vec3d? = null
    private var lastMiningSide = EnumFacing.UP
    private var canInstantMine = false

    private val delayTimer = TickTimer(TimeUnit.TICKS)
    private val rotateTimer = TickTimer(TimeUnit.TICKS)
    private val shulkerOpenTimer = TickTimer(TimeUnit.TICKS)
    private val miningTimer = TickTimer(TimeUnit.TICKS)
    private val miningTimeoutTimer = TickTimer(TimeUnit.SECONDS)
    private val miningMap = HashMap<BlockPos, Pair<Int, Long>>()
    private val renderer = ESPRenderer().apply {
        aFilled = 33
        aOutline = 233
    }

    override fun getHudInfo(): String {
        return if (isActive) {
            if (state != State.SEARCHING) state.displayName.toString() else "Searching-${searchingState.displayName}"
        } else ""
    }

    init {
        onEnable {
            state = State.SEARCHING
        }

        onDisable {
            reset()
        }

        listener<BlockBreakEvent> {
            if (it.breakerID != player.entityId) {
                miningMap[it.position] = it.breakerID to System.currentTimeMillis()
            }
        }

        parallelListener<PacketEvent.PostSend> {
            if (instantMining && it.packet is CPacketPlayerDigging) {
                val packet = it.packet as CPacketPlayerDigging
                if (packet.position != placingPos || packet.facing != lastMiningSide) {
                    canInstantMine = false
                }
            }
        }

        listener<WorldEvent.ServerBlockUpdate> {
            if (!instantMining) return@listener
            if (it.pos != placingPos) return@listener

            val prevBlock = it.oldState.block
            val newBlock = it.newState.block

            if (prevBlock != newBlock) {
                if (prevBlock != Blocks.AIR && newBlock == Blocks.AIR) {
                    canInstantMine = true
                }
                miningTimer.reset()
                miningTimeoutTimer.reset()
            }
        }

        listener<Render3DEvent> {
            if (state != State.DONE) {
                renderer.render(false)
            }
        }

        listener<TickEvent.Pre>(69) {
            if (PauseProcess.isActive || world.difficulty == EnumDifficulty.PEACEFUL || player.dimension == 1 || player.serverBrand?.contains("2b2t", true) == true) return@listener

            updateMiningMap()
            runAutoObby()
            doRotation()
        }
    }

    private fun updateMiningMap() {
        val removeTime = System.currentTimeMillis() - 5000L
        miningMap.values.removeIf { it.second < removeTime }
    }

    private fun doRotation() {
        if (rotateTimer.tick(20L)) return
        val hitVec = lastHitVec ?: return
        val rotation = RotationUtils.getRotationTo(hitVec)

        when (rotationMode) {
            RotationMode.SPOOF -> {
                PlayerPacketManager.sendPlayerPacket {
                    rotate(rotation)
                }
            }
            RotationMode.VIEW_LOCK -> {
                player.rotationYaw = rotation.x
                player.rotationPitch = rotation.y
            }
            else -> {}
        }
    }

    private fun runAutoObby() {
        if (!delayTimer.tickAndReset(delayTicks.toLong())) return

        updateState()

        when (state) {
            State.SEARCHING -> searchingState()
            State.PLACING -> placeEnderChest(placingPos)
            State.PRE_MINING -> mineBlock(placingPos, true)
            State.MINING -> mineBlock(placingPos, false)
            State.COLLECTING -> collectDroppedItem(Blocks.OBSIDIAN.id)
            State.DONE -> {
                if (!autoRefill) {
                    NoSpamMessage.sendMessage("$chatName ${fillMode.message}, disabling.")
                    disable()
                } else {
                    if (isActive) {
                        NoSpamMessage.sendMessage("$chatName ${fillMode.message}, stopping.")
                    }
                    reset()
                }
            }
        }
    }

    private fun updateState() {
        if (state != State.DONE) {
            updatePlacingPos()
            if (!isActive) {
                isActive = true
                BaritoneUtils.primary?.pathingControlManager?.registerProcess(AutoObsidianProcess)
            }
            if (state != State.COLLECTING && searchingState != SearchingState.COLLECTING) {
                goal = if (player.distanceSqToCenter(placingPos) > 4.0) GoalNear(placingPos, 2) else null
            }
        }
        updateSearchingState()
        updateMainState()
    }

    private fun updatePlacingPos() {
        val eyePos = player.getPositionEyes(1.0f)
        val currentBlockState = world.getBlockState(placingPos)

        if (isPositionValid(placingPos, currentBlockState, eyePos)) return

        val posList = VectorUtils.getBlockPosInSphere(eyePos, maxReach).filter { !miningMap.containsKey(it) }.map { it to world.getBlockState(it) }.sortedBy { it.first.distanceSqToCenter(eyePos) }.toList()

        val validPos = posList.firstOrNull { it.second.block == Blocks.ENDER_CHEST || it.second.block is BlockShulkerBox }
            ?: posList.firstOrNull { isPositionValid(it.first, it.second, eyePos) }

        if (validPos != null) {
            if (validPos.first != placingPos) {
                placingPos = validPos.first
                canInstantMine = false
                renderer.clear()
                renderer.add(placingPos, ColorRGB(64, 255, 64))
            }
        } else {
            NoSpamMessage.sendMessage("$chatName No valid position for placing shulker box / ender chest nearby, disabling.")
            mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ITEM_BREAK, 1.0f))
            disable()
        }
    }

    private fun isPositionValid(pos: BlockPos, blockState: IBlockState, eyePos: Vec3d): Boolean {
        if (world.getBlockState(pos.down()).isReplaceable) return false
        val block = blockState.block
        if (block != Blocks.ENDER_CHEST && block !is BlockShulkerBox) {
            if (!world.isPlaceable(pos)) return false
        }
        if (!world.isAir(pos.up())) return false

        val rayTrace = world.rayTraceBlocks(eyePos, pos.toVec3dCenter()) ?: return true
        return rayTrace.typeOfHit == RayTraceResult.Type.MISS
    }

    private fun updateMainState() {
        val passCountCheck = checkObbyCount()

        state = when {
            state == State.DONE && autoRefill -> {
                if (player.inventorySlots.countBlock(Blocks.OBSIDIAN) < threshold) State.SEARCHING else state
            }
            state == State.COLLECTING -> {
                if (canPickUpObby()) {
                    if (EntityUtils.getDroppedItem(Blocks.OBSIDIAN.id, 8.0f) == null) State.DONE
                    else State.COLLECTING
                } else {
                    State.DONE
                }
            }
            state != State.DONE && world.isAir(placingPos) && !passCountCheck -> State.COLLECTING
            state == State.MINING && world.isAir(placingPos) -> startPlacing()
            state == State.PLACING && !world.isAir(placingPos) -> State.PRE_MINING
            state == State.SEARCHING && searchingState == SearchingState.DONE && passCountCheck -> startPlacing()
            else -> state
        }
    }

    private fun startPlacing(): State {
        if (searchShulker) {
            if (player.inventorySlots.countBlock(Blocks.ENDER_CHEST) == 0) {
                return State.SEARCHING
            }
        }
        return State.PLACING
    }

    private fun canPickUpObby(): Boolean {
        if (fillMode == FillMode.INFINITE) return true
        val mainInventory = player.inventory?.mainInventory ?: return false
        
        return mainInventory.any { 
            it.isEmpty || (it.item.block == Blocks.OBSIDIAN && it.count < 64)
        }
    }

    private fun checkObbyCount(): Boolean {
        return when (fillMode) {
            FillMode.TARGET_STACKS -> {
                val empty = countEmptySlots()
                val dropped = countDropped()
                val total = countInventory() + dropped
                val hasEmptySlots = empty - dropped >= 8
                val belowTarget = ceil(total / 8.0f) / 8.0f < targetStacks
                hasEmptySlots && belowTarget
            }
            FillMode.FILL_INVENTORY -> countEmptySlots() - countDropped() >= 8
            FillMode.INFINITE -> true
        }
    }

    private fun countInventory(): Int = player.inventorySlots.countBlock(Blocks.OBSIDIAN)

    private fun countDropped(): Int {
        return EntityUtils.getDroppedItems(Blocks.OBSIDIAN.id, 8.0f).sumOf { it.item.count }
    }

    private fun countEmptySlots(): Int {
        return player.inventorySlots.sumOf {
            if (it.stack.isEmpty) 64 else if (it.stack.item.block == Blocks.OBSIDIAN) 64 - it.stack.count else 0
        }
    }

    private fun updateSearchingState() {
        if (state == State.SEARCHING) {
            val enderChestCount = player.inventorySlots.countBlock(Blocks.ENDER_CHEST)
            if (searchingState != SearchingState.DONE) {
                searchingState = when {
                    searchingState == SearchingState.PLACING && enderChestCount > 0 -> SearchingState.DONE
                    searchingState == SearchingState.COLLECTING && EntityUtils.getDroppedItem(shulkerID, 8.0f) == null -> SearchingState.DONE
                    searchingState == SearchingState.MINING && world.isAir(placingPos) -> if (enderChestCount > 0) SearchingState.COLLECTING else SearchingState.PLACING
                    searchingState == SearchingState.OPENING && (enderChestCount > 0 || player.inventorySlots.firstEmpty() == null) -> SearchingState.PRE_MINING
                    searchingState == SearchingState.PLACING && !world.isAir(placingPos) -> if (world.getBlockState(placingPos).block is BlockShulkerBox) SearchingState.OPENING else SearchingState.PRE_MINING
                    else -> searchingState
                }
            }
        } else {
            searchingState = SearchingState.PLACING
        }
    }

    private fun searchingState() {
        if (searchShulker) {
            when (searchingState) {
                SearchingState.PLACING -> placeShulker(placingPos)
                SearchingState.OPENING -> openShulker(placingPos)
                SearchingState.PRE_MINING -> mineBlock(placingPos, true)
                SearchingState.MINING -> mineBlock(placingPos, false)
                SearchingState.COLLECTING -> collectDroppedItem(shulkerID)
                SearchingState.DONE -> updatePlacingPos()
            }
        } else {
            searchingState = SearchingState.DONE
        }
    }

    private fun placeShulker(pos: BlockPos) {
        val hotbarSlot = player.hotbarSlots.firstByStack { it.item is ItemShulkerBox }
        
        if (hotbarSlot == null) {
            val moved = if (player.hotbarSlots.firstByStack { it.item is ItemShulkerBox } != null) {
                swapToItem { it.item is ItemShulkerBox }
                true
            } else {
                val storageSlot = player.storageSlots.firstByStack { it.item is ItemShulkerBox }
                if (storageSlot != null) {
                    val anyHotbarSlot = player.anyHotbarSlot { it.item !is ItemShulkerBox && it.item.block !is BlockEnderChest }
                    InventoryTaskManager.runNow {
                        priority = Int.MAX_VALUE
                        delay = 0
                        swapWith(storageSlot, anyHotbarSlot)
                    }
                    true
                } else {
                    false
                }
            }

            if (!moved) {
                NoSpamMessage.sendMessage("$chatName No shulker box was found in inventory, disabling.")
                mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ITEM_BREAK, 1.0f))
                disable()
            }
            onInventoryOperation()
            return
        }

        shulkerID = hotbarSlot.stack.item.id
        swapToSlot(hotbarSlot)

        if (world.getBlockState(pos).block !is BlockShulkerBox) {
            placeBlock(pos)
        }
    }

    private fun placeEnderChest(pos: BlockPos) {
        if (!swapToBlock(Blocks.ENDER_CHEST)) {
            val moved = swapToItemOrMove(Blocks.ENDER_CHEST.item) {
                it.item != Items.DIAMOND_PICKAXE && it.item.block !is BlockShulkerBox
            }
            if (!moved) {
                NoSpamMessage.sendMessage("$chatName No ender chest was found in inventory, disabling.")
                mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ITEM_BREAK, 1.0f))
                disable()
            }
            onInventoryOperation()
            return
        }
        placeBlock(pos)
    }

    private fun openShulker(pos: BlockPos) {
        if (mc.currentScreen !is GuiShulkerBox) {
            val center = pos.toVec3dCenter()
            val diff = player.getPositionEyes(1.0f).subtract(center)
            val normalizedVec = diff.normalize()
            val side = EnumFacing.getFacingFromVector(normalizedVec.x.toFloat(), normalizedVec.y.toFloat(), normalizedVec.z.toFloat())
            val hitVecOffset = getHitVecOffset(side)
            lastHitVec = getHitVec(pos, side)
            rotateTimer.reset()

            if (!shulkerOpenTimer.tickAndReset(50L)) return

            defaultScope.launch {
                delay(20L)
                onMainThread {
                    player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, side, EnumHand.MAIN_HAND, hitVecOffset.x, hitVecOffset.y, hitVecOffset.z))
                    player.swingArm(EnumHand.MAIN_HAND)
                }
            }
            return
        }

        val container = player.openContainer
        val slot = container.inventorySlots.take(27).firstBlock(Blocks.ENDER_CHEST)

        if (slot != null) {
            InventoryTaskManager.runNow {
                priority = Int.MAX_VALUE
                delay = 0
                quickMove(container.windowId, slot)
            }
            player.closeScreen()
            return
        }

        if (!shulkerOpenTimer.tick(100L)) return

        if (leaveEmptyShulkers) {
            val isEmpty = container.inventory.subList(0, 27).all { it.isEmpty }
            if (isEmpty) {
                searchingState = SearchingState.PRE_MINING
                player.closeScreen()
                return
            }
        }

        NoSpamMessage.sendMessage("$chatName No ender chest was found in shulker, disabling.")
        mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ITEM_BREAK, 1.0f))
        disable()
    }

    private fun placeBlock(pos: BlockPos) {
        val placeInfo = getPlacement(pos, 1.0, PlacementSearchOption.range(5.0), PlacementSearchOption.ENTITY_COLLISION)
        if (placeInfo == null) {
            NoSpamMessage.sendMessage("$chatName Can't find neighbor block")
            return
        }

        lastHitVec = placeInfo.hitVec
        rotateTimer.reset()

        defaultScope.launch {
            delay(20L)
            onMainThread {
                if (!player.isSneaking) {
                    player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
                    placeBlock(placeInfo)
                    player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
                } else {
                    placeBlock(placeInfo)
                }
            }
        }
    }

    private fun mineBlock(pos: BlockPos, pre: Boolean) {
        if (!swapToValidPickaxe()) return

        val center = pos.toVec3dCenter()
        val diff = player.getPositionEyes(1.0f).subtract(center)
        val normalizedVec = diff.normalize()
        var side = EnumFacing.getFacingFromVector(normalizedVec.x.toFloat(), normalizedVec.y.toFloat(), normalizedVec.z.toFloat())
        
        lastHitVec = center
        rotateTimer.reset()

        if (instantMining && canInstantMine) {
            if (!miningTimer.tick(instantMiningDelay.toLong())) return
            if (!miningTimeoutTimer.tick(2L)) {
                side = side.opposite
            } else {
                canInstantMine = false
            }
        }

        defaultScope.launch {
            delay(20L)
            onMainThread {
                if (pre || miningTimeoutTimer.tickAndReset(8L)) {
                    player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side))
                    if (state != State.SEARCHING) {
                        state = State.MINING
                    } else {
                        searchingState = SearchingState.MINING
                    }
                } else {
                    player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side))
                }
                player.swingArm(EnumHand.MAIN_HAND)
                lastMiningSide = side
            }
        }
    }

    private fun swapToValidPickaxe(): Boolean {
        if (!swapToItem(Items.DIAMOND_PICKAXE) { EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, it) == 0 }) {
            val moved = swapToItemOrMove(Items.DIAMOND_PICKAXE, 
                { EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, it) == 0 },
                { it.item.block !is BlockShulkerBox && it.item.block !is BlockEnderChest }
            )
            
            if (!moved) {
                NoSpamMessage.sendMessage("No valid pickaxe was found in inventory.")
                mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ITEM_BREAK, 1.0f))
                disable()
            }
            onInventoryOperation()
            return false
        }
        return true
    }

    private fun onInventoryOperation() {
        delayTimer.reset(20L)
        playerController.updateController()
    }

    private fun collectDroppedItem(itemId: Int) {
        val droppedItem = EntityUtils.getDroppedItem(itemId, 8.0f)
        goal = if (droppedItem != null) GoalNear(droppedItem, 0) else null
    }

    private fun reset() {
        isActive = false
        goal = null
        searchingState = SearchingState.PLACING
        placingPos = BlockPos(0, -1, 0)
        lastHitVec = null
        lastMiningSide = EnumFacing.UP
        canInstantMine = false
        onMainThread { miningMap.clear() }
    }

    private enum class FillMode(override val displayName: CharSequence, val message: String) : DisplayEnum {
        TARGET_STACKS("Target Stacks", "Target stacks reached"),
        FILL_INVENTORY("Fill Inventory", "Inventory filled"),
        INFINITE("Infinite", "")
    }

    private enum class RotationMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off"),
        SPOOF("Spoof"),
        VIEW_LOCK("View Lock")
    }

    private enum class SearchingState(override val displayName: CharSequence) : DisplayEnum {
        PLACING("Placing"),
        OPENING("Opening"),
        PRE_MINING("Pre Mining"),
        MINING("Mining"),
        COLLECTING("Collecting"),
        DONE("Done")
    }

    private enum class State(override val displayName: CharSequence) : DisplayEnum {
        SEARCHING("Searching"),
        PLACING("Placing"),
        PRE_MINING("Pre Mining"),
        MINING("Mining"),
        COLLECTING("Collecting"),
        DONE("Done")
    }
}
