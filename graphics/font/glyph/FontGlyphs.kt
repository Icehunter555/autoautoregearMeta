package dev.wizard.meta.graphics.font.glyph

import dev.fastmc.common.ParallelUtils
import dev.fastmc.common.UpdateCounter
import dev.luna5ama.kmogus.*
import dev.wizard.meta.graphics.GLFunctionsKt
import dev.wizard.meta.graphics.font.GlyphCache
import dev.wizard.meta.graphics.font.GlyphTexture
import dev.wizard.meta.graphics.font.Style
import dev.wizard.meta.graphics.texture.BC4Compression
import dev.wizard.meta.graphics.texture.Mipmaps
import dev.wizard.meta.graphics.texture.RawImage
import dev.wizard.meta.util.threads.BackgroundScope
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.threads.ThreadSafetyKt.onMainThread
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import net.minecraft.client.renderer.OpenGlHelper
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL45
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.math.max

class FontGlyphs(val id: Int, private val font: Font, private val fallbackFont: Font, private val textureSize: Int) {
    private val chunkArray = arrayOfNulls<GlyphChunk>(128)
    private val loadingFlags = AtomicIntegerArray(127)
    private val mainChunk: GlyphChunk
    private val loadingLimiter = Semaphore(4)
    private val uploadMutex = Mutex()
    private val updateCounter = UpdateCounter()
    val fontHeight: Float

    init {
        val texture = GlyphTexture(textureSize, textureSize, 4)
        val cacheEntry = runBlocking {
            withContext(DefaultScope.context) {
                GlyphCache.get(font, 0) ?: createNewGlyph(0)
            }
        }

        val bufferID = GL45.glCreateBuffers()
        GL45.glNamedBufferStorage(bufferID, cacheEntry.data.size.toLong(), 2) // GL_DYNAMIC_STORAGE_BIT
        val buffer = GLFunctionsKt.glMapNamedBufferRange(bufferID, 0L, cacheEntry.data.size.toLong(), 2)
        MemcpyKt.memcpy(cacheEntry.data, buffer.ptr, cacheEntry.data.size.toLong())
        GL45.glUnmapNamedBuffer(bufferID)

        OpenGlHelper.func_176072_g(35052, bufferID) // GL_PIXEL_UNPACK_BUFFER
        var offset = 0L
        var size = cacheEntry.baseSize
        for (i in 0..cacheEntry.levels) {
            val a = offset
            val s = size
            offset += size
            size = size shr 2
            GL45.glCompressedTextureSubImage2D(
                texture.textureID, i, 0, 0,
                cacheEntry.width shr i, cacheEntry.height shr i,
                36283, s, a // GL_COMPRESSED_RED_RGTC1
            )
        }
        OpenGlHelper.func_176072_g(35052, 0)
        GL15.glDeleteBuffers(bufferID)

        mainChunk = GlyphChunk(0, texture, cacheEntry.charInfoArray)
        chunkArray[0] = mainChunk

        fontHeight = mainChunk.charInfoArray.maxOfOrNull { it.height } ?: font.size.toFloat()
    }

    fun getCharInfo(c: Char): CharInfo {
        val chunkID = c.code shr 9
        return getChunk(chunkID).charInfoArray[c.code - (chunkID shl 9)]
    }

    fun getChunk(c: Char): GlyphChunk {
        return getChunk(c.code shr 9)
    }

    fun getChunk(chunkID: Int): GlyphChunk {
        if (chunkID !in chunkArray.indices) return mainChunk
        val chunk = chunkArray[chunkID]
        if (chunk == null) {
            val loadingID = chunkID - 1
            if (loadingID >= 0 && loadingFlags.getAndSet(loadingID, 1) == 0) {
                BackgroundScope.launch {
                    loadingLimiter.withPermit {
                        val newChunk = loadGlyphChunkAsync(chunkID)
                        chunkArray[chunkID] = newChunk
                        updateCounter.update()
                    }
                }
            }
            return mainChunk
        }
        return chunk
    }

    fun destroy() {
        for (i in chunkArray.indices) {
            val chunk = chunkArray[i]
            chunk?.texture?.deleteTexture()
            chunkArray[i] = null
        }
    }

    fun checkUpdate(): Boolean {
        return updateCounter.check()
    }

