package dev.wizard.meta.manager.managers

import com.google.gson.*
import dev.wizard.meta.MetaMod
import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.ThreadSafetyKt.onMainThreadSafe
import java.io.File
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MacroManager : Manager() {
    private var macroMap: List<MutableList<String>>
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val parser = JsonParser()

    init {
        macroMap = List(256) { ArrayList<String>() }
        
        listener<InputEvent.Keyboard> {
            if (it.state) {
                sendMacro(it.key)
            }
        }
    }

    val isEmpty: Boolean
        get() = macroMap.isEmpty()

    val macros: List<List<String>>
        get() = macroMap

    private val file get() = File("trollhack/macros.json")

    fun loadMacros(): Boolean {
        ConfigUtils.fixEmptyJson(file)
        return try {
            val jsonElement = parser.parse(file.readText())
            val newMap = List(256) { ArrayList<String>() }
            for (entry in jsonElement.asJsonObject.entrySet()) {
                val id = entry.key.toInt()
                val list = entry.value.asJsonArray
                val destination = newMap[id]
                for (item in list) {
                    destination.add(item.asString)
                }
            }
            macroMap = newMap
            MetaMod.logger.info("Macro loaded")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed loading macro", e)
            false
        }
    }

    fun saveMacros(): Boolean {
        return try {
            val jsonObject = JsonObject()
            for (i in macroMap.indices) {
                if (macroMap[i].isNotEmpty()) {
                    jsonObject.add(i.toString(), gson.toJsonTree(macroMap[i]))
                }
            }
            file.writeText(gson.toJson(jsonObject))
            MetaMod.logger.info("Macro saved")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed saving macro", e)
            false
        }
    }

    private fun sendMacro(keyCode: Int) {
        val macros = getMacros(keyCode)
        ConcurrentScope.launch {
            for (macro in macros) {
                delay(5L)
                onMainThreadSafe {
                    if (macro.startsWith(CommandManager.prefix)) {
                        MessageSendUtils.sendTrollCommand(macro)
                    } else {
                        MessageManager.sendMessageDirect(macro)
                    }
                }.await()
            }
        }
    }

    fun getMacros(keycode: Int): List<String> {
        return macroMap[keycode]
    }

    fun setMacro(keycode: Int, macro: String) {
        macroMap[keycode].clear()
        macroMap[keycode].add(macro)
    }

    fun addMacro(keycode: Int, macro: String) {
        macroMap[keycode].add(macro)
    }

    fun removeMacro(keycode: Int) {
        macroMap[keycode].clear()
    }
}
