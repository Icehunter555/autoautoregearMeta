package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.CrystalSetDeadEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.extension.synchronized
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.TaskKt
import dev.wizard.meta.util.inventory.UtilsKt
import dev.wizard.meta.util.inventory.operation.swapWith
import dev.wizard.meta.util.inventory.operation.quickMove
import dev.wizard.meta.util.inventory.operation.pickUp
import dev.wizard.meta.util.inventory.operation.throwAll
import dev.wizard.meta.util.inventory.slot.*
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.threads.ThreadSafetyKt
import dev.wizard.meta.util.world.BlockKt
import dev.wizard.meta.util.world.FastRaytraceKt
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockShulkerBox
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerShulkerBox
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemShulkerBox
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.util.*

object AutoRegear : Module(
    "AutoRegear",
    category = Category.COMBAT,
    description = "Automatically regear using container"
) {
    private val regearKey by setting(this, BindSetting(settingName("Place Shulker Key"), Bind(), { if (it) placeShulker = true }))
    private val openShulkerKey by setting(this, BindSetting(settingName("Open Shulker Key"), Bind(), { if (it) findShulker = true }))
    var placeRange by setting(this, FloatSetting(settingName("Place Range"), 4.0f, 1.0f..6.0f, 0.1f))
    private val placeOnLiquids by setting(this, BooleanSetting(settingName("Place On Liquids"), false))
    val shulkerBoxOnly by setting(this, BooleanSetting(settingName("Shulker Box Only"), true))
    private val hideInventory by setting(this, BooleanSetting(settingName("Hide Inventory"), false))
    private val closeInventory by setting(this, BooleanSetting(settingName("Close Inventory"), false))
    private val onShulker by setting(this, BooleanSetting(settingName("Place On Shulker"), false))
    private val blockPlaceRotation by setting(this, BooleanSetting(settingName("Place Rotation"), false))
    private val regearTimeout by setting(this, IntegerSetting(settingName("Regear Timeout"), 500, 0..5000, 10))
    var clickDelayMs by setting(this, IntegerSetting(settingName("Click Delay ms"), 5, 0..1000, 1))
    var postDelayMs by setting(this, IntegerSetting(settingName("Post Delay ms"), 25, 0..1000, 1))
    var moveTimeoutMs by setting(this, IntegerSetting(settingName("Move Timeout ms"), 100, 0..1000, 1))
    private val blockDelay by setting(this, IntegerSetting(settingName("Block Delay"), 100, 0..1000, 10))
    private val openDelay by setting(this, IntegerSetting(settingName("Open Delay"), 50, 20..1000, 20))
    private val sendMessage by setting(this, BooleanSetting(settingName("Send Message"), false))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 255, 255), { render }))
    private val renderTime by setting(this, IntegerSetting(settingName("Render Time"), 2000, 500..5000, 100, { render }))
    private val animate by setting(this, BooleanSetting(settingName("Animate"), true, { render }))
    private val movingLength by setting(this, IntegerSetting(settingName("Moving Length"), 250, 0..1000, 50, { animate && render }))
    private val fadeLength by setting(this, IntegerSetting(settingName("Fade Length"), 300, 0..1000, 50, { animate && render }))

    private val directions = EnumFacing.VALUES
    private val armorTimer = TickTimer()
    private val timeoutTimer = TickTimer()
    private val placementTimer = TickTimer()
    private val moveTimeMap = Int2LongOpenHashMap().apply { defaultReturnValue(Long.MIN_VALUE) }
    private val explosionPosMap: Long2LongMap = Long2LongOpenHashMap().apply { defaultReturnValue(Long.MIN_VALUE) }.synchronized()
    private val swapMap = Int2BooleanOpenHashMap().apply { defaultReturnValue(false) }
    private var lastContainer: Container? = null
    private var lastTask: InventoryTask? = null
    var placeShulker = false
    var findShulker = false
    private var closeAfterRegear = false
    private var regearing = false
    private var forceArmor = false
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val renderedBlocks = linkedMapOf<BlockPos, Long>()
    private var prevBox: AxisAlignedBB? = null
    private var currentBox: AxisAlignedBB? = null
    private var lastRenderBox: AxisAlignedBB? = null
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f
    private var isFadingOut = false
    private var currentPlacedPos: BlockPos? = null
    private val moveTimer = TickTimer(TimeUnit.TICKS)

    init {
        onDisable {
            reset()
            explosionPosMap.clear()
            resetAnimation()
        }

        onEnable {
            reset()
            resetAnimation()
        }

        safeListener<CrystalSetDeadEvent> {
            explosionPosMap.put(VectorUtils.toLong(it.x, it.y, it.z), System.currentTimeMillis() + 3000L)
        }

        safeListener<RunGameLoopEvent.Tick> {
            val currentScreen = mc.currentScreen
            val openContainer = player.openContainer
            if (!regearing) {
                if (openContainer == player.inventoryContainer || (shulkerBoxOnly && openContainer !is ContainerShulkerBox)) {
                    reset()
                    return@safeListener
                }
                if (currentScreen !is GuiContainer) {
                    reset()
                    return@safeListener
                }
            }
            regearing = true
            if (hideInventory && closeAfterRegear && currentScreen != null) {
                mc.displayGuiScreen(null)
            }
            if (!TaskKt.getExecutedOrTrue(lastTask)) return@safeListener

            if (openContainer != lastContainer) {
                moveTimeMap.clear()
                timeoutTimer.setTime(Long.MAX_VALUE)
                lastContainer = openContainer
            } else if (timeoutTimer.tick(regearTimeout.toLong())) {
                if (closeInventory && closeAfterRegear) {
                    if (mc.currentScreen == null) player.closeScreen() else mc.displayGuiScreen(null)
                    player.openContainer = player.inventoryContainer
                }
                closeAfterRegear = false
                regearing = false
                return@safeListener
            }

            val itemArray = Kit.getKitItemArray() ?: run {
                NoSpamMessage.sendError("No kit named ${Kit.kitName} was found!")
                disable()
                return@safeListener
            }

            if (forceArmor && takeArmor(openContainer)) return@safeListener
            if (doRegear(openContainer, itemArray)) return@safeListener
        }

        safeListener<Render3DEvent> {
            if (!render) return@safeListener
            val currentTime = System.currentTimeMillis()
            val iterator = renderedBlocks.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val pos = entry.key
                val timestamp = entry.value
                val age = currentTime - timestamp
                val life = renderTime.toLong()
                if (age > life) {
                    iterator.remove()
                    if (pos == currentPlacedPos) {
                        isFadingOut = true
                        startTime = currentTime
                    }
                    continue
                }
                val box = AxisAlignedBB(pos)
                if (animate) {
                    updateAnimation(pos, box)
                    renderAnimated(renderColor.value)
                    continue
                }
                val alpha = ((life - age).toFloat() / life.toFloat() * 255f).toInt().coerceIn(0, 255)
                renderer.add(box, renderColor.value.alpha(alpha))
            }
            renderer.render(true)
            if (animate && renderedBlocks.isEmpty() && isFadingOut && lastRenderBox != null) {
                renderAnimated(renderColor.value)
                if (scale <= 0.0f) resetAnimation()
            }
        }

        safeParallelListener<TickEvent.Post> {
            val currentTime = System.currentTimeMillis()
            synchronized(explosionPosMap) {
                explosionPosMap.values.removeIf { it < currentTime }
            }

            if (findShulker) {
                findShulker = false
                val eyePos = player.getPositionEyes(1.0f)
                val scanRangeSq = placeRange * placeRange
                val nearestShulker = VectorUtils.getBlockPosInSphere(eyePos, placeRange).asSequence().filter {
                    world.getBlockState(it).block is BlockShulkerBox
                }.filter {
                    player.getDistanceSqToCenter(it) <= scanRangeSq
                }.minByOrNull { player.getDistanceSqToCenter(it) }

                if (nearestShulker != null) {
                    if (sendMessage) NoSpamMessage.sendMessage("${getChatName()} Found shulker, opening...")
                    val lookVec = Vec3d(nearestShulker).addVector(0.5, 0.5, 0.5).subtract(eyePos).normalize()
                    val reachVec = eyePos.add(lookVec.scale(6.0))
                    val rayTrace = world.rayTraceBlocks(eyePos, reachVec, false, false, true)
                    if (rayTrace != null && rayTrace.typeOfHit == RayTraceResult.Type.BLOCK && rayTrace.blockPos == nearestShulker) {
                        if (blockPlaceRotation) {
                            val rotationTo = RotationUtils.getRotationTo(this, Vec3d(nearestShulker).addVector(0.5, 0.5, 0.5))
                            connection.sendPacket(CPacketPlayer.Rotation(rotationTo.x, rotationTo.y, player.onGround))
                        }
                        mc.playerController.processRightClickBlock(player, world, nearestShulker, rayTrace.sideHit, rayTrace.hitVec, EnumHand.MAIN_HAND)
                        closeAfterRegear = true
                        regearing = true
                    }
                } else if (sendMessage) {
                    NoSpamMessage.sendMessage("${getChatName()} No shulker found in range!")
                }
                return@safeParallelListener
            }

            if (placeShulker && placementTimer.tick(blockDelay.toLong())) {
                placeShulker = false
                if (sendMessage) NoSpamMessage.sendMessage("${getChatName()} Regearing...")
                val shulkerSlot = DefinedKt.getAllSlotsPrioritized(player).firstByStack {
                    val item = it.item
                    item is ItemBlock && item.block is BlockShulkerBox
                } ?: return@safeParallelListener

                ConcurrentScope.launch {
                    val explosionPosArray = synchronized(explosionPosMap) { explosionPosMap.keys.toLongArray() }
                    val playerList = EntityManager.players.filter { !EntityUtils.isSelf(it) && !EntityUtils.isFriend(it) && player.getDistanceSq(it) <= 256.0 }
                    val rangeSq = placeRange * placeRange
                    val mutable = BlockPos.MutableBlockPos()
                    val bestPlacement = findBestPlacement(this@safeParallelListener, rangeSq, mutable, explosionPosArray, playerList)

                    if (bestPlacement != null) {
                        closeAfterRegear = true
                        regearing = true
                        val placeInfo = PlaceInfo.newPlaceInfo(this@safeParallelListener, bestPlacement.first, bestPlacement.second)
                        if (blockPlaceRotation) {
                            val rotationTo = RotationUtils.getRotationTo(this@safeParallelListener, placeInfo.hitVec)
                            connection.sendPacket(CPacketPlayer.Rotation(rotationTo.x, rotationTo.y, player.onGround))
                        }
                        HotbarSwitchManager.ghostSwitch(this@safeParallelListener, shulkerSlot) {
                            if (!player.isSneaking) {
                                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
                                InteractKt.placeBlock(this@safeParallelListener, placeInfo)
                                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
                            } else {
                                InteractKt.placeBlock(this@safeParallelListener, placeInfo)
                            }
                        }
                        DefaultScope.launch {
                            delay(openDelay.toLong())
                            val shulkerPos = placeInfo.placedPos
                            val interactFace = directions.asSequence().filter { face ->
                                val faceVec = face.directionVec
                                val interactPos = Vec3d(shulkerPos.x + 0.5 + faceVec.x * 0.5, shulkerPos.y + 0.5 + faceVec.y * 0.5, shulkerPos.z + 0.5 + faceVec.z * 0.5)
                                player.getPositionEyes(1.0f).distanceTo(interactPos) <= 6.0
                            }.minByOrNull { face ->
                                val faceVec = face.directionVec
                                val interactPos = Vec3d(shulkerPos.x + 0.5 + faceVec.x * 0.5, shulkerPos.y + 0.5 + faceVec.y * 0.5, shulkerPos.z + 0.5 + faceVec.z * 0.5)
                                player.getPositionEyes(1.0f).distanceTo(interactPos)
                            } ?: EnumFacing.UP

                            connection.sendPacket(CPacketPlayerTryUseItemOnBlock(shulkerPos, interactFace, EnumHand.MAIN_HAND, 0.5f, 0.5f, 0.5f))
                            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                            updatePlacementForAnimation(placeInfo.placedPos)
                        }
                    } else if (sendMessage) {
                        NoSpamMessage.sendWarning("${getChatName()} Could not find a suitable placement for shulker box.")
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.doRegear(openContainer: Container, itemArray: Array<Kit.ItemEntry>): Boolean {
        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = DefinedKt.getContainerSlots(openContainer).filter { currentTime > moveTimeMap.get(it.slotNumber) }.toMutableList()
        if (forceArmor) return false

        val playerSlots = DefinedKt.getPlayerSlots(openContainer)
        var hasEmptyBefore = false
        for (index in playerSlots.indices) {
            val slotTo = playerSlots[index]
            val slotToStack = slotTo.stack
            if (slotToStack.isEmpty) hasEmptyBefore = true
            if (currentTime <= moveTimeMap.get(slotTo.slotNumber) || swapMap.get(slotTo.slotNumber)) continue

            val targetItem = itemArray[index]
            if (targetItem.item is ItemShulkerBox) continue

            val isHotbar = index >= playerSlots.size - 9
            if (isHotbar && slotToStack.item is ItemArmor) continue

            val slotFrom = IterableKt.findMaxCompatibleStack(containerSlots, slotTo, targetItem, true) ?: continue

            if (isHotbar && (slotFrom.stack.count >= 64 || !slotFrom.stack.isStackable)) {
                swapMap.put(slotTo.slotNumber, true)
                InventoryTask.Builder().apply {
                    priority(modulePriority)
                    swapWith(windowID, slotFrom, DefinedKt.getHotbarSlot(player, index - (playerSlots.size - 9)))
                    delay(clickDelayMs)
                    postDelay(postDelayMs)
                    runInGui()
                }.build().also {
                    InventoryTaskManager.addTask(it)
                    lastTask = it
                }
            } else if (!hasEmptyBefore && slotToStack.isStackable && UtilsKt.isStackable(slotToStack, slotFrom.stack)) {
                InventoryTask.Builder().apply {
                    priority(modulePriority)
                    quickMove(windowID, slotFrom)
                    delay(clickDelayMs)
                    postDelay(postDelayMs)
                    runInGui()
                }.build().also {
                    InventoryTaskManager.addTask(it)
                    lastTask = it
                }
            } else {
                if (!slotFrom.stack.isStackable && !slotToStack.isEmpty) continue
                InventoryTask.Builder().apply {
                    priority(modulePriority)
                    pickUp(windowID, slotFrom)
                    pickUp(windowID, slotTo)
                    pickUp(windowID, { if (it.player.inventory.itemStack.isEmpty) null else slotFrom })
                    delay(clickDelayMs)
                    postDelay(postDelayMs)
                    runInGui()
                }.build().also {
                    InventoryTaskManager.addTask(it)
                    lastTask = it
                }
            }
            moveTimeMap.put(slotTo.slotNumber, currentTime + moveTimeoutMs)
            moveTimeMap.put(slotFrom.slotNumber, currentTime + moveTimeoutMs)
            timeoutTimer.setTime(Long.MAX_VALUE)
            containerSlots.remove(slotFrom)
            return true
        }
        if (timeoutTimer.getTime() == Long.MAX_VALUE) timeoutTimer.reset()
        return false
    }

    private fun SafeClientEvent.takeArmor(openContainer: Container): Boolean {
        if (!forceArmor) return false
        AutoArmor.disable()
        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = DefinedKt.getContainerSlots(openContainer).filter { currentTime > moveTimeMap.get(it.slotNumber) }

        var hotbarSlot = DefinedKt.getHotbarSlots(player).firstEmpty() ?: run {
            DefinedKt.getHotbarSlots(player).firstOrNull { it.stack.item !is ItemShulkerBox && it.stack.item !is ItemArmor }
        } ?: return false

        for (slotFrom in containerSlots) {
            val item = slotFrom.stack.item
            if (item !is ItemArmor) continue
            val armorEquipSlot = when (item.armorType) {
                EntityEquipmentSlot.HEAD -> 5
                EntityEquipmentSlot.CHEST -> 6
                EntityEquipmentSlot.LEGS -> 7
                EntityEquipmentSlot.FEET -> 8
                else -> continue
            }

            if (!armorTimer.tickAndReset(100L)) {
                timeoutTimer.setTime(Long.MAX_VALUE)
                return true
            }

            InventoryTask.Builder().apply {
                priority(modulePriority)
                throwAll(hotbarSlot)
                delay(clickDelayMs)
                swapWith(windowID, slotFrom, hotbarSlot)
                delay(clickDelayMs)
                action { it.mc.playerController.windowClick(windowID, armorEquipSlot, 1, ClickType.THROW, it.player) }
                delay(clickDelayMs)
                action { HotbarSwitchManager.ghostSwitch(it, hotbarSlot) { it.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND)) } }
                postDelay(postDelayMs)
                runInGui()
            }.build().also {
                InventoryTaskManager.addTask(it)
                lastTask = it
                moveTimeMap.put(slotFrom.slotNumber, currentTime + moveTimeoutMs)
                timeoutTimer.setTime(Long.MAX_VALUE)
            }
            return true
        }
        forceArmor = false
        return false
    }

    private fun findBestPlacement(event: SafeClientEvent, rangeSq: Float, mutable: BlockPos.MutableBlockPos, explosionPos: LongArray, playerList: List<EntityPlayer>): Pair<BlockPos, EnumFacing>? {
        val eyePos = EntityUtils.getEyePosition(event.player)
        return VectorUtils.getBlockPosInSphere(eyePos, placeRange + 3.0f).asSequence()
            .filter { pos -> val state = event.world.getBlockState(pos); if (placeOnLiquids) !BlockKt.isReplaceable(state) || BlockKt.isLiquid(state) else !BlockKt.isReplaceable(state) }
            .flatMap { pos ->
                directions.asSequence()
                    .filter { face -> val dirVec = face.directionVec; player.getDistanceSq(pos.x + 0.5 + dirVec.x * 1.5, pos.y + 0.5 + dirVec.y * 1.5, pos.z + 0.5 + dirVec.z * 1.5) < rangeSq }
                    .filter { face ->
                        val placedPos = VectorUtils.setAndAdd(mutable, pos, face)
                        !event.world.isOutsideBuildHeight(placedPos) && event.world.worldBorder.contains(placedPos) &&
                        BlockKt.isReplaceable(event.world.getBlockState(placedPos)) &&
                        EntityManager.checkNoEntityCollision(placedPos) &&
                        checkPosIsValid(event, mutable.setPos(placedPos).move(face))
                    }
                    .map { pos to it }
            }
            .sortedWith(compareBy<Pair<BlockPos, EnumFacing>> { (pos, face) ->
                val placedPos = VectorUtils.setAndAdd(mutable, pos, face)
                explosionPos.sumBy { it -> FastRaytraceKt.fastRayTraceCorners(event.world, VectorUtils.xFromLong(it) + 0.5, VectorUtils.yFromLong(it) + 0.5, VectorUtils.zFromLong(it) + 0.5, placedPos.x, placedPos.y, placedPos.z, 200, mutable) }
            }.thenBy { (pos, _) ->
                playerList.map { it.getDistanceSqToCenter(pos) }.maxOrNull() ?: 0.0
            }.thenByDescending { (pos, _) ->
                player.getDistanceSqToCenter(pos)
            }).firstOrNull()
    }

    private fun checkPosIsValid(event: SafeClientEvent, block: BlockPos): Boolean {
        val worldBlock = event.world.getBlockState(block).block
        return worldBlock is BlockLiquid && (!Interactions.isEnabled || Interactions.isLiquidInteractEnabled) || worldBlock is BlockAir || (onShulker && worldBlock is BlockShulkerBox)
    }

    private fun updatePlacementForAnimation(pos: BlockPos) {
        val box = AxisAlignedBB(pos)
        if (pos != currentPlacedPos || prevBox == null) {
            prevBox = lastRenderBox ?: box
            currentBox = box
            lastUpdateTime = System.currentTimeMillis()
            startTime = System.currentTimeMillis()
            scale = 0.0f
            isFadingOut = false
            currentPlacedPos = pos
        }
        renderedBlocks[pos] = System.currentTimeMillis()
    }

    private fun updateAnimation(targetPos: BlockPos, box: AxisAlignedBB) {
        if (targetPos != currentPlacedPos) {
            currentPlacedPos = targetPos
            prevBox = lastRenderBox ?: box
            currentBox = box
            lastUpdateTime = System.currentTimeMillis()
            startTime = System.currentTimeMillis()
            isFadingOut = false
        }
    }

    private fun renderAnimated(baseColor: ColorRGB) {
        if (isFadingOut && lastRenderBox != null) {
            scale = Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength.toLong()))
            if (scale > 0.0f) {
                val color = baseColor.alpha((255 * scale).toInt())
                renderer.aFilled = (31 * scale).toInt()
                renderer.aOutline = (233 * scale).toInt()
                renderer.add(lastRenderBox!!, color)
            } else resetAnimation()
            return
        }

        prevBox?.let { prev ->
            currentBox?.let { curr ->
                val moveProgress = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, movingLength.toLong()))
                val interpolated = interpolateBox(prev, curr, moveProgress.toDouble())
                lastRenderBox = interpolated
                scale = Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.toLong()))
                val alpha = (255 * scale).toInt().coerceAtLeast(30)
                val color = baseColor.alpha(alpha)
                renderer.aFilled = (31 * scale).toInt()
                renderer.aOutline = (233 * scale).toInt()
                renderer.add(interpolated, color)
                return
            }
        }

        currentBox?.let { it ->
            prevBox = it
            lastRenderBox = it
            scale = Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.toLong()))
            val color = baseColor.alpha((255 * scale).toInt())
            renderer.aFilled = (31 * scale).toInt()
            renderer.aOutline = (233 * scale).toInt()
            renderer.add(it, color)
        }
    }

    private fun interpolateBox(from: AxisAlignedBB, to: AxisAlignedBB, progress: Double): AxisAlignedBB {
        return AxisAlignedBB(
            from.minX + (to.minX - from.minX) * progress,
            from.minY + (to.minY - from.minY) * progress,
            from.minZ + (to.minZ - from.minZ) * progress,
            from.maxX + (to.maxX - from.maxX) * progress,
            from.maxY + (to.maxY - from.maxY) * progress,
            from.maxZ + (to.maxZ - from.maxZ) * progress
        )
    }
}
