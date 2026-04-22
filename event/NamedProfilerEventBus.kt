package dev.wizard.meta.event

open class NamedProfilerEventBus(private val profilerName: String) : ProfilerEventBus() {
    override fun post(event: Any) {
        mc.profiler.startSection(profilerName)
        super.post(event)
        mc.profiler.endSection()
    }
}
