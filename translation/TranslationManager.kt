package dev.wizard.meta.translation

import dev.wizard.meta.MetaMod
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.IOUtilsKt
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.*
import java.io.*
import java.net.URL
import java.util.*
import java.util.zip.ZipFile

object TranslationManager {
    private var translationMap: TranslationMap? = null
    private var lastJob: Job? = null

    init {
        File(I18N_LOCAL_DIR).mkdirs()
    }

    fun checkUpdate() {
        DefaultScope.launch(Dispatchers.IO) {
            try {
                var localHash: String? = null
                val onlineCacheFile = File(I18N_ONLINE_CACHE_DIR)
                if (onlineCacheFile.exists()) {
                    ZipFile(onlineCacheFile).use { zip ->
                        val entry = zip.entries().asSequence().find { it.name.substringAfterLast('/') == ".hash" }
                        localHash = entry?.let { zip.getInputStream(it).use { input -> IOUtilsKt.readText(input) } }
                    }
                }

                val onlineHash = URL(I18N_ONLINE_HASH_URL).readBytes().toString(Charsets.UTF_8)
                if (localHash != onlineHash) {
                    URL(I18N_ONLINE_DOWNLOAD_URL).openStream().use { input ->
                        onlineCacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                MetaMod.logger.warn("Failed to check for translation updates", e)
            }
        }
    }

    fun getTranslated(key: TranslationKey): String {
        if (Settings.INSTANCE.settingLanguage.startsWith("en", ignoreCase = true)) {
            return key.rootString
        }
        val map = translationMap
        if (map != null) {
            return map[key]
        }

        if (lastJob?.isActive != true) {
            lastJob = DefaultScope.launch(Dispatchers.IO) {
                reload()
            }
        }
        return key.rootString
    }

    fun reload() {
        val language = Settings.INSTANCE.settingLanguage.lowercase(Locale.ROOT)
        try {
            val (content, source) = tryRead(language)
            translationMap = TranslationMap.fromString(language, content)
            MetaMod.logger.info("Loaded language $language from $source")
            TranslationKey.updateAll()
        } catch (e: IllegalArgumentException) {
            MetaMod.logger.warn(e.message)
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed to load language $language", e)
        }
    }

    private fun tryRead(language: String): Pair<String, String> {
        tryReadLocal(language)?.let { return it to "local" }
        tryReadOnline(language)?.let { return it to "online" }
        tryReadJar(language)?.let { return it to "jar" }
        throw IllegalArgumentException("No .lang file found for language $language")
    }

    private fun tryReadLocal(language: String): String? {
        val file = File("$I18N_LOCAL_DIR/$language.lang")
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    private fun tryReadOnline(language: String): String? {
        val file = File(I18N_ONLINE_CACHE_DIR)
        if (file.exists()) {
            ZipFile(file).use {
                val entry = zip.entries().asSequence().find { it.name.substringAfterLast('/') == "$language.lang" }
                return entry?.let { zip.getInputStream(it).use { input -> IOUtilsKt.readText(input) } }
            }
        }
        return null
    }

    private fun tryReadJar(language: String): String? {
        return this::class.java.getResource("$I18N_JAR_DIR$language.lang")?.readBytes()?.toString(Charsets.UTF_8)
    }

    fun dump() {
        val file = File("$I18N_LOCAL_DIR/en_us.lang")
        file.createNewFile()
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            TranslationKey.allKeys.sortedBy { it.keyString }.forEach {
                writer.write("${it.keyString}=${it.rootString}\n")
            }
        }
    }

    fun update() {
        val file = File("$I18N_LOCAL_DIR/${Settings.INSTANCE.settingLanguage}.lang")
        file.createNewFile()
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            TranslationKey.allKeys.sortedBy { it.keyString }.forEach {
                writer.write("${it.keyString}=$it\n")
            }
        }
    }
}
