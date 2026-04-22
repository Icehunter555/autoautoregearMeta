package dev.wizard.meta.graphics.font

import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.font.glyph.CharInfo
import java.awt.Font
import java.io.*
import java.security.MessageDigest
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GlyphCache {
    private const val oldDir = "trollhack/glyphs"
    private const val directory = "trollhack/glyph_cache_v1"

    init {
        try {
            File(oldDir).deleteRecursively()
        } catch (e: Throwable) {
            // ignore
        }
    }

    fun delete(font: Font) {
        try {
            val regularFont = font.deriveFont(Font.PLAIN)
            File("$directory/${hashName(regularFont)}").delete()
        } catch (e: Throwable) {
            // ignore
        }
    }

    suspend fun put(font: Font, chunk: Int, entry: Entry) {
        val regularFont = font.deriveFont(Font.PLAIN)
        val dirPath = "$directory/${hashName(regularFont)}"
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val path = "$dirPath/${font.style}-$chunk"
        val infoFile = File("$path.bin.gz")

        runCatching {
            saveInfo(infoFile, entry)
        }.onFailure { e ->
            MetaMod.logger.info("Failed saving glyph cache", e)
            runCatching {
                infoFile.delete()
            }.onFailure { it ->
                MetaMod.logger.error("Error saving glyph cache", it)
            }
        }
    }

    suspend fun get(font: Font, chunk: Int): Entry? {
        val regularFont = font.deriveFont(Font.PLAIN)
        val path = "$directory/${hashName(regularFont)}/${font.style}-$chunk"
        val infoFile = File("$path.bin.gz")
        if (!infoFile.exists()) return null

        return runCatching {
            loadInfo(infoFile)
        }.onFailure { it ->
            MetaMod.logger.error("Error reading glyph cache", it)
        }.getOrNull()
    }

    private suspend fun loadInfo(file: File): Entry = withContext(Dispatchers.IO) {
        val uncompressedSize = RandomAccessFile(file, "r").use { raf ->
            raf.seek(raf.length() - 4)
            val b4 = raf.read()
            val b3 = raf.read()
            val b2 = raf.read()
            val b1 = raf.read()
            (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }

        DataInputStream(GZIPInputStream(FileInputStream(file).buffered())).use { dis ->
            val count = dis.readInt()
            val width = dis.readInt()
            val height = dis.readInt()
            val levels = dis.readInt()
            val baseSize = dis.readInt()

            val charInfoArray = Array(count) {
                val widthF = dis.readFloat()
                val heightF = dis.readFloat()
                val renderWidthF = dis.readFloat()
                val uv = shortArrayOf(dis.readShort(), dis.readShort(), dis.readShort(), dis.readShort())
                CharInfo(widthF, heightF, renderWidthF, uv)
            }

            val headerSize = 20 + 20 * count
            val data = ByteArray(uncompressedSize - headerSize)
            dis.readFully(data)
            Entry(count, width, height, levels, baseSize, charInfoArray, data)
        }
    }

    private suspend fun saveInfo(file: File, entry: Entry) = withContext(Dispatchers.IO) {
        DataOutputStream(GZIPOutputStream(FileOutputStream(file))).use { dos ->
            dos.writeInt(entry.count)
            dos.writeInt(entry.width)
            dos.writeInt(entry.height)
            dos.writeInt(entry.levels)
            dos.writeInt(entry.baseSize)
            for (charInfo in entry.charInfoArray) {
                dos.writeFloat(charInfo.width)
                dos.writeFloat(charInfo.height)
                dos.writeFloat(charInfo.renderWidth)
                dos.writeShort(charInfo.uv[0].toInt())
                dos.writeShort(charInfo.uv[1].toInt())
                dos.writeShort(charInfo.uv[2].toInt())
                dos.writeShort(charInfo.uv[3].toInt())
            }
            dos.write(entry.data)
        }
    }

    private fun hashName(font: Font): String {
        val transforms = DoubleArray(6)
        font.transform.getMatrix(transforms)
        val str = "${font.name}:${font.size}:${transforms.contentToString()}"
        val hash = MessageDigest.getInstance("MD5").digest(str.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    data class Entry(
        val count: Int,
        val width: Int,
        val height: Int,
        val levels: Int,
        val baseSize: Int,
        val charInfoArray: Array<CharInfo>,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry) return false
            if (count != other.count) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (levels != other.levels) return false
            if (baseSize != other.baseSize) return false
            if (!charInfoArray.contentEquals(other.charInfoArray)) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = count
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + levels
            result = 31 * result + baseSize
            result = 31 * result + charInfoArray.contentHashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
