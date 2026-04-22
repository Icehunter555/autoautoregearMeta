package dev.wizard.meta.util

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntCollection
import java.util.Comparator
import org.jetbrains.annotations.NotNull
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class Bind(
    @NotNull val modifierKeys: IntAVLTreeSet,
    private var key: Int
) {
    private var cachedName: String = ""

    init {
        this.cachedName = this.getName()
    }

    constructor() : this(0)

    constructor(key: Int) : this(IntAVLTreeSet(Companion.keyComparator), key)

    constructor(modifierKeys: IntArray, key: Int) : this(IntAVLTreeSet(Companion.keyComparator), key) {
        for (it in modifierKeys) {
            this.modifierKeys.add(it)
        }
        this.cachedName = this.getName()
    }

    fun getKey(): Int = this.key

    fun isEmpty(): Boolean {
        val n = this.key
        if (n in 1..255) {
            return false
        }
        return n < -16 || n >= 0
    }

    fun isDown(): Boolean {
        if (this.isEmpty()) return false
        if (this.key < 0) {
            if (!Mouse.isButtonDown(-this.key - 1)) return false
            synchronized(this) {
                return this.modifierKeys.all { this.isModifierKeyDown(this.key, it) }
            }
        }
        return Keyboard.isKeyDown(this.key)
    }

    fun isDown(eventKey: Int): Boolean {
        if (this.isEmpty()) return false
        if (this.key != eventKey) return false
        synchronized(this) {
            return this.modifierKeys.all { this.isModifierKeyDown(eventKey, it) }
        }
    }

    private fun isModifierKeyDown(eventKey: Int, modifierKey: Int): Boolean {
        if (eventKey == modifierKey) return false
        when (modifierKey) {
            29, 157 -> return Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157)
            56, 184 -> return Keyboard.isKeyDown(56) || Keyboard.isKeyDown(184)
            42, 54 -> return Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54)
            219, 220 -> return Keyboard.isKeyDown(219) || Keyboard.isKeyDown(220)
        }
        if (modifierKey !in 0..255) return false
        return Keyboard.isKeyDown(modifierKey)
    }

    fun setBind(keyIn: Int) {
        val cache2 = IntArrayList(0)
        for (key in 255 downTo 1) {
            if (key == keyIn || !Keyboard.isKeyDown(key)) continue
            cache2.add(key)
        }
        this.setBind(cache2 as IntCollection, keyIn)
    }

    fun setBind(modifierKeysIn: IntCollection, keyIn: Int) {
        synchronized(this) {
            this.modifierKeys.clear()
            this.modifierKeys.addAll(modifierKeysIn)
            this.key = keyIn
            this.cachedName = this.getName()
        }
    }

    fun clear() {
        synchronized(this) {
            this.modifierKeys.clear()
            this.key = 0
            this.cachedName = this.getName()
        }
    }

    override fun toString(): String {
        return this.cachedName
    }

    private fun getName(): String {
        if (this.isEmpty()) {
            return "None"
        }
        val sb = StringBuilder()
        for (key in this.modifierKeys) {
            val k = key.toInt()
            val name = modifierName[k] ?: KeyboardUtils.getDisplayName(k)
            if (name != null) {
                sb.append(name)
                sb.append('+')
            }
        }
        if (this.key >= 0) {
            sb.append(KeyboardUtils.getDisplayName(this.key))
        } else {
            sb.append("Mouse ")
            sb.append(-this.key - 1)
        }
        return sb.toString()
    }

    companion object {
        private val modifierName = Int2ObjectLinkedOpenHashMap<String>().apply {
            put(29, "Ctrl")
            put(157, "Ctrl")
            put(56, "Alt")
            put(184, "Alt")
            put(42, "Shift")
            put(54, "Shift")
            put(219, "Meta")
            put(220, "Meta")
        }
        private val priorityMap = Int2IntLinkedOpenHashMap()
        val keyComparator: Comparator<Int> = Comparator { a, b ->
            priorityMap.get(a.toInt()).compareTo(priorityMap.get(b.toInt()))
        }

        init {
            val priorityKey = intArrayOf(29, 157, 56, 184, 42, 54, 219, 220)
            priorityKey.forEachIndexed { index, key ->
                priorityMap.put(key, index / 2)
            }
            val sortedKeys = KeyboardUtils.allKeys.sortedBy { Keyboard.getKeyName(it) }
            sortedKeys.forEachIndexed { index, key ->
                priorityMap.putIfAbsent(key, index + priorityKey.size / 2)
            }
        }
    }
}
