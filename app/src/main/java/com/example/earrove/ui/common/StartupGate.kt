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
import androidx.compose.ui.res.stringResource
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.R
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
    val title = stringResource(id = R.string.startup_config_missing_title)
    val hint = stringResource(id = R.string.startup_config_missing_hint)
    val cta = stringResource(id = R.string.startup_config_missing_cta)
    val fixMethod = stringResource(id = R.string.startup_config_missing_fix_method)
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
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = PremiumGold
            )
            Spacer(modifier = Modifier.padding(0.dp))
            Text(
                text = hint,
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
                text = fixMethod,
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
                Text(text = cta, style = MaterialTheme.typography.bodyLarge)
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

    val title = stringResource(id = R.string.startup_permission_title)
    val intro = stringResource(id = R.string.startup_permission_intro)
    val deniedHint = stringResource(id = R.string.startup_permission_denied_hint)
    val continueText = stringResource(id = R.string.startup_permission_continue)
    val permGps = stringResource(id = R.string.perm_location_gps)
    val permCamera = stringResource(id = R.string.perm_camera)
    val permMic = stringResource(id = R.string.perm_microphone)

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
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = PremiumGold
            )

            val missing = buildList {
                if (!PermissionUtils.hasLocationPermission(context)) add(permGps)
                if (!PermissionUtils.hasCameraPermission(context)) add(permCamera)
                if (!PermissionUtils.hasAudioPermission(context)) add(permMic)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = intro,
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
                    text = deniedHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumGold
                )
                Spacer(modifier = Modifier.padding(16.dp))
                OutlinedButton(
                    onClick = onContinueWithoutPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = continueText, color = PureWhite)
                }
            }
        }
    }
}

