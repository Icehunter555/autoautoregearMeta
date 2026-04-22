package dev.wizard.meta.graphics.fastrender.tileentity

import dev.wizard.meta.graphics.fastrender.model.Model
import dev.wizard.meta.graphics.fastrender.model.tileentity.ModelChest
import dev.wizard.meta.graphics.fastrender.model.tileentity.ModelLargeChest
import dev.wizard.meta.graphics.texture.TextureUtils
import dev.wizard.meta.util.interfaces.Helper
import net.minecraft.block.BlockChest
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL33
import java.nio.ByteBuffer
import java.util.*

class ChestRenderBuilder(builtPosX: Double, builtPosY: Double, builtPosZ: Double) : ITileEntityRenderBuilder<TileEntityChest> {
    private val smallChest = SmallChestRenderBuilder(builtPosX, builtPosY, builtPosZ)
    private val largeChest = LargeChestRenderBuilder(builtPosX, builtPosY, builtPosZ)

    override fun add(tileEntity: TileEntityChest) {
        if (tileEntity.hasWorld()) {
            val block = tileEntity.blockType
            if (block is BlockChest && tileEntity.blockMetadata == 0) {
                block.checkForSurroundingChests(tileEntity.world, tileEntity.pos, tileEntity.world.getBlockState(tileEntity.pos))
            }
        }
        tileEntity.checkForAdjacentChests()
        if (tileEntity.adjacentChestXNeg == null && tileEntity.adjacentChestZNeg == null) {
            if (tileEntity.adjacentChestXPos == null && tileEntity.adjacentChestZPos == null) {
                smallChest.add(tileEntity)
            } else {
                largeChest.add(tileEntity)
            }
        }
    }

    override fun build() {
        smallChest.build()
        largeChest.build()
    }

    override fun upload(): ITileEntityRenderBuilder.Renderer {
        return Renderer(smallChest.upload(), largeChest.upload())
    }

    private class Renderer(
        private val smallChest: ITileEntityRenderBuilder.Renderer,
        private val largeChest: ITileEntityRenderBuilder.Renderer
    ) : ITileEntityRenderBuilder.Renderer {
        override fun render(renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            smallChest.render(renderPosX, renderPosY, renderPosZ)
            largeChest.render(renderPosX, renderPosY, renderPosZ)
        }

        override fun destroy() {
            smallChest.destroy()
            largeChest.destroy()
        }
    }

    open class SmallChestRenderBuilder(builtPosX: Double, builtPosY: Double, builtPosZ: Double) :
        AbstractTileEntityRenderBuilder<TileEntityChest>(builtPosX, builtPosY, builtPosZ) {

        override fun add(tileEntity: TileEntityChest) {
            val pos = tileEntity.pos
            val posX = (pos.x + 0.5 - builtPosX).toFloat()
            val posY = (pos.y - builtPosY).toFloat()
            val posZ = (pos.z + 0.5 - builtPosZ).toFloat()

            floatArrayList.add(posX)
            floatArrayList.add(posY)
            floatArrayList.add(posZ)
            putTileEntityLightMapUV(tileEntity)

            val rotation = when (tileEntity.blockMetadata) {
                2 -> 2.toByte()
                4 -> (-1).toByte()
                5 -> 1.toByte()
                else -> 0.toByte()
            }
            byteArrayList.add(rotation)

            if (isChristmas) {
                byteArrayList.add(2.toByte())
            } else if (tileEntity.chestType == BlockChest.Type.TRAP) {
                byteArrayList.add(1.toByte())
            } else {
                byteArrayList.add(0.toByte())
            }

            shortArrayList.add((tileEntity.prevLidAngle * 65535.0f).toInt().toShort())
            shortArrayList.add((tileEntity.lidAngle * 65535.0f).toInt().toShort())
            size++
        }

        override fun uploadBuffer(vboBuffer: ByteBuffer): AbstractTileEntityRenderBuilder.Renderer {
            return upload(vboBuffer, model, texture)
        }

        protected fun upload(buffer: ByteBuffer, model: Model, texture: AbstractTexture): AbstractTileEntityRenderBuilder.Renderer {
            val vaoID = GL30.glGenVertexArrays()
            val vboID = GL15.glGenBuffers()
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW)
            GL30.glBindVertexArray(vaoID)

            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 20, 0L)
            GL20.glVertexAttribPointer(5, 2, GL11.GL_UNSIGNED_BYTE, true, 20, 12L)
            GL30.glVertexAttribIPointer(6, 1, GL11.GL_BYTE, 20, 14L)
            GL30.glVertexAttribIPointer(7, 1, GL11.GL_UNSIGNED_BYTE, 20, 15L)
            GL20.glVertexAttribPointer(8, 1, GL11.GL_UNSIGNED_SHORT, true, 20, 16L)
            GL20.glVertexAttribPointer(9, 1, GL11.GL_UNSIGNED_SHORT, true, 20, 18L)

            GL33.glVertexAttribDivisor(4, 1)
            GL33.glVertexAttribDivisor(5, 1)
            GL33.glVertexAttribDivisor(6, 1)
            GL33.glVertexAttribDivisor(7, 1)
            GL33.glVertexAttribDivisor(8, 1)
            GL33.glVertexAttribDivisor(9, 1)

