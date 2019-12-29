package com.mzm.sample.digit_recognizer

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * This image classifier classifies each drawing as one of the 10 digits
 */
class Classifier @Throws(IOException::class)
constructor(private val context: Context) {

    // TensorFlow Lite interpreter for running inference with the tflite model
    private val interpreter: Interpreter

    // Initialize TFLite interpreter
    init {

        // Load TFLite model
        val assetManager = context.assets
        val model = loadModelFile(assetManager)

        // Configure TFLite Interpreter options
        val options = Interpreter.Options()
        options.setNumThreads(3)
        options.setUseNNAPI(true)

        // Create & initialize TFLite interpreter
        interpreter = Interpreter(model, options)

    }

    // Memory-map the model file in Assets
    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * To classify an image, follow these steps:
     * 1. pre-process the input image
     * 2. run inference with the model
     * 3. post-process the output result for displaying in UI
     *
     * @param bitmap
     * @return the digit with the highest probability
     */
    fun classify(bitmap: Bitmap): Int {

        // 1. Pre-processing

        // Output array [batch_size, number of digits]
        val inputByteBuffer = preprocess(bitmap)                    // Input must be a ByteBuffer
        // 10 floats, each corresponds to the probability of each digit
        val outputArray = Array(DIM_BATCH_SIZE) { FloatArray(NUM_DIGITS) }

        // 2. Run inference
        interpreter.run(inputByteBuffer, outputArray)

        // 3. Post-processing
        return postprocess(outputArray)
    }

    /**
     * Preprocess the bitmap by converting it to ByteBuffer & grayscale
     *
     * @param bitmap
     */
    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, false)
        return convertBitmapToByteBuffer(scaledBitmap)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // Create input image bytebuffer
        val byteBuffer = ByteBuffer.allocateDirect(4
                * DIM_BATCH_SIZE    // 1
                * DIM_INPUT_WIDTH   // 28
                * DIM_INPUT_HEIGHT  // 28
                * DIM_PIXEL_SIZE)   // 1
        byteBuffer.order(ByteOrder.nativeOrder())

        val imagePixels = IntArray(DIM_INPUT_WIDTH * DIM_INPUT_HEIGHT)
        bitmap.getPixels(imagePixels, 0, bitmap.width, 0, 0,
                bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until DIM_INPUT_WIDTH) {
            for (j in 0 until DIM_INPUT_HEIGHT) {
                val `val` = imagePixels[pixel++]
                byteBuffer.putFloat(convertToGreyScale(`val`))
            }
        }

        return byteBuffer
    }

    private fun convertToGreyScale(color: Int): Float {
        val r = (color shr 16 and 0xFF).toFloat()
        val g = (color shr 8 and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()

        val grayscaleValue = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        return grayscaleValue / 255.0f
    }

    /**
     * Find digit prediction with the highest probability
     *
     * @return
     */
    private fun postprocess(outputArray: Array<FloatArray>): Int {
        // Index with highest probability
        var maxIndex = -1
        var maxProb = 0.0f
        for (i in 0 until outputArray[0].size) {
            if (outputArray[0][i] > maxProb) {
                maxProb = outputArray[0][i]
                maxIndex = i
            }
        }
        return maxIndex
    }

    companion object {

        private val LOG_TAG = Classifier::class.java.simpleName

        // Name of the model file (under /assets folder)
        private val MODEL_PATH = "mnist.tflite"

        // Input size
        private val DIM_BATCH_SIZE = 1    // batch size
        private val DIM_INPUT_WIDTH = 28  // input image width
        private val DIM_INPUT_HEIGHT = 28 // input image height
        private val DIM_PIXEL_SIZE = 1    // 1 for gray scale & 3 for color images

        /* Output*/
        // Output size is 10 (number of digits)
        private val NUM_DIGITS = 10
    }

}
