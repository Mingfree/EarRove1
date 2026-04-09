package com.example.earrove.ui.ocr

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.R
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.ui.theme.AppSize
import com.example.earrove.ui.theme.AppSpacing
import com.example.earrove.utils.TTSManager
import com.example.earrove.utils.rememberTTSManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private const val TAG = "EarRove_OcrScreen"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OcrScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ttsManager = rememberTTSManager()
    val ocrViewModel: OcrViewModel = viewModel()
    val ui by ocrViewModel.uiState.collectAsStateWithLifecycle()

    // 文案统一走资源，后面改措辞/做多语言不需要改业务逻辑
    val screenTitle = context.getString(R.string.ocr_screen_title)
    val resultHeading = context.getString(R.string.ocr_heading_result)
    val guideSpeak = context.getString(R.string.home_ocr_guide_speak)

    val statusInitial = context.getString(R.string.ocr_status_initial)
    val statusRecognizingAlbum = context.getString(R.string.ocr_status_recognizing_album)
    val statusRecognizing = context.getString(R.string.ocr_status_recognizing)
    val statusRecognitionDone = context.getString(R.string.ocr_status_recognition_done)
    val statusRecognitionFailedRetry = context.getString(R.string.ocr_status_recognition_failed_retry)
    val statusRecognitionFailed = context.getString(R.string.ocr_status_recognition_failed)

    val a11yRecognizing = context.getString(R.string.ocr_a11y_recognizing)
    val a11yModeHint = context.getString(R.string.ocr_a11y_mode_hint)

    val actionCopyText = context.getString(R.string.ocr_action_copy)
    val actionReplayText = context.getString(R.string.ocr_action_replay)
    val actionCopiedSpeak = context.getString(R.string.ocr_action_copied_speak)

    val flashlightOffDesc = context.getString(R.string.ocr_flashlight_desc_off)
    val flashlightOnDesc = context.getString(R.string.ocr_flashlight_desc_on)
    val flashlightEnabledState = context.getString(R.string.ocr_flashlight_state_enabled)
    val flashlightDisabledState = context.getString(R.string.ocr_flashlight_state_disabled)
    val captureButtonA11y = context.getString(R.string.ocr_capture_button_a11y)
    val pickPhotoA11y = context.getString(R.string.ocr_pick_photo_a11y)
    val photoLabel = context.getString(R.string.ocr_photo_label)

    val statusRecognizingCamera = context.getString(R.string.ocr_status_recognizing_camera)
    val cameraNotReadyToast = context.getString(R.string.ocr_camera_not_ready_toast)
    val cameraNotReadySpeak = context.getString(R.string.ocr_camera_not_ready_speak)

    val permissionRequiredText = context.getString(R.string.ocr_permission_required)
    val permissionRationaleText = context.getString(R.string.ocr_permission_rationale)
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // 相机权限管理
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        ocrViewModel.setStatusText(statusInitial)
        ttsManager.speak(guideSpeak)
    }

    // CameraX 对象
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // 相册选择器（相机不可用时用户还能继续识别）
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && !ui.isRecognizing) {
            ttsManager.speak(statusRecognizing)
            coroutineScope.launch {
                val base64 = uriToBase64(context, uri)
                Log.d(TAG, "相册图片 Base64 长度: ${base64.length}")
                ocrViewModel.runAlbumRecognition(
                    base64 = base64,
                    statusRecognizing = statusRecognizingAlbum,
                    statusDone = statusRecognitionDone,
                    statusFailedRetry = statusRecognitionFailedRetry,
                    statusFailed = statusRecognitionFailed,
                    formatFailure = { msg ->
                        context.getString(R.string.ocr_recognition_failed_with_msg, msg)
                    },
                    onSpeak = { text -> ttsManager.speak(text) },
                    onSpeakFailure = { ttsManager.speak(it) }
                )
            }
        }
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.release()
        }
    }

    // 请求权限
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val accessibilityLabel = when {
        ui.isRecognizing -> a11yRecognizing
        ui.recognitionResult.isNotEmpty() -> context.getString(R.string.ocr_a11y_result, ui.recognitionResult)
        else -> a11yModeHint
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = screenTitle, onNavigateUp = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack)
                .semantics { contentDescription = accessibilityLabel }
        ) {
            if (cameraPermissionState.status.isGranted) {
                // 摄像头预览
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptureReady = { capture -> imageCapture = capture },
                    onCameraReady = { cam -> camera = cam },
                    isFlashlightOn = ui.isFlashlightOn
                )

                // 顶部状态栏
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(AppSpacing.large)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = PureBlack.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = ui.statusText,
                        color = PremiumGold,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.medium)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                // 识别结果显示区
                if (ui.recognitionResult.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = 72.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PureBlack.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = resultHeading,
                                color = PremiumGold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ui.recognitionResult,
                                color = PureWhite,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.medium))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setPrimaryClip(
                                            ClipData.newPlainText("ocr_result", ui.recognitionResult)
                                        )
                                        ttsManager.speak(actionCopiedSpeak)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(actionCopyText)
                                }

                                OutlinedButton(
                                    onClick = {
                                        ttsManager.speak(ui.recognitionResult)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(actionReplayText)
                                }
                            }
                        }
                    }
                }

                // 底部控制区
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp, start = AppSpacing.xxLarge, end = AppSpacing.xxLarge)
                        .semantics { isTraversalGroup = true },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 闪光灯按钮
                    IconButton(
                        onClick = {
                            val next = !ui.isFlashlightOn
                            ocrViewModel.updateState { it.copy(isFlashlightOn = next) }
                            camera?.cameraControl?.enableTorch(next)
                        },
                        modifier = Modifier
                            .size(AppSize.fab)
                            .background(
                                PureBlack.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .semantics { traversalIndex = 0f }
                    ) {
                        Icon(
                            imageVector = if (ui.isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (ui.isFlashlightOn) {
                                "$flashlightEnabledState，$flashlightOffDesc"
                            } else {
                                "$flashlightDisabledState，$flashlightOnDesc"
                            },
                            tint = if (ui.isFlashlightOn) PremiumGold else PureWhite,
                            modifier = Modifier.size(AppSize.iconMedium)
                        )
                    }

                    // 拍照识别按钮
                    FloatingActionButton(
                        onClick = {
                            if (!ui.isRecognizing) {
                                ocrViewModel.updateState {
                                    it.copy(
                                        isRecognizing = true,
                                        statusText = statusRecognizingCamera,
                                        recognitionResult = ""
                                    )
                                }
                                ttsManager.speak(statusRecognizing)

                                imageCapture?.let { capture ->
                                    captureAndRecognize(
                                        context = context,
                                        imageCapture = capture,
                                        ocrViewModel = ocrViewModel,
                                        ttsManager = ttsManager,
                                        coroutineScope = coroutineScope,
                                        onResult = { result ->
                                            ocrViewModel.updateState {
                                                it.copy(
                                                    recognitionResult = result,
                                                    statusText = statusRecognitionDone,
                                                    isRecognizing = false
                                                )
                                            }
                                        },
                                        onError = { error ->
                                            ocrViewModel.updateState {
                                                it.copy(
                                                    recognitionResult = context.getString(
                                                        R.string.ocr_recognition_failed_with_msg,
                                                        error
                                                    ),
                                                    statusText = statusRecognitionFailedRetry,
                                                    isRecognizing = false
                                                )
                                            }
                                            ttsManager.speak(statusRecognitionFailedRetry)
                                        }
                                    )
                                } ?: run {
                                    ocrViewModel.updateState {
                                        it.copy(
                                            statusText = cameraNotReadyToast,
                                            isRecognizing = false
                                        )
                                    }
                                    ttsManager.speak(cameraNotReadySpeak)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(AppSize.fabLarge)
                            .semantics {
                                contentDescription = captureButtonA11y
                                traversalIndex = 1f
                            },
                        containerColor = if (ui.isRecognizing) Color.Gray else PremiumGold,
                        shape = CircleShape
                    ) {
                        if (ui.isRecognizing) {
                            CircularProgressIndicator(
                                color = PureBlack,
                                modifier = Modifier.size(AppSize.iconLarge),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(AppSize.iconLarge)
                            )
                        }
                    }

                    // 相册选择按钮
                    IconButton(
                        onClick = {
                            if (!ui.isRecognizing) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        modifier = Modifier
                            .size(AppSize.fab)
                            .background(
                                PureBlack.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .semantics {
                                contentDescription = pickPhotoA11y
                                traversalIndex = 2f
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = photoLabel,
                            tint = PureWhite,
                            modifier = Modifier.size(AppSize.iconMedium)
                        )
                    }
                }
            } else {
                // 无权限提示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = permissionRequiredText,
                        color = PremiumGold,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = permissionRationaleText,
                        color = PureWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * CameraX 预览组件
 */
@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onCameraReady: (androidx.camera.core.Camera) -> Unit,
    isFlashlightOn: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    onImageCaptureReady(imgCapture)

                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imgCapture
                    )

                    onCameraReady(cam)
                    Log.d(TAG, "CameraX 绑定成功")

                } catch (e: Exception) {
                    Log.e(TAG, "CameraX 绑定失败: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.clip(RoundedCornerShape(0.dp))
    )
}

/**
 * 拍照并进行OCR识别
 */
private fun captureAndRecognize(
    context: Context,
    imageCapture: ImageCapture,
    ocrViewModel: OcrViewModel,
    ttsManager: TTSManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                coroutineScope.launch {
                    try {
                        // 将 ImageProxy 转换为 Base64
                        val base64 = imageProxyToBase64(imageProxy)
                        imageProxy.close()

                        Log.d(TAG, "图片已捕获，Base64 长度: ${base64.length}")

                        // 调用火山引擎 API
                        val result = ocrViewModel.recognizeImage(base64)

                        result.onSuccess { text ->
                            onResult(text)
                            // TTS 朗读结果
                            ttsManager.speak(text)
                        }.onFailure { error ->
                            onError(error.message ?: context.getString(R.string.ocr_unknown_error))
                        }

                    } catch (e: Exception) {
                        imageProxy.close()
                    Log.e(TAG, "处理图片异常: ${e.message}", e)
                    onError(e.message ?: context.getString(R.string.ocr_process_image_failed))
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "拍照失败: ${exception.message}", exception)
                onError(
                    context.getString(
                        R.string.ocr_take_photo_failed_with_msg,
                        exception.message ?: ""
                    )
                )
            }
        }
    )
}

/**
 * 将 ImageProxy 转换为 Base64 字符串
 * 压缩为 JPEG 并降低分辨率以减小上传数据量
 */
private fun imageProxyToBase64(imageProxy: ImageProxy): String {
    val buffer: ByteBuffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    // 解码为 Bitmap
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // 旋转（CameraX 可能有旋转角度）
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    if (rotationDegrees != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // 降低分辨率（最大 1024px宽）以加速上传
    val maxWidth = 1024
    if (bitmap.width > maxWidth) {
        val scale = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * scale).toInt()
        bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    // 转为 JPEG Base64
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

/**
 * 将 Uri 图片转换为 Base64 字符串
 */
private fun uriToBase64(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception(context.getString(R.string.ocr_read_image_failed))

    var bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    if (bitmap == null) throw Exception(context.getString(R.string.ocr_decode_image_failed))

    // 降低分辨率（最大 1024px 宽）
    val maxWidth = 1024
    if (bitmap.width > maxWidth) {
        val scale = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * scale).toInt()
        bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
