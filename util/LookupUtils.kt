package dev.wizard.meta.util

import com.google.gson.JsonParser
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

object LookupUtils {
    fun getNameHistoryFromUUID(uuid: UUID): Map<String, String>? {
        val result = TreeMap<String, String>(reverseOrder())
        return try {
            val url = "https://laby.net/api/v2/user/$uuid/get-profile"
            val connection = URL(url).openConnection() as HttpsURLConnection
            val array = try {
                connection.doOutput = true
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser().parse(json).asJsonObject
                jsonObject.getAsJsonArray("username_history")
            } finally {
                connection.disconnect()
            }

            if (array == null) return null

            for (element in array) {
                val obj = element.asJsonObject
                val name = obj.get("username").asString
                val changedAt = if (obj.has("changed_at")) obj.get("changed_at").asString else ""
                result[changedAt] = name
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            result
        }
    }
}
