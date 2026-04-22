package dev.wizard.meta.util

import dev.wizard.meta.MetaMod
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.FMLCommonHandler
import java.lang.management.ManagementFactory
import java.net.URL
import javax.swing.JOptionPane

object SecurityUtils {
    private var lIIlIIlII: Boolean = false
    private var lIIlIIIIIlIIlIIlllI: Boolean = false
    private val lIIlllllIIl: Set<String> = setOf(
        "-agentlib:jdwp", "-Xdebug", "-Xrunjdwp", "-javaagent", "-agentlib:hprof", "-agentpath", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.sun.management.jmxremote"
    )

    fun getLIIlIIIIIlIIlIIlllI(): Boolean = lIIlIIIIIlIIlIIlllI
    fun setLIIlIIIIIlIIlIIlllI(bl: Boolean) { lIIlIIIIIlIIlIIlllI = bl }

    fun lIIlllllIIIllIIIIllIII() {
        val lIIlIIlIIIIlllllll = ManagementFactory.getRuntimeMXBean()
        val llIIIIIIllIIlllllllIIIII = lIIlIIlIIIIlllllll.inputArguments
        for (arg in llIIIIIIllIIlllllllIIIII) {
            for (blacklisted in lIIlllllIIl) {
                if (arg!!.contains(blacklisted, ignoreCase = true)) {
                    MetaMod.logger.error("Detected blacklisted argument: $arg")
                    lIIIllIIllIIIIIllII(IllIIllIIIIllII.IIllIIIllIIIIIIIIIII)
                    return
                }
            }
        }
        MetaMod.logger.info("Args check passed")
    }

    fun lIIIllIIllIIIIIllII(lIIllIIIllIIllllll: IllIIllIIIIllII) {
        val llllllIIllIIllI = lIIllIIIllIIllllll.lIIllIIIIIlIIIIIIII
        val llIIIllIIIIIIIIll = lIIllIIIllIIllllll.lIIlllllllllllIlIlIlIlIlIlIl
        MetaMod.logger.fatal(llllllIIllIIllI)
        MetaMod.logger.fatal(llIIIllIIIIIIIIll)
        try {
            JOptionPane.showMessageDialog(null, llIIIllIIIIIIIIll, "Security Violation", 0)
        } catch (ignored: Exception) {
        }
        FMLCommonHandler.instance().exitJava(1, true)
    }

    private fun lIIllIIIIIIIlIll(): List<String> {
        return try {
            val lIIllIIIIl = URL("https://gist.githubusercontent.com/AGENTISNUM1/b7bb0c2df60491a91b355bf0c93266d5/raw")
            lIIllIIIIl.openStream().bufferedReader().use { it.readLines() }
        } catch (e: Exception) {
            lIIIllIIllIIIIIllII(IllIIllIIIIllII.IllllIIllIIlIIIllIIllII)
            emptyList()
        }
    }

    fun llIIIIIlllIIIIIlllllllI(llIllIIIllIIIIIll: String): Boolean {
        if (lIIlIIlII) return true
        val llIIIIlIIllII = mutableListOf<String>()
        for (line in lIIllIIIIIIIlIll()) {
            var lllllllIIllIIllII = line
            lllllllIIllIIllII = lllllllIIllIIllII.replace("$", "1")
            lllllllIIllIIllII = lllllllIIllIIllII.replace("*", "7")
            llIIIIlIIllII.add(lllllllIIllIIllII)
        }
        if (!llIIIIlIIllII.contains(llIllIIIllIIIIIll)) {
            lIIIllIIllIIIIIllII(IllIIllIIIIllII.IIlllIIIIllII)
            return false
        }
        lIIlIIlII = true
        return true
    }

    fun llIIIIllIIlIIlII(): Boolean = lIIlIIlII && lIIlIIIIIlIIlIIlllI

    @JvmStatic
    fun llIIIIIllIllIIIIlllIIIIIIllIII() {
        INSTANCE.lIIlllllIIIllIIIIllIII()
        val playerID = Minecraft.getMinecraft().session.playerID
        if (!INSTANCE.llIIIIIlllIIIIIlllllllI(playerID)) {
            INSTANCE.lIIIllIIllIIIIIllII(IllIIllIIIIllII.IIIIIllIII)
        } else {
            lIIlIIIIIlIIlIIlllI = true
        }
    }

    enum class IllIIllIIIIllII(val lIIllIIIIIlIIIIIIII: String, val lIIlllllllllllIlIlIlIlIlIlIl: String) {
        IIIIIllIII("Unauthorized user", "Uniformly-Eating-Silo"),
        IIlllIIIIllII("Unauthorized user", "Triage-Smooth-Deserving"),
        IllllIIllIIlIIIllIIllII("Verification Exception", "Wolf-Shortcut-Prelaunch"),
        IIllIIIllIIIIIIIIIII("Unauthorized JVM args", "Freefall-Utter-Monitor"),
        IIllIIIIIIIllllIIIllIIII("PostCheck Failed", "Dilute-Geranium-Nintendo")
    }
}
