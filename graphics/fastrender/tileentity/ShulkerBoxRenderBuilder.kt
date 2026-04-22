package dev.wizard.meta.graphics.fastrender.tileentity

import dev.wizard.meta.graphics.fastrender.model.tileentity.ModelShulkerBox
import dev.wizard.meta.graphics.texture.TextureUtils
import dev.wizard.meta.util.interfaces.Helper
import net.minecraft.block.BlockShulkerBox
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.ITextureObject
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class ShulkerBoxRenderBuilder(builtPosX: Double, builtPosY: Double, builtPosZ: Double) :
    AbstractTileEntityRenderBuilder<TileEntityShulkerBox>(builtPosX, builtPosY, builtPosZ) {

    override fun add(tileEntity: TileEntityShulkerBox) {
        val pos = tileEntity.pos
        val posX = (pos.x + 0.5 - builtPosX).toFloat()
        val posY = (pos.y - builtPosY).toFloat()
        val posZ = (pos.z + 0.5 - builtPosZ).toFloat()

        val world = tileEntity.world
        val enumFacing = if (world != null) {
            val blockState = world.getBlockState(tileEntity.pos)
            if (blockState.block is BlockShulkerBox) {
                blockState.getValue(BlockShulkerBox.FACING)
            } else {
                EnumFacing.UP
            }
        } else {
            EnumFacing.UP
        }

        floatArrayList.add(posX)
        floatArrayList.add(posY)
        floatArrayList.add(posZ)
        putTileEntityLightMapUV(tileEntity)

        when (enumFacing) {
            EnumFacing.DOWN -> {
                byteArrayList.add(0.toByte())
                byteArrayList.add(2.toByte())
            }
            EnumFacing.UP -> {
                byteArrayList.add(0.toByte())
                byteArrayList.add(0.toByte())
            }
            EnumFacing.NORTH -> {
                byteArrayList.add(2.toByte())
                byteArrayList.add((-1).toByte())
            }
            EnumFacing.SOUTH -> {
                byteArrayList.add(0.toByte())
                byteArrayList.add((-1).toByte())
            }
            EnumFacing.WEST -> {
                byteArrayList.add((-1).toByte())
                byteArrayList.add(1.toByte())
            }
            EnumFacing.EAST -> {
                byteArrayList.add(1.toByte())
                byteArrayList.add(1.toByte())
            }
            else -> {
                byteArrayList.add(0.toByte())
                byteArrayList.add(0.toByte())
            }
        }

        byteArrayList.add(tileEntity.color.metadata.toByte())
        byteArrayList.add((tileEntity.getProgress(0.0f) * 255.0f).toInt().toByte())
        byteArrayList.add((tileEntity.getProgress(1.0f) * 255.0f).toInt().toByte())
        size++
    }

    override fun uploadBuffer(buffer: ByteBuffer): AbstractTileEntityRenderBuilder.Renderer {
        val vaoID = GL30.glGenVertexArrays()
        val vboID = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW)
        GL30.glBindVertexArray(vaoID)

        // pos
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 20, 0L)
        // lightmap
        GL20.glVertexAttribPointer(5, 2, GL11.GL_UNSIGNED_BYTE, true, 20, 12L)
        // rotation
        GL30.glVertexAttribIPointer(6, 1, GL11.GL_BYTE, 20, 14L)
        GL30.glVertexAttribIPointer(7, 1, GL11.GL_BYTE, 20, 15L)
        // color
        GL30.glVertexAttribIPointer(8, 1, GL11.GL_UNSIGNED_BYTE, 20, 16L)
        // progress
        GL20.glVertexAttribPointer(9, 1, GL11.GL_UNSIGNED_BYTE, true, 20, 17L)
        GL20.glVertexAttribPointer(10, 1, GL11.GL_UNSIGNED_BYTE, true, 20, 18L)

        GL33.glVertexAttribDivisor(4, 1)
        GL33.glVertexAttribDivisor(5, 1)
        GL33.glVertexAttribDivisor(6, 1)
        GL33.glVertexAttribDivisor(7, 1)
        GL33.glVertexAttribDivisor(8, 1)
        GL33.glVertexAttribDivisor(9, 1)
        GL33.glVertexAttribDivisor(10, 1)

        model.attachVBO()

        GL20.glEnableVertexAttribArray(4)
        GL20.glEnableVertexAttribArray(5)
        GL20.glEnableVertexAttribArray(6)
        GL20.glEnableVertexAttribArray(7)
        GL20.glEnableVertexAttribArray(8)
        GL20.glEnableVertexAttribArray(9)
        GL20.glEnableVertexAttribArray(10)

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
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(0.toByte()) // padding
        }
        buffer.flip()
        return buffer
    }

    companion object : Helper {
        val model: ModelShulkerBox = ModelShulkerBox().apply { init() }
        val shader: AbstractTileEntityRenderBuilder.Shader = AbstractTileEntityRenderBuilder.Shader(
            "/assets/meta/shaders/tileentity/ShulkerBox.vsh",
            "/assets/meta/shaders/tileentity/Default.fsh"
        )
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
    ) : AbstractTileEntityRenderBuilder.Renderer(shader, vaoID, vboID, modelSize, size, builtPosX, builtPosY, builtPosZ),
        Helper {

        override fun preRender() {
            GlStateManager.bindTexture(ShulkerTexture.currentInstance.texture.glTextureId)
        }

        override fun postRender() {
            GlStateManager.bindTexture(0)
        }
    }

    private class ShulkerTexture private constructor(val hash: Int) : Helper {
        val texture: ITextureObject

        init {
            val images = Array(EnumDyeColor.values().size) { i ->
                val resource = mc.resourceManager.getResource(textures[i])
                TextureUtils.readImage(resource)
            }
            val firstImage = images[0]
            val size = firstImage.width
            val finalImage = BufferedImage(size * 4, size * 4, firstImage.type)
            val graphics = finalImage.createGraphics()
            for (x in 0 until 4) {
                for (y in 0 until 4) {
                    val src = images[x + y * 4]
                    graphics.drawImage(src, x * size, y * size, null)
                }
            }
            graphics.dispose()
            texture = DynamicTexture(finalImage)
        }

        companion object : Helper {
            private val textures = Array(EnumDyeColor.values().size) { i ->
                ResourceLocation("textures/entity/shulker/shulker_${EnumDyeColor.values()[i].getName()}.png")
            }

            var instance = ShulkerTexture(textureHash)
                private set

            fun updateInstance(): ShulkerTexture {
                val newHash = textureHash
                if (newHash != instance.hash) {
                    GL11.glDeleteTextures(instance.texture.glTextureId)
                    instance = ShulkerTexture(newHash)
                }
                return instance
            }

            private val textureHash: Int
                get() {
                    var result = 1
                    for (element in textures) {
                        val textureObject = mc.textureManager.getTexture(element)
                        result = 31 * result + (textureObject?.hashCode() ?: 0)
                    }
                    return result
                }
            
            // This is called from the Renderer.preRender via instance, but we need to ensure it's up to date.
            // In the original code, Companion.getInstance() was called.
            val currentInstance: ShulkerTexture
                get() = updateInstance()
        }
    }
}
