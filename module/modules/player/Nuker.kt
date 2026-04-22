package dev.wizard.meta.module.modules.player

import com.google.gson.reflect.TypeToken
import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.collection.CollectionSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.isTool
import dev.wizard.meta.util.inventory.slot.allSlots
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.toVec3d
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Enchantments
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*

object Nuker : Module(
    "Nuker",
    category = Category.PLAYER,
    description = "Automatically mines blocks in range",
    modulePriority = 30
) {
    private val range by setting(this, FloatSetting(settingName("Range"), 4.5f, 1.0f..6.0f, 0.5f))
    private val scanDelay by setting(this, IntegerSetting(settingName("Scan Delay"), 200, 0..1000, 10))
    private val additionalDelay by setting(this, IntegerSetting(settingName("Additional Delay"), 100, 0..500, 10))
    private val rotations by setting(this, BooleanSetting(settingName("Rotations"), false))
    private val rayTrace by setting(this, BooleanSetting(settingName("Ray Trace"), true))
    private val flatten by setting(this, BooleanSetting(settingName("Flatten"), false))
    private val whitelistMode by setting(this, BooleanSetting(settingName("Whitelist Mode"), true))

    val blockList = setting(this, CollectionSetting(settingName("Block List"), linkedSetOf("minecraft:bed", "minecraft:tnt"), object : TypeToken<LinkedHashSet<String>>() {}.type))

    private val scanTimer = TickTimer(TimeUnit.MILLISECONDS)
    private val blockQueue = ArrayDeque<BlockPos>()
    private var nextMineTime = 0L
    private var blockSet = HashSet<Block>()

    init {
        onDisable {
            blockQueue.clear()
            nextMineTime = 0L
        }

        blockList.editListeners.add {
            val newSet = HashSet<Block>()
            it.forEach { name ->
                Block.getBlockFromName(name)?.let { block ->
                    if (block != Blocks.AIR) newSet.add(block)
                }
            }
            blockSet = newSet
        }

        safeListener<TickEvent.Post> {
            if (scanTimer.tickAndReset(scanDelay.toLong())) {
                scanBlocks(this)
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime >= nextMineTime && blockQueue.isNotEmpty()) {
                val pos = blockQueue.removeFirst()
                if (shouldMineBlock(this, pos)) {
                    val breakTime = calcBreakTime(this, pos)
                    nextMineTime = currentTime + breakTime + additionalDelay
                    mineBlock(this, pos)
                }
            }
        }
    }

    private fun scanBlocks(event: SafeClientEvent) {
        blockQueue.clear()
        val eyePos = EntityUtils.getEyePosition(event.player)
        val playerBlockPos = BlockPos(event.player)
        val blocksToMine = ArrayList<Pair<BlockPos, Double>>()
        val rangeInt = range.toInt() + 1

        for (x in -rangeInt..rangeInt) {
            for (y in -rangeInt..rangeInt) {
                for (z in -rangeInt..rangeInt) {
                    val pos = playerBlockPos.add(x, y, z)
                    val dist = getDistance(eyePos, pos)
                    if (dist <= range && shouldMineBlock(event, pos)) {
                        if (rayTrace && !canSeeBlock(event, eyePos, pos)) continue
                        if (flatten && pos.y < playerBlockPos.y) continue
                        blocksToMine.add(pos to dist)
                    }
                }
            }
        }

        blocksToMine.sortBy { it.second }
        blocksToMine.forEach { blockQueue.addLast(it.first) }
    }

    private fun shouldMineBlock(event: SafeClientEvent, pos: BlockPos): Boolean {
        val state = event.world.getBlockState(pos)
        val block = state.block
        if (block == Blocks.AIR) return false

        val blockName = block.registryName.toString()
        val inList = blockList.contains(blockName)
        return if (whitelistMode) inList else !inList
    }

    private fun canSeeBlock(event: SafeClientEvent, eyePos: Vec3d, pos: BlockPos): Boolean {
        val blockCenter = pos.toVec3d(0.5, 0.5, 0.5)
        val direction = blockCenter.subtract(eyePos)
        val distance = direction.lengthVector()
        val normalized = direction.normalize()
        var currentPos = eyePos
        val step = 0.5
        var travelled = 0.0
        while (travelled < distance) {
            val checkPos = BlockPos(currentPos)
            if (checkPos == pos) return true
            val state = event.world.getBlockState(checkPos)
            if (state.block != Blocks.AIR && checkPos != pos && state.isFullBlock) return false
            currentPos = currentPos.add(normalized.scale(step))
            travelled += step
        }
        return true
    }

    private fun getDistance(from: Vec3d, pos: BlockPos): Double {
        return from.distanceTo(pos.toVec3d(0.5, 0.5, 0.5))
    }

    private fun calcBreakTime(event: SafeClientEvent, pos: BlockPos): Int {
        val state = event.world.getBlockState(pos)
        val hardness = state.getBlockHardness(event.world, pos)
        if (hardness == 0.0f) return 0

        val breakSpeed = getBreakSpeed(event, state)
        val relativeDamage = breakSpeed / hardness / 30.0f
        val ticks = MathUtilKt.ceilToInt(0.7f / relativeDamage)
        return if (ticks <= 0) 0 else ticks * 50
    }

    private fun getBreakSpeed(event: SafeClientEvent, state: IBlockState): Float {
        var maxSpeed = 1.0f
        for (slot in event.player.allSlots) {
            val stack = slot.stack
            if (stack.isEmpty) continue
            val item = stack.item
            if (!isTool(item)) continue

            var speed = stack.getDestroySpeed(state)
            if (speed > 1.0f) {
                val efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack)
                if (efficiency > 0) speed += (efficiency * efficiency + 1).toFloat()
            }
            if (speed > maxSpeed) maxSpeed = speed
        }

        event.player.getActivePotionEffect(MobEffects.HASTE)?.let {
            maxSpeed *= 1.0f + (it.amplifier + 1) * 0.2f
        }
        return maxSpeed
    }

    private fun mineBlock(event: SafeClientEvent, pos: BlockPos) {
        if (rotations) {
            val blockCenter = pos.toVec3d(0.5, 0.5, 0.5)
            PlayerPacketManager.sendPlayerPacket {
                rotate(RotationUtils.getRotationTo(event, blockCenter))
            }
        }
        PacketMine.mineBlock(this, pos, getModulePriority(), true)
    }
}
