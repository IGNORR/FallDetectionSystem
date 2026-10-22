package com.example.falldetectionsystem.model

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/** Classifies a 151-sample accelerometer window with the UniMiB SHAR model. */
class ActivityClassifier(context: Context) : AutoCloseable {
    companion object {
        const val WINDOW_SIZE = 151
    }

    private val config = JSONObject(
        context.assets.open("activity_classifier_config.json").bufferedReader().use { it.readText() }
    )
    private val mean = config.getDouble("mean").toFloat()
    private val std = config.getDouble("std").toFloat()
    val classNames: List<String> = config.getJSONArray("class_names").let { names ->
        List(names.length()) { names.getString(it) }
    }
    val fallClassIndex: Int = config.getInt("fall_class_index")
    val fallThreshold: Float = config.getDouble("fall_threshold").toFloat()

    private val interpreter: Interpreter

    init {
        require(std > 0f) { "Invalid model standard deviation" }
        require(classNames.size == 4 && fallClassIndex in classNames.indices) {
            "Unexpected model classes"
        }
        val model = context.assets.openFd("activity_classifier.tflite").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength
                )
            }
        }
        interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(2) })
        require(interpreter.getInputTensor(0).shape().contentEquals(intArrayOf(1, WINDOW_SIZE, 1))) {
            "Unexpected model input shape"
        }
        require(interpreter.getOutputTensor(0).shape().contentEquals(intArrayOf(1, classNames.size))) {
            "Unexpected model output shape"
        }
    }

    @Synchronized
    fun classify(sensorWindow: FloatArray): FloatArray {
        require(sensorWindow.size == WINDOW_SIZE * 3) { "Expected 151 accelerometer triples" }
        val input = ByteBuffer.allocateDirect(WINDOW_SIZE * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        for (index in sensorWindow.indices step 3) {
            val x = sensorWindow[index]
            val y = sensorWindow[index + 1]
            val z = sensorWindow[index + 2]
            val magnitude = sqrt(x * x + y * y + z * z)
            input.putFloat((magnitude - mean) / std)
        }
        input.rewind()
        val output = Array(1) { FloatArray(classNames.size) }
        interpreter.run(input, output)
        return output[0]
    }

    @Synchronized
    override fun close() = interpreter.close()
}
