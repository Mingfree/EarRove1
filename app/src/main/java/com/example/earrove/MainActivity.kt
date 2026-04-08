package com.example.earrove

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.earrove.ui.help.HelpScreen
import com.example.earrove.ui.home.HomeScreen
import com.example.earrove.ui.navigation.NavigationScreen
import com.example.earrove.ui.ocr.OcrScreen
import com.example.earrove.ui.settings.SettingsScreen
import com.example.earrove.ui.theme.EarRoveTheme
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.utils.PrivacyUtils
import androidx.compose.foundation.background
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            EarRoveTheme {
                // 使用隐私检查包装器
                PrivacyCheckWrapper()
            }
        }
    }
}

/**
 * 隐私检查包装器
 * 检查用户是否已同意隐私政策，根据情况显示不同内容
 */
@Composable
fun PrivacyCheckWrapper() {
    val context = LocalContext.current

    // 状态：用户是否已同意隐私政策
    var privacyAgreed by remember {
        mutableStateOf(PrivacyUtils.hasUserAgreedToAppPrivacy(context))
    }

    // 状态：百度地图隐私政策是否已同意
    var baiduMapPrivacyAgreed by remember {
        mutableStateOf(PrivacyUtils.hasUserAgreedToBaiduMapPrivacy(context))
    }

    // 检查所有隐私政策是否都已同意
    val allPrivacyAgreed = remember(privacyAgreed, baiduMapPrivacyAgreed) {
        privacyAgreed && baiduMapPrivacyAgreed
    }

    if (allPrivacyAgreed) {
        // 用户已同意所有隐私政策，显示主应用
        LaunchedEffect(Unit) {
            // 延迟初始化百度地图SDK（如果尚未初始化）
            val app = context.applicationContext as? MyApplication
            app?.initializeBaiduMapSDK()
        }

        EarRoveApp()
    } else {
        // 显示隐私政策同意页面
        PrivacyAgreementScreen(
            currentAppPrivacyAgreed = privacyAgreed,
            currentBaiduMapPrivacyAgreed = baiduMapPrivacyAgreed,
            onAppPrivacyChanged = { agreed ->
                privacyAgreed = agreed
                PrivacyUtils.saveAppPrivacyAgreement(context, agreed)
            },
            onBaiduMapPrivacyChanged = { agreed ->
                baiduMapPrivacyAgreed = agreed
                PrivacyUtils.saveBaiduMapPrivacyAgreement(context, agreed)

                if (agreed) {
                    // 用户同意百度地图隐私政策，初始化SDK
                    val app = context.applicationContext as? MyApplication
                    app?.initializeBaiduMapSDK()
                }
            },
            onAllAgreed = {
                // 用户同意所有隐私政策
                privacyAgreed = true
                baiduMapPrivacyAgreed = true
                PrivacyUtils.saveAllPrivacyAgreements(context, true, true)

                // 初始化百度地图SDK
                val app = context.applicationContext as? MyApplication
                app?.initializeBaiduMapSDK()
            },
            onDisagree = {
                // 用户不同意，根据情况处理
                // 如果用户之前已经同意过，现在反悔，需要重新考虑
                // 这里简单处理：如果用户不同意，退出应用
                (context as? ComponentActivity)?.finish()
            }
        )
    }
}

/**
 * 隐私政策同意屏幕
 */
