package dev.wizard.meta.graphics.texture

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.ParallelUtils
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

object BC4Compression {
    private fun encode(block: IntArray, output: ByteBuffer) {
        var min = 255
        var max = 0
        for (i in 0 until 16) {
            val value = block[i]
            if (value < min) min = value
            if (value > max) max = value
        }

        if (min == max) {
            output.putLong((min.toLong() shl 8) or max.toLong())
            return
        }

        var table = 0L
        val min24 = min * 65793
        val max24 = max * 65793
        val step = (max24 - min24) / 7
        val halfStep = step shr 1
        val f = -min24 + halfStep

        for (i in 0 until 16) {
            val value = block[i] * 65793
            var index = (value + f) / step
            index = (7 - index) or (((index + 1) shr 3) * -1)
            index %= 7
            table = table or ((index + 1).toLong() shl (i * 3))
        }
        output.putLong((table shl 16) or (min.toLong() shl 8) or max.toLong())
    }

    private fun extractBlock(input: RawImage, output: IntArray, x: Int, y: Int) {
        val width = input.width
        val data = input.data
        for (row in 0 until 4) {
            val rowOffset = (y + row) * width
            for (col in 0 until 4) {
                val index = rowOffset + x + col
                if (index < data.size) {
                    output[row * 4 + col] = data[index].toInt() and 0xFF
                } else {
                    output[row * 4 + col] = 0
                }
            }
        }
    }

    fun getEncodedSize(rawSize: Int): Int = rawSize / 2

    suspend fun encode(input: RawImage, output: ByteBuffer) = coroutineScope {
        val xBlocks = MathUtilKt.ceilToInt(input.width / 4.0)
        val yBlocks = MathUtilKt.ceilToInt(input.height / 4.0)
        val totalBlocks = xBlocks * yBlocks

        var offset = output.position()
        val parallelism = ParallelUtils.CPU_THREADS
        val parallelSize = max(totalBlocks / parallelism, 128)

        val jobs = mutableListOf<Job>()
        var index = 0
        while (index < totalBlocks) {
            val start = index
            var end = index + parallelSize
            if (totalBlocks - end < parallelSize / 2) end = totalBlocks

            val size = (end - start) * 8
            val view = output.duplicate().order(ByteOrder.nativeOrder())
            view.position(offset)
            view.limit(offset + size)
            val slicedView = view.slice().order(ByteOrder.nativeOrder())

            offset += size

            jobs += launch {
                val temp = IntArray(16)
                for (i in start until end) {
                    val bx = i % xBlocks
                    val by = i / xBlocks
                    extractBlock(input, temp, bx * 4, by * 4)
                    encode(temp, slicedView)
                }
            }
            index = end
        }
        jobs.joinAll()
    }
}
