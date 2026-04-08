package com.example.earrove.ui.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MyLocationConfiguration
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.example.earrove.navigation.NavigationService
import com.example.earrove.navigation.NavigationStep
import com.example.earrove.navigation.ObstacleDetectionService
import com.example.earrove.navigation.TrafficLightService
import com.example.earrove.navigation.TrafficLight
import com.example.earrove.navigation.TrafficLightStatus
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.EarRoveTheme
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.ui.theme.WarningOrange
import com.example.earrove.utils.Arbitrator
import com.example.earrove.utils.BaiduMapUtils
import com.example.earrove.utils.DestinationExtractor
import com.example.earrove.utils.LocationManager
import com.example.earrove.utils.PermissionUtils
import com.example.earrove.utils.RequestPermissionsDialog
import com.example.earrove.utils.SpeechRecognizerManager
import com.example.earrove.utils.TTSManager
import com.example.earrove.utils.VibrationManager
import com.example.earrove.utils.rememberArbitrator
import com.example.earrove.utils.rememberSpeechRecognizer
import com.example.earrove.utils.rememberTTSManager
import com.example.earrove.utils.rememberVibrationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.CoordType
import com.example.earrove.utils.PrivacyUtils
import androidx.compose.material.icons.filled.LocationOff

// 导航状态枚举
enum class NavigationState {
    STANDBY,          // 待机状态，等待语音输入
    LISTENING,        // 正在监听语音
    PLANNING_ROUTE,   // 规划路线中
    NAVIGATING,       // 导航进行中
    PAUSED,           // 导航暂停
    ARRIVED           // 已到达目的地
}

// 导航屏幕的ViewModel
class NavigationViewModel : androidx.lifecycle.ViewModel() {
    val navigationState = mutableStateOf(NavigationState.STANDBY)
    val destinationText = mutableStateOf("")
    val currentStep = mutableStateOf<NavigationStep?>(null)
    val nextStep = mutableStateOf<NavigationStep?>(null)
    val totalDistance = mutableStateOf(0)
    val totalTime = mutableStateOf(0)
    val remainingDistance = mutableStateOf(0)
    val remainingTime = mutableStateOf(0)
    val isObstacleDetected = mutableStateOf(false)
    val obstacleType = mutableStateOf("")
    val obstacleDistance = mutableStateOf(0)
    val trafficLightStatus = mutableStateOf<TrafficLight?>(null)
    val isTrafficLightDetected = mutableStateOf(false)
    val currentLocation = mutableStateOf<BDLocation?>(null)
    val destinationLocation = mutableStateOf<LatLng?>(null)
    val mapView = mutableStateOf<MapView?>(null)
    val baiduMap = mutableStateOf<BaiduMap?>(null)
    val isLocationStarted = mutableStateOf(false)

    // 模拟目的地列表（用于测试）
    val destinationSuggestions = listOf(
        "中央民族大学",
        "北京西站",
        "天安门广场",
        "颐和园",
        "王府井",
        "三里屯",
        "国家体育场",
        "北京大学"
    )
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun NavigationScreen(
    navController: NavController,
    viewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 跟踪当前路线规划 Job，新导航时取消旧的
    var routePlanJob by remember { mutableStateOf<Job?>(null) }
    // 跟踪录音 Job（父级 scope，不会因 StandbyScreen 移除被取消）
    var listeningJob by remember { mutableStateOf<Job?>(null) }

    // 权限检查
    var permissionsGranted by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        if (permissionPermanentlyDenied) {
            // Don’t ask again：引导用户手动到系统设置开启权限
            Box(
                modifier = Modifier.fillMaxSize().background(PureBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "权限已被永久拒绝",
                        color = PremiumGold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "请在系统设置中开启权限后重试，否则导航可能不可用。",
                        color = PureWhite,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", (context as? Activity)?.packageName ?: context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGold,
                            contentColor = PureBlack
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("去系统设置开启权限")
                    }

                    Button(
                        onClick = {
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite.copy(alpha = 0.1f),
                            contentColor = PureWhite
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("返回首页")
                    }
                }
            }
            return
        }

        RequestPermissionsDialog(
            onPermissionGranted = { permissionsGranted = true },
            onPermissionDenied = {
                // 如果权限被拒绝，返回首页
                navController.popBackStack()
            },
            onPermissionPermanentlyDenied = { permissionPermanentlyDenied = true }
        )
        return
    }

    // 添加隐私政策检查状态
    var isPrivacyPolicyAgreed by remember { mutableStateOf(false) }
    var sdkInitializationError by remember { mutableStateOf<String?>(null) }

