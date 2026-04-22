package dev.wizard.meta.manager

import dev.wizard.meta.AsyncLoader
import dev.wizard.meta.MetaMod
import dev.wizard.meta.util.ClassUtils.getInstance
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Deferred

object ManagerLoader : AsyncLoader<List<Class<out Manager>>> {
    override var deferred: Deferred<List<Class<out Manager>>>? = null

    override suspend fun preLoad0(): List<Class<out Manager>> {
        val classes = AsyncLoader.classes.await()
        var list: List<Class<out Manager>>
        val time = measureTimeMillis {
            val clazz = Manager::class.java
            list = classes.asSequence()
                .filter { Modifier.isFinal(it.modifiers) }
                .filter { it.name.startsWith("dev.wizard.meta.manager.managers") }
                .filter { clazz.isAssignableFrom(it) }
                .sortedBy { it.simpleName }
                .toList() as List<Class<out Manager>>
        }
        MetaMod.logger.info("${list.size} managers found, took ${time}ms")
        return list
    }

    override suspend fun load0(input: List<Class<out Manager>>) {
        val time = measureTimeMillis {
            for (clazz in input) {
                clazz.getInstance().subscribe()
            }
        }
        MetaMod.logger.info("${input.size} managers loaded, took ${time}ms")
    }
}
