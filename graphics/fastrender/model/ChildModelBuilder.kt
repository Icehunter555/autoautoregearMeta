package dev.wizard.meta.graphics.fastrender.model

import java.nio.ByteBuffer

class ChildModelBuilder(
    parent: ModelBuilder,
    private val textureOffsetX: Float,
    private val textureOffsetY: Float
) : ModelBuilder(parent.idCounter++, parent.textureSizeX, parent.textureSizeY) {

    private val boxList = ArrayList<Box>()

    fun addBox(offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float) {
        boxList.add(Box(offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ))
        vertexSize += 36
    }

    override fun build(vboBuffer: ByteBuffer) {
        super.build(vboBuffer)
        boxList.forEach { box ->
            box.putDown(vboBuffer)
            box.putUp(vboBuffer)
            box.putWest(vboBuffer)
            box.putSouth(vboBuffer)
            box.putEast(vboBuffer)
            box.putNorth(vboBuffer)
        }
    }

    private inner class Box(
        val minX: Float,
        val minY: Float,
        val minZ: Float,
        val sizeX: Float,
        val sizeY: Float,
        val sizeZ: Float
    ) {
        val maxX = minX + sizeX
        val maxY = minY + sizeY
        val maxZ = minZ + sizeZ

        fun putDown(vboBuffer: ByteBuffer) {
            putPos(vboBuffer, minX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX, 0.0f)
            putNormal(vboBuffer, 0, -1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeX, sizeZ)
            putNormal(vboBuffer, 0, -1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 0, -1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX, 0.0f)
            putNormal(vboBuffer, 0, -1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeX, 0.0f)
            putNormal(vboBuffer, 0, -1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeX, sizeZ)
            putNormal(vboBuffer, 0, -1, 0)
            putID(vboBuffer)
        }

        fun putUp(vboBuffer: ByteBuffer) {
            putPos(vboBuffer, minX, maxY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ)
            putNormal(vboBuffer, 0, 1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX, 0.0f)
            putNormal(vboBuffer, 0, 1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, maxY, minZ)
            putUV(vboBuffer, sizeZ, 0.0f)
            putNormal(vboBuffer, 0, 1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, maxY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ)
            putNormal(vboBuffer, 0, 1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 0, 1, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX, 0.0f)
            putNormal(vboBuffer, 0, 1, 0)
            putID(vboBuffer)
        }

        fun putWest(vboBuffer: ByteBuffer) {
            putPos(vboBuffer, minX, maxY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ)
            putNormal(vboBuffer, -1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, minZ)
            putUV(vboBuffer, 0.0f, sizeZ + sizeY)
            putNormal(vboBuffer, -1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ + sizeY)
            putNormal(vboBuffer, -1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, maxY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ)
            putNormal(vboBuffer, -1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, maxY, minZ)
            putUV(vboBuffer, 0.0f, sizeZ)
            putNormal(vboBuffer, -1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, minZ)
            putUV(vboBuffer, 0.0f, sizeZ + sizeY)
            putNormal(vboBuffer, -1, 0, 0)
            putID(vboBuffer)
        }

        fun putSouth(vboBuffer: ByteBuffer) {
            putPos(vboBuffer, maxX, maxY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 0, 0, 1)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ + sizeY)
            putNormal(vboBuffer, 0, 0, 1)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ + sizeY)
            putNormal(vboBuffer, 0, 0, 1)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 0, 0, 1)
            putID(vboBuffer)

            putPos(vboBuffer, minX, maxY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ)
            putNormal(vboBuffer, 0, 0, 1)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, maxZ)
            putUV(vboBuffer, sizeZ, sizeZ + sizeY)
            putNormal(vboBuffer, 0, 0, 1)
            putID(vboBuffer)
        }

        fun putEast(vboBuffer: ByteBuffer) {
            putPos(vboBuffer, maxX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ, sizeZ)
            putNormal(vboBuffer, 1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ + sizeY)
            putNormal(vboBuffer, 1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ, sizeZ + sizeY)
            putNormal(vboBuffer, 1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ, sizeZ)
            putNormal(vboBuffer, 1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 1, 0, 0)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, maxZ)
            putUV(vboBuffer, sizeZ + sizeX, sizeZ + sizeY)
            putNormal(vboBuffer, 1, 0, 0)
            putID(vboBuffer)
        }

        fun putNorth(vboBuffer: ByteBuffer) {
            putPos(vboBuffer, minX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 0, 0, -1)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ, sizeZ + sizeY)
            putNormal(vboBuffer, 0, 0, -1)
            putID(vboBuffer)

            putPos(vboBuffer, minX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ + sizeX, sizeZ + sizeY)
            putNormal(vboBuffer, 0, 0, -1)
            putID(vboBuffer)

            putPos(vboBuffer, minX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ + sizeX, sizeZ)
            putNormal(vboBuffer, 0, 0, -1)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, maxY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ, sizeZ)
            putNormal(vboBuffer, 0, 0, -1)
            putID(vboBuffer)

            putPos(vboBuffer, maxX, minY, minZ)
            putUV(vboBuffer, sizeZ + sizeX + sizeZ, sizeZ + sizeY)
            putNormal(vboBuffer, 0, 0, -1)
            putID(vboBuffer)
        }

        private fun putPos(buffer: ByteBuffer, x: Float, y: Float, z: Float) {
            buffer.putFloat(x / 16.0f)
            buffer.putFloat(y / 16.0f)
            buffer.putFloat(z / 16.0f)
        }

        private fun putUV(buffer: ByteBuffer, u: Float, v: Float) {
            buffer.putShort(((u + textureOffsetX) / textureSizeX * 65535.0f).toInt().toShort())
            buffer.putShort(((v + textureOffsetY) / textureSizeY * 65535.0f).toInt().toShort())
        }

        private fun putNormal(buffer: ByteBuffer, x: Int, y: Int, z: Int) {
            buffer.put(x.toByte())
            buffer.put(y.toByte())
            buffer.put(z.toByte())
        }

        private fun putID(buffer: ByteBuffer) {
            buffer.put(id.toByte())
        }
    }
}