@Composable
fun PrivacyAgreementScreen(
    currentAppPrivacyAgreed: Boolean = false,
    currentBaiduMapPrivacyAgreed: Boolean = false,
    onAppPrivacyChanged: (Boolean) -> Unit = {},
    onBaiduMapPrivacyChanged: (Boolean) -> Unit = {},
    onAllAgreed: () -> Unit = {},
    onDisagree: () -> Unit = {}
) {
    var showDetailedPolicy by remember { mutableStateOf(false) }
    var showBaiduMapPolicy by remember { mutableStateOf(false) }

    // 本地状态
    var appPrivacyChecked by remember { mutableStateOf(currentAppPrivacyAgreed) }
    var baiduMapPrivacyChecked by remember { mutableStateOf(currentBaiduMapPrivacyAgreed) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PureBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 应用标题
            Text(
                text = "EarRove 聆途",
                style = MaterialTheme.typography.headlineLarge,
                color = PremiumGold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "欢迎使用视障辅助导航应用",
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 隐私政策卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = PureBlack.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showDetailedPolicy) {
                        // 显示详细隐私政策
                        DetailedPrivacyPolicy(
                            onBack = { showDetailedPolicy = false }
                        )
                    } else if (showBaiduMapPolicy) {
                        // 显示百度地图隐私政策
                        BaiduMapPrivacyPolicy(
                            onBack = { showBaiduMapPolicy = false }
                        )
                    } else {
                        // 显示隐私政策摘要和选择
                        PrivacyPolicySummary(
                            appPrivacyChecked = appPrivacyChecked,
                            baiduMapPrivacyChecked = baiduMapPrivacyChecked,
                            onAppPrivacyCheckedChange = { checked ->
                                appPrivacyChecked = checked
                                onAppPrivacyChanged(checked)
                            },
                            onBaiduMapPrivacyCheckedChange = { checked ->
                                baiduMapPrivacyChecked = checked
                                onBaiduMapPrivacyChanged(checked)
                            },
                            onShowDetailedPolicy = { showDetailedPolicy = true },
                            onShowBaiduMapPolicy = { showBaiduMapPolicy = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮区域
            ActionButtons(
                allChecked = appPrivacyChecked && baiduMapPrivacyChecked,
                onAgree = {
                    if (appPrivacyChecked && baiduMapPrivacyChecked) {
                        onAllAgreed()
                    }
                },
                onDisagree = onDisagree
            )
        }
    }
}

/**
 * 隐私政策摘要和选择界面
 */
@Composable
private fun PrivacyPolicySummary(
    appPrivacyChecked: Boolean,
    baiduMapPrivacyChecked: Boolean,
    onAppPrivacyCheckedChange: (Boolean) -> Unit,
    onBaiduMapPrivacyCheckedChange: (Boolean) -> Unit,
    onShowDetailedPolicy: () -> Unit,
    onShowBaiduMapPolicy: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "隐私政策同意",
            style = MaterialTheme.typography.headlineMedium,
            color = PremiumGold
        )

        Text(
            text = "为了正常使用应用功能，请阅读并同意以下隐私政策：",
            style = MaterialTheme.typography.bodyLarge,
            color = PureWhite
        )

        // 应用隐私政策选项
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = appPrivacyChecked,
                onCheckedChange = onAppPrivacyCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = PremiumGold,
                    uncheckedColor = PureWhite.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "同意 EarRove 应用隐私政策",
                style = MaterialTheme.typography.bodyLarge,
                color = if (appPrivacyChecked) PremiumGold else PureWhite,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onShowDetailedPolicy
            ) {
                Text("查看详情", color = PremiumGold)
            }
        }

        // 百度地图隐私政策选项
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = baiduMapPrivacyChecked,
                onCheckedChange = onBaiduMapPrivacyCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = PremiumGold,
                    uncheckedColor = PureWhite.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "同意百度地图SDK隐私政策",
                style = MaterialTheme.typography.bodyLarge,
                color = if (baiduMapPrivacyChecked) PremiumGold else PureWhite,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onShowBaiduMapPolicy
            ) {
                Text("查看详情", color = PremiumGold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 重要提示
        Card(
            colors = CardDefaults.cardColors(
                containerColor = PremiumGold.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "重要提示：\n" +
                        "1. 导航功能需要百度地图SDK支持\n" +
                        "2. 位置信息仅用于本地导航计算\n" +
                        "3. 所有数据均在设备本地处理，不会上传",
                style = MaterialTheme.typography.bodyMedium,
                color = PremiumGold,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * 详细隐私政策内容
 */
@Composable
private fun DetailedPrivacyPolicy(
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 返回按钮
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onBack
            ) {
                Text("← 返回", color = PremiumGold)
            }
        }

        Text(
            text = "EarRove 隐私政策",
            style = MaterialTheme.typography.headlineMedium,
            color = PremiumGold
        )

        Text(
            text = PrivacyUtils.getPrivacyPolicyText(),
            style = MaterialTheme.typography.bodyLarge,
            color = PureWhite,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
        )
    }
}

/**
 * 百度地图隐私政策内容
 */
@Composable
private fun BaiduMapPrivacyPolicy(
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 返回按钮
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onBack
            ) {
                Text("← 返回", color = PremiumGold)
            }
        }

        Text(
            text = "百度地图SDK隐私政策",
            style = MaterialTheme.typography.headlineMedium,
            color = PremiumGold
        )

        Text(
            text = PrivacyUtils.getBaiduMapPrivacySummary(),
            style = MaterialTheme.typography.bodyLarge,
            color = PureWhite,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = PureWhite.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "注意：\n" +
                        "百度地图SDK是本应用导航功能的核心依赖。\n" +
                        "如果您不同意百度地图的隐私政策，将无法使用导航功能。",
                style = MaterialTheme.typography.bodyMedium,
                color = PremiumGold,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * 操作按钮区域
 */
@Composable
private fun ActionButtons(
    allChecked: Boolean,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 同意按钮（仅在全部勾选时可用）
        Button(
            onClick = onAgree,
            modifier = Modifier.fillMaxWidth(),
            enabled = allChecked,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allChecked) PremiumGold else PremiumGold.copy(alpha = 0.5f),
                contentColor = if (allChecked) PureBlack else PureBlack.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = if (allChecked) "同意并开始使用" else "请勾选所有隐私政策",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // 不同意按钮
        OutlinedButton(
            onClick = onDisagree,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PureWhite.copy(alpha = 0.7f)
            )
        ) {
            Text("不同意并退出")
        }

        // 提示文字
        if (!allChecked) {
            Text(
                text = "必须同意所有隐私政策才能使用应用",
                style = MaterialTheme.typography.bodySmall,
                color = PremiumGold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 主应用内容
 */
@Composable
fun EarRoveApp() {
    val navController = rememberNavController()

    // 添加全局状态监听
    val context = LocalContext.current
    val app = context.applicationContext as? MyApplication

    // 监听百度地图SDK初始化状态
    var baiduMapInitialized by remember { mutableStateOf(false) }
    var initializationAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 检查百度地图SDK是否已初始化
        val isInitialized = app?.isBaiduMapSDKInitialized() ?: false
        baiduMapInitialized = isInitialized

        if (!isInitialized) {
            // 尝试重新初始化
            app?.initializeBaiduMapSDK()
            initializationAttempted = true
        }
    }

    // 定期检查SDK初始化状态
    LaunchedEffect(initializationAttempted) {
        if (initializationAttempted && !baiduMapInitialized) {
            // 每隔500ms检查一次，最多检查10次
            repeat(10) {
                delay(500)
                val isInitialized = app?.isBaiduMapSDKInitialized() ?: false
                if (isInitialized) {
                    baiduMapInitialized = true
                    return@LaunchedEffect
                }
            }
            
            // 如果10次检查后仍然未初始化，强制进入应用
            Log.w("MainActivity", "百度地图SDK初始化超时，强制进入应用")
            baiduMapInitialized = true
        }
    }

    // 如果百度地图SDK未初始化，显示加载状态
    if (!baiduMapInitialized) {
        Log.d("MainActivity", "监控检查：百度地图SDK未初始化，尝试重新初始化")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = PremiumGold)
                Text(
                    text = "正在初始化导航服务...",
                    color = PureWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        // 正常显示导航
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController = navController)
            }
            composable("navigation") {
                NavigationScreen(navController = navController)
            }
            composable("ocr") {
                OcrScreen(navController = navController)
            }
            composable("settings") {
                SettingsScreen(navController = navController)
            }
            composable("help") {
                HelpScreen(navController = navController)
            }
        }
    }
}

/**
 * 添加缺失的导入（如果IDE没有自动导入）
 * 注意：以下导入可能需要手动添加，取决于你的IDE
 */
// import androidx.compose.foundation.background
// import androidx.compose.foundation.clickable
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.verticalScroll
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.unit.dp
// import com.example.earrove.ui.theme.*
//import com.example.earrove.utils.PrivacyUtils