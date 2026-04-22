package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos

sealed class WorldEvent : Event {
    class ClientBlockUpdate(val pos: BlockPos, val oldState: IBlockState, val newState: IBlockState) : WorldEvent(), EventPosting by Companion {
        companion object : EventBus()
    }

    sealed class Entity(val entity: net.minecraft.entity.Entity) : WorldEvent() {
        class Add(entity: net.minecraft.entity.Entity) : Entity(entity), EventPosting by Companion {
            companion object : EventBus()
        }
        class Remove(entity: net.minecraft.entity.Entity) : Entity(entity), EventPosting by Companion {
            companion object : EventBus()
        }
    }

    object Load : WorldEvent(), EventPosting by EventBus()

    class RenderUpdate(val x1: Int, val y1: Int, val z1: Int, val x2: Int, val y2: Int, val z2: Int) : WorldEvent(), EventPosting by Companion {
        companion object : EventBus()
    }

    class ServerBlockUpdate(val pos: BlockPos, val oldState: IBlockState, val newState: IBlockState) : WorldEvent(), EventPosting by Companion {
        companion object : EventBus()
    }

    object Unload : WorldEvent(), EventPosting by EventBus()
}