    // 检查百度地图SDK初始化状态
    LaunchedEffect(Unit) {
        try {
            val appContext = context.applicationContext

            // 首先检查用户是否已同意隐私政策
            val privacyAgreed = PrivacyUtils.hasUserAgreedToBaiduMapPrivacy(context)

            if (!privacyAgreed) {
                sdkInitializationError = "请先同意百度地图隐私政策才能使用导航功能"
                Log.e("NavigationScreen", "用户未同意百度地图隐私政策")
                return@LaunchedEffect
            }

            // 检查SDK是否已初始化
            val sdkInitialized = runCatching {
                SDKInitializer.isInitialized()
            }.getOrElse { false }

            if (!sdkInitialized) {
                // 尝试重新初始化
                try {
                    // 再次确认隐私政策已设置（使用Application Context）
                    SDKInitializer.setAgreePrivacy(appContext, true)
                    Log.d("NavigationScreen", "隐私政策已设置")

                    // 延迟确保隐私政策设置生效
                    delay(300)

                    // 初始化SDK
                    SDKInitializer.initialize(appContext)
                    SDKInitializer.setCoordType(CoordType.BD09LL)

                    Log.d("NavigationScreen", "百度地图SDK重新初始化成功")
                    isPrivacyPolicyAgreed = true

                } catch (e: Exception) {
                    sdkInitializationError = "百度地图SDK初始化失败: ${e.message}"
                    Log.e("NavigationScreen", "SDK重新初始化失败: ${e.message}")
                }
            } else {
                // SDK已初始化，再次确认隐私政策
                SDKInitializer.setAgreePrivacy(appContext, true)
                delay(100)
                isPrivacyPolicyAgreed = true
                Log.d("NavigationScreen", "SDK已初始化，隐私政策已确认")
            }

            // 尝试检查定位服务是否可用（延迟检查确保生效）
            delay(500)

            val locationClientAvailable = runCatching {
                com.baidu.location.LocationClient(appContext)
                true
            }.getOrElse {
                Log.w("NavigationScreen", "创建LocationClient测试异常: ${it.message}")
                false
            }

            if (locationClientAvailable) {
                Log.d("NavigationScreen", "定位服务测试通过")
            } else {
                sdkInitializationError = "定位服务不可用，请检查隐私政策设置"
            }

        } catch (e: Exception) {
            sdkInitializationError = "导航服务初始化失败: ${e.message}"
            Log.e("NavigationScreen", "SDK初始化检查异常: ${e.message}")
        }
    }

    // 显示SDK初始化错误页面
    if (sdkInitializationError != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "定位服务错误",
                    tint = WarningOrange,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "导航服务初始化失败",
                    color = WarningOrange,
                    fontSize = 20.sp
                )

