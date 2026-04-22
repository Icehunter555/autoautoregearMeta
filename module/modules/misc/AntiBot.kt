package dev.wizard.meta.module.modules.misc

import com.google.common.collect.MapMaker
import dev.wizard.meta.event.ListenerKt.concurrentListener
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.player.PlayerAttackEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.vector.Vec2d
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.abs

object AntiBot : Module(
    name = "AntiBot",
    category = Category.MISC,
    description = "Avoid attacking fake players"
) {
    private val tabList by setting("Tab List", true)
    private val ping by setting("Ping", true)
    private val hp by setting("HP", true)
    private val sleeping by setting("Sleeping", false)
    private val hoverOnTop by setting("Hover On Top", true)
    private val ticksExists by setting("Ticks Exists", 200, 0..500, 10)
    private val mojangApi by setting("Mojang API", true)

    private val botMap = MapMaker().weakKeys().makeMap<EntityPlayer, Boolean>()
    private var count = 0

    override fun getHudInfo(): String {
        return count.toString()
    }

    init {
        onDisable {
            botMap.clear()
            count = 0
        }

        listener<ConnectionEvent.Disconnect> {
            botMap.clear()
        }

        listener<PlayerAttackEvent> {
            if (isBot(it.entity)) {
                it.cancel()
            }
        }

        listener<WorldEvent.Entity.Add> {
            if (it.entity is EntityPlayer) {
                // We can't safely check bot status immediately on add for all checks, but some might work.
                // However, the concurrent listener updates the map frequently.
                // But for now, let's just put it in map if we can check it safely.
                // The decompiled code calls checkBot here.
                // Note: WorldEvent.Entity.Add might happen on netty thread?
                // runSafe is needed if checkBot accesses mc.player
                // But this listener is likely on main thread?
                // SafeClientEvent usage in decompiled code suggests it expects to be safe.
                // We will assume it's safe or use runSafe if needed, but listener usually implies main thread or event bus thread.
                // Decompiled: ListenerKt.listener(..., WorldEvent.Entity.Add.class, ...)
                // This is a normal listener, likely main thread.
                botMap[it.entity] = checkBot(it.entity)
            }
        }

        concurrentListener<TickEvent.Pre> {
            runSafeSuspend {
                var newCount = 0
                for (entity in EntityManager.players) {
                    val isBot = checkBot(entity)
                    botMap[entity] = isBot
                    if (isBot) newCount++
                }
                count = newCount
            }
        }
    }

    fun isBot(entity: Entity): Boolean {
        if (!isEnabled) return false
        if (entity !is EntityPlayer) return false
        if (EntityUtils.isFakeOrSelf(entity)) return false
        return botMap[entity] ?: true
    }

    private fun checkBot(entity: EntityPlayer): Boolean {
        if (entity.name == mc.player.name) return true

        if (tabList) {
            if (mc.connection?.getPlayerInfo(entity.name) == null) return true
        }

        if (ping) {
            val networkPlayerInfo = mc.connection?.getPlayerInfo(entity.name) ?: return true
            if (networkPlayerInfo.responseTime <= 0) return true
        }

        if (hp) {
             if (entity.health !in 0.0f..20.0f) return true
        }

        if (sleeping && entity.isPlayerSleeping) {
             if (!entity.onGround) return true
        }

        if (hoverOnTop) {
            if (hoverCheck(entity)) return true
        }

        if (entity.ticksExisted < ticksExists) return true

        if (mojangApi) {
            if (UUIDManager.getByName(entity.name, true) != null) return false
            return true
        }

        return false
    }

    private fun hoverCheck(entity: EntityPlayer): Boolean {
        val distXZ = Vec2d(entity.posX, entity.posZ).minus(mc.player.posX, mc.player.posZ).lengthSq()
        return distXZ < 16.0 && entity.posY - mc.player.posY > 2.0 && abs(entity.posY - entity.prevPosY) < 0.1
    }
}
