package dev.wizard.meta.module.modules.render

import com.google.gson.reflect.TypeToken
import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.sort.ObjectIntrosort
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.color.ColorUtils
import dev.wizard.meta.graphics.esp.StaticBoxRenderer
import dev.wizard.meta.graphics.esp.StaticTracerRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.collection.CollectionSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.accessor.palette
import dev.wizard.meta.util.accessor.storage
import dev.wizard.meta.util.threads.BackgroundScope
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.BlockStateContainer
import org.lwjgl.opengl.GL11
import java.util.*

object Search : Module(
    "Search",
    category = Category.RENDER,
    description = "Highlights blocks in the world"
) {
    private val defaultSearchList = linkedSetOf("minecraft:portal", "minecraft:end_portal_frame", "minecraft:bed")

    private val forceUpdateDelay by setting(this, IntegerSetting(settingName("Force Update Delay"), 250, 50..3000, 10))
    private val updateDelay by setting(this, IntegerSetting(settingName("Update Delay"), 50, 5..500, 5))
    private val range by setting(this, IntegerSetting(settingName("Range"), 128, 0..256, 8))
    private val maximumBlocks by setting(this, IntegerSetting(settingName("Maximum Blocks"), 512, 128..8192, 128))

    private val filled by setting(this, BooleanSetting(settingName("Filled"), true))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true))
    private val tracer by setting(this, BooleanSetting(settingName("Tracer"), true))
    private val customColors by setting(this, BooleanSetting(settingName("Custom Colors"), true))
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255), { customColors }))
    private val filledAlpha by setting(this, IntegerSetting(settingName("Filled Alpha"), 63, 0..255, 1, filled.atTrue()))
    private val outlineAlpha by setting(this, IntegerSetting(settingName("Outline Alpha"), 200, 0..255, 1, outline.atTrue()))
    private val tracerAlpha by setting(this, IntegerSetting(settingName("Tracer Alpha"), 200, 0..255, 1, tracer.atTrue()))
    private val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 0.25f..5.0f, 0.25f))

    val searchList = setting(this, CollectionSetting(settingName("Search List"), defaultSearchList, object : TypeToken<LinkedHashSet<String>>() {}.type))

    private var blockSet: ObjectSet<Block> = ObjectSets.emptySet()
    private val boxRenderer = StaticBoxRenderer()
    private val tracerRenderer = StaticTracerRenderer()
    private val updateTimer = TickTimer()
    private var dirty = false
    private var lastUpdatePos: BlockPos? = null
    private var lastUpdateJob: Job? = null
    private val gcTimer = TickTimer()

    private val cachedMainList = DoubleBuffered { FastObjectArrayList<BlockRenderInfo>(16) }
    private var cachedSublistPool = ConcurrentObjectPool { FastObjectArrayList<BlockRenderInfo>(16) }

    override fun getHudInfo(): String = boxRenderer.size.toString()

    init {
        onEnable {
            updateTimer.reset(-114514L)
        }

        onDisable {
            dirty = true
            lastUpdatePos = null
            boxRenderer.clear()
            tracerRenderer.clear()
            cachedMainList.front.clearAndTrim()
            cachedMainList.back.clearAndTrim()
            cachedSublistPool = ConcurrentObjectPool { FastObjectArrayList<BlockRenderInfo>(16) }
        }

        safeListener<WorldEvent.ClientBlockUpdate> {
            val eyeX = player.posX.floorToInt()
            val eyeY = (player.posY + player.eyeHeight).floorToInt()
            val eyeZ = player.posZ.floorToInt()

            if (it.pos.distanceSqTo(eyeX, eyeY, eyeZ) <= range * range && (blockSet.contains(it.oldState.block) || blockSet.contains(it.newState.block))) {
                dirty = true
            }
        }

        safeListener<Render3DEvent> {
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
            GlStateManager.glLineWidth(width)
            GlStateUtils.depth(false)

            boxRenderer.render(if (filled) filledAlpha else 0, if (outline) outlineAlpha else 0)
            tracerRenderer.render(if (tracer) tracerAlpha else 0)

            GlStateUtils.depth(true)
            GlStateManager.glLineWidth(1.0f)

            val playerPos = EntityUtils.getFlooredPosition(player)
            if ((lastUpdateJob == null || lastUpdateJob!!.isCompleted) && (updateTimer.tick(forceUpdateDelay.toLong()) || (updateTimer.tick(updateDelay.toLong()) && (dirty || playerPos != lastUpdatePos)))) {
                updateRenderer(this)
                dirty = false
                lastUpdatePos = playerPos
                updateTimer.reset()
            }
        }

        searchList.editListeners.add {
            val newSet = ObjectOpenHashSet<Block>()
            it.forEach { name ->
                val block = Block.getBlockFromName(name)
                if (block != null && block != Blocks.AIR) newSet.add(block)
            }
            if (blockSet.size != newSet.size || !blockSet.containsAll(newSet)) {
                dirty = true
            }
            blockSet = newSet
        }
    }

    private fun updateRenderer(event: SafeClientEvent) {
        lastUpdateJob = BackgroundScope.launch {
            val cleanList = gcTimer.tickAndReset(1000L)
            val eyeX = event.player.posX.floorToInt()
            val eyeY = (event.player.posY + event.player.eyeHeight.toDouble()).floorToInt()
            val eyeZ = event.player.posZ.floorToInt()
            val renderDist = event.mc.gameSettings.renderDistanceChunks
            val playerChunkPosX = eyeX shr 4
            val playerChunkPosZ = eyeZ shr 4
            val rangeVal = range
            val rangeSq = rangeVal * rangeVal
            val maxChunkRange = rangeSq + 256

            val actor = actor<FastObjectArrayList<BlockRenderInfo>> {
                for (sublist in channel) {
                    merge(cachedMainList.front, sublist, cachedMainList.back)
                    cachedMainList.swap()
                    clearList(cleanList, sublist)
                    cachedSublistPool.put(sublist)
                }

                val pos = BlockPos.MutableBlockPos()
                tracerRenderer.update {
                    boxRenderer.update {
                        val mainList = cachedMainList.front
                        val elements = mainList.elements()
                        for (i in 0 until mainList.size().coerceAtMost(maximumBlocks)) {
                            val info = elements[i]
                            pos.setPos(info.x, info.y, info.z)
                            val state = event.world.getBlockState(pos)
                            val box = state.getSelectedBoundingBox(event.world, pos)
                            val color = getBlockColor(event, pos, state)
                            putBox(box, color)
                            this@update.putTracer(box, color)
                        }
                    }
                }
                clearList(cleanList, cachedMainList.front)
                clearList(cleanList, cachedMainList.back)
            }

            coroutineScope {
                for (x in playerChunkPosX - renderDist..playerChunkPosX + renderDist) {
                    for (z in playerChunkPosZ - renderDist..playerChunkPosZ + renderDist) {
                        val chunk = event.world.getChunk(x, z)
                        if (chunk.isLoaded && DistanceKt.distanceSq(eyeX, eyeZ, (x shl 4) + 8, (z shl 4) + 8) <= maxChunkRange) {
                            launch {
                                findBlocksInChunk(actor, chunk, eyeX, eyeY, eyeZ, rangeSq)
                            }
                        }
                    }
                }
            }
            actor.close()
        }
    }

    private suspend fun findBlocksInChunk(actor: SendChannel<FastObjectArrayList<BlockRenderInfo>>, chunk: net.minecraft.world.chunk.Chunk, eyeX: Int, eyeY: Int, eyeZ: Int, rangeSq: Int) {
        val xStart = chunk.x shl 4
        val zStart = chunk.z shl 4
        val list = cachedSublistPool.get()

        for (storage in chunk.blockStorageArray) {
            if (storage == null || storage.isEmpty) continue
            val yStart = storage.yLocation
            val container = storage.data
            val bitArray = container.storage
            val palette = container.palette

            for (i in 0 until 4096) {
                val state = palette.getBlockState(bitArray.getAt(i)) ?: continue
                if (blockSet.contains(state.block)) {
                    val x = xStart + (i and 15)
                    val y = yStart + (i shr 8 and 15)
                    val z = zStart + (i shr 4 and 15)
                    val dist = DistanceKt.distanceSq(eyeX, eyeY, eyeZ, x, y, z)
                    if (dist <= rangeSq) {
                        list.add(BlockRenderInfo(x, y, z, dist))
                    }
                }
            }
        }

        if (!list.isEmpty) {
            ObjectIntrosort.sort(list.elements(), 0, list.size())
            actor.send(list)
        } else {
            cachedSublistPool.put(list)
        }
    }

    private fun merge(mainList: FastObjectArrayList<BlockRenderInfo>, sublist: FastObjectArrayList<BlockRenderInfo>, outputList: FastObjectArrayList<BlockRenderInfo>) {
        outputList.clearFast()
        outputList.ensureCapacity(mainList.size() + sublist.size())
        val mainElements = mainList.elements()
        val subElements = sublist.elements()
        val outElements = outputList.elements()
        var i = 0
        var j = 0
        var k = 0
        while (i < mainList.size() && j < sublist.size()) {
            val a = mainElements[i]
            val b = subElements[j]
            if (a.compareTo(b) < 0) {
                outElements[k++] = a
                i++
            } else {
                outElements[k++] = b
                j++
            }
        }
        while (i < mainList.size()) outElements[k++] = mainElements[i++]
        while (j < sublist.size()) outElements[k++] = subElements[j++]
        outputList.setSize(k)
    }

    private fun clearList(clean: Boolean, list: FastObjectArrayList<BlockRenderInfo>) {
        if (clean) {
            val prevSize = list.size()
            list.clear()
            list.trim(prevSize)
        } else {
            list.clearFast()
        }
    }

    private fun getBlockColor(event: SafeClientEvent, pos: BlockPos, state: IBlockState): ColorRGB {
        if (!customColors) {
            if (state.block == Blocks.PORTAL) return ColorRGB(82, 49, 153)
            val colorArgb = state.getMapColor(event.world, pos).colorValue
            return ColorRGB(ColorUtils.argbToRgba(colorArgb)).withAlpha(255)
        }
        return color
    }

    private class BlockRenderInfo(val x: Int, val y: Int, val z: Int, val dist: Int) : Comparable<BlockRenderInfo> {
        override fun compareTo(other: BlockRenderInfo): Int = dist.compareTo(other.dist)
    }
}
