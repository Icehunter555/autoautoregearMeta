package dev.wizard.meta.module.modules.client

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.command.commands.PingCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ModuleToggleEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.translation.TranslationKey
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.InfoCalculator
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.EnumTextColor
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.ThreadSafetyKt
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.entity.item.EntityMinecartTNT
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.potion.Potion
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import java.util.*

object Alerts : Module(
    "Alerts",
    category = Category.CLIENT,
    description = "Alerts for pvp"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.ALERTS))

    var moduleToggle by setting(this, EnumSetting(settingName("Module Toggle"), NotificationMode.OFF, { page == Page.TOGGLES }))
    private val toggleStyle by setting(this, EnumSetting(settingName("Mode"), Mode.NAME, { moduleToggle != NotificationMode.OFF && page == Page.TOGGLES }))
    private val onlyVisible by setting(this, BooleanSetting(settingName("Only Visible"), true, { moduleToggle != NotificationMode.OFF && page == Page.TOGGLES }))
    private val notRender by setting(this, BooleanSetting(settingName("Not Render"), true, { moduleToggle != NotificationMode.OFF && page == Page.TOGGLES }))
    private val toggleColorOn by setting(this, EnumSetting(settingName("Enable Color"), EnumTextColor.GREEN, { moduleToggle != NotificationMode.OFF && page == Page.TOGGLES }))
    private val toggleColorOff by setting(this, EnumSetting(settingName("Disable Color"), EnumTextColor.RED, { moduleToggle != NotificationMode.OFF && page == Page.TOGGLES }))

    private val totemPops by setting(this, EnumSetting(settingName("Totem Pops"), NotificationMode.OFF, { page == Page.POPS }))
    private val totemPopColor by setting(this, EnumSetting(settingName("Totem Message Color"), EnumTextColor.BLUE, { totemPops != NotificationMode.OFF && page == Page.POPS }))
    private val popCountFriends by setting(this, BooleanSetting(settingName("Count Friend-Pop"), true, { totemPops != NotificationMode.OFF && page == Page.POPS }))
    private val friendPopColor by setting(this, EnumSetting(settingName("Friend Message Color"), EnumTextColor.DARK_RED, { totemPops != NotificationMode.OFF && popCountFriends && page == Page.POPS }))
    private val countSelf by setting(this, BooleanSetting(settingName("Count Self-Pop"), true, { totemPops != NotificationMode.OFF && page == Page.POPS }))
    private val selfPopColor by setting(this, EnumSetting(settingName("Self Message Color"), EnumTextColor.RED, { totemPops != NotificationMode.OFF && countSelf && page == Page.POPS }))
    private val popCoords by setting(this, BooleanSetting(settingName("Pop Coords"), false, { totemPops != NotificationMode.OFF && page == Page.POPS }))
    private val popPing by setting(this, BooleanSetting(settingName("Pop Ping"), false, { totemPops != NotificationMode.OFF && page == Page.POPS }))
    private val popsPersistent by setting(this, BooleanSetting(settingName("Save Pop Messages"), true, { totemPops != NotificationMode.OFF && page == Page.POPS }))

    private val visualRangeAlerts by setting(this, EnumSetting(settingName("VisualRange Alerts"), NotificationMode.OFF, { page == Page.VISUALRANGE }))
    private val visualRangeCountLeaving by setting(this, BooleanSetting(settingName("Count Leaving"), false, { page == Page.VISUALRANGE && visualRangeAlerts != NotificationMode.OFF }))
    private val visualRangeCountFriends by setting(this, BooleanSetting(settingName("Count Friends"), true, { page == Page.VISUALRANGE && visualRangeAlerts != NotificationMode.OFF }))
    private val visualRangeGearLevel by setting(this, BooleanSetting(settingName("Show Gear Level"), true, { page == Page.VISUALRANGE && visualRangeAlerts != NotificationMode.OFF }))
    private val visualRangeMessageAura by setting(this, BooleanSetting(settingName("Message Aura"), false, { page == Page.VISUALRANGE && visualRangeAlerts != NotificationMode.OFF }))
    private val visualRangeAuraMessage by setting(this, StringSetting(settingName("Message Aura Message"), "I see you :D", { page == Page.VISUALRANGE && visualRangeAlerts != NotificationMode.OFF && visualRangeMessageAura }))
    private val visualRangePersistent by setting(this, BooleanSetting(settingName("Save VisualRange Messages"), true, { page == Page.VISUALRANGE && visualRangeAlerts != NotificationMode.OFF }))

    private val tpsAlerts by setting(this, EnumSetting(settingName("Tps Alerts"), NotificationMode.OFF, { page == Page.SERVER }))
    private val tpsAlertThreshold by setting(this, IntegerSetting(settingName("Tps Threshold"), 16, 1..19, 1, { page == Page.SERVER && tpsAlerts != NotificationMode.OFF }))
    private val tpsDipThreshold by setting(this, IntegerSetting(settingName("Tps Dip Threshold"), 2, 1..10, 1, { page == Page.SERVER && tpsAlerts != NotificationMode.OFF }))
    private val tpsCheckDelay by setting(this, IntegerSetting(settingName("Tps Check Delay"), 5, 1..20, 1, { page == Page.SERVER && tpsAlerts != NotificationMode.OFF }))

    private val pingAlerts by setting(this, EnumSetting(settingName("Ping Alerts"), NotificationMode.OFF, { page == Page.SERVER }))
    private val pingSpikeThreshold by setting(this, IntegerSetting(settingName("Ping Spike Threshold"), 30, 10..200, 10, { page == Page.SERVER && pingAlerts != NotificationMode.OFF }))
    private val highPingThreshold by setting(this, IntegerSetting(settingName("High Ping Threshold"), 200, 1..300, 10, { page == Page.SERVER && pingAlerts != NotificationMode.OFF }))
    private val pingCheckDelay by setting(this, IntegerSetting(settingName("Ping Check Delay"), 5, 1..20, 1, { page == Page.SERVER && pingAlerts != NotificationMode.OFF }))

    private val breakAlerts by setting(this, EnumSetting(settingName("Break Alerts"), NotificationMode.OFF, { page == Page.BREAK }))
    private val breakAlertFriend by setting(this, BooleanSetting(settingName("Break Alert Friend"), false, { page == Page.BREAK && breakAlerts != NotificationMode.OFF }))
    private val burrowBreakAlerts by setting(this, EnumSetting(settingName("Burrow Break Alerts"), BreakAlertMode.OBSIDIAN, { page == Page.BREAK && breakAlerts != NotificationMode.OFF }))
    private val surroundBreakAlerts by setting(this, EnumSetting(settingName("Surround Break Alerts"), BreakAlertMode.OBSIDIAN, { page == Page.BREAK && breakAlerts != NotificationMode.OFF }))
    private val headBlockBreakAlerts by setting(this, EnumSetting(settingName("HeadBlock Break Alerts"), BreakAlertMode.OBSIDIAN, { page == Page.BREAK && breakAlerts != NotificationMode.OFF }))
    private val nearbyBreakAlerts by setting(this, EnumSetting(settingName("Nearby Break Alerts"), BreakAlertMode.NONE, { page == Page.BREAK && breakAlerts != NotificationMode.OFF }))
    private val nearbyBreakRange by setting(this, FloatSetting(settingName("Nearby Break Range"), 16.0f, 4.0f..32.0f, 1.0f, { page == Page.BREAK && breakAlerts != NotificationMode.OFF && nearbyBreakAlerts != BreakAlertMode.NONE }))

    private val pearlNotify by setting(this, EnumSetting(settingName("Pearl Notify"), NotificationMode.OFF, { page == Page.ALERTS }))
    private val pearlAlertMode by setting(this, EnumSetting(settingName("Pearl Alert Mode"), PearlAlertMode.BOTH, { page == Page.ALERTS && pearlNotify != NotificationMode.OFF }))
    private val pearlNotifyFriends by setting(this, BooleanSetting(settingName("Notify Friend Pearl"), false, { page == Page.ALERTS && pearlNotify != NotificationMode.OFF }))
    private val pearlNotifyDelay by setting(this, IntegerSetting(settingName("Pearl Notify Delay"), 3, 0..10, 1, { page == Page.ALERTS && pearlNotify != NotificationMode.OFF }))
    private val pearlNotifyRange by setting(this, IntegerSetting(settingName("Pearl Notify Range"), 60, 16..256, 5, { page == Page.ALERTS && pearlNotify != NotificationMode.OFF }))

    private val potionNotify by setting(this, EnumSetting(settingName("Potion Notify"), NotificationMode.OFF, { page == Page.ALERTS }))
    private val speedPotionNotify by setting(this, BooleanSetting(settingName("Speed Notify"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF }))
    private val checkOtherSpeed by setting(this, BooleanSetting(settingName("Notify Other's Speed"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF && speedPotionNotify }))
    private val weaknessNotify by setting(this, BooleanSetting(settingName("Weakness Notify"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF }))
    private val checkOtherWeakness by setting(this, BooleanSetting(settingName("Notify Other's Weakness"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF && weaknessNotify }))
    private val strengthNotify by setting(this, BooleanSetting(settingName("Strength Notify"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF }))
    private val checkOtherStrength by setting(this, BooleanSetting(settingName("Notify Other's Strength"), false, { page == Page.ALERTS && strengthNotify && potionNotify != NotificationMode.OFF }))
    private val jumpBoostNotify by setting(this, BooleanSetting(settingName("Jump Boost Notify"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF }))
    private val checkOtherJumpBoost by setting(this, BooleanSetting(settingName("Notify Other's Jump Boost "), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF && jumpBoostNotify }))
    private val witherNotify by setting(this, BooleanSetting(settingName("Wither Notify"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF }))
    private val checkOtherWither by setting(this, BooleanSetting(settingName("Notify Other's Wither"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF && witherNotify }))
    private val slownessNotify by setting(this, BooleanSetting(settingName("Slowness Notify"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF }))
    private val checkOtherSlowness by setting(this, BooleanSetting(settingName("Notify Other's Slowness"), false, { page == Page.ALERTS && potionNotify != NotificationMode.OFF && slownessNotify }))

    private val cartNotify by setting(this, EnumSetting(settingName("Cart Notify"), NotificationMode.OFF, { page == Page.ALERTS }))
    private val cartScanDelay by setting(this, IntegerSetting(settingName("Cart Scan Delay"), 5, 1..10, 1, { page == Page.ALERTS && cartNotify != NotificationMode.OFF }))

    private val healthNotify by setting(this, EnumSetting(settingName("Health Notify"), NotificationMode.OFF, { page == Page.ALERTS }))
    private val healthCheckDelay by setting(this, IntegerSetting(settingName("Health Check Delay"), 3, 1..10, 1, { page == Page.ALERTS && healthNotify != NotificationMode.OFF }))

    private val burrowNotify by setting(this, EnumSetting(settingName("Burrow Notify"), NotificationMode.OFF, { page == Page.ALERTS }))
    private val burrowNotifyFriends by setting(this, BooleanSetting(settingName("Burrow Notify Friends"), false, { page == Page.ALERTS && burrowNotify != NotificationMode.OFF }))
    private val burrowNotifyRange by setting(this, IntegerSetting(settingName("Burrow Notify Range"), 16, 8..256, 5, { page == Page.ALERTS && burrowNotify != NotificationMode.OFF }))
    private val burrowNotifyScan by setting(this, IntegerSetting(settingName("Burrow Scan Delay"), 4, 1..10, 1, { page == Page.ALERTS && burrowNotify != NotificationMode.OFF }))

    private val chorusNotify by setting(this, EnumSetting(settingName("Chorus Notify"), NotificationMode.OFF, { page == Page.ALERTS }))
    private val chorusNotifyFriends by setting(this, BooleanSetting(settingName("Chorus Notify Friends"), false, { page == Page.ALERTS && chorusNotify != NotificationMode.OFF }))
    private val chorusNotifyRange by setting(this, IntegerSetting(settingName("Chorus Notify Range"), 16, 8..256, 5, { page == Page.ALERTS && chorusNotify != NotificationMode.OFF }))
    private val chorusScanDelay by setting(this, IntegerSetting(settingName("Chorus Scan Delay"), 4, 1..10, 1, { page == Page.ALERTS && chorusNotify != NotificationMode.OFF }))

    private val alertPrefix by setting(this, EnumSetting(settingName("Alert Prefix"), AlertPrefix.EXCLAMATION, { page == Page.MESSAGE }))
    private val alertBrackets by setting(this, EnumSetting(settingName("Alert Brackets"), AlertBrackets.SQUARE, { page == Page.MESSAGE }))
    private val alertBracketsColor by setting(this, EnumSetting(settingName("Alert Bracket Color"), EnumTextColor.GRAY, { page == Page.MESSAGE && alertBrackets != AlertBrackets.NONE }))
    private val alertPrefixBold by setting(this, BooleanSetting(settingName("Alert Prefix Bold"), false, { page == Page.MESSAGE }))
    private val alertPrefixUnderline by setting(this, BooleanSetting(settingName("Alert Prefix Underline"), false, { page == Page.MESSAGE }))
    private val alertBracketBold by setting(this, BooleanSetting(settingName("Alert Bracket Bold"), false, { page == Page.MESSAGE && alertBrackets != AlertBrackets.NONE }))
    private val alertBracketUnderline by setting(this, BooleanSetting(settingName("Alert Bracket Underline"), false, { page == Page.MESSAGE && alertBrackets != AlertBrackets.NONE }))

    private val friendColor by setting(this, EnumSetting(settingName("Friend Color"), EnumTextColor.GREEN, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))
    private val themeColorOne by setting(this, EnumSetting(settingName("Theme Color One"), EnumTextColor.AQUA, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))
    private val themeColorTwo by setting(this, EnumSetting(settingName("Theme Color Two"), EnumTextColor.LIGHT_PURPLE, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))
    private val badColor by setting(this, EnumSetting(settingName("Bad Color"), EnumTextColor.RED, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))
    private val goodColor by setting(this, EnumSetting(settingName("Good Color"), EnumTextColor.GREEN, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))
    private val toneColor by setting(this, EnumSetting(settingName("Tone Color"), EnumTextColor.GRAY, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))
    private val targetColor by setting(this, EnumSetting(settingName("Target Color"), EnumTextColor.RED, LambdaUtilsKt.atValue({ page }, Page.MESSAGE)))

    private val playerSet = linkedSetOf<EntityPlayer>()
    private val visualRangeTimer = TickTimer()
    private val cartScanTimer = TickTimer()
    private val cartMap = mutableMapOf<BlockPos, MutableList<EntityMinecartTNT>>()
    private val cartWarningTimer = TickTimer()
    private var areCartsDetected = false
    private var warnedAboutHealth = false
    private val healthScanTimer = TickTimer()
    private val tpsScanTimer = TickTimer()
    private val pingScanTimer = TickTimer()
    private val tpsBuffer = CircularArray<Float>(20)
    private val breakScanTimer = TickTimer()
    private val reset = TextFormatting.RESET
    private val potionTimer = TickTimer()
    private var hasWeakness = false
    private var hasSpeed = false
    private var hasStrength = false
    private var hasJump = false
    private var hasSlow = false
    private var hasWither = false
    private var lastTps: Float? = null
    private var warnedAboutTps = false
    private var lastPing: Int? = null
    private var warnedAboutPing = false
    private val pearlTimer = TickTimer()
    private val notifiedPearls = mutableMapOf<UUID, PearlInfo>()
    private val notificationQueue = ArrayDeque<Int>()
    private val burrowScanTimer = TickTimer()
    private val burrowedPlayers = mutableSetOf<String>()
    private val chorusScanTimer = TickTimer()
    private val playerChorusHolding = mutableSetOf<String>()
    private val playerPotionMap = mutableMapOf<String, MutableSet<Potion>>()
    private val ignorelist = listOf<TranslationKey>()

    init {
        onDisable {
            hasSpeed = false
            hasStrength = false
            hasJump = false
            hasSlow = false
            hasWither = false
            hasWeakness = false
            burrowedPlayers.clear()
            playerChorusHolding.clear()
            playerPotionMap.clear()
            lastTps = null
            lastPing = null
            playerSet.clear()
            notificationQueue.clear()
            notifiedPearls.clear()
            cartMap.clear()
        }

        listener<WorldEvent.Unload> {
            playerSet.clear()
            notificationQueue.clear()
            burrowedPlayers.clear()
            playerChorusHolding.clear()
            playerPotionMap.clear()
            notifiedPearls.clear()
            cartMap.clear()
        }

        listener<WorldEvent.Entity.Remove> {
            if (it.entity is EntityEnderPearl && pearlNotify != NotificationMode.OFF) {
                val pearl = it.entity as EntityEnderPearl
                val pearlInfo = notifiedPearls.remove(pearl.uniqueID)
                if (pearlInfo != null && (pearlAlertMode == PearlAlertMode.DESPAWN || pearlAlertMode == PearlAlertMode.BOTH)) {
                    val (x, y, z) = pearlInfo.lastPos
                    sendAlert("${pearlInfo.throwerColor}${pearlInfo.throwerName}$reset's pearl despawned at ${TextFormatting.GRAY}(${"%.1f".format(x)}, ${"%.1f".format(y)}, ${"%.1f".format(z)})$reset!", 2, false)
                }
            }
            if (it.entity is EntityPlayer) {
                if (playerSet.remove(it.entity)) {
                    onLeave(it.entity as EntityPlayer)
                }
                burrowedPlayers.remove(it.entity.name)
                playerChorusHolding.remove(it.entity.name)
                playerPotionMap.remove(it.entity.name)
            }
        }

        safeListener<ModuleToggleEvent> {
            if (moduleToggle == NotificationMode.OFF) return@safeListener
            val moduleName = it.module.nameAsString
            val enableMessage = when (toggleStyle) {
                Mode.NAME -> "${toggleColorOn}$moduleName"
                Mode.BRACKETS -> "$reset$moduleName [${toggleColorOn}+$reset]"
                Mode.ON_OFF -> "$reset$moduleName${toggleColorOn} on"
                Mode.ARROW -> "$reset$moduleName \u27a1 ${toggleColorOn} Enabled"
                Mode.HTML -> "$reset<${toggleColorOn}enable$reset:${moduleName.toLowerCase(Locale.ROOT)}>"
            }
            val disableMessage = when (toggleStyle) {
                Mode.NAME -> "${toggleColorOff}$moduleName"
                Mode.BRACKETS -> "$reset$moduleName [${toggleColorOff}-$reset]"
                Mode.ON_OFF -> "$reset$moduleName${toggleColorOff} off"
                Mode.ARROW -> "$reset$moduleName \u27a1 ${toggleColorOff} Disabled"
                Mode.HTML -> "$reset<${toggleColorOff}disable$reset:${moduleName.toLowerCase(Locale.ROOT)}>"
            }
            if (!shouldHide(it.module)) {
                if (it.module.isDisabled) NoSpamMessage.sendMessage(enableMessage)
                if (it.module.isEnabled) NoSpamMessage.sendMessage(disableMessage)
            }
        }

        safeListener<TotemPopEvent.Pop> {
            if (!countSelf && it.entity == player) return@safeListener
            if (!popCountFriends && FriendManager.isFriend(it.entity.name)) return@safeListener
            sendAlert(getTotemNotif(it.entity, it.count), if (it.entity == player) 1 else if (EntityUtils.isFriend(it.entity)) 3 else 0, !popsPersistent)
        }

        safeListener<TotemPopEvent.Death> {
            if (!countSelf && it.entity == player) return@safeListener
            if (!popCountFriends && FriendManager.isFriend(it.entity.name)) return@safeListener
            sendAlert(getDeathPopNotif(it.entity, it.count), if (it.entity == player) 1 else if (EntityUtils.isFriend(it.entity)) 2 else 0, !popsPersistent)
        }

        safeListener<TickEvent.Post> {
            if (pearlNotify != NotificationMode.OFF && pearlTimer.tickAndReset(pearlNotifyDelay.toLong())) {
                val pearls = world.loadedEntityList.filterIsInstance<EntityEnderPearl>()
                if (pearlAlertMode == PearlAlertMode.SPAWN || pearlAlertMode == PearlAlertMode.BOTH) {
                    for (pearl in pearls) {
                        if (notifiedPearls.containsKey(pearl.uniqueID) || player.getDistance(pearl) > pearlNotifyRange) continue
                        val thrower = world.playerEntities.filter {
                            !EntityUtils.isFakeOrSelf(it) && !it.isDead && it.getDistanceSq(pearl.posX, pearl.posY, pearl.posZ) < 50.0
                        }.minByOrNull { it.getDistanceSq(pearl.posX, pearl.posY, pearl.posZ) }

                        if (thrower == null || thrower == player || (!pearlNotifyFriends && EntityUtils.isFriend(thrower))) continue

                        val horizSpeed = Math.sqrt(pearl.motionX * pearl.motionX + pearl.motionZ * pearl.motionZ)
                        val yaw = Math.toDegrees(Math.atan2(-pearl.motionX, pearl.motionZ)).toFloat()
                        val angle = (yaw + 180.0 + 22.5) % 360.0
                        val direction = when {
                            angle <= 45.0 -> "south"
                            angle <= 90.0 -> "south-west"
                            angle <= 135.0 -> "west"
                            angle <= 180.0 -> "north-west"
                            angle <= 225.0 -> "north"
                            angle <= 270.0 -> "north-east"
                            angle <= 315.0 -> "east"
                            else -> "south-east"
                        }
                        val color = if (EntityUtils.isFriend(thrower)) friendColor else targetColor
                        sendAlert("${color}${thrower.name}${reset} threw a pearl ${themeColorTwo}\u2192 $direction${reset} ${toneColor}(${thrower.posX.toInt()}, ${thrower.posY.toInt()}, ${thrower.posZ.toInt()})", 2, false)
                        notifiedPearls[pearl.uniqueID] = PearlInfo(thrower.name, color, Triple(pearl.posX, pearl.posY, pearl.posZ))
                    }
                }
                val currentPearlIds = pearls.map { it.uniqueID }.toSet()
                notifiedPearls.keys.retainAll(currentPearlIds)
                for (pearl in pearls) {
                    notifiedPearls[pearl.uniqueID]?.let {
                        notifiedPearls[pearl.uniqueID] = it.copy(lastPos = Triple(pearl.posX, pearl.posY, pearl.posZ))
                    }
                }
            }

            if (potionNotify != NotificationMode.OFF && potionTimer.tickAndReset(5000L, TimeUnit.MILLISECONDS)) {
                doPotionNotif(MobEffects.SPEED)
                doPotionNotif(MobEffects.WEAKNESS)
                doPotionNotif(MobEffects.STRENGTH)
                doPotionNotif(MobEffects.WITHER)
                doPotionNotif(MobEffects.JUMP_BOOST)
                doPotionNotif(MobEffects.SLOWNESS)

                for (entity in world.playerEntities) {
                    if (entity == null || EntityUtils.isFakeOrSelf(entity)) continue
                    checkOtherPlayerPotion(entity, MobEffects.SPEED, checkOtherSpeed, speedPotionNotify)
                    checkOtherPlayerPotion(entity, MobEffects.WEAKNESS, checkOtherWeakness, weaknessNotify)
                    checkOtherPlayerPotion(entity, MobEffects.STRENGTH, checkOtherStrength, strengthNotify)
                    checkOtherPlayerPotion(entity, MobEffects.WITHER, checkOtherWither, witherNotify)
                    checkOtherPlayerPotion(entity, MobEffects.JUMP_BOOST, checkOtherJumpBoost, jumpBoostNotify)
                    checkOtherPlayerPotion(entity, MobEffects.SLOWNESS, checkOtherSlowness, slownessNotify)
                }
            }
        }
    }

    private fun shouldHide(module: AbstractModule): Boolean {
        return (onlyVisible && !module.isVisible()) || (notRender && module.category == Category.RENDER) || module.isDevOnly || ignorelist.contains(module.name)
    }

    private fun SafeClientEvent.getTotemNotif(eplayer: EntityPlayer, count: Int): String {
        val s = if (count == 1) "" else "s"
        val coords = if (popCoords) " ${toneColor}[${eplayer.posX.toInt()}, ${eplayer.posY.toInt()}, ${eplayer.posZ.toInt()}]${TextFormatting.RESET}" else ""
        val ping = if (popPing) {
            if (eplayer == player && FastLatency.isEnabled()) " (${PingCommand.getPingWithColor(FastLatency.lastPacketPing.toInt())})"
            else " (${PingCommand.getPingWithColor(InfoCalculator.getPlayerPing(eplayer))})"
        } else ""

        return if (eplayer == player) {
            "${selfPopColor}You popped ${TextFormatting.WHITE}$count ${selfPopColor}totem$s!${TextFormatting.RESET}$ping"
        } else {
            if (FriendManager.isFriend(eplayer.name)) {
                "${friendPopColor}Your friend ${TextFormatting.WHITE}${eplayer.name}${friendPopColor} popped ${TextFormatting.WHITE}$count${friendPopColor} totem$s!${TextFormatting.RESET}$coords$ping"
            } else {
                "${TextFormatting.WHITE}${eplayer.name}${totemPopColor} popped ${TextFormatting.WHITE}$count${totemPopColor} totem$s!${TextFormatting.RESET}$coords$ping"
            }
        }
    }

    private fun SafeClientEvent.getDeathPopNotif(eplayer: EntityPlayer, count: Int): String {
        val s = if (count == 1) "" else "s"
        val coords = if (popCoords) " ${toneColor}[${eplayer.posX.toInt()}, ${eplayer.posY.toInt()}, ${eplayer.posZ.toInt()}]${TextFormatting.RESET}" else ""
        val ping = if (popPing) {
            if (eplayer == player && FastLatency.isEnabled()) " (${PingCommand.getPingWithColor(FastLatency.lastPacketPing.toInt())})"
            else " (${PingCommand.getPingWithColor(InfoCalculator.getPlayerPing(eplayer))})"
        } else ""

        return if (eplayer == player) {
            "${selfPopColor} You died after popping ${TextFormatting.WHITE}$count${selfPopColor} totem$s${TextFormatting.RESET}$ping"
        } else {
            if (FriendManager.isFriend(eplayer.name)) {
                "${friendPopColor}Your friend ${TextFormatting.WHITE}${eplayer.name}${friendPopColor} died after popping ${TextFormatting.WHITE}$count${friendPopColor} totem$s${TextFormatting.RESET}$coords$ping"
            } else {
                "${TextFormatting.WHITE}${eplayer.name}${totemPopColor} died after popping${TextFormatting.WHITE} $count ${totemPopColor}totem$s${TextFormatting.RESET}$coords$ping"
            }
        }
    }

    private fun calculateRelativeDirection(entity: EntityPlayer): String {
        val player = mc.player ?: return "unknown direction from"
        val deltaX = entity.posX - player.posX
        val deltaZ = entity.posZ - player.posZ
        val angleToEntity = Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI
        val relativeAngle = (angleToEntity - player.rotationYaw + 360.0) % 360.0
        return when {
            relativeAngle < 22.5 || relativeAngle >= 337.5 -> "to the right of you"
            relativeAngle < 67.5 -> "ahead and to the right of you"
            relativeAngle < 112.5 -> "ahead of you"
            relativeAngle < 157.5 -> "ahead and to the left of you"
            relativeAngle < 202.5 -> "to the left of you"
            relativeAngle < 247.5 -> "behind and to the left of you"
            relativeAngle < 292.5 -> "behind you"
            relativeAngle < 337.5 -> "behind and to the right of you"
            else -> "unknown direction from"
        }
    }

    private fun getGearLevel(player: EntityPlayer): String {
        var armorCount = 0
        var diamondArmorCount = 0
        var highStackCount = 0
        var mediumStackCount = 0
        for (armor in player.armorInventoryList) {
            if (armor.isEmpty) continue
            armorCount++
            if (armor.item == Items.ELYTRA) {
                if (player.isElytraFlying) return "Wasping"
                continue
            }
            val isDiamond = armor.item == Items.DIAMOND_HELMET || armor.item == Items.DIAMOND_CHESTPLATE || armor.item == Items.DIAMOND_LEGGINGS || armor.item == Items.DIAMOND_BOOTS
            if (isDiamond) {
                diamondArmorCount++
                if (armor.count > 64) highStackCount++
                else if (armor.count > 30) mediumStackCount++
            }
        }
        return when {
            armorCount == 0 -> "Naked"
            diamondArmorCount < armorCount -> "Low-Geared"
            diamondArmorCount == 4 && highStackCount == 4 -> "Geared"
            diamondArmorCount == 4 && mediumStackCount + highStackCount == 4 -> "Semi-Geared"
            else -> "Low-Geared"
        }
    }

    private fun onEnter(player: EntityPlayer) {
        val pos = EntityUtils.getFlooredPosition(player)
        val direction = calculateRelativeDirection(player)
        val gearLevel = if (visualRangeGearLevel) getGearLevel(player) else null
        val playerColor = getColor(player)
        val coords = "${toneColor}(${pos.x}, ${pos.y}, ${pos.z})${TextFormatting.WHITE}"
        val directionText = "${themeColorOne}$direction${TextFormatting.WHITE}"
        val gearText = gearLevel?.let { " ${getGearColor(it)}[$it]${TextFormatting.WHITE}" } ?: ""
        val message = "$playerColor${player.name}${TextFormatting.WHITE}$gearText spotted! $coords ($directionText)"
        sendNotification(player, message)
        if (visualRangeMessageAura) {
            MessageSendUtils.sendServerMessage("/w ${player.name} $visualRangeAuraMessage")
        }
    }

    private fun onLeave(player: EntityPlayer) {
        if (!visualRangeCountLeaving) return
        val pos = EntityUtils.getFlooredPosition(player)
        val direction = calculateRelativeDirection(player)
        val playerColor = getColor(player)
        val coords = "${toneColor}(${pos.x}, ${pos.y}, ${pos.z})${TextFormatting.WHITE}"
        val directionText = "${themeColorOne}$direction${TextFormatting.WHITE}"
        val message = "$playerColor${player.name}${TextFormatting.WHITE} left $coords ($directionText)"
        sendNotification(player, message)
    }

    private fun getColor(player: EntityPlayer): TextFormatting = if (FriendManager.isFriend(player.name)) TextFormatting.GREEN else TextFormatting.RED

    private fun getGearColor(gearLevel: String): TextFormatting = when (gearLevel) {
        "Naked" -> TextFormatting.BLUE
        "Wasping" -> TextFormatting.AQUA
        "Low-Geared" -> TextFormatting.GREEN
        "Semi-Geared" -> TextFormatting.YELLOW
        "Geared" -> TextFormatting.RED
        else -> TextFormatting.WHITE
    }

    private fun SafeClientEvent.doPotionNotif(effect: Potion) {
        val shouldRun = (effect == MobEffects.SPEED && speedPotionNotify) ||
                        (effect == MobEffects.WEAKNESS && weaknessNotify) ||
                        (effect == MobEffects.STRENGTH && strengthNotify) ||
                        (effect == MobEffects.WITHER && witherNotify) ||
                        (effect == MobEffects.JUMP_BOOST && jumpBoostNotify) ||
                        (effect == MobEffects.SLOWNESS && slownessNotify)
        if (!shouldRun) return

        val potionName = when (effect) {
            MobEffects.SPEED -> "${TextFormatting.AQUA}Speed"
            MobEffects.STRENGTH -> "${TextFormatting.RED}Strength"
            MobEffects.WEAKNESS -> "${TextFormatting.DARK_GRAY}Weakness"
            MobEffects.JUMP_BOOST -> "${TextFormatting.DARK_GREEN}Jump Boost"
            MobEffects.WITHER -> "${TextFormatting.BLUE}Wither"
            MobEffects.SLOWNESS -> "${TextFormatting.GRAY}Slowness"
            else -> ""
        }

        val severity = when (effect) {
            MobEffects.SPEED, MobEffects.STRENGTH -> 0
            MobEffects.WEAKNESS -> 2
            MobEffects.WITHER, MobEffects.JUMP_BOOST, MobEffects.SLOWNESS -> 4
            else -> 0
        }

        val currentlyHas = player.isPotionActive(effect)
        val previouslyHad = hasEffect(effect)

        if (currentlyHas && !previouslyHad) {
            sendAlert("You now have $potionName$reset!", severity)
            setEffect(effect, true)
        } else if (!currentlyHas && previouslyHad) {
            sendAlert("You no longer have $potionName$reset!", if (effect == MobEffects.SPEED || effect == MobEffects.STRENGTH) 3 else 0)
            setEffect(effect, false)
        }
    }

    private fun SafeClientEvent.checkOtherPlayerPotion(entity: EntityPlayer, effect: Potion, checkOther: Boolean, baseNotify: Boolean) {
        if (!checkOther || !baseNotify) return

        val potionName = when (effect) {
            MobEffects.SPEED -> "${TextFormatting.AQUA}Speed"
            MobEffects.STRENGTH -> "${TextFormatting.RED}Strength"
            MobEffects.WEAKNESS -> "${TextFormatting.DARK_GRAY}Weakness"
            MobEffects.JUMP_BOOST -> "${TextFormatting.DARK_GREEN}Jump Boost"
            MobEffects.WITHER -> "${TextFormatting.BLUE}Wither"
            MobEffects.SLOWNESS -> "${TextFormatting.GRAY}Slowness"
            else -> return
        }

        val severity = when (effect) {
            MobEffects.SPEED, MobEffects.STRENGTH -> 2
            MobEffects.WITHER -> 3
            else -> 0
        }

        val playerPotions = playerPotionMap.getOrPut(entity.name) { mutableSetOf() }
        val hasEffectNow = entity.isPotionActive(effect)
        val hadEffectBefore = playerPotions.contains(effect)
        val color = if (EntityUtils.isFriend(entity)) TextFormatting.GREEN else TextFormatting.RED

        if (hasEffectNow && !hadEffectBefore) {
            sendAlert("$color${entity.name}$reset now has $potionName$reset!", severity)
            playerPotions.add(effect)
        } else if (!hasEffectNow && hadEffectBefore) {
            sendAlert("$color${entity.name}$reset no longer has $potionName$reset!", 0)
            playerPotions.remove(effect)
        }
    }

    private fun setEffect(effect: Potion, value: Boolean) {
        when (effect) {
            MobEffects.SPEED -> hasSpeed = value
            MobEffects.STRENGTH -> hasStrength = value
            MobEffects.WEAKNESS -> hasWeakness = value
            MobEffects.JUMP_BOOST -> hasJump = value
            MobEffects.WITHER -> hasWither = value
            MobEffects.SLOWNESS -> hasSlow = value
        }
    }

    private fun hasEffect(effect: Potion): Boolean = when (effect) {
        MobEffects.SPEED -> hasSpeed
        MobEffects.STRENGTH -> hasStrength
        MobEffects.WEAKNESS -> hasWeakness
        MobEffects.JUMP_BOOST -> hasJump
        MobEffects.WITHER -> hasWither
        MobEffects.SLOWNESS -> hasSlow
        else -> false
    }

    fun sendAlert(message: String, severity: Int = 0, delete: Boolean = true) {
        sendChatAlert(message.hashCode(), message, severity, delete)
    }

    fun sendChatAlert(identifier: Any, message: String, severity: Int, delete: Boolean = true) {
        val color = when (severity) {
            1 -> TextFormatting.DARK_RED
            2 -> TextFormatting.RED
            3 -> TextFormatting.GOLD
            4 -> TextFormatting.YELLOW
            5 -> TextFormatting.DARK_GRAY
            else -> TextFormatting.GRAY
        }
        val prefixText = when (alertPrefix) {
            AlertPrefix.EXCLAMATION -> "!"
            AlertPrefix.ALERTS -> "Alerts"
            AlertPrefix.STAR -> "*"
            AlertPrefix.HASH -> "#"
            AlertPrefix.NONE -> ""
        }
        val formattedPrefix = "$reset$color${if (alertPrefixBold) TextFormatting.BOLD else ""}${if (alertPrefixUnderline) TextFormatting.UNDERLINE else ""}$prefixText$reset"
        val bracketFormatting = "$reset$alertBracketsColor${if (alertBracketBold) TextFormatting.BOLD else ""}${if (alertBracketUnderline) TextFormatting.UNDERLINE else ""}"

        val leftBracket = when (alertBrackets) {
            AlertBrackets.NONE -> ""
            AlertBrackets.ANGLE -> "<"
            AlertBrackets.PARENTHESES -> "("
            AlertBrackets.SQUARE -> "["
            AlertBrackets.CURLY -> "{"
            AlertBrackets.DOUBLE_ANGLE -> "\u226a"
        }
        val rightBracket = when (alertBrackets) {
            AlertBrackets.NONE -> ""
            AlertBrackets.CURLY -> "}"
            AlertBrackets.DOUBLE_ANGLE -> "\u226b"
            AlertBrackets.SQUARE -> "]"
            AlertBrackets.ANGLE -> ">"
            AlertBrackets.PARENTHESES -> ")"
        }

        val notifPrefix = if (alertBrackets == AlertBrackets.NONE) formattedPrefix else "$reset$bracketFormatting$leftBracket$reset$formattedPrefix$bracketFormatting$rightBracket$reset"
        val messageId = identifier.hashCode()

        ThreadSafetyKt.onMainThreadSafe {
            if (delete) {
                notificationQueue.addLast(messageId)
                if (notificationQueue.size >= 2) {
                    val oldestId = notificationQueue.removeFirst()
                    it.mc.ingameGUI.chatGUI.deleteChatLine(oldestId)
                }
            }
            it.mc.ingameGUI.chatGUI.printChatMessageWithOptionalGuid(TextComponentString("$notifPrefix $message"), messageId)
        }
    }

    private fun sendNotification(player: EntityPlayer, message: String) {
        NoSpamMessage.sendMessage(hashCode() xor player.name.hashCode(), message, !visualRangePersistent)
    }

    private enum class AlertBrackets { NONE, ANGLE, PARENTHESES, SQUARE, CURLY, DOUBLE_ANGLE }
    private enum class AlertPrefix { EXCLAMATION, ALERTS, STAR, HASH, NONE }
    private enum class BreakAlertMode { NONE, OBSIDIAN, ANY }
    private enum class Mode { NAME, BRACKETS, ON_OFF, ARROW, HTML }
    private enum class NotificationMode { OFF, CHAT, NOTIFICATION, BOTH }
    private enum class Page(override val displayName: CharSequence) : DisplayEnum { ALERTS("Alerts"), POPS("Pops"), VISUALRANGE("VisualRange"), SERVER("Server"), BREAK("Break"), TOGGLES("Module Toggles"), MESSAGE("Message") }
    private enum class PearlAlertMode { SPAWN, DESPAWN, BOTH }
    private data class PearlInfo(val throwerName: String, val throwerColor: EnumTextColor, val lastPos: Triple<Double, Double, Double>)
}
