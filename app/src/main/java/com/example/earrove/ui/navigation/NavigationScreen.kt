package com.example.earrove.ui.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
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
import com.example.earrove.ui.theme.AppSize
import com.example.earrove.ui.theme.AppSpacing
import com.example.earrove.R
import com.example.earrove.domain.arbitration.AccessibilityEvent
import com.example.earrove.utils.Arbitrator
import com.example.earrove.utils.BaiduMapUtils
import com.example.earrove.utils.NearbyPoiCandidate
import com.example.earrove.utils.DestinationExtractor
import com.example.earrove.utils.LocationManager
import com.example.earrove.utils.PermissionUtils
import com.example.earrove.utils.RequestPermissionsDialog
import com.example.earrove.data.privacy.PrivacyRepository
import com.example.earrove.data.privacy.PrivacyRepositoryImpl
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.data.settings.SettingsRepositoryImpl
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
import kotlinx.coroutines.withContext
import kotlin.random.Random
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.CoordType
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
}

private data class DestinationSuggestion(
    val label: String,
    val query: String
)

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun NavigationScreen(
    navController: NavController,
    viewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val privacyRepository = remember(context) { PrivacyRepositoryImpl(context) }
    val settingsRepository = remember(context) { SettingsRepositoryImpl(context) }

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
                val permDeniedTitle = stringResource(id = R.string.nav_perm_denied_title)
                val permDeniedBody = stringResource(id = R.string.nav_perm_denied_body)
                val permDeniedOpenSettings = stringResource(id = R.string.nav_perm_denied_open_settings)
                val permDeniedBackHome = stringResource(id = R.string.nav_perm_denied_back_home)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
                ) {
                    Text(
                        text = permDeniedTitle,
                        color = PremiumGold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = permDeniedBody,
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
                        Text(permDeniedOpenSettings)
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
                        Text(permDeniedBackHome)
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
            val privacyAgreed = privacyRepository.hasUserAgreedToBaiduMapPrivacy()

            if (!privacyAgreed) {
                sdkInitializationError = context.getString(R.string.nav_privacy_need)
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
                    sdkInitializationError = context.getString(
                        R.string.nav_sdk_init_failed,
                        e.message ?: ""
                    )
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
                sdkInitializationError = context.getString(R.string.nav_location_unavailable)
            }

        } catch (e: Exception) {
            sdkInitializationError = context.getString(
                R.string.nav_service_init_failed,
                e.message ?: ""
            )
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
                    contentDescription = stringResource(id = R.string.nav_location_error_a11y),
                    tint = WarningOrange,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = stringResource(id = R.string.nav_service_init_failed_title),
                    color = WarningOrange,
                    fontSize = 20.sp
                )

                Text(
                    text = sdkInitializationError ?: stringResource(id = R.string.nav_unknown_error),
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
                        Text(text = stringResource(id = R.string.nav_go_privacy_settings))
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
                    Text(text = stringResource(id = R.string.nav_retry_init))
                }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite.copy(alpha = 0.1f),
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = stringResource(id = R.string.nav_back_home))
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
                    modifier = Modifier.size(AppSize.iconXLarge)
                )

                Text(
                    text = stringResource(id = R.string.nav_init_navigation_service),
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = stringResource(id = R.string.nav_confirm_baidu_privacy),
                    color = PremiumGold,
                    fontSize = 14.sp
                )

                // 添加手动检查按钮
                Button(
                    onClick = {
                        // 重新检查隐私政策状态
                        val privacyAgreed = privacyRepository.hasUserAgreedToBaiduMapPrivacy()
                        if (privacyAgreed) {
                            // 如果已同意，重新初始化SDK
                            sdkInitializationError = null
                        } else {
                            sdkInitializationError = context.getString(R.string.nav_privacy_need_baidu_short)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold.copy(alpha = 0.8f),
                        contentColor = PureBlack
                    ),
                    modifier = Modifier.padding(top = AppSpacing.large)
                ) {
                    Text(text = stringResource(id = R.string.nav_check_privacy_status))
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
                    Text(text = stringResource(id = R.string.nav_back_home_view_privacy))
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
            sdkInitializationError = context.getString(
                R.string.nav_location_manager_create_failed,
                e.message ?: ""
            )
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
                    contentDescription = stringResource(id = R.string.nav_location_unavailable_a11y),
                    tint = WarningOrange,
                    modifier = Modifier.size(AppSize.iconXLarge)
                )

                Text(
                    text = stringResource(id = R.string.nav_location_init_failed_title),
                    color = WarningOrange,
                    fontSize = 20.sp
                )

                Text(
                    text = stringResource(id = R.string.nav_location_init_failed_reasons),
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // 检查隐私政策
                Button(
                    onClick = {
                        val privacyAgreed = privacyRepository.hasUserAgreedToBaiduMapPrivacy()
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
                    Text(text = stringResource(id = R.string.nav_check_privacy_status))
                }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite.copy(alpha = 0.1f),
                        contentColor = PureWhite
                    )
                ) {
                    Text(text = stringResource(id = R.string.nav_back_home))
                }
            }
        }
        return
    }

    // 其他管理器初始化
    val ttsManager = rememberTTSManager()
    val vibrationManager = rememberVibrationManager()
    val speechRecognizer = rememberSpeechRecognizer()
    val arbitrator = rememberArbitrator(context, ttsManager, vibrationManager)

    // 初始化导航服务
    val navigationService = remember { NavigationService(context) }
    val obstacleService = remember { ObstacleDetectionService() }
    val trafficLightService = remember { TrafficLightService() }

    // 初始化目的地提取器（Qwen 大模型清洗 ASR 原文）
    val destinationExtractor = remember { DestinationExtractor() }

    // 初始语音提示
    LaunchedEffect(Unit) {
        delay(500)
        if (viewModel.navigationState.value == NavigationState.STANDBY) {
            ttsManager.speak(context.getString(R.string.nav_tts_prompt_center_mic))
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
                    arbitrator.submit(AccessibilityEvent.Obstacle(viewModel.obstacleType.value))

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
                    arbitrator.submit(
                        AccessibilityEvent.Nav.TrafficLight(
                            trafficLightService.getTrafficLightDescription(trafficLight),
                            trafficLight.countdown
                        )
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
                        arbitrator.submit(
                            AccessibilityEvent.Nav.RouteStart(
                                destination,
                                route.totalDistance,
                                route.totalDuration / 60
                            )
                        )
                    } else {
                        ttsManager.speak(context.getString(R.string.nav_tts_route_plan_timeout))
                        viewModel.navigationState.value = NavigationState.STANDBY
                    }
                } else {
                    ttsManager.speak(context.getString(R.string.nav_tts_gps_missing))
                    viewModel.navigationState.value = NavigationState.STANDBY
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ttsManager.speak(context.getString(R.string.nav_tts_route_plan_failed))
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
                settingsRepository = settingsRepository,
                privacyRepository = privacyRepository,
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
                            ttsManager.speakAndWait(context.getString(R.string.nav_tts_listening_ask_destination))
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
                                        ttsManager.speak(context.getString(R.string.nav_tts_cannot_find_destination))
                                        viewModel.navigationState.value = NavigationState.STANDBY
                                    }
                                } else {
                                    ttsManager.speak(context.getString(R.string.nav_tts_voice_not_recognized))
                                    viewModel.navigationState.value = NavigationState.STANDBY
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            ttsManager.speak(context.getString(R.string.nav_tts_voice_recognition_failed))
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
                    ttsManager.speak(context.getString(R.string.nav_tts_cancelled_voice_input))
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
                    ttsManager.speak(context.getString(R.string.nav_tts_navigation_paused))
                    locationManager.stopLocation()
                },
                onStop = {
                    viewModel.navigationState.value = NavigationState.ARRIVED
                    navigationService.stopNavigation()
                    locationManager.stopLocation()
                    obstacleService.stopMonitoring()
                    trafficLightService.stopMonitoring()
                    arbitrator.submit(AccessibilityEvent.Nav.Destination(viewModel.destinationText.value))
                },
                onNextStep = {
                    navigationService.moveToNextStep()?.let { nextStep ->
                        // 播报下一步指令
                        val direction = when (nextStep.turnType) {
                            "LEFT" -> context.getString(R.string.nav_turn_left)
                            "RIGHT" -> context.getString(R.string.nav_turn_right)
                            "STRAIGHT" -> context.getString(R.string.nav_turn_straight)
                            "ARRIVE" -> context.getString(R.string.nav_turn_arrive)
                            else -> context.getString(R.string.nav_turn_continue_forward)
                        }
                        arbitrator.submit(AccessibilityEvent.Nav.Turn(direction, nextStep.distance))
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
                    ttsManager.speak(context.getString(R.string.nav_tts_continue_navigation))
                    viewModel.isLocationStarted.value = true
                },
                onStop = {
                    viewModel.navigationState.value = NavigationState.ARRIVED
                    navigationService.stopNavigation()
                    locationManager.stopLocation()
                    obstacleService.stopMonitoring()
                    trafficLightService.stopMonitoring()
                    arbitrator.submit(AccessibilityEvent.Nav.Destination(viewModel.destinationText.value))
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
                    ttsManager.speak(context.getString(R.string.nav_tts_say_new_destination))
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
    settingsRepository: SettingsRepository,
    privacyRepository: PrivacyRepository,
    navController: NavController,
    onMicClick: () -> Unit,
    onStartNavigation: (String, LatLng) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategoryLabel by remember { mutableStateOf("") }
    var candidateList by remember { mutableStateOf<List<NearbyPoiCandidate>>(emptyList()) }
    var showCandidateSheet by remember { mutableStateOf(false) }
    var isLoadingCandidates by remember { mutableStateOf(false) }

    val standbyTopTitle = stringResource(id = R.string.nav_standby_top_title)
    val privacyNeedSpeak = stringResource(id = R.string.nav_privacy_need_speak)
    val currentLocationLabel = stringResource(id = R.string.nav_current_location_label)
    val locatingText = stringResource(id = R.string.nav_locating_text)
    val micA11yDesc = stringResource(id = R.string.nav_mic_a11y_desc)
    val micIconDesc = stringResource(id = R.string.nav_mic_icon_desc)
    val standbyPromptTitle = stringResource(id = R.string.nav_standby_prompt_title)
    val standbyPromptA11y = stringResource(id = R.string.nav_standby_prompt_a11y)
    val standbyPromptHint = stringResource(id = R.string.nav_standby_prompt_hint)
    val parseAddressFailSpeak = stringResource(id = R.string.nav_parse_address_fail_speak)
    val cannotFindDestination = stringResource(id = R.string.nav_tts_cannot_find_destination)
    val locatingRetrySpeak = stringResource(id = R.string.nav_locating_retry_speak)
    val saveHomeAddressSpeak = stringResource(id = R.string.nav_save_home_address_speak)
    val planToHomeSpeak = stringResource(id = R.string.nav_plan_to_home_speak)
    val recommendHomeLabel = stringResource(id = R.string.nav_recommend_home_label)
    val recommendSupermarketLabel = stringResource(id = R.string.nav_recommend_supermarket_label)
    val recommendSubwayLabel = stringResource(id = R.string.nav_recommend_subway_label)
    val recommendBusStopLabel = stringResource(id = R.string.nav_recommend_bus_stop_label)
    val recommendSchoolLabel = stringResource(id = R.string.nav_recommend_school_label)
    val recommendHospitalLabel = stringResource(id = R.string.nav_recommend_hospital_label)
    val recommendHomeQuery = stringResource(id = R.string.nav_recommend_home_query)
    val recommendSupermarketQuery = stringResource(id = R.string.nav_recommend_supermarket_query)
    val recommendSubwayQuery = stringResource(id = R.string.nav_recommend_subway_query)
    val recommendBusStopQuery = stringResource(id = R.string.nav_recommend_bus_stop_query)
    val recommendSchoolQuery = stringResource(id = R.string.nav_recommend_school_query)
    val recommendHospitalQuery = stringResource(id = R.string.nav_recommend_hospital_query)
    val fixedRecommendedDestinations = remember(
        recommendHomeLabel,
        recommendSupermarketLabel,
        recommendSubwayLabel,
        recommendBusStopLabel,
        recommendSchoolLabel,
        recommendHospitalLabel,
        recommendHomeQuery,
        recommendSupermarketQuery,
        recommendSubwayQuery,
        recommendBusStopQuery,
        recommendSchoolQuery,
        recommendHospitalQuery
    ) {
        listOf(
            DestinationSuggestion(recommendHomeLabel, recommendHomeQuery),
            DestinationSuggestion(recommendSupermarketLabel, recommendSupermarketQuery),
            DestinationSuggestion(recommendSubwayLabel, recommendSubwayQuery),
            DestinationSuggestion(recommendBusStopLabel, recommendBusStopQuery),
            DestinationSuggestion(recommendSchoolLabel, recommendSchoolQuery),
            DestinationSuggestion(recommendHospitalLabel, recommendHospitalQuery)
        )
    }
    val changeDestinationSpeak = stringResource(id = R.string.nav_change_destination_speak)
    val changeDestinationA11y = stringResource(id = R.string.nav_change_destination_a11y)
    val navControlNextStepA11y = stringResource(id = R.string.nav_control_next_step_a11y)
    val navControlPauseStateA11y = stringResource(id = R.string.nav_control_pause_state_a11y)
    val navControlResumeStateA11y = stringResource(id = R.string.nav_control_resume_state_a11y)
    val navControlEndA11y = stringResource(id = R.string.nav_control_end_a11y)

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
        val privacyAgreed = privacyRepository.hasUserAgreedToBaiduMapPrivacy()
        if (!privacyAgreed) {
            // 如果未同意，显示提示
            ttsManager.speak(privacyNeedSpeak)
        }
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = standbyTopTitle,
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
                    .padding(AppSpacing.xxLarge)
                    .semantics { isTraversalGroup = true },
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
                                contentDescription = currentLocationLabel,
                                tint = PremiumGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentLocationLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = location.addrStr ?: locatingText,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f),
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
                                    contentDescription = micA11yDesc
                                    traversalIndex = 0f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = micIconDesc,
                                tint = PremiumGold,
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(micScale)
                            )
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        // 提示文字
                        Text(
                            text = standbyPromptTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .semantics {
                                    contentDescription = standbyPromptA11y
                                }
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.small))

                        Text(
                            text = standbyPromptHint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        Text(
                            text = stringResource(id = R.string.nav_recommended_destinations),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PremiumGold,
                            modifier = Modifier.semantics { traversalIndex = 1f }
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.small))

                        fixedRecommendedDestinations.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { destination ->
                                    DestinationSuggestionButton(
                                        destination = destination.label,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (destination.label == recommendHomeLabel) {
                                                val latestHomeAddress = settingsRepository.getHomeAddress()?.trim()
                                                if (latestHomeAddress.isNullOrEmpty()) {
                                                    ttsManager.speak(saveHomeAddressSpeak)
                                                    navController.navigate("settings")
                                                    return@DestinationSuggestionButton
                                                }
                                                ttsManager.speak(planToHomeSpeak)
                                                scope.launch {
                                                    try {
                                                        val location = withContext(Dispatchers.IO) {
                                                            baiduMapUtils.geocodeAddress(latestHomeAddress).first()
                                                        }
                                                        if (location != null) {
                                                            onStartNavigation(recommendHomeLabel, location)
                                                        } else {
                                                            ttsManager.speak(cannotFindDestination)
                                                        }
                                                    } catch (_: Exception) {
                                                        ttsManager.speak(parseAddressFailSpeak)
                                                    }
                                                }
                                                return@DestinationSuggestionButton
                                            }

                                            val currentLoc = viewModel.currentLocation.value
                                            if (currentLoc == null) {
                                                ttsManager.speak(locatingRetrySpeak)
                                                return@DestinationSuggestionButton
                                            }
                                            ttsManager.speak(
                                                context.getString(
                                                    R.string.nav_plan_to_destination_template,
                                                    destination.label
                                                )
                                            )
                                            selectedCategoryLabel = destination.label
                                            showCandidateSheet = true
                                            isLoadingCandidates = true
                                            candidateList = emptyList()

                                            scope.launch {
                                                try {
                                                    val candidates = withContext(Dispatchers.IO) {
                                                        baiduMapUtils.searchNearbyPoiCandidates(
                                                            keyword = destination.query,
                                                            center = LatLng(currentLoc.latitude, currentLoc.longitude),
                                                            limit = 5
                                                        ).first()
                                                    }
                                                    candidateList = candidates
                                                    if (candidates.isEmpty()) {
                                                        ttsManager.speak(cannotFindDestination)
                                                    }
                                                } catch (_: Exception) {
                                                    ttsManager.speak(parseAddressFailSpeak)
                                                } finally {
                                                    isLoadingCandidates = false
                                                }
                                            }
                                        },
                                        contentDescription = context.getString(
                                            R.string.nav_recommended_destination_a11y_template,
                                            destination.label
                                        )
                                    )
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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
                                ttsManager.speak(changeDestinationSpeak)
                            }
                        )
                        .semantics {
                            contentDescription = changeDestinationA11y
                        }
                ) {
                    // 这里是空的，只是为了捕获点击事件
                }
            }
        }
    }

    if (showCandidateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCandidateSheet = false },
            containerColor = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = selectedCategoryLabel,
                    color = PremiumGold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    isLoadingCandidates -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = PremiumGold)
                        }
                    }

                    candidateList.isEmpty() -> {
                        Text(
                            text = cannotFindDestination,
                            color = PureWhite.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    else -> {
                        candidateList.forEach { candidate ->
                            Button(
                                onClick = {
                                    showCandidateSheet = false
                                    onStartNavigation(candidate.name, candidate.location)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PremiumGold.copy(alpha = 0.8f),
                                    contentColor = PureBlack
                                )
                            ) {
                                Text("${candidate.name}  ${formatDistance(candidate.distanceMeters)}")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DestinationSuggestionButton(
    destination: String,
    modifier: Modifier = Modifier,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
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
            modifier = Modifier
                .padding(12.dp)
                .semantics { this.contentDescription = contentDescription }
        )
    }
}

private fun formatDistance(distanceMeters: Int): String {
    return if (distanceMeters >= 1000) {
        String.format("%.1f公里", distanceMeters / 1000f)
    } else {
        "${distanceMeters}米"
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
                    title = stringResource(id = R.string.nav_listening_title),
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
                        contentDescription = stringResource(id = R.string.nav_listening_a11y),
                        tint = PremiumGold,
                        modifier = Modifier.size(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 提示文字
                Text(
                    text = stringResource(id = R.string.nav_listening_prompt),
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
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
                    Text(text = stringResource(id = R.string.nav_cancel_voice_input))
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
                title = stringResource(id = R.string.nav_planning_title),
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
                    text = stringResource(id = R.string.nav_planning_route_template, destination),
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.nav_planning_wait),
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
    val context = LocalContext.current
    val navControlNextStepA11y = stringResource(id = R.string.nav_control_next_step_a11y)
    val navControlPauseStateA11y = stringResource(id = R.string.nav_control_pause_state_a11y)
    val navControlResumeStateA11y = stringResource(id = R.string.nav_control_resume_state_a11y)
    val navControlEndA11y = stringResource(id = R.string.nav_control_end_a11y)

    // 确保定位开启
    LaunchedEffect(Unit) {
        viewModel.isLocationStarted.value = true
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = stringResource(id = R.string.nav_navigating_title),
                onNavigateUp = { navController.navigateUp() }
            )
        },
        bottomBar = {
            NavigationControlBar(
                onPause = onPause,
                onStop = onStop,
                onNextStep = onNextStep,
                isPaused = false,
                nextStepA11y = navControlNextStepA11y,
                pauseStateA11y = navControlPauseStateA11y,
                resumeStateA11y = navControlResumeStateA11y,
                endA11y = navControlEndA11y
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
                            contentDescription = context.getString(
                                R.string.nav_destination_template,
                                viewModel.destinationText.value
                            )
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
                                    label = stringResource(id = R.string.nav_label_distance),
                                    value = stringResource(id = R.string.nav_value_meters, step.distance),
                                    icon = Icons.Default.Navigation
                                )
                                StatusChip(
                                    label = stringResource(id = R.string.nav_label_time),
                                    value = stringResource(id = R.string.nav_value_seconds, step.duration),
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
                        label = stringResource(id = R.string.nav_label_total_distance),
                        value = stringResource(id = R.string.nav_value_meters, viewModel.totalDistance.value)
                    )
                    StatusItem(
                        label = stringResource(id = R.string.nav_label_remaining),
                        value = stringResource(id = R.string.nav_value_meters, viewModel.remainingDistance.value)
                    )
                    StatusItem(
                        label = stringResource(id = R.string.nav_label_remaining_time),
                        value = stringResource(id = R.string.nav_value_minutes, viewModel.remainingTime.value)
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
                                    text = stringResource(id = R.string.nav_obstacle_alert_title),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = PureBlack,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                    text = stringResource(
                                        id = R.string.nav_obstacle_alert_template,
                                        viewModel.obstacleDistance.value,
                                        viewModel.obstacleType.value
                                    ),
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
                                TrafficLightStatus.RED -> stringResource(id = R.string.nav_traffic_light_red)
                                TrafficLightStatus.GREEN -> stringResource(id = R.string.nav_traffic_light_green)
                                TrafficLightStatus.YELLOW -> stringResource(id = R.string.nav_traffic_light_yellow)
                                else -> stringResource(id = R.string.nav_traffic_light_unknown)
                            },
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = PureBlack,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        viewModel.trafficLightStatus.value?.let { trafficLight ->
                            Text(
                                text = stringResource(
                                    id = R.string.nav_traffic_light_countdown_template,
                                    trafficLight.distance,
                                    trafficLight.countdown
                                ),
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
    isPaused: Boolean,
    nextStepA11y: String,
    pauseStateA11y: String,
    resumeStateA11y: String,
    endA11y: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack.copy(alpha = 0.9f))
                    .padding(AppSpacing.large)
                    .semantics { isTraversalGroup = true },
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
                .semantics {
                    traversalIndex = 0f
                    contentDescription = nextStepA11y
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.nav_next_step)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.nav_next_step))
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
                .semantics {
                    traversalIndex = 1f
                    contentDescription = if (isPaused) resumeStateA11y else pauseStateA11y
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) {
                        stringResource(id = R.string.nav_continue)
                    } else {
                        stringResource(id = R.string.nav_pause)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaused) {
                        stringResource(id = R.string.nav_continue)
                    } else {
                        stringResource(id = R.string.nav_pause)
                    }
                )
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
                .semantics {
                    traversalIndex = 2f
                    contentDescription = endA11y
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(id = R.string.nav_end_navigation)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.nav_end_navigation))
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
                title = stringResource(id = R.string.nav_paused_title),
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
                    contentDescription = stringResource(id = R.string.nav_pause_a11y),
                    tint = PremiumGold,
                    modifier = Modifier.size(120.dp)
                )

                Text(
                    text = stringResource(id = R.string.nav_paused_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = PureWhite
                )

                Text(
                    text = stringResource(
                        id = R.string.nav_paused_destination_template,
                        destination
                    ),
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
                        Text(
                            text = stringResource(id = R.string.nav_continue_navigation),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningOrange,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.nav_end_navigation),
                            style = MaterialTheme.typography.headlineMedium
                        )
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
    val context = LocalContext.current
    Scaffold(
        topBar = {
            EarRoveTopAppBar(
                title = stringResource(id = R.string.nav_arrived_title),
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
                    text = stringResource(id = R.string.nav_arrived_title),
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
                            contentDescription = context.getString(
                                R.string.nav_destination_template,
                                destination
                            )
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
                        Text(
                            text = stringResource(id = R.string.nav_start_new_navigation),
                            style = MaterialTheme.typography.headlineMedium
                        )
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
                        Text(
                            text = stringResource(id = R.string.nav_exit_navigation),
                            style = MaterialTheme.typography.headlineMedium
                        )
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
                text = stringResource(id = R.string.nav_preview_title),
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite
            )
            Text(
                text = stringResource(id = R.string.nav_preview_body),
                style = MaterialTheme.typography.bodyLarge,
                color = PremiumGold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}