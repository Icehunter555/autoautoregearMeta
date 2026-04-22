package dev.wizard.meta.translation

import dev.fastmc.common.collection.FastIntMap

class TranslationMap private constructor(
    val language: String,
    private val translations: FastIntMap<String>
) {
    operator fun get(key: TranslationKey): String {
        return translations.get(key.id) ?: key.rootString
    }

    companion object {
        private val regex = Regex("^(\\\$.+\\$)=(.+)$")

        fun fromString(language: String, input: String): TranslationMap {
            val map = FastIntMap<String>()
            input.lines().forEach { line ->
                regex.matchEntire(line)?.let {
                    val keyString = it.groupValues[1]
                    TranslationKey[keyString]?.let { key ->
                        map.set(key.id, it.groupValues[2])
                    }
                }
            }
            return TranslationMap(language, map)
        }
    }
}
