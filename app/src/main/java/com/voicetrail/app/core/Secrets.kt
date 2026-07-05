package com.voicetrail.app.core

import com.voicetrail.app.BuildConfig

object Secrets {
    val huggingFaceToken: String?
        get() = BuildConfig.HUGGING_FACE_TOKEN.takeIf { it.isNotBlank() }

    val huggingFaceEndpoint: String
        get() = BuildConfig.HUGGING_FACE_ENDPOINT
}

