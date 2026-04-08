package com.example.earrove.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.utils.ConfigValidator
import com.example.earrove.utils.PermissionUtils
import com.example.earrove.utils.RequestPermissionsDialog

@Composable
fun StartupConfigAndPermissionGate(
    onReady: @Composable () -> Unit
) {
    var recheckToken by remember { mutableStateOf(0) }
    val configCheck = remember(recheckToken) { ConfigValidator.checkEssentialConfig() }

    if (!configCheck.isOk) {
        ConfigMissingScreen(
            missing = configCheck.missing,
            onRetry = { recheckToken++ }
        )
        return
    }

    // 配置正常后，再做权限自检/引导
    val context = LocalContext.current
    var proceedAnyway by remember { mutableStateOf(false) }
    val hasAllPermissions =
        PermissionUtils.hasLocationPermission(context) &&
            PermissionUtils.hasCameraPermission(context) &&
            PermissionUtils.hasAudioPermission(context)

    if (hasAllPermissions || proceedAnyway) {
        onReady()
        return
    }

    PermissionRequestScreen(
        onGranted = { proceedAnyway = true },
        onContinueWithoutPermissions = { proceedAnyway = true }
    )
}

@Composable
private fun ConfigMissingScreen(
    missing: List<String>,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PureBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "启动自检失败：配置缺失",
                style = MaterialTheme.typography.headlineMedium,
                color = PremiumGold
            )
            Spacer(modifier = Modifier.padding(0.dp))
            Text(
                text = "当前构建使用了占位符 Key，相关功能可能不可用。请按以下步骤修复后重新构建/重启应用：",
                style = MaterialTheme.typography.bodyLarge,
                color = PureWhite,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.padding(0.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    missing.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                text = "修复方式：复制 `config.sample.properties` → `local.properties`，填写真实值后重新构建。 ",
                style = MaterialTheme.typography.bodyMedium,
                color = PremiumGold
            )

            Spacer(modifier = Modifier.padding(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = PremiumGold,
                    contentColor = PureBlack
                )
            ) {
                Text(text = "我已完成配置（请重建/重启后生效）", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    onGranted: () -> Unit,
    onContinueWithoutPermissions: () -> Unit
) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PureBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "需要授权",
                style = MaterialTheme.typography.headlineMedium,
                color = PremiumGold
            )

            val missing = buildList {
                if (!PermissionUtils.hasLocationPermission(context)) add("定位（GPS）")
                if (!PermissionUtils.hasCameraPermission(context)) add("相机")
                if (!PermissionUtils.hasAudioPermission(context)) add("麦克风")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "为了使用导航/文字识别，需要授权以下权限：",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PureWhite
                    )
                    missing.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(16.dp))

            // 触发系统权限弹窗（如果已有权限会直接回调 onGranted）
            RequestPermissionsDialog(
                permissions = PermissionUtils.requiredPermissions,
                onPermissionGranted = {
                    permissionDenied = false
                    onGranted()
                },
                onPermissionDenied = {
                    permissionDenied = true
                }
            )

            if (permissionDenied) {
                Spacer(modifier = Modifier.padding(16.dp))
                Text(
                    text = "权限未授权。你可以继续进入主页，但导航/文字识别功能可能不可用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumGold
                )
                Spacer(modifier = Modifier.padding(16.dp))
                OutlinedButton(
                    onClick = onContinueWithoutPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "继续（稍后在页面内授权）", color = PureWhite)
                }
            }
        }
    }
}

