package dev.wizard.meta

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.gui.GuiManager
import dev.wizard.meta.manager.ManagerLoader
import dev.wizard.meta.module.ModuleManager
import kotlinx.coroutines.runBlocking

object LoaderWrapper {
    private val loaderList = ArrayList<AsyncLoader<*>>()

    init {
        loaderList.add(ModuleManager.INSTANCE)
        loaderList.add(CommandManager.INSTANCE)
        loaderList.add(ManagerLoader.INSTANCE)
        loaderList.add(GuiManager.INSTANCE)
    }

    @JvmStatic
    fun preLoadAll() {
        loaderList.forEach { it.preLoad() }
    }

    @JvmStatic
    fun loadAll() {
        runBlocking {
            loaderList.forEach { it.load() }
        }
    }
}