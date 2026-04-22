package dev.wizard.meta.module.modules.beta

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.module.modules.combat.*
import dev.wizard.meta.module.modules.exploit.PacketEat
import dev.wizard.meta.module.modules.movement.Strafe
import dev.wizard.meta.module.modules.player.*
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.interfaces.DisplayEnum

object MetaSwapper : Module(
    "MetaSwapper",
    category = Category.BETA,
    description = "wizards module, autoconfigs for a meta",
    modulePriority = 100
) {
    private val config = setting(this, EnumSetting(settingName("Config Mode"), ConfigMode.CRYSTALPVP))
    private val page by setting(this, EnumSetting(settingName("Page"), Page.AUTOPOT))
    private val switchKit by setting(this, BooleanSetting(settingName("Switch Kit"), false))
    private val eatModule by setting(this, EnumSetting(settingName("Eat Module"), Eat.NONE))
    private val badBetterEat by setting(this, BooleanSetting(settingName("Bad BetterEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { eatModule == Eat.BETTEREAT }))

    private val bedKitName by setting(this, StringSetting(settingName("Bed Kit Name"), "bpvp2", { switchKit }))
    private val crystalKitName by setting(this, StringSetting(settingName("Crystal Kit Name"), "cpvp2", { switchKit }))

    // Bed AutoPot settings
    private val bedAutoPotHealth by setting(this, FloatSetting(settingName("Bed Autopot Heal Health"), 19.0f, 0.5f..20.0f, 0.5f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotHealDelay by setting(this, IntegerSetting(settingName("Bed Autopot Heal Delay"), 500, 0..10000, 10, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotRotation by setting(this, FloatSetting(settingName("Bed Autopot Rotation"), 90.0f, 70.0f..90.0f, 1.0f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotUpdateController by setting(this, BooleanSetting(settingName("Bed Autopot Update Controller"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotDoubleHealth by setting(this, BooleanSetting(settingName("Bed Autopot Double Health"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotAdaptiveHealing by setting(this, BooleanSetting(settingName("Bed Autopot Adaptive Healing"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotAdaptiveHealth by setting(this, FloatSetting(settingName("Bed Autopot Adaptive Health"), 5.5f, 0.5f..20.0f, 0.5f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotAdaptiveHealing }))
    private val bedAutoPotInstantMode by setting(this, EnumSetting(settingName("Bed Autopot Instant Mode"), AutoSplashPotion.InstantMode.NONE, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotInstantAmount by setting(this, IntegerSetting(settingName("Bed Autopot Instant Amount"), 3, 1..5, 1, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotInstantMode != AutoSplashPotion.InstantMode.NONE }))
    private val bedAutoPotInstantResetHealth by setting(this, FloatSetting(settingName("Bed Autopot Instant Reset Health"), 15.0f, 0.5f..20.0f, 0.5f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotInstantMode == AutoSplashPotion.InstantMode.RESET_HP }))
    private val bedAutoPotInstantResetDelay by setting(this, IntegerSetting(settingName("Bed Autopot Instant Reset Delay"), 1000, 0..10000, 50, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotInstantMode == AutoSplashPotion.InstantMode.RESET_DELAY }))
    private val bedAutoPotPrediction by setting(this, BooleanSetting(settingName("Bed Autopot Prediction"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotPredictionTicks by setting(this, IntegerSetting(settingName("Bed Autopot Prediction Ticks"), 3, 1..10, 1, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotPrediction }))
    private val bedAutoPotPredictionDecay by setting(this, FloatSetting(settingName("Bed Autopot Prediction Decay"), 0.8f, 0.0f..2.0f, 0.1f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotPrediction }))
    private val bedAutoPotUpThrow by setting(this, BooleanSetting(settingName("Bed Autopot Upthrow"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT }))
    private val bedAutoPotUpThrowTimeout by setting(this, IntegerSetting(settingName("Bed Autopot Upthrow Timeout"), 1000, 0..10000, 50, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.AUTOPOT && bedAutoPotUpThrow }))

    // Bed Module Toggles
    private val bedEnablePacketMine by setting(this, BooleanSetting(settingName("Bed Enable PacketMine"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisablePacketMine by setting(this, BooleanSetting(settingName("Bed Disable PacketMine"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableAutoRegear by setting(this, BooleanSetting(settingName("Bed Enable AutoRegear"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableAutoRegear by setting(this, BooleanSetting(settingName("Bed Disable AutoRegear"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableAutoHoleFill by setting(this, BooleanSetting(settingName("Bed Enable AutoHoleFill"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableAutoHoleFill by setting(this, BooleanSetting(settingName("Bed Disable AutoHoleFill"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableStrafe by setting(this, BooleanSetting(settingName("Bed Enable Strafe"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableStrafe by setting(this, BooleanSetting(settingName("Bed Disable Strafe"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnablePacketEat by setting(this, BooleanSetting(settingName("Bed Enable PacketEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES && eatModule == Eat.PACKETEAT }))
    private val bedDisablePacketEat by setting(this, BooleanSetting(settingName("Bed Disable PacketEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES && eatModule == Eat.PACKETEAT }))
    private val bedEnableBetterEat by setting(this, BooleanSetting(settingName("Bed Enable BetterEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES && eatModule == Eat.BETTEREAT }))
    private val bedDisableBetterEat by setting(this, BooleanSetting(settingName("Bed Disable BetterEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES && eatModule == Eat.BETTEREAT }))
    private val bedEnableBounceBegone by setting(this, BooleanSetting(settingName("Bed Enable BounceBegone"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableBounceBegone by setting(this, BooleanSetting(settingName("Bed Disable BounceBegone"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableFastUse by setting(this, BooleanSetting(settingName("Bed Enable FastUse"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableFastUse by setting(this, BooleanSetting(settingName("Bed Disable FastUse"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableCrystalPlaceBreak by setting(this, BooleanSetting(settingName("Bed Enable CrystalPlaceBreak"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableCrystalPlaceBreak by setting(this, BooleanSetting(settingName("Bed Disable CrystalPlaceBreak"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableAutoMineCart by setting(this, BooleanSetting(settingName("Bed Enable AutoMineCart"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableAutoMineCart by setting(this, BooleanSetting(settingName("Bed Disable AutoMineCart"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableAutoTrap by setting(this, BooleanSetting(settingName("Bed Enable AutoTrap"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableAutoTrap by setting(this, BooleanSetting(settingName("Bed Disable AutoTrap"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableSurround by setting(this, BooleanSetting(settingName("Bed Enable Surround"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableSurround by setting(this, BooleanSetting(settingName("Bed Disable Surround"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableInteractions by setting(this, BooleanSetting(settingName("Bed Enable Interactions"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableInteractions by setting(this, BooleanSetting(settingName("Bed Disable Interactions"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableMultiTask by setting(this, BooleanSetting(settingName("Bed Enable MultiTask"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableMultiTask by setting(this, BooleanSetting(settingName("Bed Disable MultiTask"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedEnableBedAura by setting(this, BooleanSetting(settingName("Bed Enable BedAura"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))
    private val bedDisableBedAura by setting(this, BooleanSetting(settingName("Bed Disable BedAura"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.BEDPVP)) { page == Page.TOGGLES }))

    // Crystal AutoPot settings
    private val crystalAutoPotHealth by setting(this, FloatSetting(settingName("Crystal Autopot Heal Health"), 9.0f, 0.5f..20.0f, 0.5f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotHealDelay by setting(this, IntegerSetting(settingName("Crystal Autopot Heal Delay"), 500, 0..10000, 10, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotRotation by setting(this, FloatSetting(settingName("Crystal Autopot Rotation"), 90.0f, 70.0f..90.0f, 1.0f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotUpdateController by setting(this, BooleanSetting(settingName("Crystal Autopot Update Controller"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotDoubleHealth by setting(this, BooleanSetting(settingName("Crystal Autopot Double Health"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotAdaptiveHealing by setting(this, BooleanSetting(settingName("Crystal Autopot Adaptive Healing"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotAdaptiveHealth by setting(this, FloatSetting(settingName("Crystal Autopot Adaptive Health"), 5.5f, 0.5f..20.0f, 0.5f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotAdaptiveHealing }))
    private val crystalAutoPotInstantMode by setting(this, EnumSetting(settingName("Crystal Autopot Instant Mode"), AutoSplashPotion.InstantMode.NONE, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotInstantAmount by setting(this, IntegerSetting(settingName("Crystal Autopot Instant Amount"), 3, 1..5, 1, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotInstantMode != AutoSplashPotion.InstantMode.NONE }))
    private val crystalAutoPotInstantResetHealth by setting(this, FloatSetting(settingName("Crystal Autopot Instant Reset Health"), 15.0f, 0.5f..20.0f, 0.5f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotInstantMode == AutoSplashPotion.InstantMode.RESET_HP }))
    private val crystalAutoPotInstantResetDelay by setting(this, IntegerSetting(settingName("Crystal Autopot Instant Reset Delay"), 1000, 0..10000, 50, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotInstantMode == AutoSplashPotion.InstantMode.RESET_DELAY }))
    private val crystalAutoPotPrediction by setting(this, BooleanSetting(settingName("Crystal Autopot Prediction"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotPredictionTicks by setting(this, IntegerSetting(settingName("Crystal Autopot Prediction Ticks"), 3, 1..10, 1, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotPrediction }))
    private val crystalAutoPotPredictionDecay by setting(this, FloatSetting(settingName("Crystal Autopot Prediction Decay"), 0.8f, 0.0f..2.0f, 0.1f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotPrediction }))
    private val crystalAutoPotUpThrow by setting(this, BooleanSetting(settingName("Crystal Autopot Upthrow"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT }))
    private val crystalAutoPotUpThrowTimeout by setting(this, IntegerSetting(settingName("Crystal Autopot Upthrow Timeout"), 1000, 0..10000, 50, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.AUTOPOT && crystalAutoPotUpThrow }))

    // Crystal Module Toggles
    private val crystalEnablePacketMine by setting(this, BooleanSetting(settingName("Crystal Enable PacketMine"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisablePacketMine by setting(this, BooleanSetting(settingName("Crystal Disable PacketMine"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES && eatModule == Eat.PACKETEAT }))
    private val crystalEnableAutoRegear by setting(this, BooleanSetting(settingName("Crystal Enable AutoRegear"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableAutoRegear by setting(this, BooleanSetting(settingName("Crystal Disable AutoRegear"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableAutoHoleFill by setting(this, BooleanSetting(settingName("Crystal Enable AutoHoleFill"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableAutoHoleFill by setting(this, BooleanSetting(settingName("Crystal Disable AutoHoleFill"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableStrafe by setting(this, BooleanSetting(settingName("Crystal Enable Strafe"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableStrafe by setting(this, BooleanSetting(settingName("Crystal Disable Strafe"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableInteractions by setting(this, BooleanSetting(settingName("Crystal Enable Interactions"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableInteractions by setting(this, BooleanSetting(settingName("Crystal Disable Interactions"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableMultiTask by setting(this, BooleanSetting(settingName("Crystal Enable MultiTask"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableMultiTask by setting(this, BooleanSetting(settingName("Crystal Disable MultiTask"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableBetterEat by setting(this, BooleanSetting(settingName("Crystal Enable BetterEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES && eatModule == Eat.BETTEREAT && !badBetterEat }))
    private val crystalDisableBetterEat by setting(this, BooleanSetting(settingName("Crystal Disable BetterEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES && eatModule == Eat.BETTEREAT && badBetterEat }))
    private val crystalEnablePacketEat by setting(this, BooleanSetting(settingName("Crystal Enable PacketEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES && eatModule == Eat.PACKETEAT }))
    private val crystalDisablePacketEat by setting(this, BooleanSetting(settingName("Crystal Disable PacketEat"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES && eatModule == Eat.PACKETEAT }))
    private val crystalEnableBedAura by setting(this, BooleanSetting(settingName("Crystal Enable BedAura"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableBedAura by setting(this, BooleanSetting(settingName("Crystal Disable BedAura"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableBounceBegone by setting(this, BooleanSetting(settingName("Crystal Enable BounceBegone"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableBounceBegone by setting(this, BooleanSetting(settingName("Crystal Disable BounceBegone"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableAutoTrap by setting(this, BooleanSetting(settingName("Crystal Enable AutoTrap"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableAutoTrap by setting(this, BooleanSetting(settingName("Crystal Disable AutoTrap"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableSurround by setting(this, BooleanSetting(settingName("Crystal Enable Surround"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableSurround by setting(this, BooleanSetting(settingName("Crystal Disable Surround"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableFastUse by setting(this, BooleanSetting(settingName("Crystal Enable FastUse"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableFastUse by setting(this, BooleanSetting(settingName("Crystal Disable FastUse"), true, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableCrystalPlaceBreak by setting(this, BooleanSetting(settingName("Crystal Enable CrystalPlaceBreak"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableCrystalPlaceBreak by setting(this, BooleanSetting(settingName("Crystal Disable CrystalPlaceBreak"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalEnableAutoMineCart by setting(this, BooleanSetting(settingName("Crystal Enable AutoMineCart"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))
    private val crystalDisableAutoMineCart by setting(this, BooleanSetting(settingName("Crystal Disable AutoMineCart"), false, LambdaUtilsKt.and(LambdaUtilsKt.atValue(config, ConfigMode.CRYSTALPVP)) { page == Page.TOGGLES }))

    private var lastMode: ConfigMode? = null

    init {
        onEnable {
            applyConfig(config.value)
            lastMode = config.value
            config.value = if (config.value == ConfigMode.BEDPVP) ConfigMode.CRYSTALPVP else ConfigMode.BEDPVP
            disable()
        }
    }

    private fun applyConfig(mode: ConfigMode) {
        when (mode) {
            ConfigMode.CRYSTALPVP -> applyCrystalConfig()
            ConfigMode.BEDPVP -> applyBedConfig()
        }
    }

    private fun applyCrystalConfig() {
        handleModuleToggle(PacketMine, crystalEnablePacketMine, crystalDisablePacketMine)
        handleModuleToggle(AutoRegear, crystalEnableAutoRegear, crystalDisableAutoRegear)
        handleModuleToggle(AutoHoleFill, crystalEnableAutoHoleFill, crystalDisableAutoHoleFill)
        handleModuleToggle(Strafe, crystalEnableStrafe, crystalDisableStrafe)
        handleModuleToggle(Interactions, crystalEnableInteractions, crystalDisableInteractions)
        handleModuleToggle(MultiTask, crystalEnableMultiTask, crystalDisableMultiTask)
        handleModuleToggle(BedAura, crystalEnableBedAura, crystalDisableBedAura)
        handleModuleToggle(BounceBegone, crystalEnableBounceBegone, crystalDisableBounceBegone)
        handleModuleToggle(AutoTrap, crystalEnableAutoTrap, crystalDisableAutoTrap)
        handleModuleToggle(Surround, crystalEnableSurround, crystalDisableSurround)
        handleModuleToggle(FastUse, crystalEnableFastUse, crystalDisableFastUse)
        handleModuleToggle(CrystalPlaceBreak, crystalEnableCrystalPlaceBreak, crystalDisableCrystalPlaceBreak)
        handleModuleToggle(AutoMineCart, crystalEnableAutoMineCart, crystalDisableAutoMineCart)

        if (eatModule == Eat.BETTEREAT) {
            handleModuleToggle(BetterEat, crystalEnableBetterEat, crystalDisableBetterEat)
        }
        if (eatModule == Eat.PACKETEAT) {
            handleModuleToggle(PacketEat, crystalEnablePacketEat, crystalDisablePacketEat)
        }

        AutoSplashPotion.apply {
            healHealth = crystalAutoPotHealth
            healDelay = crystalAutoPotHealDelay
            updateController = crystalAutoPotUpdateController
            rotationDegrees = crystalAutoPotRotation
            predictiveHealing = crystalAutoPotPrediction
            predictionTicks = crystalAutoPotPredictionTicks
            predictionSensitivity = crystalAutoPotPredictionDecay
            doubleHealth = crystalAutoPotDoubleHealth
            adaptiveHealing = crystalAutoPotAdaptiveHealing
            adaptiveHealth = crystalAutoPotAdaptiveHealth
            instantThrow = crystalAutoPotInstantMode
            instantAmount = crystalAutoPotInstantAmount
            instantResetHP = crystalAutoPotInstantResetHealth
            instantResetDelay = crystalAutoPotInstantResetDelay
            upThrow = crystalAutoPotUpThrow
            upThrowTimeout = crystalAutoPotUpThrowTimeout
        }

        if (switchKit) {
            Kit.kitName = crystalKitName
        }
    }

    private fun applyBedConfig() {
        handleModuleToggle(PacketMine, bedEnablePacketMine, bedDisablePacketMine)
        handleModuleToggle(AutoRegear, bedEnableAutoRegear, bedDisableAutoRegear)
        handleModuleToggle(AutoHoleFill, bedEnableAutoHoleFill, bedDisableAutoHoleFill)
        handleModuleToggle(Strafe, bedEnableStrafe, bedDisableStrafe)
        handleModuleToggle(BounceBegone, bedEnableBounceBegone, bedDisableBounceBegone)
        handleModuleToggle(FastUse, bedEnableFastUse, bedDisableFastUse)
        handleModuleToggle(CrystalPlaceBreak, bedEnableCrystalPlaceBreak, bedDisableCrystalPlaceBreak)
        handleModuleToggle(AutoMineCart, bedEnableAutoMineCart, bedDisableAutoMineCart)
        handleModuleToggle(AutoTrap, bedEnableAutoTrap, bedDisableAutoTrap)
        handleModuleToggle(Surround, bedEnableSurround, bedDisableSurround)
        handleModuleToggle(Interactions, bedEnableInteractions, bedDisableInteractions)
        handleModuleToggle(MultiTask, bedEnableMultiTask, bedDisableMultiTask)
        handleModuleToggle(BedAura, bedEnableBedAura, bedDisableBedAura)

        if (eatModule == Eat.PACKETEAT) {
            handleModuleToggle(PacketEat, bedEnablePacketEat, bedDisablePacketEat)
        }
        if (eatModule == Eat.BETTEREAT) {
            handleModuleToggle(BetterEat, bedEnableBetterEat, bedDisableBetterEat)
        }

        AutoSplashPotion.apply {
            healHealth = bedAutoPotHealth
            healDelay = bedAutoPotHealDelay
            updateController = bedAutoPotUpdateController
            rotationDegrees = bedAutoPotRotation
            predictiveHealing = bedAutoPotPrediction
            predictionTicks = bedAutoPotPredictionTicks
            predictionSensitivity = bedAutoPotPredictionDecay
            doubleHealth = bedAutoPotDoubleHealth
            adaptiveHealing = bedAutoPotAdaptiveHealing
            adaptiveHealth = bedAutoPotAdaptiveHealth
            instantThrow = bedAutoPotInstantMode
            instantAmount = bedAutoPotInstantAmount
            instantResetHP = bedAutoPotInstantResetHealth
            instantResetDelay = bedAutoPotInstantResetDelay
            upThrow = bedAutoPotUpThrow
            upThrowTimeout = bedAutoPotUpThrowTimeout
        }

        if (switchKit) {
            Kit.kitName = bedKitName
        }
    }

    private fun handleModuleToggle(module: Module, enable: Boolean, disable: Boolean) {
        if (enable && !module.isEnabled()) {
            module.enable()
        } else if (disable && module.isEnabled()) {
            module.disable()
        }
    }

    enum class ConfigMode(override val displayName: CharSequence) : DisplayEnum { CRYSTALPVP("Crystal Pvp"), BEDPVP("Bed Pvp") }
    enum class Eat { BETTEREAT, PACKETEAT, NONE }
    private enum class Page(override val displayName: CharSequence) : DisplayEnum { AUTOPOT("AutoPot"), TOGGLES("Module Toggles") }
}
