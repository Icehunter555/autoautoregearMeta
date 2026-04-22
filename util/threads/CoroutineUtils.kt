package dev.wizard.meta.util.threads

import dev.fastmc.common.ParallelUtils
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.crash.CrashReport
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

val defaultContext: CoroutineContext = CoroutineName("Troll Hack Default") + Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
    Minecraft.getMinecraft().displayCrashReport(CrashReport.makeCrashReport(exception, "Troll Hack Default Scope"))
}

val concurrentContext: CoroutineContext = CoroutineName("Troll Hack Concurrent") + Dispatchers.Default.limitedParallelism(max(ParallelUtils.CPU_THREADS / 2, 1)) + CoroutineExceptionHandler { _, exception ->
    Minecraft.getMinecraft().displayCrashReport(CrashReport.makeCrashReport(exception, "Troll Hack Concurrent Scope"))
}

val backgroundPool: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(ParallelUtils.CPU_THREADS, CountingThreadFactory("Troll Hack Background") {
    it.isDaemon = true
    it.priority = 3
})

val backgroundContext: CoroutineContext = CoroutineName("Troll Hack Background") + backgroundPool.asCoroutineDispatcher() + CoroutineExceptionHandler { _, exception ->
    Minecraft.getMinecraft().displayCrashReport(CrashReport.makeCrashReport(exception, "Troll Hack Background Scope"))
}

val Job?.isActiveOrFalse: Boolean get() = this?.isActive ?: false

suspend fun delay(timeMillis: Int) {
    kotlinx.coroutines.delay(timeMillis.toLong())
}
