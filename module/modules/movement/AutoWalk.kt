package dev.wizard.meta.module.modules.movement

import baritone.api.pathing.goals.GoalXZ
import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.baritone.BaritoneCommandEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.render.LagNotifier
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.Direction
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.util.MovementInputFromOptions

object AutoWalk : Module(
    name = "AutoWalk",
    category = Category.MOVEMENT,
    description = "Automatically walks somewhere",
    priority = 1010
) {
    private val mode by setting("Direction", AutoWalkMode.BARITONE)
    private val disableOnDisconnect by setting("Disable On Disconnect", true)
    
    private const val border = 30000000
    private val messageTimer = TickTimer(TimeUnit.SECONDS)
    var direction = Direction.NORTH
        private set

    val baritoneWalk: Boolean
        get() = isEnabled && mode == AutoWalkMode.BARITONE

    override fun isActive(): Boolean {
        return isEnabled && (mode != AutoWalkMode.BARITONE || BaritoneUtils.isActive || BaritoneUtils.isPathing())
    }

    override fun getHudInfo(): String {
        return if (mode == AutoWalkMode.BARITONE && (BaritoneUtils.isActive || BaritoneUtils.isPathing())) {
            direction.getDisplayString()
        } else {
            mode.displayName.toString()
        }
    }

    private fun SafeClientEvent.startPathing() {
        if (!world.isBlockLoaded(player.chunkCoordX, player.chunkCoordZ)) return
        
        direction = Direction.fromEntity(player)
        val x = MathUtilKt.floorToInt(player.posX) + direction.directionVec.x * border
        val z = MathUtilKt.floorToInt(player.posZ) + direction.directionVec.z * border
        
        BaritoneUtils.cancelEverything()
        BaritoneUtils.primary?.customGoalProcess?.setGoalAndPath(GoalXZ(x, z))
    }

    private fun checkBaritoneElytra(): Boolean {
        val player = mc.player ?: return true
        if (player.isElytraFlying) {
            if (messageTimer.tickAndReset(10L)) {
                NoSpamMessage.sendError("${getChatName()} Baritone mode isn't currently compatible with Elytra flying! Choose a different mode if you want to use AutoWalk while Elytra flying")
            }
            return true
        }
        return false
    }

    init {
        onDisable {
            if (mode == AutoWalkMode.BARITONE) {
                BaritoneUtils.cancelEverything()
            }
        }

        listener<BaritoneCommandEvent> {
            if (it.command.contains("cancel", ignoreCase = true)) {
                disable()
            }
        }

        listener<ConnectionEvent.Disconnect> {
            if (disableOnDisconnect) {
                disable()
            }
        }

        listener<InputUpdateEvent>(6969) { event ->
            if (LagNotifier.paused && LagNotifier.pauseAutoWalk) return@listener
            if (event.movementInput !is MovementInputFromOptions) return@listener

            when (mode) {
                AutoWalkMode.FORWARD -> event.movementInput.moveForward = 1.0f
                AutoWalkMode.BACKWARD -> event.movementInput.moveForward = -1.0f
                else -> {}
            }
        }

        listener<TickEvent.Pre> {
            if (mode == AutoWalkMode.BARITONE && !checkBaritoneElytra() && !isActive()) {
                startPathing()
            }
        }

        mode.getListeners().add {
            if (isDisabled || mc.player == null) return@add
            if (mode == AutoWalkMode.BARITONE) {
                if (!checkBaritoneElytra()) {
                    SafeClientEvent.instance?.let { startPathing() }
                }
            } else {
                BaritoneUtils.cancelEverything()
            }
        }
    }

    private enum class AutoWalkMode(override val displayName: CharSequence) : DisplayEnum {
        FORWARD("Forward"),
        BACKWARD("Backward"),
        BARITONE("Baritone")
    }
}
