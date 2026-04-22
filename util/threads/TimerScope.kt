package dev.wizard.meta.util.threads

import dev.wizard.meta.MetaMod
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object TimerScope : CoroutineScope {
    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher = newFixedThreadPoolContext(2, "Troll Hack Timer")
    override val coroutineContext: CoroutineContext = dispatcher

    private val jobs = LinkedHashMap<BackgroundJob, Job?>()
    private var started = false

    fun start() {
        started = true
        jobs.forEach { (job, _) ->
            jobs[job] = startJob(job)
        }
    }

    fun launchLooping(name: String, delay: Long, block: suspend CoroutineScope.() -> Unit): BackgroundJob {
        return launchLooping(BackgroundJob(name, delay, block))
    }

    fun launchLooping(job: BackgroundJob): BackgroundJob {
        if (!started) {
            jobs[job] = null
        } else {
            jobs[job] = startJob(job)
        }
        return job
    }

    fun cancel(job: BackgroundJob) {
        jobs.remove(job)?.cancel()
    }

    private fun startJob(job: BackgroundJob): Job {
        return launch {
            while (isActive) {
                try {
                    job.block(this)
                } catch (e: Exception) {
                    MetaMod.logger.warn("Error occurred while running background job ${job.name}", e)
                }
                delay(job.delay)
            }
        }
    }
}
