package dev.wizard.meta.util

import dev.wizard.meta.util.math.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl

object SoundUtils {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSound(volume: Float = 1.0f, url: () -> String) {
        scope.launch {
            try {
                play(url(), volume)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun play(sound: String, volume: Float) {
        val clip = AudioSystem.getClip()
        val inputStream = this::class.java.getResourceAsStream("/assets/minecraft/sounds/$sound") ?: return
        val bufferedIn = BufferedInputStream(inputStream)
        val audioInputStream = AudioSystem.getAudioInputStream(bufferedIn)
        clip.open(audioInputStream)
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        gainControl.value = lerp(-30.0f, 0.0f, volume)
        clip.start()
    }
}