                Text(
                    text = sdkInitializationError ?: "未知错误",
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // 检查是否是因为隐私政策问题
                if (sdkInitializationError?.contains("隐私政策") == true) {
                    Button(
                        onClick = {
                            // 返回到主页面重新同意隐私政策
                            navController.popBackStack("home", inclusive = false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGold,
                            contentColor = PureBlack
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("前往设置隐私政策")
                    }
                }

                Button(
                    onClick = {
                        // 重新进入页面
                        navController.popBackStack()
                        navController.navigate("navigation")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold.copy(alpha = 0.8f),
                        contentColor = PureBlack
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = "重试初始化")
                }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite.copy(alpha = 0.1f),
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = "返回首页")
                }
            }
        }
        return
    }

    // 等待隐私政策同意
    if (!isPrivacyPolicyAgreed) {
        Box(
            modifier = Modifier.fillMaxSize().background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CircularProgressIndicator(
                    color = PremiumGold,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "正在初始化导航服务...",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "请确保已同意百度地图隐私政策",
                    color = PremiumGold,
                    fontSize = 14.sp
                )

                // 添加手动检查按钮
                Button(
                    onClick = {
                        // 重新检查隐私政策状态
                        val privacyAgreed = PrivacyUtils.hasUserAgreedToBaiduMapPrivacy(context)
                        if (privacyAgreed) {
                            // 如果已同意，重新初始化SDK
                            sdkInitializationError = null
                        } else {
                            sdkInitializationError = "请先同意百度地图隐私政策"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold.copy(alpha = 0.8f),
                        contentColor = PureBlack
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("检查隐私政策状态")
                }

                Button(
                    onClick = {
                        // 返回到首页查看隐私政策
                        navController.popBackStack("home", inclusive = false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite.copy(alpha = 0.1f),
                        contentColor = PureWhite
                    )
                ) {
                    Text("返回首页查看隐私政策")
                }
            }
        }
        return
    }

    // 初始化管理器 - 现在可以安全地创建LocationManager
    val locationManager = remember {
        try {
            Log.d("NavigationScreen", "开始创建LocationManager")

            // 尝试使用LocationClient的静态方法设置隐私政策
            try {
                val appContext = context.applicationContext
                val locationClientClass = Class.forName("com.baidu.location.LocationClient")
                val setAgreePrivacyMethod = locationClientClass.getMethod("setAgreePrivacy", android.content.Context::class.java, Boolean::class.java)
                setAgreePrivacyMethod.invoke(null, appContext, true)
                Log.d("NavigationScreen", "LocationClient静态隐私政策已设置")
            } catch (e: Exception) {
                Log.w("NavigationScreen", "LocationClient.setAgreePrivacy静态方法不存在: ${e.message}")
            }

            // 再次设置SDKInitializer的隐私政策（SDK已在LaunchedEffect中初始化完成）
            SDKInitializer.setAgreePrivacy(context.applicationContext, true)
            Log.d("NavigationScreen", "SDKInitializer隐私政策已再次设置")

            // 创建LocationManager
            val manager = LocationManager(context)
            Log.d("NavigationScreen", "LocationManager创建成功")
            manager
        } catch (e: Exception) {
            Log.e("NavigationScreen", "创建LocationManager失败: ${e.message}")
            // 这里不再直接返回null，而是抛出错误让上面的错误处理捕获
            sdkInitializationError = "创建定位管理器失败: ${e.message}"
            null
        }
    }

    // 如果LocationManager创建失败，提供重试选项
    if (locationManager == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = "定位服务不可用",
                    tint = WarningOrange,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "定位服务初始化失败",
                    color = WarningOrange,
                    fontSize = 20.sp
                )

                Text(
                    text = "请检查以下可能原因：\n1. 隐私政策未同意\n2. 定位权限未授予\n3. 百度地图服务异常",
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // 检查隐私政策
                Button(
                    onClick = {
                        val privacyAgreed = PrivacyUtils.hasUserAgreedToBaiduMapPrivacy(context)
                        if (privacyAgreed) {
                            // 如果已同意，重新进入页面
                            navController.popBackStack()
                            navController.navigate("navigation")
                        } else {
                            // 返回到首页设置隐私政策
                            navController.popBackStack("home", inclusive = false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold,
                        contentColor = PureBlack
                    )
                ) {
                    Text(text = "检查隐私政策状态")
                }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite.copy(alpha = 0.1f),
                        contentColor = PureWhite
                    )
                ) {
                    Text(text = "返回首页")
                }
            }
        }
        return
    }

    // 其他管理器初始化
    val ttsManager = rememberTTSManager()
    val vibrationManager = rememberVibrationManager()
    val speechRecognizer = rememberSpeechRecognizer()
    val arbitrator = rememberArbitrator(ttsManager, vibrationManager)

    // 初始化导航服务
    val navigationService = remember { NavigationService(context) }
    val obstacleService = remember { ObstacleDetectionService() }
    val trafficLightService = remember { TrafficLightService() }

    // 初始化目的地提取器（Qwen 大模型清洗 ASR 原文）
    val destinationExtractor = remember { DestinationExtractor() }

    // 管理TTS生命周期
    LaunchedEffect(ttsManager) {
        ttsManager.setSpeechRate(1.0f)
    }

    // 初始语音提示
    LaunchedEffect(Unit) {
        delay(500)
        if (viewModel.navigationState.value == NavigationState.STANDBY) {
            ttsManager.speak("请点击屏幕中央麦克风后，说明目的地。")
        }
    }

    // 处理位置更新（直接在 LaunchedEffect 内 collect，key 变化时旧协程自动取消）
    LaunchedEffect(viewModel.isLocationStarted.value) {
        if (viewModel.isLocationStarted.value) {
            try {
                locationManager.startLocation().flowOn(Dispatchers.IO).collectLatest { location ->
                    viewModel.currentLocation.value = location

                    // 更新地图上的位置
                    viewModel.baiduMap.value?.let { baiduMap ->
                        val myLocationData = MyLocationData.Builder()
                            .accuracy(location.radius)
                            .direction(location.direction)
                            .latitude(location.latitude)
                            .longitude(location.longitude)
                            .build()

                        baiduMap.setMyLocationData(myLocationData)

                        // 如果是首次定位、待机状态或导航中，移动到当前位置
                        if (viewModel.navigationState.value == NavigationState.NAVIGATING ||
                            viewModel.navigationState.value == NavigationState.STANDBY) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            val update = MapStatusUpdateFactory.newLatLng(latLng)
                            baiduMap.animateMapStatus(update)
                        }
                    }

                    // 导航中更新当前位置
                    if (viewModel.navigationState.value == NavigationState.NAVIGATING) {
                        navigationService.updateCurrentLocation(
                            LatLng(location.latitude, location.longitude)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("NavigationScreen", "位置更新错误: ${e.message}")
            }
        }
    }

    LaunchedEffect(navigationService.currentRoute.collectAsState().value) {
        navigationService.currentRoute.value?.let { route ->
            viewModel.totalDistance.value = route.totalDistance
            viewModel.totalTime.value = route.totalDuration
            viewModel.remainingDistance.value = route.totalDistance
            viewModel.remainingTime.value = route.totalDuration / 60
        }
    }

    LaunchedEffect(navigationService.currentStepIndex.collectAsState().value) {
        val route = navigationService.currentRoute.value
        val stepIndex = navigationService.currentStepIndex.value

        route?.steps?.let { steps ->
            if (stepIndex < steps.size) {
                viewModel.currentStep.value = steps[stepIndex]
                viewModel.nextStep.value = steps.getOrNull(stepIndex + 1)

                // 计算剩余距离和时间
                val remainingSteps = steps.subList(stepIndex, steps.size)
                viewModel.remainingDistance.value = remainingSteps.sumOf { it.distance }
                viewModel.remainingTime.value = remainingSteps.sumOf { it.duration } / 60
            }
        }
    }

    // 处理障碍物检测（模拟）—— 仅在正式导航时启动，避免干扰语音输入
    LaunchedEffect(viewModel.navigationState.value) {
        if (viewModel.navigationState.value == NavigationState.NAVIGATING) {
            obstacleService.startMonitoring().collectLatest { obstacle ->
                obstacle?.let {
                    viewModel.isObstacleDetected.value = true
                    viewModel.obstacleType.value = obstacleService.getObstacleDescription(obstacle)
                    viewModel.obstacleDistance.value = obstacle.distance

                    // 通过仲裁器播报障碍物
                    arbitrator.announceObstacle(viewModel.obstacleType.value)

                    // 3秒后清除障碍物提示
                    delay(3000)
                    viewModel.isObstacleDetected.value = false
                }
            }
        } else {
            // 非导航状态时停止监测并清除提示
            obstacleService.stopMonitoring()
            viewModel.isObstacleDetected.value = false
        }
    }

    // 处理红绿灯检测（模拟）—— 仅在正式导航时启动，避免干扰语音输入
    LaunchedEffect(viewModel.navigationState.value) {
        if (viewModel.navigationState.value == NavigationState.NAVIGATING) {
            trafficLightService.startMonitoring().collectLatest { trafficLight ->
                trafficLight?.let {
                    viewModel.isTrafficLightDetected.value = true
                    viewModel.trafficLightStatus.value = trafficLight

                    // 通过仲裁器播报红绿灯
                    arbitrator.announceTrafficLight(
                        trafficLightService.getTrafficLightDescription(trafficLight),
                        trafficLight.countdown
                    )

                    // 5秒后清除红绿灯提示
                    delay(5000)
                    viewModel.isTrafficLightDetected.value = false
                }
            }
        } else {
            // 非导航状态时停止监测并清除提示
            trafficLightService.stopMonitoring()
            viewModel.isTrafficLightDetected.value = false
        }
    }

    // 地图生命周期管理 - 使用 DisposableEffect 代替
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.mapView.value?.onResume()
                    // 开始定位
                    viewModel.isLocationStarted.value = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.mapView.value?.onPause()
                    // 停止定位
                    locationManager.stopLocation()
                    viewModel.isLocationStarted.value = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.mapView.value?.onDestroy()
                    navigationService.release()
                    obstacleService.stopMonitoring()
                    trafficLightService.stopMonitoring()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 清理 TTS 和语音识别资源
            ttsManager.release()
            speechRecognizer.destroy()
        }
    }

    // 共享路线规划逻辑：清理旧状态 → 规划路线 → 等待真实结果 → 开始导航
    val planAndStartNavigation: (String, LatLng) -> Unit = { destination, location ->
        speechRecognizer.cancel()
        ttsManager.stop()
        routePlanJob?.cancel()
        listeningJob?.cancel()
        navigationService.stopNavigation()

        viewModel.destinationText.value = destination
        viewModel.destinationLocation.value = location
        viewModel.navigationState.value = NavigationState.PLANNING_ROUTE

        routePlanJob = scope.launch(Dispatchers.IO) {
            try {
                val currentLocation = viewModel.currentLocation.value
                if (currentLocation != null) {
                    val start = LatLng(currentLocation.latitude, currentLocation.longitude)
                    navigationService.planRoute(start, location, destination)

                    // 等待路线规划完成（最长 15 秒），替代原硬编码 delay(3000)
                    val route = withTimeoutOrNull(15_000L) {
                        navigationService.currentRoute.filterNotNull().first()
                    }

                    if (route != null) {
                        viewModel.navigationState.value = NavigationState.NAVIGATING
                        navigationService.startNavigation(destination)
                        arbitrator.announceRouteStart(
                            destination,
                            route.totalDistance,
                            route.totalDuration / 60
                        )
                    } else {
                        ttsManager.speak("路线规划超时，请重试")
                        viewModel.navigationState.value = NavigationState.STANDBY
                    }
                } else {
                    ttsManager.speak("无法获取当前位置，请检查GPS")
                    viewModel.navigationState.value = NavigationState.STANDBY
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ttsManager.speak("路线规划失败，请重试")
                viewModel.navigationState.value = NavigationState.STANDBY
            }
        }
    }

    // 导航状态处理
    when (viewModel.navigationState.value) {
        NavigationState.STANDBY -> {
            StandbyScreen(
                viewModel = viewModel,
                ttsManager = ttsManager,
                navigationService = navigationService,
                baiduMapUtils = BaiduMapUtils,
                navController = navController,
                onMicClick = {
                    // 取消旧的录音和路线规划
                    listeningJob?.cancel()
                    speechRecognizer.cancel()
                    ttsManager.stop()

                    // UI 切换到监听动画
                    viewModel.navigationState.value = NavigationState.LISTENING

                    // 在父级 scope 启动录音（不会因 StandbyScreen 移除而被取消）
                    listeningJob = scope.launch {
                        try {
                            ttsManager.speakAndWait("正在监听，请说出目的地")
                            delay(300)

                            speechRecognizer.startListening().collectLatest { result ->
                                if (result.isNotBlank()) {
                                    val placeName = destinationExtractor.extract(result)
                                    viewModel.destinationText.value = placeName
                                    // 优先使用 POI 周边搜索（就近匹配），无当前位置时回退到全国地理编码
                                    val currentLoc = viewModel.currentLocation.value
                                    val location = if (currentLoc != null) {
                                        BaiduMapUtils.searchNearby(
                                            placeName,
                                            LatLng(currentLoc.latitude, currentLoc.longitude)
                                        ).first()
                                    } else {
                                        BaiduMapUtils.geocodeAddress(placeName).first()
                                    }
                                    if (location != null) {
                                        planAndStartNavigation(placeName, location)
                                    } else {
                                        ttsManager.speak("无法找到目的地，请重试")
                                        viewModel.navigationState.value = NavigationState.STANDBY
                                    }
                                } else {
                                    ttsManager.speak("未识别到语音，请重试")
                                    viewModel.navigationState.value = NavigationState.STANDBY
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            ttsManager.speak("语音识别失败，请重试")
                            viewModel.navigationState.value = NavigationState.STANDBY
                        }
                    }
                },
                onStartNavigation = { destination, location ->
                    planAndStartNavigation(destination, location)
                }
            )
        }

        NavigationState.LISTENING -> {
            ListeningScreen(
                viewModel = viewModel,
                navController = navController,
                onCancel = {
                    // 取消父级录音协程，释放音频资源后再播报
                    listeningJob?.cancel()
                    speechRecognizer.cancel()
                    viewModel.navigationState.value = NavigationState.STANDBY
                    ttsManager.speak("已取消语音输入")
                }
            )
        }

        NavigationState.PLANNING_ROUTE -> {
            PlanningRouteScreen(
                destination = viewModel.destinationText.value,
                navController = navController
            )
        }

        NavigationState.NAVIGATING -> {
            NavigatingScreen(
                viewModel = viewModel,
                arbitrator = arbitrator,
                navigationService = navigationService,
                locationManager = locationManager,
                obstacleService = obstacleService,
                trafficLightService = trafficLightService,
                navController = navController,
                onPause = {
                    viewModel.navigationState.value = NavigationState.PAUSED
                    ttsManager.speak("导航已暂停")
                    locationManager.stopLocation()
                },
                onStop = {
                    viewModel.navigationState.value = NavigationState.ARRIVED
                    navigationService.stopNavigation()
                    locationManager.stopLocation()
                    obstacleService.stopMonitoring()
                    trafficLightService.stopMonitoring()
                    arbitrator.announceDestination(viewModel.destinationText.value)
                },
                onNextStep = {
                    navigationService.moveToNextStep()?.let { nextStep ->
                        // 播报下一步指令
                        val direction = when (nextStep.turnType) {
                            "LEFT" -> "左转"
                            "RIGHT" -> "右转"
                            "STRAIGHT" -> "直行"
                            "ARRIVE" -> "到达"
                            else -> "继续前进"
                        }
                        arbitrator.announceTurn(direction, nextStep.distance)
                    }
                }
            )
        }

        NavigationState.PAUSED -> {
            PausedScreen(
                destination = viewModel.destinationText.value,
                navController = navController,
                onResume = {
                    viewModel.navigationState.value = NavigationState.NAVIGATING
                    ttsManager.speak("继续导航")
                    viewModel.isLocationStarted.value = true
                },
                onStop = {
                    viewModel.navigationState.value = NavigationState.ARRIVED
                    navigationService.stopNavigation()
                    locationManager.stopLocation()
                    obstacleService.stopMonitoring()
                    trafficLightService.stopMonitoring()
                    arbitrator.announceDestination(viewModel.destinationText.value)
                }
            )
        }

        NavigationState.ARRIVED -> {
            ArrivedScreen(
                destination = viewModel.destinationText.value,
                navController = navController,
                onRestart = {
                    viewModel.navigationState.value = NavigationState.STANDBY
                    viewModel.destinationText.value = ""
                    viewModel.destinationLocation.value = null
                    ttsManager.speak("请说出新的目的地")
                },
                onExit = {
                    navigationService.release()
                    locationManager.stopLocation()
                    obstacleService.stopMonitoring()
                    trafficLightService.stopMonitoring()
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun StandbyScreen(
    viewModel: NavigationViewModel,
    ttsManager: TTSManager,
    navigationService: NavigationService,
    baiduMapUtils: BaiduMapUtils,
    navController: NavController,
    onMicClick: () -> Unit,
    onStartNavigation: (String, LatLng) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSuggestions by remember { mutableStateOf(false) }
    val micScale by animateFloatAsState(
        targetValue = if (viewModel.navigationState.value == NavigationState.LISTENING) 1.2f else 1f,
        label = "micScale"
    )

    // 初始化地图
    LaunchedEffect(Unit) {
        viewModel.isLocationStarted.value = true
    }

    // 再次检查隐私政策状态
    LaunchedEffect(Unit) {
        val privacyAgreed = PrivacyUtils.hasUserAgreedToBaiduMapPrivacy(context)
        if (!privacyAgreed) {
            // 如果未同意，显示提示
            ttsManager.speak("请先同意隐私政策才能使用导航功能")
        }
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = "智能导航",
                onNavigateUp = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack)
        ) {
            // 百度地图
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        viewModel.mapView.value = this
                        viewModel.baiduMap.value = map

                        // 配置地图
                        map.apply {
                            uiSettings.isCompassEnabled = true
                            uiSettings.isZoomGesturesEnabled = true
                            uiSettings.isOverlookingGesturesEnabled = false
                            uiSettings.isRotateGesturesEnabled = true

                            // 设置定位配置
                            val config = MyLocationConfiguration(
                                MyLocationConfiguration.LocationMode.NORMAL,
                                true,
                                null
                            )
                            setMyLocationConfiguration(config)
                            isMyLocationEnabled = true

                            // 设置地图初始化
                            val update = MapStatusUpdateFactory.zoomTo(18f)
                            animateMapStatus(update)
                        }

                        // 配置导航服务地图
                        navigationService.setupMapView(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 当前位置显示
                viewModel.currentLocation.value?.let { location ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PureBlack.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "当前位置",
                                tint = PremiumGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "当前位置",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = location.addrStr ?: "正在定位...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 控制卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PureBlack.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 麦克风按钮
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(PremiumGold.copy(alpha = 0.2f))
                                .clickable { onMicClick() }
                                .semantics {
                                    contentDescription = "双击开始语音输入目的地"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "语音输入",
                                tint = PremiumGold,
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(micScale)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 提示文字
                        Text(
                            text = "请点击麦克风说出目的地",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .semantics {
                                    contentDescription = "请点击麦克风说出目的地，如需更改目的地请点击结束导航按钮"
                                }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "可描述您的目的地，比如：导航到最近的火车站",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 快速目的地按钮
                        Button(
                            onClick = { showSuggestions = !showSuggestions },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PremiumGold.copy(alpha = 0.8f),
                                contentColor = PureBlack
                            )
                        ) {
                            Text(text = if (showSuggestions) "隐藏推荐目的地" else "显示推荐目的地")
                        }

                        // 目的地建议列表
                        AnimatedVisibility(
                            visible = showSuggestions,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "推荐目的地:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PremiumGold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                viewModel.destinationSuggestions.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        rowItems.forEach { destination ->
                                            DestinationSuggestionButton(
                                                destination = destination,
                                                onClick = {
                                                    // 优先 POI 周边搜索（就近匹配）
                                                    scope.launch(Dispatchers.IO) {
                                                        try {
                                                            val currentLoc = viewModel.currentLocation.value
                                                            if (currentLoc != null) {
                                                                baiduMapUtils.searchNearby(
                                                                    destination,
                                                                    LatLng(currentLoc.latitude, currentLoc.longitude)
                                                                ).collectLatest { location ->
                                                                    if (location != null) {
                                                                        onStartNavigation(destination, location)
                                                                    }
                                                                }
                                                            } else {
                                                                baiduMapUtils.geocodeAddress(destination).collectLatest { location ->
                                                                    if (location != null) {
                                                                        onStartNavigation(destination, location)
                                                                    }
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            ttsManager.speak("地址解析失败，请重试")
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 双击屏幕区域（用于更改目的地）- 仅覆盖控制区域，不遮挡地图
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                // 单击逻辑
                                ttsManager.speak("请说出新的目的地")
                            }
                        )
                        .semantics {
                            contentDescription = "点击此处更改目的地"
                        }
                ) {
                    // 这里是空的，只是为了捕获点击事件
                }
            }
        }
    }
}

@Composable
private fun DestinationSuggestionButton(
    destination: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = PremiumGold.copy(alpha = 0.1f),
        border = BorderStroke(
            1.dp,
            PremiumGold.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = destination,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun ListeningScreen(
    viewModel: NavigationViewModel,
    navController: NavController,
    onCancel: () -> Unit
) {
    val listeningPulse by animateFloatAsState(
        targetValue = if ((System.currentTimeMillis() / 500) % 2 == 0L) 1.1f else 0.9f,
        label = "listeningPulse"
    )

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                // 创建自定义顶部栏，支持返回按钮点击
                EarRoveTopAppBar(
                    title = "正在监听...",
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 动画麦克风
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(PremiumGold.copy(alpha = 0.3f))
                        .scale(listeningPulse),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "正在监听",
                        tint = PremiumGold,
                        modifier = Modifier.size(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 提示文字
                Text(
                    text = "请说出目的地...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 取消按钮
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarningOrange.copy(alpha = 0.8f),
                        contentColor = PureBlack
                    )
                ) {
                    Text(text = "取消语音输入")
                }
            }
        }
    }
}

@Composable
private fun PlanningRouteScreen(
    destination: String,
    navController: NavController
) {
    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = "规划路线中",
                onNavigateUp = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = PremiumGold,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "正在规划前往\n$destination\n的路线",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "请稍候...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureWhite.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NavigatingScreen(
    viewModel: NavigationViewModel,
    arbitrator: Arbitrator,
    navigationService: NavigationService,
    locationManager: LocationManager,
    obstacleService: ObstacleDetectionService,
    trafficLightService: TrafficLightService,
    navController: NavController,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNextStep: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 确保定位开启
    LaunchedEffect(Unit) {
        viewModel.isLocationStarted.value = true
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = "导航中",
                onNavigateUp = { navController.navigateUp() }
            )
        },
        bottomBar = {
            NavigationControlBar(
                onPause = onPause,
                onStop = onStop,
                onNextStep = onNextStep,
                isPaused = false
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack)
        ) {
            // 百度地图
            AndroidView(
                factory = { context ->
                    viewModel.mapView.value ?: MapView(context).apply {
                        viewModel.mapView.value = this
                        viewModel.baiduMap.value = map
                        navigationService.setupMapView(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 导航信息区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack.copy(alpha = 0.85f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 目的地显示
                Text(
                    text = viewModel.destinationText.value,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                    color = PremiumGold,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .semantics {
                            contentDescription = "目的地：${viewModel.destinationText.value}"
                        }
                )

                // 当前指令
                viewModel.currentStep.value?.let { step ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PremiumGold.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 转向图标
                                val turnIcon = when (step.turnType) {
                                    "LEFT" -> Icons.Default.RotateLeft
                                    "RIGHT" -> Icons.Default.RotateRight
                                    else -> Icons.Default.Navigation
                                }

                                Icon(
                                    imageVector = turnIcon,
                                    contentDescription = step.turnType,
                                    tint = PremiumGold,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = step.instruction,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                                    color = PureWhite,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatusChip(
                                    label = "距离",
                                    value = "${step.distance}米",
                                    icon = Icons.Default.Navigation
                                )
                                StatusChip(
                                    label = "时间",
                                    value = "${step.duration}秒",
                                    icon = Icons.Default.VolumeUp
                                )
                            }
                        }
                    }
                }

                // 总体进度
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusItem(
                        label = "总距离",
                        value = "${viewModel.totalDistance.value}米"
                    )
                    StatusItem(
                        label = "剩余",
                        value = "${viewModel.remainingDistance.value}米"
                    )
                    StatusItem(
                        label = "剩余时间",
                        value = "${viewModel.remainingTime.value}分钟"
                    )
                }
            }

            // 障碍物警示
            AnimatedVisibility(
                visible = viewModel.isObstacleDetected.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WarningOrange.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ 前方危险！",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = PureBlack,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "前方${viewModel.obstacleDistance.value}米检测到${viewModel.obstacleType.value}，请避让",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureBlack,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 红绿灯提示
            AnimatedVisibility(
                visible = viewModel.isTrafficLightDetected.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (viewModel.trafficLightStatus.value?.status) {
                                TrafficLightStatus.RED -> Color.Red.copy(alpha = 0.85f)
                                TrafficLightStatus.GREEN -> Color.Green.copy(alpha = 0.85f)
                                TrafficLightStatus.YELLOW -> Color.Yellow.copy(alpha = 0.85f)
                                else -> Color.Gray.copy(alpha = 0.85f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (viewModel.trafficLightStatus.value?.status) {
                                TrafficLightStatus.RED -> "🔴 红灯"
                                TrafficLightStatus.GREEN -> "🟢 绿灯"
                                TrafficLightStatus.YELLOW -> "🟡 黄灯"
                                else -> "交通灯"
                            },
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = PureBlack,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        viewModel.trafficLightStatus.value?.let { trafficLight ->
                            Text(
                                text = "距离${trafficLight.distance}米\n倒计时${trafficLight.countdown}秒",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureBlack,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationControlBar(
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNextStep: () -> Unit,
    isPaused: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack.copy(alpha = 0.9f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 下一步按钮
        Button(
            onClick = onNextStep,
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumGold.copy(alpha = 0.8f),
                contentColor = PureBlack
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "下一步"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "下一步")
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 暂停/继续按钮
        Button(
            onClick = onPause,
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumGold.copy(alpha = 0.8f),
                contentColor = PureBlack
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "继续" else "暂停"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isPaused) "继续" else "暂停")
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 结束导航按钮
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(
                containerColor = WarningOrange.copy(alpha = 0.8f),
                contentColor = PureBlack
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "结束导航"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "结束导航")
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PremiumGold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = PremiumGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PausedScreen(
    destination: String,
    navController: NavController,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = "导航已暂停",
                onNavigateUp = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "暂停",
                    tint = PremiumGold,
                    modifier = Modifier.size(120.dp)
                )

                Text(
                    text = "导航已暂停",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PureWhite
                )

                Text(
                    text = "目的地：$destination",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PremiumGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGold,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(text = "继续导航", style = MaterialTheme.typography.headlineMedium)
                    }

                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningOrange,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(text = "结束导航", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrivedScreen(
    destination: String,
    navController: NavController,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = "已到达目的地",
                onNavigateUp = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 成功图标
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(PremiumGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 80.sp),
                        color = PremiumGold
                    )
                }

                Text(
                    text = "已到达目的地",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    color = PremiumGold,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = destination,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .semantics {
                            contentDescription = "目的地：$destination"
                        }
                )

                Spacer(modifier = Modifier.height(48.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGold,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(text = "开始新的导航", style = MaterialTheme.typography.headlineMedium)
                    }

                    Button(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite.copy(alpha = 0.1f),
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(
                            1.dp,
                            PureWhite.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(text = "退出导航", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

// 预览函数
@Preview(name = "Navigation Screen Preview")
@Composable
fun NavigationScreenPreview() {
    EarRoveTheme {
        // 简化预览
        Column(
            modifier = Modifier.fillMaxSize().background(PureBlack),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "导航屏幕预览",
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite
            )
            Text(
                text = "实际运行时将显示完整导航界面",
                style = MaterialTheme.typography.bodyLarge,
                color = PremiumGold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}