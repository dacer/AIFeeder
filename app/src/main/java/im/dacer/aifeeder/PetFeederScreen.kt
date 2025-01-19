package im.dacer.aifeeder

import android.Manifest
import android.app.Activity
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import im.dacer.aifeeder.utils.captureImageAsBitmap
import im.dacer.aifeeder.utils.feedPet
import im.dacer.aifeeder.utils.isFeederLowOnFood
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PetFeederScreen() {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(false) }
    val logList = remember { mutableStateListOf<String>() }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(isRunning) {
        val activity = context as? Activity ?: return@LaunchedEffect
        adjustScreenBrightness(activity, dim = isRunning)
    }

    if (!isRunning) {
        // If not running, show camera preview
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreviewView(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptureCreated = { capture ->
                        imageCapture = capture
                    }
                )
            } else {
                LaunchedEffect(key1 = Unit) {
                    cameraPermissionState.launchPermissionRequest()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Camera permission is required for preview.")
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { isRunning = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(text = "Start AI-Controlled Feeder")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { feedPet() },
                    modifier = Modifier
                        .padding(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Feed Now (Manual)")
                }
            }
        }
    } else {
        // If running, show a list of logs and a "Stop" button.
        Column(modifier = Modifier.fillMaxSize()) {

            // A LazyColumn for displaying log messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(logList) { logItem ->
                    Text(text = logItem)
                }
            }

            Button(
                onClick = {
                    isRunning = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Stop")
            }
        }

        // When we switch to "running", we want to start taking a photo every 5 minutes.
        // We'll use a LaunchedEffect that depends on 'isRunning'.
        LaunchedEffect(key1 = isRunning) {
            while (isRunning) {
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                val capture = imageCapture
                if (capture != null) {
                    val photoBitmap = captureImageAsBitmap(capture, context)

                    if (photoBitmap != null) {
                        try {
                            val needFood = isFeederLowOnFood(photoBitmap)

                            if (needFood) {
                                feedPet()
                                logList.add("$currentTime Bowl check done: Need to feed.")
                            } else {
                                logList.add("$currentTime Bowl check done: No need to feed.")
                            }

                        } catch (e: Exception) {
                            logList.add("$currentTime Error: ${e.localizedMessage}")
                            Log.e("PetFeeder", "Error uploading or sending image", e)
                        }
                    } else {
                        logList.add("$currentTime Failed to take photo.")
                    }
                } else {
                    logList.add("$currentTime ImageCapture is null.")
                }

                // Wait for 5 minutes before taking the next picture
                delay(5 * 60 * 1000L)
            }
        }
    }
}

/**
 * CameraPreviewView is a custom composable that sets up CameraX preview.
 * It also initializes the ImageCapture so that we can use it to take pictures.
 */
@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onImageCaptureCreated: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(Size(640, 480))
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                    onImageCaptureCreated(imageCapture)

                } catch (exc: Exception) {
                    Log.e("PetFeeder", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

/**
 * Adjusts the screen brightness to dim or restore it.
 *
 * @param activity The activity whose screen brightness will be adjusted.
 * @param dim If true, dims the screen to near-zero brightness. If false, restores the brightness.
 */
fun adjustScreenBrightness(activity: Activity, dim: Boolean) {
    val window = activity.window
    val layoutParams = window.attributes

    if (dim) {
        layoutParams.screenBrightness = 0.01f // Dim screen to nearly off
    } else {
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE // Restore brightness
    }

    window.attributes = layoutParams
}
