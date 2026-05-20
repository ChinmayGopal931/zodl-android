@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.room

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.media.rememberCameraCaptureState
import co.electriccoin.zcash.ui.screen.chat.view.ChatRoomView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ChatRoomScreen(args: ChatRoomArgs) {
    val viewModel = koinViewModel<ChatRoomVM> { parametersOf(args) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
            if (permissions.values.any { it }) {
                scope.launch { shareLocation(context, viewModel) }
            } else {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.chat_room_toast_location_permission_required),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ChatRoomEffect.PickMedia ->
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                    )

                ChatRoomEffect.PickFile ->
                    filePickerLauncher.launch(arrayOf("*/*"))

                ChatRoomEffect.TakePhoto -> {
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

                ChatRoomEffect.ShareLocation -> {
                    val granted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        scope.launch { shareLocation(context, viewModel) }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                }
            }
        }
    }

    val currentState = state
    if (currentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ZappTheme.colors.accent)
        }
        return
    }

    BackHandler { currentState.onBack() }
    val fallbackTitle = stringResource(R.string.chat_room_title_fallback)
    ChatRoomView(state = currentState, fallbackTitle = fallbackTitle, modifier = Modifier.fillMaxSize())
}

@Suppress("TooGenericExceptionCaught", "MissingPermission")
private suspend fun shareLocation(
    context: android.content.Context,
    viewModel: ChatRoomVM,
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        val location =
            suspendCancellableCoroutine<android.location.Location?> { cont ->
                client
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        if (location != null) {
            viewModel.onLocationObtained(location.latitude, location.longitude, location.accuracy)
        } else {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.chat_room_toast_location_unavailable),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    } catch (e: Exception) {
        Toast
            .makeText(
                context,
                context.getString(R.string.chat_room_toast_location_error_fmt, e.message.orEmpty()),
                Toast.LENGTH_SHORT,
            ).show()
    }
}
