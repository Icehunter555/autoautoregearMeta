package dev.wizard.meta.translation

import dev.wizard.meta.util.IDRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

interface ITranslationType {
    val typeName: String
    fun commonKey(string: String): TranslationKey = throw UnsupportedOperationException()
    fun commonKey(pair: Pair<String, String>): TranslationKey = throw UnsupportedOperationException()
    fun transform(src: ITranslateSrc, string: String): String
}

enum class TranslateType(override val typeName: String) : ITranslationType {
    COMMON("COMMON") {
        override fun commonKey(string: String): TranslationKey {
            return TranslationKey.getOrPut(this, transform(string), string)
        }

        override fun commonKey(pair: Pair<String, String>): TranslationKey {
            return TranslationKey.getOrPut(this, transform(pair.first), pair.second)
        }

        override fun transform(src: ITranslateSrc, string: String): String {
            return transform(string)
        }

        private fun transform(string: String): String {
            return "$I18N_PREFIX$typeName.${replaceChars(string)}$I18N_SUFFIX"
        }
    },
    SPECIFIC("SPECIFIC") {
        override fun transform(src: ITranslateSrc, string: String): String {
            return "$I18N_PREFIX$typeName.${src.srcIdentifier}.${replaceChars(string)}$I18N_SUFFIX"
        }
    },
    LONG("LONG") {
        override fun transform(src: ITranslateSrc, string: String): String {
            return "$I18N_PREFIX$typeName.${src.srcIdentifier}.${src.keyID++}.${replaceChars(string)}$I18N_SUFFIX"
        }
    }
}

class TranslateSrc(srcIdentifierIn: String) : ITranslateSrc {
    override val srcIdentifier: String = replaceChars(srcIdentifierIn)
    override var keyID: Int = 0
}

class TranslationKey private constructor(
    val type: TranslateType,
    val keyString: String,
    val rootString: String
) : CharSequence {
    val id: Int = idRegistry.register()
    private var cached: String? = null

    init {
        translationKeyMap[keyString] = this
    }

    override val length: Int get() = get().length
    override fun get(index: Int): Char get() = get()[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = get().subSequence(startIndex, endIndex)

    fun update() {
        cached = null
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = get()

    fun get(): String {
        return cached ?: TranslationManager.getTranslated(this).also { cached = it }
    }

    override fun toString(): String = get()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TranslationKey) return false
        return type == other.type && keyString == other.keyString && rootString == other.rootString
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + keyString.hashCode()
        result = 31 * result + rootString.hashCode()
        return result
    }

    companion object {
        private val idRegistry = IDRegistry()
        private val translationKeyMap = ConcurrentHashMap<String, TranslationKey>()

        val allKeys: Collection<TranslationKey> get() = translationKeyMap.values

        fun getOrPut(type: TranslateType, keyString: String, rootString: String): TranslationKey {
            return translationKeyMap.getOrPut(keyString) {
                TranslationKey(type, keyString, rootString)
            }
        }

        operator fun get(string: String): TranslationKey? = translationKeyMap[string]

        fun updateAll() {
            allKeys.forEach { it.update() }
        }
    }
}

private fun replaceChars(string: String): String {
    return string.map {
        val c = it.lowercaseChar()
        if (c == ' ' || c == '.') '_' else c
    }.joinToString("")
}
