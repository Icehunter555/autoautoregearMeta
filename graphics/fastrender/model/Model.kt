package dev.wizard.meta.graphics.fastrender.model

import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

abstract class Model(private val textureSizeX: Int, private val textureSizeY: Int) {
    private var vboID: Int = 0
    var modelSize: Int = 0
        private set

    fun init() {
        val builder = ModelBuilder(0, textureSizeX, textureSizeY)
        buildModel(builder)
        vboID = GL15.glGenBuffers()
        modelSize = builder.vertexSize
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, builder.build(), GL15.GL_STATIC_READ)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    }

    protected abstract fun buildModel(builder: ModelBuilder)

    fun attachVBO() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
        GL20.glVertexAttribPointer(0, 3, 5126, false, 20, 0L)
        GL20.glVertexAttribPointer(1, 2, 5123, true, 20, 12L)
        GL20.glVertexAttribPointer(2, 3, 5120, false, 20, 16L)
        GL30.glVertexAttribIPointer(3, 1, 5121, 20, 19L)
        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)
        GL20.glEnableVertexAttribArray(2)
        GL20.glEnableVertexAttribArray(3)
    }
}
