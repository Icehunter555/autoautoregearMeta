package dev.wizard.meta.graphics.texture

import dev.luna5ama.kmogus.*
import dev.wizard.meta.graphics.GLFunctionsKt
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.math.MathUtils
import net.minecraft.client.resources.IResource
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object TextureUtils {
    private var bufferID = 0
    private var bufferSize = -1L

    fun uploadRGBA(bufferedImage: BufferedImage, format: Int) {
        uploadRGBA(getRGB(bufferedImage), format, bufferedImage.width, bufferedImage.height)
    }

    fun uploadRGBA(data: IntArray, format: Int, width: Int, height: Int) {
        putData(data)
        GL15.glBindBuffer(35052, bufferID)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0, 32993, 33639, 0L)
        GL15.glBindBuffer(35052, 0)
    }

    fun uploadAlpha(bufferedImage: BufferedImage) {
        uploadAlpha(getAlpha(bufferedImage), bufferedImage.width, bufferedImage.height)
    }

    fun uploadAlpha(data: ByteArray, width: Int, height: Int) {
        putData(data)
        GL15.glBindBuffer(35052, bufferID)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, width, height, 0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, 0L)
        GL15.glBindBuffer(35052, 0)
    }

    fun uploadRed(bufferedImage: BufferedImage) {
        uploadRed(getAlpha(bufferedImage), bufferedImage.width, bufferedImage.height)
    }

    fun uploadRed(data: ByteArray, width: Int, height: Int) {
        putData(data)
        GL15.glBindBuffer(35052, bufferID)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 33321, width, height, 0, 6403, GL11.GL_UNSIGNED_BYTE, 0L)
        GL15.glBindBuffer(35052, 0)
    }

    fun getAlpha(image: BufferedImage): ByteArray {
        val numBands = image.raster.numBands
        val dataType = image.raster.dataBuffer.dataType
        val data = when (dataType) {
            0 -> ByteArray(numBands)
            1 -> ShortArray(numBands)
            3 -> IntArray(numBands)
            4 -> FloatArray(numBands)
            5 -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }
        val alphaArray = ByteArray(image.width * image.height)
        var index = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                alphaArray[index++] = image.colorModel.getAlpha(image.raster.getDataElements(x, y, data)).toByte()
            }
        }
        return alphaArray
    }

    fun getRGB(image: BufferedImage): IntArray {
        val numBands = image.raster.numBands
        val dataType = image.raster.dataBuffer.dataType
        val data = when (dataType) {
            0 -> ByteArray(numBands)
            1 -> ShortArray(numBands)
            3 -> IntArray(numBands)
            4 -> FloatArray(numBands)
            5 -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }
        val rgbArray = IntArray(image.width * image.height)
        var index = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                rgbArray[index++] = image.colorModel.getRGB(image.raster.getDataElements(x, y, data))
            }
        }
        return rgbArray
    }

    private fun putData(data: ByteArray) {
        val length = data.size.toLong()
        checkBuffer(length)
        val arr = GLFunctionsKt.glMapNamedBufferRange(bufferID, 0L, length, 2)
        MemcpyKt.memcpy(data, arr.ptr, length)
        GL45.glUnmapNamedBuffer(bufferID)
    }

    private fun putData(data: IntArray) {
        val length = (data.size * 4).toLong()
        checkBuffer(length)
        val arr = GLFunctionsKt.glMapNamedBufferRange(bufferID, 0L, length, 2)
        MemcpyKt.memcpy(data, arr.ptr, length)
        GL45.glUnmapNamedBuffer(bufferID)
    }

    private fun checkBuffer(capacity: Long) {
        if (capacity > bufferSize) {
            if (bufferID != 0) {
                GL15.glDeleteBuffers(bufferID)
            }
            bufferID = GL45.glCreateBuffers()
            GL45.glNamedBufferStorage(bufferID, maxOf(capacity, bufferSize * 2), 2)
            bufferSize = capacity
        } else {
            GL43.glInvalidateBufferData(bufferID)
        }
    }

    @JvmStatic
    fun combineTexturesVertically(images: Array<BufferedImage>): BufferedImage {
        check(images.isNotEmpty())
        val firstImage = images[0]
        val height = firstImage.height
        val totalHeight = MathUtils.ceilToPOT(height * images.size)
        val finalImage = BufferedImage(firstImage.width, totalHeight, firstImage.type)
        val graphics = finalImage.createGraphics()
        images.forEachIndexed { i, src ->
            graphics.drawImage(src, 0, height * i, null)
        }
        graphics.dispose()
        return finalImage
    }

    @JvmStatic
    fun ResourceLocation.readImage(): BufferedImage {
        val resource = Wrapper.getMinecraft().resourceManager.getResource(this)
        return readImage(resource)
    }

    @JvmStatic
    fun readImage(resource: IResource): BufferedImage {
        return resource.inputStream.use { ImageIO.read(it) }
    }
}
