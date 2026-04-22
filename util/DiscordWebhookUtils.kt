package dev.wizard.meta.util

import com.google.gson.JsonObject
import dev.wizard.meta.MetaMod
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.io.IOUtils

object DiscordWebhookUtils {
    fun sendMessage(content: String, url: String, username: String, avatar: String) {
        DefaultScope.launch(Dispatchers.IO) {
            ConnectionUtils.runConnection(url, { connection ->
                val json = JsonObject().apply {
                    addProperty("username", username)
                    addProperty("content", content)
                    addProperty("avatar_url", avatar)
                }
                val bytes = json.toString().toByteArray(Charsets.UTF_8)
                
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "")
                connection.requestMethod = "POST"
                
                connection.outputStream.use { it.write(bytes) }
                
                val response = connection.inputStream.use { IOUtils.toString(it, Charsets.UTF_8) }
                if (response.isNotEmpty()) {
                    MetaMod.logger.info("Unexpected response from discord webhook http request: $response")
                }
                Unit
            }, { e ->
                MetaMod.logger.error("Error while sending webhook", e)
            })
        }
    }
}
