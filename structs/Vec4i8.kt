package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class Vec4i8(val address: Long) {
    var x: Byte
        get() = UNSAFE.getByte(address)
        set(value) { UNSAFE.putByte(address, value) }

    var y: Byte
        get() = UNSAFE.getByte(address + 1L)
        set(value) { UNSAFE.putByte(address + 1L, value) }

    var z: Byte
        get() = UNSAFE.getByte(address + 2L)
        set(value) { UNSAFE.putByte(address + 2L, value) }

    var w: Byte
        get() = UNSAFE.getByte(address + 3L)
        set(value) { UNSAFE.putByte(address + 3L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): Vec4i8 = Vec4i8(address + 4L)
    fun dec(): Vec4i8 = Vec4i8(address - 4L)

    operator fun get(index: Int): Vec4i8 = Vec4i8(address + index * 4L)
    operator fun set(index: Int, value: Vec4i8) {
        UNSAFE.copyMemory(value.address, address + index * 4L, 4L)
    }

    operator fun plus(offset: Long): Vec4i8 = Vec4i8(address + offset)
    operator fun minus(offset: Long): Vec4i8 = Vec4i8(address - offset)

    fun copyTo(dest: Vec4i8) {
        UNSAFE.copyMemory(address, dest.address, 4L)
    }

    override fun toString(): String = "Vec4i8(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 4L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, x: Byte, y: Byte, z: Byte, w: Byte): Vec4i8 {
            val v = Vec4i8(container)
            v.x = x
            v.y = y
            v.z = z
            v.w = w
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, x: Byte, y: Byte, z: Byte, w: Byte): Vec4i8 {
            val v = Vec4i8(container)
            v.x = x
            v.y = y
            v.z = z
            v.w = w
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): Vec4i8 = Vec4i8(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, x: Byte, y: Byte, z: Byte, w: Byte): Vec4i8 {
            val v = Vec4i8(ptr)
            v.x = x
            v.y = y
            v.z = z
            v.w = w
            return v
        }
    }
}
