package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class Vec2i16(val address: Long) {
    var x: Short
        get() = UNSAFE.getShort(address)
        set(value) { UNSAFE.putShort(address, value) }

    var y: Short
        get() = UNSAFE.getShort(address + 2L)
        set(value) { UNSAFE.putShort(address + 2L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): Vec2i16 = Vec2i16(address + 4L)
    fun dec(): Vec2i16 = Vec2i16(address - 4L)

    operator fun get(index: Int): Vec2i16 = Vec2i16(address + index * 4L)
    operator fun set(index: Int, value: Vec2i16) {
        UNSAFE.copyMemory(value.address, address + index * 4L, 4L)
    }

    operator fun plus(offset: Long): Vec2i16 = Vec2i16(address + offset)
    operator fun minus(offset: Long): Vec2i16 = Vec2i16(address - offset)

    fun copyTo(dest: Vec2i16) {
        UNSAFE.copyMemory(address, dest.address, 4L)
    }

    override fun toString(): String = "Vec2i16(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 4L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, x: Short, y: Short): Vec2i16 {
            val v = Vec2i16(container)
            v.x = x
            v.y = y
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, x: Short, y: Short): Vec2i16 {
            val v = Vec2i16(container)
            v.x = x
            v.y = y
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): Vec2i16 = Vec2i16(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, x: Short, y: Short): Vec2i16 {
            val v = Vec2i16(ptr)
            v.x = x
            v.y = y
            return v
        }
    }
}