            model.attachVBO()
            GL20.glEnableVertexAttribArray(4)
            GL20.glEnableVertexAttribArray(5)
            GL20.glEnableVertexAttribArray(6)
            GL20.glEnableVertexAttribArray(7)
            GL20.glEnableVertexAttribArray(8)
            GL20.glEnableVertexAttribArray(9)

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
            GL30.glBindVertexArray(0)

            return Renderer(shader, vaoID, vboID, model.modelSize, size, builtPosX, builtPosY, builtPosZ, texture)
        }

        override fun buildBuffer(): ByteBuffer {
            val buffer = GLAllocation.createDirectByteBuffer(size * 20)
            var floatIdx = 0
            var shortIdx = 0
            var byteIdx = 0
            for (i in 0 until size) {
                buffer.putFloat(floatArrayList.getFloat(floatIdx++))
                buffer.putFloat(floatArrayList.getFloat(floatIdx++))
                buffer.putFloat(floatArrayList.getFloat(floatIdx++))
                buffer.put(byteArrayList.getByte(byteIdx++))
                buffer.put(byteArrayList.getByte(byteIdx++))
                buffer.put(byteArrayList.getByte(byteIdx++))
                buffer.put(byteArrayList.getByte(byteIdx++))
                buffer.putShort(shortArrayList.getShort(shortIdx++))
                buffer.putShort(shortArrayList.getShort(shortIdx++))
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
            builtPosZ: Double,
            private val texture: AbstractTexture
        ) : AbstractTileEntityRenderBuilder.Renderer(shader, vaoID, vboID, modelSize, size, builtPosX, builtPosY, builtPosZ), Helper {
            override fun preRender() {
                GlStateManager.setActiveTexture(33984)
                GlStateManager.bindTexture(texture.glTextureId)
            }

            override fun postRender() {
                GlStateManager.setActiveTexture(33984)
                GlStateManager.bindTexture(0)
            }
        }

        companion object {
            private val model = ModelChest().apply { init() }
            private val texture: DynamicTexture = run {
                val images = arrayOf(
                    TextureUtils.readImage(ResourceLocation("textures/entity/chest/normal.png")),
                    TextureUtils.readImage(ResourceLocation("textures/entity/chest/trapped.png")),
                    TextureUtils.readImage(ResourceLocation("textures/entity/chest/christmas.png"))
                )
                DynamicTexture(TextureUtils.combineTexturesVertically(images))
            }
            val shader = AbstractTileEntityRenderBuilder.Shader("/assets/meta/shaders/tileentity/Chest.vsh", "/assets/meta/shaders/tileentity/Default.fsh")
            val isChristmas: Boolean = run {
                val calendar = Calendar.getInstance()
                calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DATE) in 24..26
            }
        }
    }

    class LargeChestRenderBuilder(builtPosX: Double, builtPosY: Double, builtPosZ: Double) :
        SmallChestRenderBuilder(builtPosX, builtPosY, builtPosZ) {

        override fun add(tileEntity: TileEntityChest) {
            val pos = tileEntity.pos
            var posX = (pos.x + 0.5 - builtPosX).toFloat()
            val posY = (pos.y - builtPosY).toFloat()
            var posZ = (pos.z + 0.5 - builtPosZ).toFloat()

            if (tileEntity.adjacentChestZPos != null) {
                posZ += 0.5f
            } else {
                posX += 0.5f
            }

            floatArrayList.add(posX)
            floatArrayList.add(posY)
            floatArrayList.add(posZ)
            putTileEntityLightMapUV(tileEntity)

            val rotation = when (tileEntity.blockMetadata) {
                2 -> 2.toByte()
                4 -> (-1).toByte()
                5 -> 1.toByte()
                else -> 0.toByte()
            }
            byteArrayList.add(rotation)

            if (SmallChestRenderBuilder.isChristmas) {
                byteArrayList.add(2.toByte())
            } else if (tileEntity.chestType == BlockChest.Type.TRAP) {
                byteArrayList.add(1.toByte())
            } else {
                byteArrayList.add(0.toByte())
            }

            shortArrayList.add((tileEntity.prevLidAngle * 65535.0f).toInt().toShort())
            shortArrayList.add((tileEntity.lidAngle * 65535.0f).toInt().toShort())
            size++
        }

        override fun uploadBuffer(vboBuffer: ByteBuffer): AbstractTileEntityRenderBuilder.Renderer {
            return upload(vboBuffer, model, texture)
        }

        companion object {
            private val model = ModelLargeChest().apply { init() }
            private val texture: DynamicTexture = run {
                val images = arrayOf(
                    TextureUtils.readImage(ResourceLocation("textures/entity/chest/normal_double.png")),
                    TextureUtils.readImage(ResourceLocation("textures/entity/chest/trapped_double.png")),
                    TextureUtils.readImage(ResourceLocation("textures/entity/chest/christmas_double.png"))
                )
                DynamicTexture(TextureUtils.combineTexturesVertically(images))
            }
        }
    }
}
