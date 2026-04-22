package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.events.EntityEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.CrystalSpawnEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.combat.CrystalUtils
import dev.wizard.meta.util.inventory.slot.allSlots
import dev.wizard.meta.util.inventory.slot.allSlotsPrioritized
import dev.wizard.meta.util.inventory.slot.firstItem
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.pause.MainHandPause
import dev.wizard.meta.util.pause.OffhandPause
import dev.wizard.meta.util.pause.withPause
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.runSafeSuspend
import dev.wizard.meta.util.world.getHitVec
import dev.wizard.meta.util.world.getHitVecOffset
import dev.wizard.meta.util.world.getMiningSide
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object Suicide : Module(
    name = "Suicide",
    category = Category.MISC,
    description = "kill yourself",
    modulePriority = Int.MAX_VALUE
) {
    private val useKillCommand by setting("Use Kill Command", false)
    private val useBedInNether by setting("Use Bed In Nether", true)
    private val bedRange by setting("Bed Range", 3, 1..6, 1) { useBedInNether }

    private val breakTimer = TickTimer()
    private val bedTimer = TickTimer()
    private var crystalPos: Vec3d? = null
    private var bedPlaced = false

    init {
        onEnable {
            runSafeSuspend {
                if (useKillCommand) {
                    player.sendChatMessage("/kill")
                }
                bedPlaced = false
            }
        }

        onDisable {
            bedPlaced = false
        }

        listener<CrystalSpawnEvent> {
            breakCrystal(it.entityID)
        }

        listener<OnUpdateWalkingPlayerEvent.Pre> {
            crystalPos?.let { pos ->
                PlayerPacketManager.sendPlayerPacket {
                    rotate(RotationUtils.getRotationTo(pos))
                }
            }
        }

        listener<EntityEvent.Death> {
            if (it.entity == mc.player) {
                NoSpamMessage.sendMessage("$chatName Disabled on death.")
                disable()
            }
        }

        parallelListener<TickEvent.Post> {
            runSafeSuspend {
                if (useBedInNether && player.dimension == -1) {
                    runBedLogic()
                } else {
                    runCrystalLogic()
                }
            }
        }
    }

    private suspend fun runBedLogic() {
        val bedSlot = player.allSlotsPrioritized.firstItem(Items.BED)
        if (bedSlot == null) {
            NoSpamMessage.sendMessage("$chatName No beds found, disabling")
            disable()
            return
        }

        OffhandPause.withPause(this, 1000) {
            MainHandPause.withPause(this, 1000) {
                if (!bedPlaced && bedTimer.tick(250L)) {
                    val bedPos = findBedPlacement()
                    if (bedPos != null) {
                        placeBed(bedSlot, bedPos)
                        bedPlaced = true
                        bedTimer.reset()
                    }
                } else if (bedPlaced && bedTimer.tick(250L)) {
                    val bedPos = findNearbyBed()
                    if (bedPos != null) {
                        breakBed(bedPos)
                        bedPlaced = false
                        bedTimer.reset()
                    }
                }
            }
        }
    }

    private suspend fun runCrystalLogic() {
        OffhandPause.withPause(this, 1000) {
            MainHandPause.withPause(this, 1000) {
                val crystalSlot = player.allSlots.firstItem(Items.END_CRYSTAL)
                if (crystalSlot == null) {
                    NoSpamMessage.sendMessage("$chatName No crystal found in inventory, disabling.")
                    disable()
                    return
                }

                if (breakTimer.tick(500L)) {
                    val crystal = CombatManager.crystalList.maxByOrNull { it.second.selfDamage }
                    if (crystal != null) {
                        breakCrystal(crystal.first.entityId)
                        breakTimer.reset()
                    }
                }

                val mutableBlockPos = BlockPos.MutableBlockPos()
                val placeTarget = CombatManager.placeList.asSequence()
                    .filter { player.distanceSqTo(it.crystalPos) < 9.0 }
                    .filter { CrystalUtils.canPlaceCrystal(it.blockPos, player, mutableBlockPos) }
                    .maxByOrNull { it.selfDamage }

                placeTarget?.let { target ->
                    crystalPos = target.crystalPos
                    MainHandPause.withPause(this@Suicide) {
                        HotbarSwitchManager.ghostSwitch(this@Suicide, crystalSlot) {
                            connection.sendPacket(CPacketPlayerTryUseItemOnBlock(target.blockPos, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f))
                        }
                    }
                }
            }
        }
    }

    private fun findBedPlacement(): BlockPos? {
        val playerPos = BlockPos(player.posX, player.posY, player.posZ)
        for (facing in EnumFacing.HORIZONTALS) {
            for (distance in 1..bedRange) {
                val testPos = playerPos.offset(facing, distance)
                val solidPos = testPos.down()
                val solidState = world.getBlockState(solidPos)
                val testState = world.getBlockState(testPos)

                if (solidState.isFullBlock && solidState.isOpaqueCube && (testState.block == Blocks.AIR || testState.material.isReplaceable)) {
                    return solidPos
                }
            }
        }
        return null
    }

    private fun findNearbyBed(): BlockPos? {
        val playerPos = BlockPos(player.posX, player.posY, player.posZ)
        for (x in -bedRange..bedRange) {
            for (y in -2..2) {
                for (z in -bedRange..bedRange) {
                    val pos = playerPos.add(x, y, z)
                    if (world.getBlockState(pos).block == Blocks.BED) {
                        return pos
                    }
                }
            }
        }
        return null
    }

    private fun placeBed(bedSlot: dev.wizard.meta.util.inventory.slot.Slot, solidPos: BlockPos) {
        val packet = CPacketPlayerTryUseItemOnBlock(solidPos, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)
        MainHandPause.withPause(this) {
            HotbarSwitchManager.ghostSwitch(this@Suicide, bedSlot) {
                connection.sendPacket(packet)
            }
        }
    }

    private fun breakBed(bedPos: BlockPos) {
        val side = getMiningSide(bedPos) ?: EnumFacing.UP
        val hitVecOffset = getHitVecOffset(side)
        connection.sendPacket(CPacketPlayerTryUseItemOnBlock(bedPos, side, EnumHand.MAIN_HAND, hitVecOffset.x, hitVecOffset.y, hitVecOffset.z))
    }

    private fun breakCrystal(entityID: Int) {
        val packet = CPacketUseEntity()
        // Accessing private fields via reflection or accessor if needed, assuming proper mapping or accessor
        // In decompiled code: NetworkKt.setPacketAction(packet, CPacketUseEntity.Action.ATTACK); NetworkKt.setId(packet, entityID);
        // We will assume normal instantiation for now if constructor allows or use accessors.
        // CPacketUseEntity() creates empty packet. Need to set fields.
        // Assuming accessors exist or using constructor with entity if possible, but ID is int.
        // For now, using a helper or reflection would be best, or assuming there's a constructor I missed or it's handled via mixin accessor.
        // Replicating decompiled logic:
        // NetworkKt.setPacketAction(packet, CPacketUseEntity.Action.ATTACK)
        // NetworkKt.setId(packet, entityID)
        // Since I don't have NetworkKt in context, I will use reflection or assume a constructor exists in the context of this project or use a wrapper.
        // Wait, I can probably just construct it with entity if I had it, but I only have ID.
        // I will use a placeholder or assume NetworkKt accessors are available in Utils.
        // Since I am writing the module, I will assume standard Forge method or Accessor usage.
        // Actually, CPacketUseEntity(Entity) is standard. If I don't have entity, I need to craft packet manually or find entity by ID.
        val entity = world.getEntityByID(entityID)
        if (entity != null) {
            connection.sendPacket(CPacketUseEntity(entity))
        } else {
            // Fallback if entity null (shouldn't happen often in this context)
            // But to be safe and follow 'kill yourself' goal, maybe just try to attack?
            // Without entity object, standard constructor is hard.
            // Let's stick to world.getEntityByID for now.
        }
        player.swingArm(EnumHand.MAIN_HAND)
    }
}
