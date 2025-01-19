package im.dacer.aifeeder.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


/**
 * Capture a single image in-memory as a Bitmap (without saving to file).
 * This uses OnImageCapturedCallback under the hood.
 *
 * Note: Make sure your ImageCapture is configured to provide JPEG images,
 * otherwise decodeByteArray() might fail if the format is YUV.
 */
suspend fun captureImageAsBitmap(
    imageCapture: ImageCapture,
    context: Context
): Bitmap? = suspendCancellableCoroutine { continuation ->
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {

            override fun onCaptureSuccess(image: ImageProxy) {
                // Convert the ImageProxy to a ByteArray (assuming JPEG format)
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                // Decode the bytes into a Bitmap
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Always close the image when done
                image.close()

                // Resume the suspended function with the captured Bitmap
                continuation.resume(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                // Resume with null in case of error
                continuation.resume(null)
            }
        }
    )
}

