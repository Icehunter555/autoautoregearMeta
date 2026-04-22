package dev.wizard.meta

import dev.wizard.meta.event.ForgeEventProcessor
import dev.wizard.meta.event.events.ShutdownEvent
import dev.wizard.meta.graphics.IconUtils
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.modules.misc.AltProtect
import dev.wizard.meta.translation.TranslationManager
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.SecurityUtils
import dev.wizard.meta.util.threads.TimerScope
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.GameSettings
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.opengl.Display
import java.io.File
import java.nio.ByteBuffer

@Mod(modid = Metadata.ID, name = Metadata.NAME, version = Metadata.VERSION)
class MetaMod {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        val directory = File(DIRECTORY)
        if (!directory.exists()) {
            directory.mkdir()
        }
        Display.setTitle("Loading $NAME $VERSION")
        LoaderWrapper.preLoadAll()
        TranslationManager.checkUpdate()
        try {
            val icon16 = Minecraft::class.java.getResourceAsStream("/assets/minecraft/textures/icon16.png")
            val icon32 = Minecraft::class.java.getResourceAsStream("/assets/minecraft/textures/icon32.png")
            if (icon16 != null && icon32 != null) {
                val icons = arrayOf(
                    IconUtils.readImageToBuffer(icon16),
                    IconUtils.readImageToBuffer(icon32)
                )
                icon16.close()
                icon32.close()
                Display.setIcon(icons)
            } else {
                logger.error("Meta couldn't find window icons!")
            }
        } catch (e: Exception) {
            logger.error("Meta couldn't set the window icon!", e)
        }
        Thread.currentThread().priority = 10
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        logger.info("Initializing $NAME $VERSION")
        LoaderWrapper.loadAll()
        MinecraftForge.EVENT_BUS.register(ForgeEventProcessor)
        ConfigUtils.loadAll()
        TimerScope.start()
        MainFontRenderer.reloadFonts()
        GameSettings.Options.FOV.setValueMax(180.0f)
        logger.info("Meta initialized!")
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        SecurityUtils.llIIIIIllIllIIIIlllIIIIIIllIII()
        AltProtect.storeInitialAccount()
        ready = true
        Runtime.getRuntime().addShutdownHook(Thread {
            ShutdownEvent.post()
            ConfigUtils.saveAll()
        })
    }

    companion object {
        const val NAME = Metadata.NAME
        const val ID = Metadata.ID
        const val VERSION = Metadata.VERSION
        const val DIRECTORY = "trollhack"

        @JvmField
        val title: String = Display.getTitle()

        @JvmField
        val logger: Logger = LogManager.getLogger(NAME)

        @JvmStatic
        var ready: Boolean = false
            private set
    }
}