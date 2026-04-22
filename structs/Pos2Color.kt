package dev.wizard.meta.structs

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import sun.misc.Unsafe

@JvmInline
value class Pos2Color(val address: Long) {
    val pos: Vec2f32 get() = Vec2f32(address)

    var color: Int
        get() = UNSAFE.getInt(address + 8L)
        set(value) { UNSAFE.putInt(address + 8L, value) }

    constructor(container: Arr) : this(container.ptr)
    constructor(container: MutableArr) : this(container.ptr)

    fun inc(): Pos2Color = Pos2Color(address + 12L)
    fun dec(): Pos2Color = Pos2Color(address - 12L)

    operator fun get(index: Int): Pos2Color = Pos2Color(address + index * 12L)
    operator fun set(index: Int, value: Pos2Color) {
        UNSAFE.copyMemory(value.address, address + index * 12L, 12L)
    }

    operator fun plus(offset: Long): Pos2Color = Pos2Color(address + offset)
    operator fun minus(offset: Long): Pos2Color = Pos2Color(address - offset)

    fun copyTo(dest: Pos2Color) {
        UNSAFE.copyMemory(address, dest.address, 12L)
    }

    override fun toString(): String = "Pos2Color(address=$address)"

    val ptr: Long get() = address

    companion object {
        const val size = 12L
        private val UNSAFE: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as Unsafe

        @JvmStatic
        operator fun invoke(container: Arr, pos: Long, color: Int): Pos2Color {
            val v = Pos2Color(container)
            v.pos.address.let { UNSAFE.copyMemory(pos, it, 8L) }
            v.color = color
            return v
        }

        @JvmStatic
        operator fun invoke(container: MutableArr, pos: Long, color: Int): Pos2Color {
            val v = Pos2Color(container)
            v.pos.address.let { UNSAFE.copyMemory(pos, it, 8L) }
            v.color = color
            return v
        }

        @JvmStatic
        operator fun invoke(ptr: Long): Pos2Color = Pos2Color(ptr)

        @JvmStatic
        operator fun invoke(ptr: Long, pos: Long, color: Int): Pos2Color {
            val v = Pos2Color(ptr)
            v.pos.address.let { UNSAFE.copyMemory(pos, it, 8L) }
            v.color = color
            return v
        }
    }
}
