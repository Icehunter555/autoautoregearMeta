package dev.wizard.meta.util.alting

import com.google.common.base.MoreObjects
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.util.UUIDTypeAdapter
import dev.wizard.meta.util.readText
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.UIManager

class MSA {
    private val logger = LogManager.getLogger("DevLogin-MSA")
    private val urlRegex = Regex("<a href=\"(.*?)\">.*?</a>")
    private val tagRegex = Regex("<([A-Za-z]*?).*?>(.*?)</\\1>")
    private val tokenFile: File = File(System.getenv("MSA_TOKEN_FILE") ?: "trollhack/dev_login_token.json")
    private var proxy: Proxy = Proxy.NO_PROXY
    private var noDialog: Boolean = false
    private var mainDialog: JFrame? = null
    private var deviceCode: String? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var xblToken: String? = null
    private var userHash: String? = null
    private var xstsToken: String? = null
    private var mcToken: String? = null
    var profile: MinecraftProfile? = null
        private set
    private var isCancelled: Boolean = false

    fun isLoggedIn(): Boolean = mcToken != null

    fun loginBlocking(proxy: Proxy, storeRefreshToken: Boolean, noDialog: Boolean) {
        runBlocking {
            login(proxy, storeRefreshToken, noDialog)
        }
    }

    suspend fun login(proxy: Proxy, storeRefreshToken: Boolean, noDialog: Boolean) {
        if (!noDialog) {
            System.setProperty("java.awt.headless", "false")
        }
        this.proxy = proxy
        this.noDialog = noDialog

        if (tokenFile.exists()) {
            val data = readData() ?: emptyMap()
            refreshToken = data["refreshToken"]
            mcToken = data["mcToken"]
            if (reqProfile()) {
                logger.info("Cached token is valid.")
                return
            }
        }

        if (refreshToken != null) {
            logger.info("Cached token is invalid, requesting new one using refresh token.")
            refreshToken().onFailure {
                reqTokens()
            }
        } else {
            logger.info("Cached token is invalid.")
            reqTokens()
        }

        if (accessToken == null) return

        reqXBLToken()
        if (xblToken == null) return

        reqXSTSToken()
        if (xstsToken == null) return

        reqMinecraftToken()
        if (mcToken == null) {
            refreshToken = null
        } else if (reqProfile()) {
            saveData(storeRefreshToken)
        }
    }

    private suspend fun reqTokens() {
        doRequest(
            "POST",
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
            "client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8") + "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", "UTF-8"),
            mapOf("Content-Type" to "application/x-www-form-urlencoded")
        ).onSuccess {
            val respObj = JsonParser().parse(it).asJsonObject
            deviceCode = respObj.get("device_code").asString
            val verificationUri = respObj.get("verification_uri").asString
            val userCode = respObj.get("user_code").asString
            mainDialog = showDialog(
                "DevLogin MSA Authentication",
                "Please visit <a href=\\