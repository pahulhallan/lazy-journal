package com.voicetrail.app.data.audio

import android.media.MediaPlayer

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(path: String, onCompletion: () -> Unit) {
        releaseCurrent()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            setOnCompletionListener {
                releaseCurrent()
                onCompletion()
            }
            prepare()
            start()
        }
    }

    fun stop() {
        releaseCurrent()
    }

    private fun releaseCurrent() {
        mediaPlayer?.runCatching {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}

