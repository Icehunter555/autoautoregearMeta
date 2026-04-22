package dev.wizard.meta.graphics

import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

object IconUtils {
    @Throws(IOException::class)
    fun readImageToBuffer(`in`: InputStream): ByteBuffer {
        val bufferedimage: BufferedImage = ImageIO.read(`in`)
        val pixelIndex = bufferedimage.getRGB(
            0, 0, bufferedimage.width, bufferedimage.height,
            null, 0, bufferedimage.width
        )
        val bytebuffer = ByteBuffer.allocate(4 * pixelIndex.size)
        for (pixel in pixelIndex) {
            bytebuffer.putInt(pixel shl 8 or (pixel shr 24 and 0xFF))
        }
        bytebuffer.flip()
        return bytebuffer
    }
}