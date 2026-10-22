package com.example.falldetectionsystem.model

data class AiInsight(
    val status: String = "Накопичення даних",
    val probabilities: List<Float> = emptyList(),
    val samplesCollected: Int = 0,
    val isFallHint: Boolean = false
)
