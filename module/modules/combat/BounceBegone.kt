package dev.wizard.meta.module.modules.combat

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.movement.Step
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.world.CheckKt
import net.minecraft.block.BlockBed
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

object BounceBegone : Module(
    "BounceBegone",
    category = Category.COMBAT,
    description = "Improves beds",
    modulePriority = 200
) {
    private val range by setting(this, FloatSetting(settingName("Range"), 0.5f, 0.5f..2.0f, 0.5f))
    private val mineFeet by setting(this, BooleanSetting(settingName("Mine Feet"), true))
    private val mineAdjacent by setting(this, BooleanSetting(settingName("Mine Adjacent"), false))
    private val noBedStep by setting(this, BooleanSetting(settingName("No Bed Step"), true))
    private val antiBedStepAlways by setting(this, BooleanSetting(settingName("Always No Step"), false))
    private val noBounce by setting(this, BooleanSetting(settingName("No Bounce"), false))

    private var lastMinePos: BlockPos? = null

    init {
        onEnable { enable() }
        onDisable {
            lastMinePos = null
            PacketMine.reset(this)
            mc.player?.stepHeight = 0.6f
        }

        safeParallelListener<TickEvent.Post> {
            val playerPos = EntityUtils.getBetterPosition(player)
            val minePos = BlockPos.MutableBlockPos()

            if (noBedStep) {
                player.stepHeight = if (!antiBedStepAlways && checkBed(playerPos) && !Step.isEnabled) 0.1f else if (antiBedStepAlways) 0.1f else 0.6f
            }

            if (mineFeet) {
                minePos.setPos(playerPos)
                if (checkBed(minePos)) {
                    mineBed(minePos.toImmutable())
                    return@safeParallelListener
                }
            }

            if (mineAdjacent) {
                for (facing in EnumFacing.VALUES) {
                    if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) continue
                    if (checkBed(VectorUtils.setAndAdd(minePos, playerPos, facing))) {
                        mineBed(minePos.toImmutable())
                        return@safeParallelListener
                    }

                    val r = range.toInt()
                    for (x in -r..r) {
                        for (y in -1..1) {
                            for (z in -r..r) {
                                if (x == 0 && y == 0 && z == 0) continue
                                if (checkBed(VectorUtils.setAndAdd(minePos, playerPos, x, y, z))) {
                                    mineBed(minePos.toImmutable())
                                    return@safeParallelListener
                                }
                            }
                        }
                    }
                }
            }

            lastMinePos?.let {
                if (world.getBlockState(it).block is BlockBed && CheckKt.canBreakBlock(world, it)) {
                    PacketMine.mineBlock(this@BounceBegone, it, 1000, true)
                } else {
                    lastMinePos = null
                }
            }
        }
    }

    override fun isActive(): Boolean = isEnabled && lastMinePos != null

    private fun SafeClientEvent.checkBed(pos: BlockPos): Boolean {
        return CheckKt.canBreakBlock(world, pos) && world.getBlockState(pos).block is BlockBed
    }

    private fun mineBed(pos: BlockPos) {
        PacketMine.mineBlock(this, pos, modulePriority)
        lastMinePos = pos
    }

    @JvmStatic
    fun stopBounce(): Boolean = isEnabled && noBounce
}
