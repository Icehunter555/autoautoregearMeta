package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.firstItem
import dev.wizard.meta.util.math.scale
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.block.BlockFire
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemFlintAndSteel
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

object Igniter : Module(
    "Igniter",
    category = Category.COMBAT,
    description = "chezburger fire experiment"
) {
    private val mode by setting("Mode", Mode.PLAYERS)
    private val bypassMode by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.NONE)
    private val autoDisable by setting("Auto Disable", false)
    private val igniteDelay by setting("Ignite Delay", 100, 0..1000, 1)
    private val multiIgnite by setting("Multi-Ignite", 2, 1..12, 1)
    private val singleTarget by setting("Single Target", false, { mode != Mode.TNT })
    private val friends by setting("Target Friends", false, { mode != Mode.TNT })
    private val targetRange by setting("Range", 5.0f, 1.0f..6.0f, 1.0f, { mode != Mode.TNT })
    private val targetOnGround by setting("Target On Ground", false, { mode != Mode.TNT })
    private val render by setting("Render", true)
    private val renderColor by setting("Render Color", ColorRGB(255, 80, 50), { render })
    private val renderFade by setting("Render Fade", true, { render })
    private val renderTime by setting("Render Time", 2000, 500..5000, 100, { render })
    private val animate by setting("Animate", false, { render })
    private val fadeLength by setting("Fade Length", 200, 0..1000, 50, { animate && render })

    private val igniteTimer = TickTimer()
    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val ignitedBlocks = LinkedHashMap<BlockPos, Long>()

    override val hudInfo: String
        get() = mode.displayString

    init {
        onDisable {
            igniteTimer.reset(-114514L)
            ignitedBlocks.clear()
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            if (igniteTimer.tick(igniteDelay.toLong())) {
                runAutoIgnite()
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                val now = System.currentTimeMillis()
                val iterator = ignitedBlocks.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val pos = entry.key
                    val time = entry.value
                    val age = now - time
                    val life = renderTime.toLong()

                    if (age > life) {
                        iterator.remove()
                        continue
                    }

                    var alpha = 255
                    var box = AxisAlignedBB(
                        pos.x.toDouble(),
                        pos.y.toDouble(),
                        pos.z.toDouble(),
                        pos.x.toDouble() + 1.0,
                        pos.y.toDouble() + 0.4,
                        pos.z.toDouble() + 1.0
                    )

                    if (animate) {
                        var inc = 1.0f
                        if (age < fadeLength) {
                            inc = Easing.OUT_CUBIC.inc(age.toFloat() / fadeLength)
                        }
                        var dec = 1.0f
                        if (life - age < fadeLength) {
                            dec = Easing.IN_CUBIC.dec((life - age).toFloat() / fadeLength)
                        }
                        val multiplier = inc * dec
                        alpha = (255 * multiplier).toInt()
                        
                        val centerX = pos.x + 0.5
                        val centerY = pos.y + 0.2
                        val centerZ = pos.z + 0.5
                        
                        box = box.offset(-centerX, -centerY, -centerZ).scale(multiplier.toDouble()).offset(centerX, centerY, centerZ)
                    } else if (renderFade) {
                        val timeLeft = life - age
                        alpha = ((timeLeft.toFloat() / life * 255).toInt()).coerceIn(0, 255)
                    }

                    renderer.aFilled = (31 * alpha) / 255
                    renderer.aOutline = (233 * alpha) / 255
                    renderer.add(box, renderColor.alpha(alpha))
                }
                renderer.render(true)
            }
        }
    }

    private fun SafeClientEvent.runAutoIgnite() {
        var ignitedCount = 0

        if (mode == Mode.PLAYERS || mode == Mode.BOTH) {
            val targets = getTargets().entries.sortedBy { it.value }.map { it.key }
            if (targets.isNotEmpty()) {
                val target = if (singleTarget) targets.first() else null
                for (player in targets) {
                    if (target != null && player != target) continue
                    if (ignitedCount >= multiIgnite) break

                    val pos = BlockPos(player.posX, player.posY, player.posZ)
                    if (!world.getBlockState(pos).material.isReplaceable || pos.y >= 256) continue

                    ignite(pos)
                    ignitedBlocks[pos] = System.currentTimeMillis()
                    ignitedCount++
                }
            }
        }

        if (mode == Mode.TNT || mode == Mode.BOTH) {
            val nearbyTNT = getNearbyTNT(6)
            for (pos in nearbyTNT) {
                if (ignitedCount >= multiIgnite) break
                ignite(pos.up())
                ignitedBlocks[pos] = System.currentTimeMillis()
                ignitedCount++
            }
        }

        if (autoDisable && ignitedCount == 0) {
            disable()
        }
    }

    private fun SafeClientEvent.getTargets(): Map<EntityPlayer, Double> {
        val targetMap = LinkedHashMap<EntityPlayer, Double>()
        for (target in world.playerEntities) {
            if (target.isDead || target.health <= 0) continue
            if (EntityUtils.isSelf(target)) continue
            if (!friends && EntityUtils.isFriend(target)) continue
            if (targetOnGround && !target.onGround) continue
            if (!world.getBlockState(BlockPos(target).down()).isFullBlock) continue

            val dist = player.getDistance(target).toDouble()
            if (dist > targetRange) continue

            targetMap[target] = dist
        }
        return targetMap
    }

    private fun SafeClientEvent.getNearbyTNT(range: Int): List<BlockPos> {
        val list = ArrayList<BlockPos>()
        val playerPos = BlockPos(player)
        
        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val pos = playerPos.add(x, y, z)
                    if (world.getBlockState(pos).block == Blocks.TNT) {
                        list.add(pos)
                    }
                }
            }
        }
        return list
    }

    private fun SafeClientEvent.ignite(pos: BlockPos) {
        val flintSlot = player.inventory.firstItem(Items.FLINT_AND_STEEL)
        
        if (flintSlot == null) {
            NoSpamMessage.sendError("$chatName No flint and steel found in inventory!")
            if (autoDisable) disable()
            return
        }

        if (world.getBlockState(pos).block is BlockFire) return

        HotbarSwitchManager.ghostSwitch(this, bypassMode, flintSlot) {
            connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos.down(), EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f))
        }
        player.swingArm(EnumHand.MAIN_HAND)
    }

    private enum class Mode(override val displayName: String) : DisplayEnum {
        PLAYERS("Players"),
        TNT("TNT"),
        BOTH("Both")
    }
}
