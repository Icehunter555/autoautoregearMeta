package dev.wizard.meta.module.modules.render

import dev.wizard.meta.MetaMod
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.threads.onMainThreadSafe
import kotlinx.coroutines.*
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

object Cosmetics : Module(
    "Cosmetics",
    category = Category.RENDER,
    description = "render the client's capes"
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val onlyUserCape by setting(this, BooleanSetting(settingName("Only User"), false))
    private val reloadCapes = setting(this, BooleanSetting(settingName("Reload Capes"), true))

    private const val CAPE_REPO = "https://raw.githubusercontent.com/AGENTISNUM1/Meta-Assets/refs/heads/main/"

    val chinese = listOf("passcope", "obamacity", "imloveimlovewee", "cute_lemon_owo", "dennis911", "thr0wing", "qay", "nekononakama")
    val chill = listOf("ccetl", "wheatemperor", "liketinos2341", "mmmmchezburgers", "blazyparty", "why_me_")
    val users = listOf("berry_o7", "barackobamar", "innocentbanana", "3uer", "x1angg", "sirwrong", "s1owwalk", "guofuo", "quackhack")

    val userCape = ResourceLocation("textures/capes/meta.png")
    val wizardCape = ResourceLocation("textures/capes/wizard.png")

    private var chinaCape: ResourceLocation? = null
    private var chillCape: ResourceLocation? = null
    private var passCape: ResourceLocation? = null

    init {
        onEnable {
            loadRemoteCapes()
            reloadCapes.value = true
        }

        reloadCapes.valueListeners.add { _, _ ->
            loadRemoteCapes()
            reloadCapes.value = true
        }
    }

    private fun loadRemoteCapes() {
        scope.launch {
            try {
                chinaCape = loadCapeTexture("china.png", "meta_china_cape")
                chillCape = loadCapeTexture("chill.png", "meta_chill_cape")
                passCape = loadCapeTexture("pass.png", "meta_pass_cape")
                MetaMod.logger.info("[Cosmetics] loading complete!")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun loadCapeTexture(fileName: String, resourceName: String): ResourceLocation? {
        return try {
            val url = URL(CAPE_REPO + fileName)
            val image = ImageIO.read(url)
            val texture = DynamicTexture(image)
            val resourceLocation = ResourceLocation("meta", resourceName)
            onMainThreadSafe {
                mc.textureManager.loadTexture(resourceLocation, texture)
            }
            resourceLocation
        } catch (e: Exception) {
            MetaMod.logger.error("[Cosmetics] Failed to load $fileName: ${e.message}")
            null
        }
    }

    @JvmStatic
    fun handlePlayer(player: AbstractClientPlayer, cir: CallbackInfoReturnable<ResourceLocation>) {
        val playerName = player.name.lowercase(Locale.ROOT)

        if (INSTANCE.onlyUserCape) {
            if (users.contains(playerName) || playerName == "wizard_11") {
                cir.returnValue = userCape
            }
            return
        }

        when {
            chinese.contains(playerName) -> chinaCape?.let { cir.returnValue = it }
            chill.contains(playerName) -> chillCape?.let { cir.returnValue = it }
            users.contains(playerName) -> cir.returnValue = userCape
            playerName == "wizard_11" -> cir.returnValue = wizardCape
            playerName == "passc0de" -> passCape?.let { cir.returnValue = it }
        }
    }
}
