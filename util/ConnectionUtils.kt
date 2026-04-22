package dev.wizard.meta.util

import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ConnectionUtils {
    fun requestRawJsonFrom(url: String, onCatch: (Exception) -> Unit = { it.printStackTrace() }): String? {
        return runConnection(url, { connection ->
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.requestMethod = "GET"
            connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        }, onCatch)
    }

    fun <T> runConnection(url: String, block: (HttpsURLConnection) -> T?, onCatch: (Exception) -> Unit = { it.printStackTrace() }): T? {
        val connection = URL(url).openConnection() as HttpsURLConnection
        return try {
            connection.doOutput = true
            connection.doInput = true
            block(connection)
        } catch (e: Exception) {
            onCatch(e)
            null
        } finally {
            connection.disconnect()
        }
    }
}
