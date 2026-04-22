package dev.wizard.meta.graphics

import dev.fastmc.common.BufferUtils
import dev.luna5ama.kmogus.Arr
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL45
import org.lwjgl.opengl.GLContext
import org.lwjgl.opengl.GLSync
import sun.misc.Unsafe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

object GLFunctions {
    private val unsafe: Unsafe
    private val trustedLookUp: MethodHandles.Lookup
    private val getFunctionAddress: (String) -> Long
    private val nglNamedBufferSubData: MethodHandle
    private val glNamedBufferSubDataFunctionPointer: Long
    private val nglNamedBufferData: MethodHandle
    private val glNamedBufferDataFunctionPointer: Long
    private val nglNamedBufferStorage: MethodHandle
    private val glNamedBufferStorageFunctionPointer: Long
    private val nglDrawElementsBOMethod: MethodHandle
    private val glDrawElementsFunctionPointer: Long
    private val nglMapNamedBufferRange: MethodHandle
    private val glMapNamedBufferRangeFunctionPointer: Long
    private val dummyBuffer: ByteBuffer
    private val glSyncInstance: GLSync
    private val pointerSetter: MethodHandle
    private val lengthBuffer: IntBuffer
    private val valueBuffer: IntBuffer
    private val nglCompressedTextureSubImage2D: MethodHandle
    private val glCompressedTextureSubImage2DFunctionPointer: Long

    init {
        val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        unsafe = theUnsafe.get(null) as Unsafe

        val trustedLookupField = MethodHandles.Lookup::class.java.getDeclaredField("IMPL_LOOKUP")
        trustedLookUp = unsafe.getObject(
            unsafe.staticFieldBase(trustedLookupField),
            unsafe.staticFieldOffset(trustedLookupField)
        ) as MethodHandles.Lookup

        val getFunctionAddressHandle = trustedLookUp.findStatic(
            GLContext::class.java,
            "getFunctionAddress",
            MethodType.methodType(Long::class.javaPrimitiveType, String::class.java)
        )
        getFunctionAddress = { getFunctionAddressHandle.invokeExact(it) as Long }

        nglNamedBufferSubData = trustedLookUp.findStatic(
            GL45::class.java,
            "nglNamedBufferSubData",
            MethodType.methodType(
                Void::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
        )
        glNamedBufferSubDataFunctionPointer = getFunctionAddress("glNamedBufferSubData")

        nglNamedBufferData = trustedLookUp.findStatic(
            GL45::class.java,
            "nglNamedBufferData",
            MethodType.methodType(
                Void::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
        )
        glNamedBufferDataFunctionPointer = getFunctionAddress("glNamedBufferData")

        nglNamedBufferStorage = trustedLookUp.findStatic(
            GL45::class.java,
            "nglNamedBufferStorage",
            MethodType.methodType(
                Void::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
        )
        glNamedBufferStorageFunctionPointer = getFunctionAddress("glNamedBufferStorage")

        nglDrawElementsBOMethod = trustedLookUp.findStatic(
            GL11::class.java,
            "nglDrawElementsBO",
            MethodType.methodType(
                Void::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
        )
        glDrawElementsFunctionPointer = getFunctionAddress("glDrawElements")

        nglMapNamedBufferRange = trustedLookUp.findStatic(
            GL45::class.java,
            "nglMapNamedBufferRange",
            MethodType.methodType(
                ByteBuffer::class.java,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                ByteBuffer::class.java,
                Long::class.javaPrimitiveType
            )
        )
        glMapNamedBufferRangeFunctionPointer = getFunctionAddress("glMapNamedBufferRange")

        dummyBuffer = unsafe.allocateInstance(BufferUtils.DIRECT_BYTE_BUFFER_CLASS) as ByteBuffer
        glSyncInstance = unsafe.allocateInstance(GLSync::class.java) as GLSync
        pointerSetter = trustedLookUp.findSetter(GLSync::class.java, "pointer", Long::class.javaPrimitiveType)

        lengthBuffer = BufferUtils.allocateInt(1).apply {
            put(1)
            flip()
        }
        valueBuffer = BufferUtils.allocateInt(1)

        nglCompressedTextureSubImage2D = trustedLookUp.findStatic(
            GL45::class.java,
            "nglCompressedTextureSubImage2D",
            MethodType.methodType(
                Void::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
        )
        glCompressedTextureSubImage2DFunctionPointer = getFunctionAddress("glCompressedTextureSubImage2D")
    }

    @JvmStatic
    fun glNamedBufferSubData(buffer: Int, offset: Long, dataSize: Long, data: Long) {
        nglNamedBufferSubData.invokeExact(buffer, offset, dataSize, data, glNamedBufferSubDataFunctionPointer)
    }

    @JvmStatic
    fun glNamedBufferData(buffer: Int, dataSize: Long, data: Long, usage: Int) {
        nglNamedBufferData.invokeExact(buffer, dataSize, data, usage, glNamedBufferDataFunctionPointer)
    }

    @JvmStatic
    fun glNamedBufferStorage(buffer: Int, dataSize: Long, data: Long, flags: Int) {
        nglNamedBufferStorage.invokeExact(buffer, dataSize, data, flags, glNamedBufferStorageFunctionPointer)
    }

    @JvmStatic
    fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) {
        nglDrawElementsBOMethod.invokeExact(mode, count, type, indices, glDrawElementsFunctionPointer)
    }

    @JvmStatic
    fun glMapNamedBufferRange(buffer: Int, offset: Long, length: Long, access: Int): Arr {
        val byteBuffer = nglMapNamedBufferRange.invokeExact(
            buffer,
            offset,
            length,
            access,
            dummyBuffer,
            glMapNamedBufferRangeFunctionPointer
        ) as ByteBuffer
        return Arr.wrap(byteBuffer)
    }

    @JvmStatic
    fun glFenceSync(condition: Int, flags: Int): Long {
        return GL32.glFenceSync(condition, flags).pointer
    }

    @JvmStatic
    fun glDeleteSync(sync: Long) {
        pointerSetter.invokeExact(glSyncInstance, sync)
        GL32.glDeleteSync(glSyncInstance)
    }

    @JvmStatic
    fun glGetSynciv(sync: Long, pname: Int): Int {
        pointerSetter.invokeExact(glSyncInstance, sync)
        GL32.glGetSync(glSyncInstance, pname, lengthBuffer, valueBuffer)
        return valueBuffer.get(0)
    }

    @JvmStatic
    fun glCompressedTextureSubImage2D(
        texture: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        imageSize: Int,
        data: Long
    ) {
        nglCompressedTextureSubImage2D.invokeExact(
            texture,
            level,
            xOffset,
            yOffset,
            width,
            height,
            format,
            imageSize,
            data,
            glCompressedTextureSubImage2DFunctionPointer
        )
    }
}