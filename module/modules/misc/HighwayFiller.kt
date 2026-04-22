package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.util.inventory.slot.countBlock
import dev.wizard.meta.util.inventory.slot.firstBlock
import dev.wizard.meta.util.inventory.slot.hotbarSlots
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.concurrentScope
import dev.wizard.meta.util.threads.isActiveOrFalse
import dev.wizard.meta.util.threads.runSafeSuspend
import dev.wizard.meta.util.world.PlacementSearchOption
import dev.wizard.meta.util.world.getPlacement
import dev.wizard.meta.util.world.isPlaceable
import dev.wizard.meta.util.world.placeBlock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

object HighwayFiller : Module(
    name = "HighwayFiller",
    category = Category.MISC,
    description = "NOOOO AAAA NOO"
) {
    private val bypassMode by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.NONE)
    private val placeDelay by setting("Delay", 100, 0..500, 10)
    private val wallHeight by setting("Height", 5, 1..5, 1)
    private val wallWidth by setting("Width", 5, 1..6, 1)
    private val render by setting("Render", true)
    private val renderColor by setting("Render Color", ColorRGB(255, 0, 0), false) { render }
    private val renderFade by setting("Render Fade", true) { render }
    private val renderTime by setting("Render Time", 2000, 500..5000, 100) { render }

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = LinkedHashMap<BlockPos, Long>()
    private var job: Job? = null
    private var initialYaw = 0.0f

    override val isActive: Boolean
        get() = isEnabled && isActiveOrFalse(job)

    override fun getHudInfo(): String {
        return placedBlocks.entries.count { System.currentTimeMillis() - it.value <= 10000L }.toString()
    }

    init {
        onEnable {
            if (mc.player != null) {
                initialYaw = mc.player.rotationYaw
            } else {
                disable()
            }
        }

        onDisable {
            placedBlocks.clear()
        }

        listener<TickEvent.Post> {
            if (!isActiveOrFalse(job)) {
                job = runClogger()
            }

            if (isActiveOrFalse(job) && AntiCheat.blockPlaceRotation) {
                PlayerPacketManager.sendPlayerPacket {
                    cancelAll()
                }
            }
        }

        listener<Render3DEvent> {
            if (render) {
                renderer.aFilled = if (renderFade) 15 else 31
                renderer.aOutline = if (renderFade) 100 else 233
                
                for (pos in placedBlocks.keys) {
                    val box = AxisAlignedBB(pos)
                    if (renderFade) {
                        val timeLeft = renderTime - (System.currentTimeMillis() - placedBlocks[pos]!!)
                        val alpha = (timeLeft.toFloat() / renderTime.toFloat() * 255).toInt().coerceIn(0, 255)
                        renderer.add(box, renderColor.alpha(alpha))
                    } else {
                        renderer.add(box, renderColor)
                    }
                }
                renderer.render(true)
            }
        }
    }

    private fun runClogger(): Job {
        return concurrentScope.launch {
            runSafeSuspend {
                val rad = Math.toRadians(initialYaw.toDouble())
                val forwardX = -sin(rad)
                val forwardZ = cos(rad)
                val right = Vec3d(-forwardZ, 0.0, forwardX)
                val centerX = player.posX + forwardX * 2
                val centerZ = player.posZ + forwardZ * 2
                val horizontalOffset = (wallWidth - 1) / 2.0
                val baseX = centerX - right.x * horizontalOffset
                val baseZ = centerZ - right.z * horizontalOffset
                val baseY = player.posY.toInt() - 1

                var placedBlock = false
                var j = 0
                while (j < wallHeight && !placedBlock) {
                    var i = 0
                    while (i < wallWidth && !placedBlock) {
                        val posX = baseX + right.x * i
                        val posZ = baseZ + right.z * i
                        val targetPos = BlockPos(posX, (baseY + j).toDouble(), posZ)

                        if (world.isPlaceable(targetPos) && getPlacement(targetPos, 3.0, PlacementSearchOption.range(4.25f), PlacementSearchOption.ENTITY_COLLISION) != null) {
                            placeBlock(targetPos)
                            placedBlock = true
                            placedBlocks[targetPos] = System.currentTimeMillis()
                            delay(placeDelay.toLong())
                        }
                        i++
                    }
                    j++
                }
            }
        }
    }

    private suspend fun placeBlock(pos: BlockPos) {
        runSafeSuspend {
            val obsidianSlot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
            val hasObsidian = player.hotbarSlots.countBlock(Blocks.OBSIDIAN) > 0

            if (!hasObsidian || obsidianSlot == null) {
                NoSpamMessage.sendMessage("$chatName No obsidian in hotbar, disabling!")
                disable()
                return@runSafeSuspend
            }

            val placeInfo = getPlacement(pos, 3.0, PlacementSearchOption.range(4.25f), PlacementSearchOption.ENTITY_COLLISION) ?: return@runSafeSuspend
            val placePacket = placeInfo.toPlacePacket(EnumHand.MAIN_HAND)

            if (!player.isSneaking) {
                player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
                
                val heldItem = player.heldItemMainhand
                if (heldItem.item !is ItemBlock || (heldItem.item as ItemBlock).block != Blocks.OBSIDIAN) {
                    HotbarSwitchManager.ghostSwitch(bypassMode, obsidianSlot) {
                        player.connection.sendPacket(placePacket)
                    }
                } else {
                    player.connection.sendPacket(placePacket)
                }
                player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
            } else {
                val heldItem = player.heldItemMainhand
                if (heldItem.item !is ItemBlock || (heldItem.item as ItemBlock).block != Blocks.OBSIDIAN) {
                    HotbarSwitchManager.ghostSwitch(bypassMode, obsidianSlot) {
                        player.connection.sendPacket(placePacket)
                    }
                } else {
                    player.connection.sendPacket(placePacket)
                }
            }
            player.swingArm(EnumHand.MAIN_HAND)
        }
    }
}
