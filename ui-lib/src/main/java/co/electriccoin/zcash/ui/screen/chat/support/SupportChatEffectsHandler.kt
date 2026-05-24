package co.electriccoin.zcash.ui.screen.chat.support

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.screen.chat.media.rememberCameraCaptureState

@Composable
internal fun SupportChatEffectsHandler(viewModel: SupportChatVM) {
    val context = LocalContext.current

    val mediaPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(viewModel::onMediaPicked)
        }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::onFilePicked)
        }

    val cameraCaptureState =
        rememberCameraCaptureState(context) { uri -> viewModel.onCameraCaptured(uri) }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                cameraCaptureState.launch()
            } else {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.chat_room_toast_camera_permission_required),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SupportChatEffect.PickMedia ->
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                    )

                SupportChatEffect.PickFile ->
                    filePickerLauncher.launch(arrayOf("*/*"))

                SupportChatEffect.TakePhoto -> {
                    val granted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        cameraCaptureState.launch()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }
}
