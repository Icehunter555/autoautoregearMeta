package dev.wizard.meta.manager.managers

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.delegate.AsyncCachedValue
import dev.wizard.meta.util.threads.BackgroundScope
import io.netty.util.internal.ConcurrentSet
import kotlinx.coroutines.launch
import net.minecraft.network.play.server.SPacketChunkData
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.Chunk

object ChunkManager : Manager() {
    private val newChunks0 = ConcurrentSet<ChunkPos>()
    val newChunks: List<ChunkPos> by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        newChunks0.toList()
    }

    init {
        listener<ConnectionEvent.Disconnect> {
            newChunks0.clear()
        }

        safeListener<PacketEvent.PostReceive> { event ->
            val packet = event.packet
            if (packet is SPacketChunkData && !packet.isFullChunk) {
                BackgroundScope.launch {
                    val chunk = world.getChunk(packet.chunkX, packet.chunkZ)
                    if (!chunk.isEmpty) {
                        if (newChunks0.add(chunk.pos) && newChunks0.size > 8192) {
                            val playerX = MathUtilKt.floorToInt(player.posX)
                            val playerZ = MathUtilKt.floorToInt(player.posZ)
                            newChunks0.maxByOrNull {
                                playerX - it.x + (playerZ - it.z)
                            }?.let {
                                newChunks0.remove(it)
                            }
                        }
                    }
                }
            }
        }
    }

    fun isNewChunk(chunk: Chunk): Boolean {
        return isNewChunk(chunk.pos)
    }

    fun isNewChunk(chunkPos: ChunkPos): Boolean {
        return newChunks0.contains(chunkPos)
    }
}
