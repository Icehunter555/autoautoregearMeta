package dev.wizard.meta.graphics.texture

import dev.fastmc.common.ParallelUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max

object Mipmaps {
    private suspend fun half(input: RawImage): RawImage = coroutineScope {
        val inputChannels = input.channels
        val w = input.width shr 1
        val h = input.height shr 1
        val pixels = w * h
        val outputData = ByteArray(pixels * inputChannels)
        val inputData = input.data
        val inputWidth = input.width

        val parallelism = ParallelUtils.CPU_THREADS
        val parallelSize = max(pixels / parallelism, 128)

        val jobs = mutableListOf<Job>()
        var index = 0
        while (index < pixels) {
            val start = index
            var end = index + parallelSize
            if (pixels - end < parallelSize / 2) end = pixels
            
            jobs += launch {
                for (i in start until end) {
                    val px = i % w
                    val py = i / w
                    val srcX = px * 2
                    val srcY = py * 2
                    
                    val srcRow1 = srcY * inputWidth * inputChannels
                    val srcRow2 = srcRow1 + inputWidth * inputChannels
                    
                    for (c in 0 until inputChannels) {
                        val i00 = srcRow1 + srcX * inputChannels + c
                        val i10 = i00 + inputChannels
                        val i01 = srcRow2 + srcX * inputChannels + c
                        val i11 = i01 + inputChannels
                        
                        val sum = (inputData[i00].toInt() and 0xFF) +
                                  (inputData[i10].toInt() and 0xFF) +
                                  (inputData[i01].toInt() and 0xFF) +
                                  (inputData[i11].toInt() and 0xFF)
                        outputData[i * inputChannels + c] = (sum shr 2).toByte()
                    }
                }
            }
            index = end
        }
        jobs.joinAll()
        RawImage(outputData, w, h, inputChannels)
    }

    fun getTotalSize(size: Int, levels: Int): Int {
        var total = size
        for (i in 1..levels) {
            total += size shr (i * 2)
        }
        return total
    }

    fun generate(image: RawImage, levels: Int): Flow<RawImage> = flow {
        var prev = image
        emit(prev)
        repeat(levels) {
            prev = half(prev)
            emit(prev)
        }
    }
}
