package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.combat.CrystalSetDeadEvent
import dev.wizard.meta.event.events.combat.CrystalSpawnEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.exploit.Burrow
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.setId
import dev.wizard.meta.util.accessor.setPacketAction
import dev.wizard.meta.util.combat.CrystalUtils
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.vector.ConversionKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.math.vector.Vec3f
import dev.wizard.meta.util.world.CheckKt
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import dev.wizard.meta.util.world.PlacementSearchOption
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object AutoCevBreaker : Module(
    "AutoCevBreaker",
    category = Category.COMBAT,
    description = "Troll module",
    modulePriority = 400
) {
    private val minHealth by setting(this, FloatSetting(settingName("Min Health"), 8.0f, 0.0f..20.0f, 0.5f))
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 500, 0..1000, 1))
    private val breakDelay by setting(this, IntegerSetting(settingName("Break Delay"), 100, 0..1000, 1))
    private val range by setting(this, FloatSetting(settingName("Range"), 5.0f, 0.0f..6.0f, 0.25f))

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val packetTimer = TickTimer()
    private var posInfo: Info? = null
    private var crystalID = -69420

    init {
        onEnable {
            PacketMine.enable()
        }

        onDisable {
            reset()
        }

        listener<Render3DEvent> {
            posInfo?.let {
                renderer.render(false)
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            posInfo?.let { info ->
                if (event.pos == info.pos) {
                    if (event.newState.block == Blocks.AIR && crystalID != -69420 && safeCheck()) {
                        breakCrystal(crystalID)
                    }
                }
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketSoundEffect && packet.category == SoundCategory.BLOCKS && packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                posInfo?.let { info ->
                    if (info.pos.distanceSq(packet.x, packet.y - 0.5, packet.z) < 0.25) {
                        crystalID = -69420
                    }
                }
            }
        }

        safeListener<CrystalSpawnEvent> {
            posInfo?.let { info ->
                if (it.crystalDamage.blockPos == info.pos || CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(info.pos, it.crystalDamage.crystalPos)) {
                    crystalID = it.entityID
                }
            }
        }

        safeListener<CrystalSetDeadEvent> { event ->
            posInfo?.let { info ->
                if (event.crystals.none { it.entityBoundingBox.intersects(info.placeBB) }) {
                    return@safeListener
                }
                crystalID = -69420
                place(info)
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!safeCheck()) {
                posInfo = null
                return@safeParallelListener
            }
            updateTarget(this)
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (!safeCheck()) return@safeListener
            posInfo?.let { info ->
                if (world.getBlockState(info.pos).block == Blocks.AIR) {
                    var id = crystalID
                    if (id == -69420) {
                        CombatManager.crystalList.asSequence().filter { !it.first.isDead }.firstOrNull { it.first.entityBoundingBox.intersects(info.placeBB) }?.let {
                            id = it.first.entityId
                        }
                    }
                    PacketMine.mineBlock(INSTANCE, info.pos, modulePriority)
                    if (id != -69420 && breakTimer.tick(breakDelay.toLong())) {
                        breakCrystal(id)
                    }
                    if (placeTimer.tick(placeDelay.toLong())) {
                        place(info)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.safeCheck(): Boolean = player.health >= minHealth

    private fun SafeClientEvent.updateTarget(event: SafeClientEvent) {
        val targetInfo = calcTarget()
        if (targetInfo == null) {
            reset()
        } else if (targetInfo != posInfo) {
            reset()
            renderer.clear()
            renderer.add(AxisAlignedBB(targetInfo.pos), ColorRGB(255, 255, 255))
            packetTimer.reset(-69420)
            PacketMine.mineBlock(INSTANCE, targetInfo.pos, modulePriority)
        }
        posInfo = targetInfo
    }

    private fun SafeClientEvent.calcTarget(): Info? {
        val target = CombatManager.target ?: return null
        if (Burrow.isBurrowed(target)) return null
        val pos = BlockPos(target.posX, target.posY + 2.5, target.posZ)
        if (player.getDistanceSqToCenter(pos) > range * range) return null
        if (!CheckKt.canBreakBlock(world, pos)) return null
        if (!CrystalUtils.hasValidSpaceForCrystal(this, pos)) return null
        if (!wallCheck(pos)) return null

        val side = InteractKt.getMiningSide(this, pos) ?: EnumFacing.UP
        return Info(pos, side)
    }

    private fun SafeClientEvent.wallCheck(pos: BlockPos): Boolean {
        val eyePos = EntityUtils.getEyePosition(player)
        return eyePos.distanceSq(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) <= 9.0 || world.rayTraceBlocks(eyePos, ConversionKt.toVec3d(pos, 0.5, 2.7, 0.5), false, true, false) == null
    }

    private fun SafeClientEvent.place(info: Info) {
        val obsidianSlot = IterableKt.firstBlock(DefinedKt.getAllSlotsPrioritized(player), Blocks.OBSIDIAN) ?: return
        val crystalSlot = IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(player), Items.END_CRYSTAL) ?: return

        val sideList = EnumFacing.HORIZONTALS.toMutableList().apply { add(EnumFacing.DOWN) }.toTypedArray()
        val placeInfo = InteractKt.getPlacement(this, info.pos, sideList, arrayOf(PlacementSearchOption.ENTITY_COLLISION, PlacementSearchOption.range(6.0f))) ?: return

        synchronized(InventoryTaskManager) {
            HotbarSwitchManager.ghostSwitch(this, obsidianSlot) {
                InteractKt.placeBlock(it, placeInfo)
            }
            HotbarSwitchManager.ghostSwitch(this, crystalSlot) {
                it.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(info.pos, info.side, EnumHand.MAIN_HAND, info.hitVecOffset.x, info.hitVecOffset.y, info.hitVecOffset.z))
                it.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            }
        }
        placeTimer.reset()
    }

    private fun SafeClientEvent.breakCrystal(id: Int) {
        val packet = CPacketUseEntity().apply {
            setId(id)
            setPacketAction(CPacketUseEntity.Action.ATTACK)
        }
        connection.sendPacket(packet)
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        breakTimer.reset()
    }

    private fun reset() {
        placeTimer.reset(-69420)
        breakTimer.reset(-69420)
        packetTimer.reset(-69420)
        posInfo = null
        crystalID = -69420
        PacketMine.reset(this)
    }

    private class Info(val pos: BlockPos, val side: EnumFacing) {
        val placeBB = AxisAlignedBB(pos.x - 1.0, pos.y + 0.0, pos.z - 1.0, pos.x + 2.0, pos.y + 3.0, pos.z + 2.0)
        val hitVecOffset: Vec3f = InteractKt.getHitVecOffset(side)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Info) return false
            return pos == other.pos
        }

        override fun hashCode(): Int = pos.hashCode()
    }
}
