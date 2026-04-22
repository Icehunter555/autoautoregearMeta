package dev.wizard.meta.graphics.buffer

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.ListenerKt
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.graphics.GLDataType
import dev.wizard.meta.graphics.GLFunctions
import dev.wizard.meta.graphics.VertexAttribute
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL45

object PersistentMappedVBO : AlwaysListening {
    private val vbo: Int
    val arr: MutableArr
    var drawOffset: Int = 0
    private var sync: Long = 0L

    val POS2_COLOR: Int
    val POS3_COLOR: Int

    init {
        vbo = GL45.glCreateBuffers()
        GLFunctions.glNamedBufferStorage(vbo, 0x4000000L, 0L, 194) // GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT
        arr = GLFunctions.glMapNamedBufferRange(
            vbo,
            0L,
            0x4000000L,
            226 // GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_UNSYNCHRONIZED_BIT
        ) as MutableArr

        ListenerKt.listener(this, RunGameLoopEvent.End::class.java) {
            if (sync == 0L) {
                if (arr.pos >= arr.len / 2) {
                    sync = GLFunctions.glFenceSync(37143, 0) // GL_SYNC_GPU_COMMANDS_COMPLETE
                }
            } else if (GLFunctions.glGetSynciv(sync, 37140) == 37145) { // GL_SYNC_STATUS, GL_SIGNALED
                GLFunctions.glDeleteSync(sync)
                sync = 0L
                arr.pos = 0L
                drawOffset = 0
            }
        }

        POS2_COLOR = createVao(
            VertexAttribute.Builder(16)
                .float(0, 2, GLDataType.GL_FLOAT, false)
                .float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
                .build()
        )

        POS3_COLOR = createVao(
            VertexAttribute.Builder(16)
                .float(0, 3, GLDataType.GL_FLOAT, false)
                .float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
                .build()
        )
    }

    fun end() {
        drawOffset = (arr.pos / 16).toInt()
    }

    fun createVao(vertexAttribute: VertexAttribute): Int {
        val vaoID = GL45.glCreateVertexArrays()
        GL30.glBindVertexArray(vaoID)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
        vertexAttribute.apply()
        GL30.glBindVertexArray(0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        return vaoID
    }
}