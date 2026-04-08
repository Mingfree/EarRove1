package com.example.earrove.ui.ocr

import android.Manifest
import android.content.Context
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.utils.AppConfig
import com.example.earrove.utils.TTSManager
import com.example.earrove.utils.VolcengineArkService
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

    // 权限管理
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // 状态
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionResult by remember { mutableStateOf("") }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("对准指示牌，点击拍照按钮识别") }

    // 火山引擎服务
    val arkService = remember { VolcengineArkService() }

    // CameraX
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // 相册选择器
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && !isRecognizing) {
            isRecognizing = true
            statusText = "正在识别相册图片..."
            recognitionResult = ""
            ttsManager.speak("正在识别")

            coroutineScope.launch {
                try {
                    val base64 = uriToBase64(context, uri)
                    Log.d(TAG, "相册图片 Base64 长度: ${base64.length}")

                    val result = arkService.recognizeImage(base64)
                    result.onSuccess { text ->
                        recognitionResult = text
                        statusText = "识别完成，点击重新识别"
                        isRecognizing = false
                        ttsManager.speak(text)
                    }.onFailure { error ->
                        recognitionResult = "识别失败：${error.message}"
                        statusText = "识别失败，请重试"
                        isRecognizing = false
                        ttsManager.speak("识别失败，请重试")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理相册图片异常: ${e.message}", e)
                    recognitionResult = "识别失败：${e.message}"
                    statusText = "识别失败，请重试"
                    isRecognizing = false
                    ttsManager.speak("识别失败")
                }
            }
        }
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            arkService.shutdown()
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
        isRecognizing -> "正在识别中，请稍候"
        recognitionResult.isNotEmpty() -> "识别结果：$recognitionResult"
        else -> "视觉识别模式。对准指示牌，点击拍照按钮识别。"
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = "视觉识别", onNavigateUp = { navController.popBackStack() })
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
                    isFlashlightOn = isFlashlightOn
                )

                // 顶部状态栏
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = PureBlack.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        color = PremiumGold,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }

                // 识别结果显示区
                if (recognitionResult.isNotEmpty()) {
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
                                text = "识别结果",
                                color = PremiumGold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recognitionResult,
                                color = PureWhite,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // 底部控制区
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 闪光灯按钮
                    IconButton(
                        onClick = {
                            isFlashlightOn = !isFlashlightOn
                            camera?.cameraControl?.enableTorch(isFlashlightOn)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                PureBlack.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (isFlashlightOn) "关闭闪光灯" else "打开闪光灯",
                            tint = if (isFlashlightOn) PremiumGold else PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // 拍照识别按钮
                    FloatingActionButton(
                        onClick = {
                            if (!isRecognizing) {
                                isRecognizing = true
                                statusText = "正在拍照识别中..."
                                recognitionResult = ""
                                ttsManager.speak("正在识别")

                                imageCapture?.let { capture ->
                                    captureAndRecognize(
                                        context = context,
                                        imageCapture = capture,
                                        arkService = arkService,
                                        ttsManager = ttsManager,
                                        coroutineScope = coroutineScope,
                                        onResult = { result ->
                                            recognitionResult = result
                                            statusText = "识别完成，点击重新识别"
                                            isRecognizing = false
                                        },
                                        onError = { error ->
                                            recognitionResult = "识别失败：$error"
                                            statusText = "识别失败，请重试"
                                            isRecognizing = false
                                            ttsManager.speak("识别失败，请重试")
                                        }
                                    )
                                } ?: run {
                                    statusText = "相机未就绪，请稍候再试"
                                    isRecognizing = false
                                    ttsManager.speak("相机未就绪")
                                }
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .semantics { contentDescription = "拍照识别按钮。点击拍照并识别前方指示牌。" },
                        containerColor = if (isRecognizing) Color.Gray else PremiumGold,
                        shape = CircleShape
                    ) {
                        if (isRecognizing) {
                            CircularProgressIndicator(
                                color = PureBlack,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // 相册选择按钮
                    IconButton(
                        onClick = {
                            if (!isRecognizing) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                PureBlack.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .semantics { contentDescription = "从相册选择图片识别" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "相册",
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
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
                        text = "需要相机权限",
                        color = PremiumGold,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "请授予相机权限以使用视觉识别功能",
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
 * 拍照并识别
 */
private fun captureAndRecognize(
    context: Context,
    imageCapture: ImageCapture,
    arkService: VolcengineArkService,
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
                        val result = arkService.recognizeImage(base64)

                        result.onSuccess { text ->
                            onResult(text)
                            // TTS 朗读结果
                            ttsManager.speak(text)
                        }.onFailure { error ->
                            onError(error.message ?: "未知错误")
                        }

                    } catch (e: Exception) {
                        imageProxy.close()
                        Log.e(TAG, "处理图片异常: ${e.message}", e)
                        onError(e.message ?: "处理图片失败")
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "拍照失败: ${exception.message}", exception)
                onError("拍照失败: ${exception.message}")
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
        ?: throw Exception("无法读取图片")

    var bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    if (bitmap == null) throw Exception("图片解码失败")

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
