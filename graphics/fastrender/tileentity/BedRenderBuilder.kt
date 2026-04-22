package dev.wizard.meta.graphics.fastrender.tileentity

import dev.wizard.meta.graphics.fastrender.model.tileentity.ModelBed
import dev.wizard.meta.graphics.texture.TextureUtils
import dev.wizard.meta.util.interfaces.Helper
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntityBed
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class BedRenderBuilder(builtPosX: Double, builtPosY: Double, builtPosZ: Double) : AbstractTileEntityRenderBuilder<TileEntityBed>(builtPosX, builtPosY, builtPosZ) {

    override fun add(tileEntity: TileEntityBed) {
        val pos = tileEntity.pos
        val posX = (pos.x + 0.5 - builtPosX).toFloat()
        val posY = (pos.y - builtPosY).toFloat()
        val posZ = (pos.z + 0.5 - builtPosZ).toFloat()
        val isHead = if (tileEntity.hasWorld()) tileEntity.isHeadPiece else true

        floatArrayList.add(posX)
        floatArrayList.add(posY)
        floatArrayList.add(posZ)
        putTileEntityLightMapUV(tileEntity)

        val rotation = when (getTileEntityBlockMetadata(tileEntity) and 3) {
            1 -> -1
            2 -> 0
            3 -> 1
            else -> 2
        }
        byteArrayList.add(rotation.toByte())
        byteArrayList.add(tileEntity.color.metadata.toByte())
        byteArrayList.add(if (isHead) 1.toByte() else 0.toByte())
        size++
    }

    override fun uploadBuffer(vboBuffer: ByteBuffer): AbstractTileEntityRenderBuilder.Renderer {
        val vaoID = GL30.glGenVertexArrays()
        val vboID = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboBuffer, GL15.GL_STATIC_DRAW)
        GL30.glBindVertexArray(vaoID)

        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 20, 0L)
        GL20.glVertexAttribPointer(5, 2, GL11.GL_UNSIGNED_BYTE, true, 20, 12L)
        GL30.glVertexAttribIPointer(6, 1, GL11.GL_BYTE, 20, 14L)
        GL30.glVertexAttribIPointer(7, 1, GL11.GL_UNSIGNED_BYTE, 20, 15L)
        GL30.glVertexAttribIPointer(8, 1, GL11.GL_UNSIGNED_BYTE, 20, 16L)

        GL33.glVertexAttribDivisor(4, 1)
        GL33.glVertexAttribDivisor(5, 1)
        GL33.glVertexAttribDivisor(6, 1)
        GL33.glVertexAttribDivisor(7, 1)
        GL33.glVertexAttribDivisor(8, 1)

        model.attachVBO()
        GL20.glEnableVertexAttribArray(4)
        GL20.glEnableVertexAttribArray(5)
        GL20.glEnableVertexAttribArray(6)
        GL20.glEnableVertexAttribArray(7)
        GL20.glEnableVertexAttribArray(8)

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)

        return Renderer(shader, vaoID, vboID, model.modelSize, size, builtPosX, builtPosY, builtPosZ)
    }

    override fun buildBuffer(): ByteBuffer {
        val buffer = GLAllocation.createDirectByteBuffer(size * 20)
        var floatIndex = 0
        var byteIndex = 0
        for (i in 0 until size) {
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
        }
        buffer.flip()
        return buffer
    }

    private class Renderer(
        shader: AbstractTileEntityRenderBuilder.Shader,
        vaoID: Int,
        vboID: Int,
        modelSize: Int,
        size: Int,
        builtPosX: Double,
        builtPosY: Double,
        builtPosZ: Double
    ) : AbstractTileEntityRenderBuilder.Renderer(shader, vaoID, vboID, modelSize, size, builtPosX, builtPosY, builtPosZ), Helper {
        override fun preRender() {
            GlStateManager.bindTexture(BedTexture.instance.texture.glTextureId)
        }

        override fun postRender() {
            GlStateManager.bindTexture(0)
        }
    }

    private class BedTexture(val hash: Int) : Helper {
        val texture = DynamicTexture(transformTextures())

        private fun transformBedTexture(oldImage: BufferedImage): BufferedImage {
            val newImage = BufferedImage(oldImage.width, oldImage.height, oldImage.type)
            val scale = oldImage.width / 64
            val g = newImage.createGraphics()
            val identity = AffineTransform()
            drawHead(g, oldImage, scale, identity)
            drawFoot(g, oldImage, scale, identity)
            drawLeg0(g, oldImage, scale)
            drawLeg1(g, oldImage, scale)
            drawLeg2(g, oldImage, scale)
            drawLeg3(g, oldImage, scale)
            g.dispose()
            return newImage
        }

        private fun transformTextures(): BufferedImage {
            val images = Array(EnumDyeColor.values().size) { i ->
                val resource = mc.resourceManager.getResource(textures[i])
                transformBedTexture(TextureUtils.readImage(resource))
            }
            val first = images[0]
            val size = first.width
            val finalImage = BufferedImage(size * 4, size * 4, first.type)
            val g = finalImage.createGraphics()
            for (x in 0 until 4) {
                for (y in 0 until 4) {
                    g.drawImage(images[x + y * 4], x * size, y * size, null)
                }
            }
            g.dispose()
            return finalImage
        }

        private fun drawHead(g: Graphics2D, old: BufferedImage, s: Int, id: AffineTransform) {
            g.rotate(-Math.PI / 2, 0.0, 22.0 * s)
            g.drawImage(old, 0, 22 * s, 6 * s, 38 * s, 0, 6 * s, 6 * s, 22 * s, null)
            g.transform = id
            g.rotate(-Math.PI, 48.0 * s, 16.0 * s)
            g.drawImage(old, 32 * s, 10 * s, 48 * s, 16 * s, 6 * s, 0, 22 * s, 6 * s, null)
            g.transform = id
            g.rotate(Math.PI / 2, 48.0 * s, 22.0 * s)
            g.drawImage(old, 42 * s, 22 * s, 48 * s, 38 * s, 22 * s, 6 * s, 28 * s, 22 * s, null)
            g.transform = id
            g.drawImage(old, 16 * s, 0, 32 * s, 16 * s, 6 * s, 6 * s, 22 * s, 22 * s, null)
            g.drawImage(old, 32 * s, 0, 48 * s, 16 * s, 28 * s, 28 * s, 44 * s, 44 * s, null)
        }

        private fun drawFoot(g: Graphics2D, old: BufferedImage, s: Int, id: AffineTransform) {
            g.rotate(-Math.PI / 2, 0.0, 44.0 * s)
            g.drawImage(old, 0, 44 * s, 6 * s, 60 * s, 0, 28 * s, 6 * s, 44 * s, null)
            g.transform = id
            g.rotate(Math.PI, 16.0 * s, 38.0 * s)
            g.drawImage(old, 0, 32 * s, 16 * s, 38 * s, 22 * s, 22 * s, 38 * s, 28 * s, null)
            g.transform = id
            g.rotate(Math.PI / 2, 48.0 * s, 44.0 * s)
            g.drawImage(old, 42 * s, 44 * s, 48 * s, 60 * s, 22 * s, 28 * s, 28 * s, 44 * s, null)
            g.transform = id
            g.drawImage(old, 16 * s, 22 * s, 32 * s, 38 * s, 6 * s, 28 * s, 22 * s, 44 * s, null)
            g.drawImage(old, 32 * s, 22 * s, 48 * s, 38 * s, 28 * s, 28 * s, 44 * s, 44 * s, null)
        }

        private fun drawLeg0(g: Graphics2D, old: BufferedImage, s: Int) {
            g.drawImage(old, 3 * s, 44 * s, 9 * s, 47 * s, 53 * s, 0, 59 * s, 3 * s, null)
            g.drawImage(old, 0, 47 * s, 9 * s, 50 * s, 53 * s, 3 * s, 62 * s, 6 * s, null)
            g.drawImage(old, 9 * s, 47 * s, 12 * s, 50 * s, 50 * s, 3 * s, 53 * s, 6 * s, null)
        }

        private fun drawLeg1(g: Graphics2D, old: BufferedImage, s: Int) {
            g.drawImage(old, 3 * s, 50 * s, 9 * s, 53 * s, 53 * s, 0, 59 * s, 3 * s, null)
            g.drawImage(old, 0, 53 * s, 6 * s, 56 * s, 56 * s, 3 * s, 62 * s, 6 * s, null)
            g.drawImage(old, 6 * s, 53 * s, 12 * s, 56 * s, 50 * s, 3 * s, 56 * s, 6 * s, null)
        }

        private fun drawLeg2(g: Graphics2D, old: BufferedImage, s: Int) {
            g.drawImage(old, 15 * s, 44 * s, 21 * s, 47 * s, 53 * s, 0, 59 * s, 3 * s, null)
            g.drawImage(old, 12 * s, 47 * s, 24 * s, 50 * s, 50 * s, 3 * s, 62 * s, 6 * s, null)
        }

        private fun drawLeg3(g: Graphics2D, old: BufferedImage, s: Int) {
            g.drawImage(old, 15 * s, 50 * s, 21 * s, 53 * s, 53 * s, 0, 59 * s, 3 * s, null)
            g.drawImage(old, 12 * s, 53 * s, 15 * s, 56 * s, 59 * s, 3 * s, 62 * s, 6 * s, null)
            g.drawImage(old, 15 * s, 53 * s, 24 * s, 56 * s, 50 * s, 3 * s, 59 * s, 6 * s, null)
        }

        companion object {
            private val textures = Array(EnumDyeColor.values().size) { i ->
                ResourceLocation("textures/entity/bed/${EnumDyeColor.values()[i].getName()}.png")
            }
            var instance = BedTexture(getTextureHash())
                private set

            fun getInstance(): BedTexture {
                val newHash = getTextureHash()
                if (newHash != instance.hash) {
                    GL11.glDeleteTextures(instance.texture.glTextureId)
                    instance = BedTexture(newHash)
                }
                return instance
            }

            private fun getTextureHash(): Int {
                var result = 1
                textures.forEach {
                    val tex = Helper.mc.textureManager.getTexture(it)
                    result = 31 * result + (tex?.hashCode() ?: 0)
                }
                return result
            }
        }
    }

    companion object {
        private val model = ModelBed().apply { init() }
        private val shader = AbstractTileEntityRenderBuilder.Shader("/assets/meta/shaders/tileentity/Bed.vsh", "/assets/meta/shaders/tileentity/Default.fsh")
    }
}
