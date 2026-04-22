package dev.wizard.meta.translation

interface ITranslateSrc {
    val srcIdentifier: String
    var keyID: Int

    fun TranslateType.key(string: String): TranslationKey {
        return TranslationKey.getOrPut(this, transform(this@ITranslateSrc, string), string)
    }

    fun TranslateType.key(pair: Pair<String, String>): TranslationKey {
        return TranslationKey.getOrPut(this, transform(this@ITranslateSrc, pair.first), pair.second)
    }
}
