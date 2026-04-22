package dev.wizard.meta.module

import dev.fastmc.common.TimeUnit
import dev.wizard.meta.AsyncLoader
import dev.wizard.meta.MetaMod
import dev.wizard.meta.util.ClassUtils
import dev.wizard.meta.util.collections.AliasSet
import dev.wizard.meta.util.delegate.AsyncCachedValue
import dev.wizard.meta.util.extension.InterfacesKt
import dev.wizard.meta.util.interfaces.Alias
import dev.wizard.meta.util.interfaces.Helper
import kotlinx.coroutines.Deferred
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis

object ModuleManager : AsyncLoader<List<Class<out AbstractModule>>>, Helper {
    override var deferred: Deferred<List<Class<out AbstractModule>>>? = null

    private val moduleSet = AliasSet<AbstractModule>()

    private val modulesDelegate = AsyncCachedValue(5L, TimeUnit.SECONDS) {
        moduleSet.distinct().sortedBy { InterfacesKt.getRootName(it) }
    }

    val modules: List<AbstractModule> by modulesDelegate

    override suspend fun preLoad0(): List<Class<out AbstractModule>> {
        val classes = AsyncLoader.classes.await()
        var list: List<Class<out AbstractModule>> = emptyList()
        val time = measureTimeMillis {
            val clazz = AbstractModule::class.java
            list = classes.asSequence()
                .filter { Modifier.isFinal(it.modifiers) }
                .filter { it.name.startsWith("dev.wizard.meta.module.modules") }
                .filter { clazz.isAssignableFrom(it) }
                .map { 
                    @Suppress("UNCHECKED_CAST")
                    it as Class<out AbstractModule> 
                }
                .sortedBy { it.simpleName }
                .toList()
        }
        MetaMod.logger.info("${list.size} modules found, took ${time}ms")
        return list
    }

    override suspend fun load0(input: List<Class<out AbstractModule>>) {
        val time = measureTimeMillis {
            for (clazz in input) {
                register(ClassUtils.getInstance(clazz))
            }
        }
        MetaMod.logger.info("${input.size} modules loaded, took ${time}ms")
    }

    fun register(module: AbstractModule) {
        @Suppress("UNCHECKED_CAST")
        moduleSet.add(module as Alias)
        if ((module.enabledByDefault || module.alwaysEnabled) && !module.isDevOnly()) {
            module.enable()
        }
        if (module.isDevOnly()) {
            module.disable()
        }
        if (module.alwaysListening) {
            module.subscribe()
        }
        modulesDelegate.update()
    }

    fun unregister(module: AbstractModule) {
        moduleSet.remove(module)
        module.unsubscribe()
        modulesDelegate.update()
    }

    fun getModuleOrNull(moduleName: String?): AbstractModule? {
        return moduleName?.let { moduleSet[it] }
    }
}
