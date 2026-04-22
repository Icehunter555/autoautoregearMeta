package dev.wizard.meta.module.modules.client

import baritone.api.Settings
import dev.wizard.meta.event.events.baritone.BaritoneSettingsInitEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.util.BaritoneUtils

object Baritone : Module(
    "Baritone",
    category = Category.CLIENT,
    description = "Configures Baritone settings",
    alwaysEnabled = true
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.BASIC))
    private val prefix by setting(this, StringSetting(settingName("Chat Prefix"), "#", { page == Page.BASIC }))
    private val allowBreak by setting(this, BooleanSetting(settingName("Allow Break"), true, { page == Page.BASIC }, "Allow Baritone to break blocks."))
    private val allowPlace by setting(this, BooleanSetting(settingName("Allow Place"), true, { page == Page.BASIC }, "Allow Baritone to place blocks."))
    private val allowSprint by setting(this, BooleanSetting(settingName("Allow Sprint"), true, { page == Page.BASIC }, "Allow Baritone to sprint."))
    private val allowInventory by setting(this, BooleanSetting(settingName("Allow Inventory"), false, { page == Page.BASIC }, "Allow Baritone to move items in your inventory to your hotbar."))
    private val allowDownwardTunneling by setting(this, BooleanSetting(settingName("Downward Tunneling"), true, { page == Page.BASIC }, "Allow mining blocks directly beneath you."))
    private val allowParkour by setting(this, BooleanSetting(settingName("Allow Parkour"), true, { page == Page.BASIC }))
    private val allowParkourPlace by setting(this, BooleanSetting(settingName("Allow Parkour Place"), true, { allowParkour && page == Page.BASIC }))
    private val chatControl by setting(this, BooleanSetting(settingName("Chat Control"), true, { page == Page.BASIC }))
    private val antiCheatCompat by setting(this, BooleanSetting(settingName("AntiCheat Compatability"), false, { page == Page.BASIC }, "Not needed for 5b5t or NCP"))

    private val freeLook by setting(this, BooleanSetting(settingName("Free Look"), true, { page == Page.VISUAL }, "Move without having to force the client-sided rotations."))
    private val renderGoal by setting(this, BooleanSetting(settingName("Render Goals"), true, { page == Page.VISUAL }, "Render the goal."))
    private val censorCoordinates by setting(this, BooleanSetting(settingName("Censor Coordinates"), false, { page == Page.VISUAL }, "Censor coordinates in goals and block positions."))
    private val censorRanCommands by setting(this, BooleanSetting(settingName("Censor RanCommands"), false, { page == Page.VISUAL }, "Censor arguments to ran commands, to hide, for example, coordinates to #goal."))
    private val shortBaritonePrefix by setting(this, BooleanSetting(settingName("Short Baritone Prefix"), false, { page == Page.VISUAL }, "Use a short Baritone prefix [B] instead of [Baritone] when logging to chat."))
    private val echoCommands by setting(this, BooleanSetting(settingName("Echo Commands"), true, { page == Page.VISUAL }))
    private val showOnRadar by setting(this, BooleanSetting(settingName("Show Path on Radar"), true, { page == Page.VISUAL }, "Show the current path on radar."))
    private val color by setting(this, ColorSetting(settingName("Path Color"), ColorRGB(32, 250, 32), { showOnRadar && page == Page.VISUAL }, "Path color for the radar."))

    private val maxFallHeightNoWater by setting(this, IntegerSetting(settingName("Max Fall Height"), 3, 3..1000, 20, { page == Page.FALL }, "Distance baritone can fall without water."))
    private val allowWaterBucketFall by setting(this, BooleanSetting(settingName("Water Bucket Clutch"), true, { page == Page.FALL }, "Uses a water bucket to get down quickly."))
    private val maxFallHeightBucket by setting(this, IntegerSetting(settingName("Max Bucket Height"), 20, 10..250, 10, { allowWaterBucketFall && page == Page.FALL }, "Max height that baritone can use a water bucket."))

    private val buildInLayers by setting(this, BooleanSetting(settingName("Build In Layers"), false, { page == Page.BUILD }, "Build/mine one layer at a time in schematics and selections."))
    private val layerOrder by setting(this, BooleanSetting(settingName("Top To Bottom"), false, { buildInLayers && page == Page.BUILD }, "Build/mine from top to bottom in schematics and selections."))
    private val startAtLayer by setting(this, IntegerSetting(settingName("Start At Layer"), 0, 0..256, 1, { buildInLayers && page == Page.BUILD }, "Start building the schematic at a specific layer."))
    private val layerHeight by setting(this, IntegerSetting(settingName("Layer Height"), 1, 1..50, 1, { buildInLayers && page == Page.BUILD }, "How high should the individual layers be?"))
    private val skipFailedLayers by setting(this, BooleanSetting(settingName("Skip Failed Layers"), false, { buildInLayers && page == Page.BUILD }, "If a layer is unable to be constructed, just skip it."))
    private val buildOnlySelection by setting(this, BooleanSetting(settingName("Build Only Selection"), false, { page == Page.BUILD }, "Only build the selected part of schematics"))
    private val buildIgnoreExisting by setting(this, BooleanSetting(settingName("Build Ignore Existing"), false, { page == Page.BUILD }, "If this is true, the builder will treat all non-air blocks as correct. It will only place new blocks."))
    private val buildIgnoreDirection by setting(this, BooleanSetting(settingName("Build Ignore Direction"), false, { page == Page.BUILD }, "If this is true, the builder will ignore directionality of certain blocks like glazed terracotta."))
    private val mapArtMode by setting(this, BooleanSetting(settingName("Map Art Mode"), false, { page == Page.BUILD }, "Build in map art mode, which makes baritone only care about the top block in each column"))
    private val schematicOrientationX by setting(this, BooleanSetting(settingName("Schematic Orientation X"), false, { page == Page.BUILD }, "When this setting is true, build a schematic with the highest X coordinate being the origin, instead of the lowest"))
    private val schematicOrientationY by setting(this, BooleanSetting(settingName("Schematic Orientation Y"), false, { page == Page.BUILD }, "When this setting is true, build a schematic with the highest Y coordinate being the origin, instead of the lowest"))
    private val schematicOrientationZ by setting(this, BooleanSetting(settingName("Schematic Orientation Z"), false, { page == Page.BUILD }, "When this setting is true, build a schematic with the highest Z coordinate being the origin, instead of the lowest"))
    private val okIfWater by setting(this, BooleanSetting(settingName("Ok If Water"), false, { page == Page.BUILD }, "Override builder's behavior to not attempt to correct blocks that are currently water"))
    private val incorrectSize by setting(this, IntegerSetting(settingName("Incorrect Size"), 100, 1..1000, 1, { page == Page.BUILD }, "The set of incorrect blocks can never grow beyond this size"))

    private val blockReachDistance by setting(this, FloatSetting(settingName("Reach Distance"), 4.5f, 1.0f..10.0f, 0.5f, { page == Page.ADVANCED }, "Max distance baritone can place blocks."))
    private val enterPortals by setting(this, BooleanSetting(settingName("Enter Portals"), true, { page == Page.ADVANCED }, "Baritone will walk all the way into the portal, instead of stopping one block before."))
    private val blockPlacementPenalty by setting(this, IntegerSetting(settingName("Block Placement Penalty"), 20, 0..40, 5, { page == Page.ADVANCED }, "Decrease to make Baritone more often consider paths that would require placing blocks."))
    private val jumpPenalty by setting(this, IntegerSetting(settingName("Jump Penalty"), 2, 0..10, 1, { page == Page.ADVANCED }, "Additional penalty for hitting the space bar (ascend, pillar, or parkour) because it uses hunger."))
    private val assumeWalkOnWater by setting(this, BooleanSetting(settingName("Assume Walk On Water"), false, { page == Page.ADVANCED }, "Allow Baritone to assume it can walk on still water just like any other block. Requires jesus to be enabled."))
    private val failureTimeout by setting(this, IntegerSetting(settingName("Fail Timeout"), 2, 1..20, 1, { page == Page.ADVANCED }))
    private val avoidance by setting(this, BooleanSetting(settingName("Avoidance"), false, { page == Page.ADVANCED }, "Enables the 4 avoidance settings. It's disabled by default because of the noticeable performance impact."))
    private val mobAvoidanceRadius by setting(this, IntegerSetting(settingName("Mob Avoidance Radius"), 15, 0..65, 5, { avoidance && page == Page.ADVANCED }, "Distance to avoid mobs."))
    private val mobAvoidanceCoefficient by setting(this, FloatSetting(settingName("Mob Avoidance Coefficient"), 1.5f, 0.0f..5.0f, 0.5f, { avoidance && page == Page.ADVANCED }, "Set to 1.0 to effectively disable this feature. Set below 1.0 to go out of your way to walk near mobs."))
    private val mobSpawnerAvoidanceRadius by setting(this, IntegerSetting(settingName("Mob Spawner Avoidance Radius"), 10, 0..60, 10, { avoidance && page == Page.ADVANCED }, "Distance to avoid mob spawners."))
    private val mobSpawnerAvoidanceCoefficient by setting(this, FloatSetting(settingName("Mob Spawner Avoidance Coefficient"), 1.5f, 0.0f..5.0f, 0.5f, { avoidance && page == Page.ADVANCED }, "Set to 1.0 to effectively disable this feature. Set below 1.0 to go out of your way to walk near mob spawners."))
    private val preferSilkTouch by setting(this, BooleanSetting(settingName("Prefer Silk Touch"), false, { page == Page.ADVANCED }, "Always prefer silk touch tools over regular tools."))
    private val backfill by setting(this, BooleanSetting(settingName("Backfill"), false, { page == Page.ADVANCED }, "Fill in blocks behind you"))
    private val chunkCaching by setting(this, BooleanSetting(settingName("Chunk Caching"), true, { page == Page.ADVANCED }, "Download all chunks in simplified 2-bit format and save them for better very-long-distance pathing."))
    private val assumeStep by setting(this, BooleanSetting(settingName("Assume Step"), false, { page == Page.ADVANCED }, "Assume step functionality; don't jump on an Ascend."))
    private val assumeExternalAutoTool by setting(this, BooleanSetting(settingName("Assume External AutoTool"), false, { page == Page.ADVANCED }, "Disable baritone's auto-tool at runtime, but still assume that another mod will provide auto tool functionality"))
    private val autoTool by setting(this, BooleanSetting(settingName("AutoTool"), true, { page == Page.ADVANCED }, "Automatically select the best available tool"))
    private val assumeSafeWalk by setting(this, BooleanSetting(settingName("Assume Safe Walk"), false, { page == Page.ADVANCED }, "Assume safe walk functionality; don't sneak on a backplace traverse."))
    private val allowJumpAt256 by setting(this, BooleanSetting(settingName("Allow Jump At 256"), false, { page == Page.ADVANCED }, "If true, parkour is allowed to make jumps when standing on blocks at the maximum height, so player feet is y=256"))
    private val allowDiagonalDescend by setting(this, BooleanSetting(settingName("Allow Diagonal Descend"), false, { page == Page.ADVANCED }, "Allow descending diagonally. Safer than allowParkour yet still slightly unsafe, can make contact with unchecked adjacent blocks, so it's unsafe in the nether."))
    private val allowDiagonalAscend by setting(this, BooleanSetting(settingName("Allow Diagonal Ascend"), false, { page == Page.ADVANCED }, "Allow diagonal ascending. Actually pretty safe, much safer than diagonal descend tbh"))
    private val allowWalkOnBottomSlab by setting(this, BooleanSetting(settingName("Allow Walk On Bottom Slab"), false, { page == Page.ADVANCED }, "Slab behavior is complicated, disable this for higher path reliability. Leave enabled if you have bottom slabs everywhere in your base."))
    private val allowVines by setting(this, BooleanSetting(settingName("Advanced Vines"), false, { page == Page.ADVANCED }, "Enables some more advanced vine features. They're honestly just gimmicks and won't ever be needed in real pathing scenarios. And they can cause Baritone to get trapped indefinitely in a strange scenario."))

    init {
        settingList.forEach { setting ->
            (setting as AbstractSetting<*>).listeners.add { sync() }
        }

        listener<BaritoneSettingsInitEvent> {
            sync()
        }
    }

    private fun sync() {
        BaritoneUtils.settings?.let {
            it.prefix.value = prefix
            it.allowBreak.value = allowBreak
            it.allowSprint.value = allowSprint
            it.allowPlace.value = allowPlace
            it.allowInventory.value = allowInventory
            it.allowDownward.value = allowDownwardTunneling
            it.allowParkour.value = allowParkour
            it.allowParkourPlace.value = allowParkourPlace
            it.chatControl.value = false
            it.antiCheatCompatibility.value = antiCheatCompat
            it.freeLook.value = freeLook
            it.renderGoal.value = renderGoal
            it.censorCoordinates.value = censorCoordinates
            it.censorRanCommands.value = censorRanCommands
            it.shortBaritonePrefix.value = shortBaritonePrefix
            it.echoCommands.value = echoCommands
            it.maxFallHeightNoWater.value = maxFallHeightNoWater
            it.allowWaterBucketFall.value = allowWaterBucketFall
            it.maxFallHeightBucket.value = maxFallHeightBucket
            it.buildInLayers.value = buildInLayers
            it.layerOrder.value = layerOrder
            it.layerHeight.value = layerHeight
            it.startAtLayer.value = startAtLayer
            it.skipFailedLayers.value = skipFailedLayers
            it.buildOnlySelection.value = buildOnlySelection
            it.buildIgnoreExisting.value = buildIgnoreExisting
            it.buildIgnoreDirection.value = buildIgnoreDirection
            it.mapArtMode.value = mapArtMode
            it.schematicOrientationX.value = schematicOrientationX
            it.schematicOrientationY.value = schematicOrientationY
            it.schematicOrientationZ.value = schematicOrientationZ
            it.okIfWater.value = okIfWater
            it.incorrectSize.value = incorrectSize
            it.preferSilkTouch.value = preferSilkTouch
            it.backfill.value = backfill
            it.chunkCaching.value = chunkCaching
            it.blockReachDistance.value = blockReachDistance
            it.enterPortal.value = enterPortals
            it.blockPlacementPenalty.value = blockPlacementPenalty.toDouble()
            it.jumpPenalty.value = jumpPenalty.toDouble()
            it.assumeWalkOnWater.value = assumeWalkOnWater
            it.assumeStep.value = assumeStep
            it.assumeExternalAutoTool.value = assumeExternalAutoTool
            it.autoTool.value = autoTool
            it.assumeSafeWalk.value = assumeSafeWalk
            it.allowJumpAt256.value = allowJumpAt256
            it.allowDiagonalDescend.value = allowDiagonalDescend
            it.allowDiagonalAscend.value = allowDiagonalAscend
            it.failureTimeoutMS.value = failureTimeout.toLong() * 1000L
            it.avoidance.value = avoidance
            it.mobAvoidanceRadius.value = mobAvoidanceRadius
            it.mobAvoidanceCoefficient.value = mobAvoidanceCoefficient.toDouble()
            it.mobSpawnerAvoidanceRadius.value = mobSpawnerAvoidanceRadius
            it.mobSpawnerAvoidanceCoefficient.value = mobSpawnerAvoidanceCoefficient.toDouble()
            it.allowVines.value = allowVines
            it.allowWalkOnBottomSlab.value = allowWalkOnBottomSlab
        }
    }

    private enum class Page { BASIC, VISUAL, FALL, BUILD, ADVANCED }
}
