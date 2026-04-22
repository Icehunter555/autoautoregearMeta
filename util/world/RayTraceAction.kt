package dev.wizard.meta.util.world

import net.minecraft.util.math.RayTraceResult

sealed class RayTraceAction {
    object Calc : RayTraceAction()
    object Null : RayTraceAction()
    class Result(val rayTraceResult: RayTraceResult) : RayTraceAction()
    object Skip : RayTraceAction()
}
