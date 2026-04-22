package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class Vec4f32(val address: Long) {
    var x: Float
        get() = UNSAFE.getFloat(address)
        set(value) { UNSAFE.putFloat(address, value) }

    var y: Float
        get() = UNSAFE.getFloat(address + 4L)
        set(value) { UNSAFE.putFloat(address + 4L, value) }

    var z: Float
        get() = UNSAFE.getFloat(address + 8L)
        set(value) { UNSAFE.putFloat(address + 8L, value) }

    var w: Float
        get() = UNSAFE.getFloat(address + 12L)
        set(value) { UNSAFE.putFloat(address + 12L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): Vec4f32 = Vec4f32(address + 16L)
    fun dec(): Vec4f32 = Vec4f32(address - 16L)

    operator fun get(index: Int): Vec4f32 = Vec4f32(address + index * 16L)
    operator fun set(index: Int, value: Vec4f32) {
        UNSAFE.copyMemory(value.address, address + index * 16L, 16L)
    }

    operator fun plus(offset: Long): Vec4f32 = Vec4f32(address + offset)
    operator fun minus(offset: Long): Vec4f32 = Vec4f32(address - offset)

    fun copyTo(dest: Vec4f32) {
        UNSAFE.copyMemory(address, dest.address, 16L)
    }

    override fun toString(): String = "Vec4f32(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 16L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, x: Float, y: Float, z: Float, w: Float): Vec4f32 {
            val v = Vec4f32(container)
            v.x = x
            v.y = y
            v.z = z
            v.w = w
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, x: Float, y: Float, z: Float, w: Float): Vec4f32 {
            val v = Vec4f32(container)
            v.x = x
            v.y = y
            v.z = z
            v.w = w
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): Vec4f32 = Vec4f32(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, x: Float, y: Float, z: Float, w: Float): Vec4f32 {
            val v = Vec4f32(ptr)
            v.x = x
            v.y = y
            v.z = z
            v.w = w
            return v
        }
    }
}
