package dev.wizard.meta.util

import java.awt.Desktop
import java.io.File
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

object FolderUtils {
    @JvmStatic
    val minecraftFolder: String
        get() = File("").absolutePath + File.separator

    @JvmStatic
    val modsFolder: String
        get() = minecraftFolder + "mods" + File.separator

    @JvmStatic
    val logFolder: String
        get() = minecraftFolder + "logs" + File.separator

    @JvmStatic
    val resourcepackFolder: String
        get() = minecraftFolder + "resourcepacks" + File.separator

    @JvmStatic
    val mcConfigFolder: String
        get() = minecraftFolder + "config" + File.separator

    @JvmStatic
    val screenshotFolder: String
        get() = minecraftFolder + "screenshots" + File.separator

    @JvmStatic
    val trollFolder: String
        get() = minecraftFolder + "trollhack" + File.separator

    @JvmStatic
    val configFolder: String
        get() = trollFolder + "config" + File.separator

    @JvmStatic
    val moduleFolder: String
        get() = configFolder + "modules" + File.separator

    @JvmStatic
    val guiFolder: String
        get() = configFolder + "gui" + File.separator

    fun openFolder(path: String) {
        thread {
            val file = File(path)
            if (!file.exists()) {
                file.mkdir()
            }
            if (getOS() == OperatingSystem.WINDOWS) {
                Desktop.getDesktop().open(file)
            } else {
                val url = file.toURI().toURL()
                Runtime.getRuntime().exec(getURLOpenCommand(url))
            }
        }
    }

    private fun getURLOpenCommand(url: URL): Array<String> {
        var urlStr = url.toString()
        if (url.protocol == "file") {
            urlStr = urlStr.replace("file:", "file://")
        }
        return arrayOf("xdg-open", urlStr)
    }

    private fun getOS(): OperatingSystem {
        val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
        return when {
            osName.contains("nux") -> OperatingSystem.UNIX
            osName.contains("darwin") || osName.contains("mac") -> OperatingSystem.OSX
            osName.contains("win") -> OperatingSystem.WINDOWS
            else -> throw RuntimeException("Operating system couldn't be detected! Report this to the developers")
        }
    }

    enum class OperatingSystem {
        UNIX, OSX, WINDOWS
    }
}