    private suspend fun loadGlyphChunkAsync(chunkID: Int): GlyphChunk = coroutineScope {
        val deferredTexture = onMainThread {
            GlyphTexture(textureSize, textureSize, 4)
        }
        val cache = GlyphCache.get(font, chunkID) ?: createNewGlyph(chunkID)

        val deferredBuffer = onMainThread {
            val bufferID = GL45.glCreateBuffers()
            GL45.glNamedBufferStorage(bufferID, cache.data.size.toLong(), 2)
            val buffer = GLFunctionsKt.glMapNamedBufferRange(bufferID, 0L, cache.data.size.toLong(), 2)
            bufferID to buffer
        }

        val (bufferID, buffer) = deferredBuffer.await()
        MemcpyKt.memcpy(cache.data, buffer.ptr, cache.data.size.toLong())

        onMainThread {
            GL45.glUnmapNamedBuffer(bufferID)
        }.await()

        val texture = deferredTexture.await()
        var offset = 0L
        var size = cache.baseSize
        for (i in 0..cache.levels) {
            val a = offset
            val s = size
            offset += size
            size = size shr 2
            uploadSmoothed {
                OpenGlHelper.func_176072_g(35052, bufferID)
                GL45.glCompressedTextureSubImage2D(
                    texture.textureID, i, 0, 0,
                    cache.width shr i, cache.height shr i,
                    36283, s, a
                )
                OpenGlHelper.func_176072_g(35052, 0)
            }
        }

        onMainThread {
            GL15.glDeleteBuffers(bufferID)
        }

        GlyphChunk(chunkID, texture, cache.charInfoArray)
    }

    private suspend fun uploadSmoothed(block: () -> Unit) {
        uploadMutex.withLock {
            onMainThread(block).join()
        }
    }

    private suspend fun createNewGlyph(chunkID: Int): GlyphCache.Entry {
        val image = BufferedImage(textureSize, textureSize, 10) // TYPE_BYTE_GRAY
        val charInfoArray = drawGlyphs(image, chunkID shl 9)

        val outputData = dumpAlphaParallel(image)
        val rawImage = RawImage(outputData, image.width, image.height, 1)

        val data = ByteArray(BC4Compression.getEncodedSize(Mipmaps.getTotalSize(rawImage.data.size, 4)))

        coroutineScope {
            var offset = 0
            Mipmaps.generate(rawImage, 4).collect { it ->
                val size = BC4Compression.getEncodedSize(it.data.size)
                val i = offset
                offset += size
                launch(Dispatchers.Default) {
                    val view = ByteBuffer.wrap(data, i, size).order(ByteOrder.nativeOrder())
                    BC4Compression.encode(it, view)
                }
            }
        }

        val entry = GlyphCache.Entry(512, image.width, image.height, 4, BC4Compression.getEncodedSize(rawImage.data.size), charInfoArray, data)
        BackgroundScope.launch {
            GlyphCache.put(font, chunkID, entry)
        }
        return entry
    }

    private fun drawGlyphs(bufferedImage: BufferedImage, chunkStart: Int): Array<CharInfo> {
        val graphics2D = bufferedImage.createGraphics()
        graphics2D.background = Color.BLACK
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        var rowHeight = 0
        var positionX = 0
        var positionY = 0
        val textureSizeFloat = textureSize.toFloat()
        val xPadding = if (id == Style.ITALIC.ordinal) 12 else 16
        val extraWidth = if (id == Style.ITALIC.ordinal) 4.0f else 0.0f
        val charInfoArray = arrayOfNulls<CharInfo>(512)

        for (i in 0 until 512) {
            val c = (chunkStart + i).toChar()
            graphics2D.font = if (font.canDisplay(c)) font else fallbackFont
            val fontMetrics = graphics2D.fontMetrics
            val w = fontMetrics.charWidth(c)
            val h = fontMetrics.height

            val width = if (w > 0) w else 8
            val height = if (h > 0) h else font.size

            if (positionX + width >= textureSize) {
                positionX = 1
                positionY += rowHeight + 8
                rowHeight = 0
            }

            graphics2D.drawString(c.toString(), positionX, positionY + fontMetrics.ascent)
            val charInfo = CharInfo(textureSizeFloat, width, height, positionX, positionY, extraWidth)
            if (height > rowHeight) {
                rowHeight = height
            }
            positionX += width + xPadding
            charInfoArray[i] = charInfo
        }

        graphics2D.dispose()
        @Suppress("UNCHECKED_CAST")
        return charInfoArray as Array<CharInfo>
    }

    private suspend fun dumpAlphaParallel(image: BufferedImage): ByteArray = coroutineScope {
        val size = image.width * image.height
        val outputData = ByteArray(size)
        val parallelism = ParallelUtils.CPU_THREADS
        val minSize = 128
        val parallelSize = max(size / parallelism, minSize)
        val combineThreshold = parallelSize / 2

        if (size <= parallelSize + combineThreshold) {
            launch {
                val data = ByteArray(1)
                for (i in 0 until size) {
                    image.raster.getDataElements(i % image.width, i / image.width, data)
                    outputData[i] = data[0]
                }
            }
        } else {
            var index = 0
            while (index < size) {
                val start = index
                var end = index + parallelSize
                val remaining = size - end
                if (remaining < 0 || remaining < combineThreshold) {
                    end = size
                }
                launch {
                    val data = ByteArray(1)
                    for (i in start until end) {
                        image.raster.getDataElements(i % image.width, i / image.width, data)
                        outputData[i] = data[0]
                    }
                }
                index = end
            }
        }
        outputData
    }
}
