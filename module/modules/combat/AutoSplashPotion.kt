package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.manager.managers.MetaManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.accessor.setPitch
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.TaskKt
import dev.wizard.meta.util.inventory.ItemKt
import dev.wizard.meta.util.inventory.operation.swapWith
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.inventory.slot.MiscKt
import dev.wizard.meta.util.inventory.slot.isHotbarSlot
import dev.wizard.meta.util.math.vector.toVec2f
import dev.wizard.meta.util.threads.ThreadSafetyKt
import dev.wizard.meta.util.world.BlockKt
import dev.wizard.meta.util.world.CheckKt
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.potion.Potion
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.abs

object AutoSplashPotion : Module(
    "AutoSplashPotion",
    category = Category.COMBAT,
    description = "autopot on crack",
    modulePriority = 120
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.HEALTH))
    private val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.NONE, { page == Page.HEALTH || page == Page.SPEED }))

    val heal by setting(this, BooleanSetting(settingName("Heal"), true, { page == Page.HEALTH }))
    private val keepHealInHotbar by setting(this, BooleanSetting(settingName("Keep Heal Potion In Hotbar"), true, { page == Page.HEALTH && heal }))
    private val healHotbar by setting(this, IntegerSetting(settingName("Heal Potion Hotbar"), 7, 1..9, 1, { page == Page.HEALTH && heal }))
    var healHealth by setting(this, FloatSetting(settingName("Heal Health"), 12.0f, 0.0f..20.0f, 0.5f, { page == Page.HEALTH && heal }))
    var healDelay by setting(this, IntegerSetting(settingName("Heal Delay"), 500, 0..10000, 50, { page == Page.HEALTH && heal }))
    private val checkGround by setting(this, BooleanSetting(settingName("Ground Check"), false, { page == Page.HEALTH && heal }))
    private val groundRange by setting(this, IntegerSetting(settingName("Ground Range"), 3, 1..6, 1, { page == Page.HEALTH && checkGround && heal }))
    private val forceHealthBind by setting(this, BindSetting(settingName("Force Health Bind"), Bind(), null, { page == Page.HEALTH && heal }))

    val speed by setting(this, BooleanSetting(settingName("Speed"), true, { page == Page.SPEED }))
    private val speedDelay by setting(this, IntegerSetting(settingName("Speed Delay"), 5000, 0..10000, 50, { page == Page.SPEED && speed }))
    private val speedHealth by setting(this, FloatSetting(settingName("Speed Health"), 4.0f, 0.0f..20.0f, 0.5f, { page == Page.SPEED && speed }))
    private val speedCheckBelow by setting(this, BooleanSetting(settingName("Check Below"), true, { page == Page.SPEED && speed }))
    private val speedCheckGround by setting(this, BooleanSetting(settingName("Speed Ground Check"), false, { page == Page.SPEED && speed }))
    private val forceSpeedBind by setting(this, BindSetting(settingName("Force Speed Bind"), Bind(), null, { page == Page.SPEED && speed }))

    var doubleHealth by setting(this, BooleanSetting(settingName("Double Healing"), false, { page == Page.DEV && heal }))
    var adaptiveHealing by setting(this, BooleanSetting(settingName("Adaptive Healing"), false, { page == Page.CALCS }))
    var adaptiveHealth by setting(this, FloatSetting(settingName("Adaptive Health"), 5.5f, 0.5f..20.0f, 0.5f, { page == Page.CALCS && adaptiveHealing }))
    var instantThrow by setting(this, EnumSetting(settingName("Instant Healing Mode"), InstantMode.NONE, { page == Page.CALCS }))
    var instantResetHP by setting(this, FloatSetting(settingName("Instant Reset HP"), 15.0f, 0.0f..20.0f, 0.5f, { page == Page.CALCS && instantThrow == InstantMode.RESET_HP }))
    var instantResetDelay by setting(this, IntegerSetting(settingName("Instant Reset Delay"), 1000, 0..10000, 50, { page == Page.CALCS && instantThrow == InstantMode.RESET_DELAY }))
    var instantAmount by setting(this, IntegerSetting(settingName("Instant Amount"), 3, 1..5, 1, { page == Page.CALCS && instantThrow != InstantMode.NONE }))
    var predictiveHealing by setting(this, BooleanSetting(settingName("Predictive Healing"), false, { page == Page.CALCS && heal }))
    var predictionTicks by setting(this, IntegerSetting(settingName("Prediction Ticks"), 3, 1..10, 1, { page == Page.CALCS && heal && predictiveHealing }))
    var predictionSensitivity by setting(this, FloatSetting(settingName("Prediction Decay"), 0.8f, 0.0f..2.0f, 0.1f, { page == Page.CALCS && heal && predictiveHealing }))

    var upThrow by setting(this, BooleanSetting(settingName("Upwards Throw"), false, { page == Page.DEV && heal }))
    var upThrowTimeout by setting(this, IntegerSetting(settingName("Upwards Throw Timeout"), 1000, 0..10000, 50, { page == Page.DEV && upThrow }))
    private val hudDisplay by setting(this, EnumSetting(settingName("Hud Display"), HudDisplay.POTIONS, { page == Page.DEV }))
    var rotationDegrees by setting(this, FloatSetting(settingName("Rotation Degrees"), 90.0f, 70.0f..90.0f, 1.0f, { page == Page.DEV && (speed || heal) }))
    var updateController by setting(this, BooleanSetting(settingName("Update Controller"), false, { page == Page.DEV }))
    var rotateOnPacket by setting(this, BooleanSetting(settingName("Rotate On Packet"), false, { page == Page.DEV && heal }))
    var resetRotation by setting(this, BooleanSetting(settingName("Reset Rotation"), false, { page == Page.DEV }))

    private var currentPotion = PotionType.NONE
    private var lastUsedHand = EnumHand.MAIN_HAND
    private var lastTask: InventoryTask? = null
    private var shouldThrowAgain = false
    private var instantThrowUsed = false
    private var lastInstantThrowResetTime = 0L
    private var upThrowStartTime = 0L
    private var lastHealth = 20.0f
    private var damageTrend = 0.0f
    private var forceHealth = false
    private var forceSpeed = false

    init {
        onDisable {
            currentPotion = PotionType.NONE
            lastTask = null
            shouldThrowAgain = false
            instantThrowUsed = false
            lastInstantThrowResetTime = 0L
            forceHealth = false
            forceSpeed = false
        }

        safeListener<InputEvent.Keyboard> {
            if (!it.state) return@safeListener
            if (heal && forceHealthBind.isDown(it.key)) forceHealth = true
            if (speed && forceSpeedBind.isDown(it.key)) forceSpeed = true
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (checkGround && !groundCheck(this)) {
                currentPotion = PotionType.NONE
                return@safeListener
            }
            if (currentPotion == PotionType.NONE) {
                if (forceHealth && heal && hasPotion(DefinedKt.getAllSlots(player), PotionType.INSTANT_HEALTH)) {
                    currentPotion = PotionType.INSTANT_HEALTH
                    forceHealth = false
                } else if (forceSpeed && speed && hasPotion(DefinedKt.getAllSlots(player), PotionType.SPEED)) {
                    currentPotion = PotionType.SPEED
                    forceSpeed = false
                } else {
                    currentPotion = PotionType.VALUES.firstOrNull { it.check(this) } ?: PotionType.NONE
                }
            }

            if (currentPotion == PotionType.SPEED) {
                PlayerPacketManager.sendPlayerPacket {
                    rotate(player.rotationYaw.toVec2f(90.0f))
                }
            }
            if (currentPotion == PotionType.INSTANT_HEALTH) {
                val rotPitch = if (upThrow && hasBlockAboveHead(this)) {
                    if (upThrowStartTime == 0L) upThrowStartTime = System.currentTimeMillis()
                    if (System.currentTimeMillis() - upThrowStartTime >= upThrowTimeout) -90.0f else rotationDegrees
                } else {
                    upThrowStartTime = 0L
                    rotationDegrees
                }
                PlayerPacketManager.sendPlayerPacket {
                    rotate(player.rotationYaw.toVec2f(rotPitch))
                }
            }
        }

        safeListener<PacketEvent.PostSend> {
            if (it.packet is CPacketPlayerTryUseItem) {
                lastUsedHand = it.packet.hand
            }
        }

        safeListener<PacketEvent.Send> {
            if (rotateOnPacket && currentPotion == PotionType.INSTANT_HEALTH) {
                val packet = it.packet
                if (packet is CPacketPlayer) {
                    packet.setPitch(90.0f)
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            val potionType = currentPotion
            if (potionType == PotionType.NONE) return@safeListener

            when (instantThrow) {
                InstantMode.NONE -> {}
                InstantMode.RESET_HP -> if (player.health >= instantResetHP && instantThrowUsed) instantThrowUsed = false
                InstantMode.RESET_DELAY -> if (System.currentTimeMillis() - lastInstantThrowResetTime >= instantResetDelay && instantThrowUsed) instantThrowUsed = false
            }

            val slot = getSlot(this, potionType)
            if (slot != null) {
                HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, slot) {
                    if (potionType == PotionType.INSTANT_HEALTH) {
                        val count = if (instantThrow != InstantMode.NONE) {
                            instantThrowUsed = true
                            lastInstantThrowResetTime = System.currentTimeMillis()
                            instantAmount
                        } else if (doubleHealth) 2 else 1
                        val finalCount = if (adaptiveHealing && player.health <= adaptiveHealth && count <= 2) count + 1 else count
                        repeat(finalCount) {
                            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        }
                    }
                    if (potionType == PotionType.SPEED && doSpeedCheck(this)) {
                        connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                    }
                    potionType.timer.reset()
                    currentPotion = PotionType.NONE
                    if (updateController) mc.playerController.updateController()
                    if (resetRotation) {
                        PlayerPacketManager.sendPlayerPacket {
                            rotate(player.rotationYaw.toVec2f(85.0f))
                        }
                    }
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!heal || !keepHealInHotbar) return@safeParallelListener
            if (hasPotion(DefinedKt.getHotbarSlots(player), PotionType.INSTANT_HEALTH)) return@safeParallelListener
            if (lastTask != null && !lastTask!!.executed) return@safeParallelListener
            if (currentPotion != PotionType.NONE && currentPotion != PotionType.INSTANT_HEALTH) return@safeParallelListener

            val current = player.health
            val diff = lastHealth - current
            damageTrend = damageTrend * 0.8f + diff * 0.2f
            lastHealth = current

            val slotFrom = findPotion(DefinedKt.getAllSlots(player), PotionType.INSTANT_HEALTH) ?: return@safeParallelListener
            InventoryTask.Builder().apply {
                priority(modulePriority)
                swapWith(slotFrom, DefinedKt.getHotbarSlots(player)[healHotbar - 1])
                postDelay(100L)
                runInGui()
            }.build().let {
                InventoryTaskManager.addTask(it)
                lastTask = it
            }
        }
    }

    private fun SafeClientEvent.hasBlockAboveHead(event: SafeClientEvent): Boolean {
        val pos = BlockPos(player.posX, player.posY, player.posZ)
        return !world.isAirBlock(pos.up(2)) || !world.isAirBlock(BlockPos(player.posX, player.posY + player.height, player.posZ))
    }

    private fun SafeClientEvent.doSpeedCheck(event: SafeClientEvent): Boolean {
        val withinGround = player.onGround || player.posY - CheckKt.getGroundLevel(world, player) < 3.0
        if (speedCheckBelow) {
            if (world.isAirBlock(EntityUtils.getBetterPosition(player).down()) && withinGround) return false
        }
        if (player.health < speedHealth) return false
        return !speedCheckGround || player.onGround
    }

    private fun getSlot(event: SafeClientEvent, potionType: PotionType): Slot? = findPotion(DefinedKt.getAllSlotsPrioritized(event.player), potionType)

    private fun findPotion(slots: List<Slot>, potionType: PotionType): Slot? {
        return slots.asSequence()
            .filter { slot -> slot.stack.item == Items.SPLASH_POTION && ItemKt.hasPotion(slot.stack, potionType.potion) }
            .minByOrNull { slot -> if (slot.isHotbarSlot) -1 else slot.stack.count }
    }

    private fun hasPotion(slots: List<Slot>, potionType: PotionType): Boolean = slots.any { slot -> slot.stack.item == Items.SPLASH_POTION && ItemKt.hasPotion(slot.stack, potionType.potion) }

    private enum class PotionType(override val displayName: CharSequence, val potion: Potion) : DisplayEnum {
        INSTANT_HEALTH("Heal", MobEffects.INSTANT_HEALTH) {
            override fun check(event: SafeClientEvent): Boolean {
                if (!heal || !timer.tick(healDelay.toLong())) return false
                val health = if (predictiveHealing) event.player.health - damageTrend * predictionTicks * predictionSensitivity else event.player.health
                if (health > healHealth) return false
                return super.check(event)
            }
        },
        SPEED("Speed", MobEffects.SPEED) {
            override fun check(event: SafeClientEvent): Boolean {
                return speed && !event.player.isElytraFlying && timer.tick(speedDelay.toLong()) && !event.player.isPotionActive(MobEffects.SPEED) && super.check(event)
            }
        },
        NONE("", MobEffects.LUCK) {
            override fun check(event: SafeClientEvent): Boolean = false
        };

        val timer = TickTimer()
        open fun check(event: SafeClientEvent): Boolean = INSTANCE.hasPotion(DefinedKt.getAllSlots(event.player), this)

        companion object {
            val VALUES = values()
        }
    }

    enum class InstantMode(override val displayName: CharSequence) : DisplayEnum { NONE("None"), RESET_HP("Reset On Health"), RESET_DELAY("Reset On Delay") }
    enum class HudDisplay { POTIONS, HEALHEALTH, NONE }
    enum class Page(override val displayName: CharSequence) : DisplayEnum { HEALTH("Healing"), SPEED("Speed"), CALCS("Calculations"), DEV("Experimental") }
}
