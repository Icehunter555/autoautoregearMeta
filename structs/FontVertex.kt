package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class FontVertex(val address: Long) {
    val position: Vec2f32 get() = Vec2f32(address)
    val vertUV: Vec2i16 get() = Vec2i16(address + 8L)

    var colorIndex: Byte
        get() = UNSAFE.getByte(address + 12L)
        set(value) { UNSAFE.putByte(address + 12L, value) }

    var overrideColor: Byte
        get() = UNSAFE.getByte(address + 13L)
        set(value) { UNSAFE.putByte(address + 13L, value) }

    var shadow: Byte
        get() = UNSAFE.getByte(address + 14L)
        set(value) { UNSAFE.putByte(address + 14L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): FontVertex = FontVertex(address + 16L)
    fun dec(): FontVertex = FontVertex(address - 16L)

    operator fun get(index: Int): FontVertex = FontVertex(address + index * 16L)
    operator fun set(index: Int, value: FontVertex) {
        UNSAFE.copyMemory(value.address, address + index * 16L, 16L)
    }

    operator fun plus(offset: Long): FontVertex = FontVertex(address + offset)
    operator fun minus(offset: Long): FontVertex = FontVertex(address - offset)

    fun copyTo(dest: FontVertex) {
        UNSAFE.copyMemory(address, dest.address, 16L)
    }

    override fun toString(): String = "FontVertex(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 16L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, position: Long, vertUV: Long, colorIndex: Byte, overrideColor: Byte, shadow: Byte): FontVertex {
            val v = FontVertex(container)
            v.position.address.let { UNSAFE.copyMemory(position, it, 8L) }
            v.vertUV.address.let { UNSAFE.copyMemory(vertUV, it, 4L) }
            v.colorIndex = colorIndex
            v.overrideColor = overrideColor
            v.shadow = shadow
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, position: Long, vertUV: Long, colorIndex: Byte, overrideColor: Byte, shadow: Byte): FontVertex {
            val v = FontVertex(container)
            v.position.address.let { UNSAFE.copyMemory(position, it, 8L) }
            v.vertUV.address.let { UNSAFE.copyMemory(vertUV, it, 4L) }
            v.colorIndex = colorIndex
            v.overrideColor = overrideColor
            v.shadow = shadow
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): FontVertex = FontVertex(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, position: Long, vertUV: Long, colorIndex: Byte, overrideColor: Byte, shadow: Byte): FontVertex {
            val v = FontVertex(ptr)
            v.position.address.let { UNSAFE.copyMemory(position, it, 8L) }
            v.vertUV.address.let { UNSAFE.copyMemory(vertUV, it, 4L) }
            v.colorIndex = colorIndex
            v.overrideColor = overrideColor
            v.shadow = shadow
            return v
        }
    }
}
