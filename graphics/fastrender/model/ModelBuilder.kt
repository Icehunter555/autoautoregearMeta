package dev.wizard.meta.graphics.fastrender.model

import net.minecraft.client.renderer.GLAllocation
import java.nio.ByteBuffer

open class ModelBuilder(
    val id: Int,
    val textureSizeX: Int,
    val textureSizeY: Int
) {
    var idCounter: Int = 0
    private val childModels = ArrayList<ChildModelBuilder>()
    var vertexSize: Int = 0
        protected set

    fun childModel(block: ChildModelBuilder.() -> Unit) {
        childModel(0.0f, 0.0f, block)
    }

    fun childModel(textureOffsetX: Float, textureOffsetY: Float, block: ChildModelBuilder.() -> Unit) {
        val child = ChildModelBuilder(this, textureOffsetX, textureOffsetY)
        child.block()
        childModel(child)
    }

    fun childModel(childModelBuilder: ChildModelBuilder) {
        childModels.add(childModelBuilder)
        vertexSize += childModelBuilder.vertexSize
    }

    fun build(): ByteBuffer {
        val buffer = GLAllocation.createDirectByteBuffer(vertexSize * 20)
        build(buffer)
        buffer.flip()
        return buffer
    }

    open fun build(vboBuffer: ByteBuffer) {
        childModels.forEach { it.build(vboBuffer) }
    }
}
