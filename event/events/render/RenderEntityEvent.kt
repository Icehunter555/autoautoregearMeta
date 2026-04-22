package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Cancellable
import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity

sealed class RenderEntityEvent(val entity: Entity) : Cancellable(), Event {

    abstract fun render()

    companion object {
        var renderingEntities: Boolean = false
    }

    sealed class All(
        entity: Entity,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val partialTicks: Float,
        private val renderer: Render<Entity>
    ) : RenderEntityEvent(entity) {
        override fun render() {
            renderer.doRender(entity, x, y, z, yaw, partialTicks)
        }

        class Pre(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTicks: Float, renderer: Render<Entity>) :
            All(entity, x, y, z, yaw, partialTicks, renderer), EventPosting by Companion {
            companion object : EventBus()
        }

        class Post(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTicks: Float, renderer: Render<Entity>) :
            All(entity, x, y, z, yaw, partialTicks, renderer), EventPosting by Companion {
            companion object : EventBus()
        }
    }

    sealed class Model(
        entity: Entity,
        private val block: () -> Unit
    ) : RenderEntityEvent(entity) {
        override fun render() {
            block()
        }

        class Pre(entity: Entity, block: () -> Unit) : Model(entity, block), EventPosting by Companion {
            companion object : EventBus() {
                @JvmStatic
                fun of(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTicks: Float, render: Render<Entity>) =
                    Pre(entity) { render.doRender(entity, x, y, z, yaw, partialTicks) }

                @JvmStatic
                fun of(entity: Entity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scaleFactor: Float, model: ModelBase) =
                    Pre(entity) { model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor) }
            }
        }

        class Post(entity: Entity, block: () -> Unit) : Model(entity, block), EventPosting by Companion {
            companion object : EventBus() {
                @JvmStatic
                fun of(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTicks: Float, render: Render<Entity>) =
                    Post(entity) { render.doRender(entity, x, y, z, yaw, partialTicks) }

                @JvmStatic
                fun of(entity: Entity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scaleFactor: Float, model: ModelBase) =
                    Post(entity) { model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor) }
            }
        }
    }
}
