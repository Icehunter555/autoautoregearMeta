package dev.wizard.meta.gui

import dev.fastmc.common.TimeUnit
import dev.wizard.meta.AsyncLoader
import dev.wizard.meta.MetaMod
import dev.wizard.meta.gui.clickgui.TrollClickGui
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.TrollHudGui
import dev.wizard.meta.util.ClassUtils
import dev.wizard.meta.util.collections.AliasSet
import dev.wizard.meta.util.delegate.AsyncCachedValue
import dev.wizard.meta.util.interfaces.Alias
import kotlinx.coroutines.Deferred
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis

object GuiManager : AsyncLoader<List<Class<out AbstractHudElement>>> {
    override var deferred: Deferred<List<Class<out AbstractHudElement>>>? = null

    private val hudElementSet = AliasSet<AbstractHudElement>()
    val hudElements: List<AbstractHudElement> by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        hudElementSet.distinct().sortedBy { it.nameAsString }
    }

    override suspend fun preLoad0(): List<Class<out AbstractHudElement>> {
        val classes = AsyncLoader.classes.await()
        var list: List<Class<out AbstractHudElement>>? = null
        val time = measureTimeMillis {
            val clazz = AbstractHudElement::class.java
            list = classes.asSequence()
                .filter { Modifier.isFinal(it.modifiers) }
                .filter { it.name.startsWith("dev.wizard.meta.gui.hudgui.elements") }
                .filter { clazz.isAssignableFrom(it) }
                .sortedBy { it.simpleName }
                .toList() as List<Class<out AbstractHudElement>>
        }
        MetaMod.logger.info("${list?.size} hud elements found, took ${time}ms")
        return list!!
    }

    override suspend fun load0(input: List<Class<out AbstractHudElement>>) {
        val time = measureTimeMillis {
            for (clazz in input) {
                register(ClassUtils.getInstance(clazz))
            }
        }
        MetaMod.logger.info("${input.size} hud elements loaded, took ${time}ms")
        TrollClickGui.onGuiClosed()
        TrollHudGui.onGuiClosed()
        TrollClickGui.subscribe()
        TrollHudGui.subscribe()
    }

    fun register(hudElement: AbstractHudElement) {
        hudElementSet.add(hudElement as Alias<AbstractHudElement>)
        TrollHudGui.register(hudElement)
    }

    fun unregister(hudElement: AbstractHudElement) {
        hudElementSet.remove(hudElement)
        TrollHudGui.unregister(hudElement)
    }

    fun getHudElementOrNull(name: String?): AbstractHudElement? {
        return name?.let { hudElementSet[it] }
    }
}
