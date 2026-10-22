package com.example.falldetectionsystem.model

/** A fixed-size window of accelerometer triples in chronological order. */
class SensorWindow(private val capacity: Int = ActivityClassifier.WINDOW_SIZE) {
    private val values = FloatArray(capacity * 3)
    private var next = 0
    var size: Int = 0
        private set

    fun add(x: Float, y: Float, z: Float) {
        val offset = next * 3
        values[offset] = x
        values[offset + 1] = y
        values[offset + 2] = z
        next = (next + 1) % capacity
        if (size < capacity) size++
    }

    fun snapshot(): FloatArray? {
        if (size < capacity) return null
        val result = FloatArray(values.size)
        for (i in 0 until capacity) {
            val source = ((next + i) % capacity) * 3
            val target = i * 3
            result[target] = values[source]
            result[target + 1] = values[source + 1]
            result[target + 2] = values[source + 2]
        }
        return result
    }

    fun clear() {
        next = 0
        size = 0
    }
}
