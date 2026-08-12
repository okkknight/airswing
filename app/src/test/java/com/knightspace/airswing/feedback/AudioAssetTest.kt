package com.knightspace.airswing.feedback

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Test
import kotlin.math.sqrt
import kotlin.test.assertTrue

class AudioAssetTest {
    @Test
    fun `impact transient starts within fifteen milliseconds`() {
        listOf("hit_soft.wav", "hit_medium.wav", "hit_hard.wav").forEach { name ->
            val transientMs = dominantTransientMs(File("src/main/res/raw/$name"))
            assertTrue(transientMs <= 15, "$name dominant transient starts at ${transientMs}ms")
        }
    }

    private fun dominantTransientMs(file: File): Int {
        val bytes = file.readBytes()
        val sampleRate = ByteBuffer.wrap(bytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val channels = ByteBuffer.wrap(bytes, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        val dataOffset = findDataOffset(bytes)
        val samplesPerMs = sampleRate / 1_000
        val frameBytes = channels * 2
        val frameCount = (bytes.size - dataOffset) / frameBytes
        var strongestMs = 0
        var strongestRms = 0.0
        for (startFrame in 0 until frameCount step samplesPerMs) {
            val endFrame = minOf(startFrame + samplesPerMs, frameCount)
            var energy = 0.0
            var count = 0
            for (frame in startFrame until endFrame) {
                for (channel in 0 until channels) {
                    val offset = dataOffset + frame * frameBytes + channel * 2
                    val sample = ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toDouble()
                    energy += sample * sample
                    count++
                }
            }
            val rms = sqrt(energy / count)
            if (rms > strongestRms) {
                strongestRms = rms
                strongestMs = startFrame / samplesPerMs
            }
        }
        return strongestMs
    }

    private fun findDataOffset(bytes: ByteArray): Int {
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = bytes.copyOfRange(offset, offset + 4).decodeToString()
            val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") return offset + 8
            offset += 8 + size + (size and 1)
        }
        error("WAV data chunk not found")
    }
}
