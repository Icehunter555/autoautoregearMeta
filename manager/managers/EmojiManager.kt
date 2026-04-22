package dev.wizard.meta.manager.managers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.texture.MipmapTexture
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.IOUtilsKt.readText
import dev.wizard.meta.util.threads.DefaultScope
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.lwjgl.opengl.GL11

object EmojiManager : Manager() {
    private val localFile = File("trollhack/emoji.zip")
    private const val versionURL = "https://raw.githubusercontent.com/AGENTISNUM1/meta-emojis/master/version.json"
    private const val zipUrl = "https://github.com/AGENTISNUM1/meta-emojis/archive/master.zip"
    private val parser = JsonParser()
    private val emojiMap = HashMap<String, MipmapTexture>()
    private var zipCache: ZipCache? = null
    private val job: Job

    init {
        job = DefaultScope.launch(Dispatchers.IO) {
            try {
                checkEmojiUpdate()
            } catch (e: Exception) {
                MetaMod.logger.warn("Failed to check emoji update", e)
            }
            try {
                if (localFile.exists()) {
                    zipCache = ZipCache(ZipFile(localFile))
                    MetaMod.logger.info("Emoji Initialized")
                }
            } catch (e: Exception) {
                MetaMod.logger.warn("Failed to load emojis", e)
            }
        }
    }

    private fun checkEmojiUpdate() {
        val globalVer = streamToJson(URL(versionURL).openStream())
        val localVer = getLocalVersion()
        if (localVer == null || (globalVer != null && globalVer["version"] != localVer["version"])) {
            URL(zipUrl).openStream().use { online ->
                localFile.parentFile?.mkdirs()
                FileOutputStream(localFile).use { output ->
                    online.copyTo(output)
                }
            }
        }
    }

    private fun getLocalVersion(): JsonObject? {
        if (!localFile.exists()) return null
        val zipFile = ZipFile(localFile)
        val entry = zipFile.entries().asSequence()
            .filterNot { it.isDirectory }
            .find { it.name.substringAfterLast('/') == "version.json" }
        return entry?.let { streamToJson(zipFile.getInputStream(it)) }
    }

    private fun streamToJson(stream: InputStream): JsonObject? {
        return try {
            stream.use {
                parser.parse(it.readText()).asJsonObject
            }
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed to parse emoji version Json", e)
            null
        }
    }

    fun getEmoji(name: String?): MipmapTexture? {
        if (name == null || job.isActive) return null
        if (!emojiMap.containsKey(name)) {
            loadEmoji(name)
        }
        return emojiMap[name]
    }

    fun isEmoji(name: String?): Boolean {
        return getEmoji(name) != null
    }

    private fun loadEmoji(name: String) {
        val inputStream = zipCache?.get(name) ?: return
        try {
            val image = inputStream.use { ImageIO.read(it) } ?: return
            val texture = MipmapTexture(image, GL11.GL_RGBA, 3)
            texture.bindTexture()
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR)
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_LOD_BIAS, -0.5f)
            texture.unbindTexture()
            emojiMap[name] = texture
        } catch (e: IOException) {
            MetaMod.logger.warn("Failed to load emoji", e)
        }
    }

    private class ZipCache(private val zipFile: ZipFile) {
        private val entries: Map<String, ZipEntry>

        init {
            entries = zipFile.entries().asSequence()
                .filterNot { it.isDirectory }
                .filter { it.name.endsWith(".png") }
                .associateBy { entry ->
                    entry.name.substringAfterLast('/').substringBeforeLast('.')
                }
        }

        fun get(name: String): InputStream? {
            return entries[name]?.let { zipFile.getInputStream(it) }
        }
    }
}
