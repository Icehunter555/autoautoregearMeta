package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import dev.luna5ama.kmogus.Ptr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class Vec2f32(val address: Long) {
    var x: Float
        get() = UNSAFE.getFloat(address)
        set(value) { UNSAFE.putFloat(address, value) }

    var y: Float
        get() = UNSAFE.getFloat(address + 4L)
        set(value) { UNSAFE.putFloat(address + 4L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): Vec2f32 = Vec2f32(address + 8L)
    fun dec(): Vec2f32 = Vec2f32(address - 8L)

    operator fun get(index: Int): Vec2f32 = Vec2f32(address + index * 8L)
    operator fun set(index: Int, value: Vec2f32) {
        UNSAFE.copyMemory(value.address, address + index * 8L, 8L)
    }

    operator fun plus(offset: Long): Vec2f32 = Vec2f32(address + offset)
    operator fun minus(offset: Long): Vec2f32 = Vec2f32(address - offset)

    fun copyTo(dest: Vec2f32) {
        UNSAFE.copyMemory(address, dest.address, 8L)
    }

    override fun toString(): String = "Vec2f32(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 8L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, x: Float, y: Float): Vec2f32 {
            val v = Vec2f32(container)
            v.x = x
            v.y = y
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, x: Float, y: Float): Vec2f32 {
            val v = Vec2f32(container)
            v.x = x
            v.y = y
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): Vec2f32 = Vec2f32(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, x: Float, y: Float): Vec2f32 {
            val v = Vec2f32(ptr)
            v.x = x
            v.y = y
            return v
        }
    }
}
