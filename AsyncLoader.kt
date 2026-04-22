package dev.wizard.meta

import dev.wizard.meta.util.ClassUtils
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.*
import java.util.*

interface AsyncLoader<T : Any> {
    var deferred: Deferred<T>?

    fun preLoad() {
        deferred = preLoadAsync()
    }

    private fun preLoadAsync(): Deferred<T> {
        return DefaultScope.async(singleContext) {
            preLoad0()
        }
    }

    suspend fun load() {
        val result = deferred?.await() ?: preLoadAsync().await()
        load0(result)
    }

    suspend fun preLoad0(): T

    suspend fun load0(input: T)

    companion object {
        val singleContext = Dispatchers.Default.limitedParallelism(1)
        val classes: Deferred<List<Class<*>>> = DefaultScope.async(singleContext) {
            val start = System.currentTimeMillis()
            val list = ClassUtils.findClasses("dev.wizard.meta") {
                !it.contains("mixins")
            }
            val time = System.currentTimeMillis() - start
            MetaMod.logger.info("${list.size} classes found, took ${time}ms")
            list
        }
    }
}