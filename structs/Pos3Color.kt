package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class Pos3Color(val address: Long) {
    val pos: Vec3f32 get() = Vec3f32(address)

    var color: Int
        get() = UNSAFE.getInt(address + 12L)
        set(value) { UNSAFE.putInt(address + 12L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): Pos3Color = Pos3Color(address + 16L)
    fun dec(): Pos3Color = Pos3Color(address - 16L)

    operator fun get(index: Int): Pos3Color = Pos3Color(address + index * 16L)
    operator fun set(index: Int, value: Pos3Color) {
        UNSAFE.copyMemory(value.address, address + index * 16L, 16L)
    }

    operator fun plus(offset: Long): Pos3Color = Pos3Color(address + offset)
    operator fun minus(offset: Long): Pos3Color = Pos3Color(address - offset)

    fun copyTo(dest: Pos3Color) {
        UNSAFE.copyMemory(address, dest.address, 16L)
    }

    override fun toString(): String = "Pos3Color(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 16L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, pos: Long, color: Int): Pos3Color {
            val v = Pos3Color(container)
            v.pos.address.let { UNSAFE.copyMemory(pos, it, 12L) }
            v.color = color
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, pos: Long, color: Int): Pos3Color {
            val v = Pos3Color(container)
            v.pos.address.let { UNSAFE.copyMemory(pos, it, 12L) }
            v.color = color
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): Pos3Color = Pos3Color(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, pos: Long, color: Int): Pos3Color {
            val v = Pos3Color(ptr)
            v.pos.address.let { UNSAFE.copyMemory(pos, it, 12L) }
            v.color = color
            return v
        }
    }
}
