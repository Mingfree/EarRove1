package com.example.earrove.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.data.privacy.PrivacyRepositoryImpl
import com.example.earrove.data.settings.SettingsRepositoryImpl
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.AppSpacing
import com.example.earrove.ui.theme.EarRoveTheme
import com.example.earrove.R
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsInteractor = remember(context) {
        SettingsInteractor(SettingsRepositoryImpl(context))
    }
    val privacyInteractor = remember(context) {
        PrivacyConsentInteractor(PrivacyRepositoryImpl(context))
    }

    var speechRate by remember { mutableFloatStateOf(settingsInteractor.loadSnapshot().ttsSpeechRate) }
    var hapticFeedbackEnabled by remember { mutableStateOf(settingsInteractor.loadSnapshot().hapticEnabled) }
    var visualAssistEnabled by remember { mutableStateOf(settingsInteractor.loadSnapshot().visualAssistEnabled) }
    var homeAddress by remember { mutableStateOf(settingsInteractor.loadSnapshot().homeAddress) }

    var showRevokeDialog by remember { mutableStateOf(false) }

    val settingsTitle = stringResource(id = R.string.settings_title)
    val ttsSpeedTitle = stringResource(id = R.string.settings_tts_speed_title)
    val hapticTitle = stringResource(id = R.string.settings_haptic_title)
    val privacyTitle = stringResource(id = R.string.settings_privacy_title)
    val visualAssistTitle = stringResource(id = R.string.settings_visual_assist_title)

    val revokeButtonText = stringResource(id = R.string.settings_revoke_button)
    val revokeDialogTitle = stringResource(id = R.string.settings_revoke_dialog_title)
    val revokeDialogText = stringResource(id = R.string.settings_revoke_dialog_text)
    val revokeConfirmText = stringResource(id = R.string.settings_revoke_confirm)
    val revokeCancelText = stringResource(id = R.string.settings_revoke_cancel)

    val a11ySpeechRateDesc = stringResource(id = R.string.a11y_speech_rate_slider_desc, speechRate)
    val a11yHapticToggleDesc =
        if (hapticFeedbackEnabled) stringResource(id = R.string.a11y_haptic_toggle_desc_enabled)
        else stringResource(id = R.string.a11y_haptic_toggle_desc_disabled)
    val a11yVisualAssistToggleDesc =
        if (visualAssistEnabled) stringResource(id = R.string.a11y_visual_assist_toggle_desc_enabled)
        else stringResource(id = R.string.a11y_visual_assist_toggle_desc_disabled)

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = settingsTitle, onNavigateUp = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSpacing.large)
                .verticalScroll(rememberScrollState())
        ) {
            // Speech Rate Setting
            SettingItem(
                title = ttsSpeedTitle,
                content = {
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            settingsInteractor.updateTtsSpeechRate(it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5, // Provides 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0
                        modifier = Modifier.semantics { 
                            contentDescription = a11ySpeechRateDesc
                        }
                    )
                }
            )

            // Haptic Feedback Setting
            SettingItem(
                title = hapticTitle,
                content = {
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = {
                            hapticFeedbackEnabled = it
                            settingsInteractor.updateHapticEnabled(it)
                        },
                        modifier = Modifier.semantics { 
                            contentDescription = a11yHapticToggleDesc
                        }
                    )
                }
            )

            SettingItem(
                title = visualAssistTitle,
                content = {
                    Switch(
                        checked = visualAssistEnabled,
                        onCheckedChange = {
                            visualAssistEnabled = it
                            settingsInteractor.updateVisualAssistEnabled(it)
                            (context as? ComponentActivity)?.recreate()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = a11yVisualAssistToggleDesc
                        }
                    )
                }
            )

            SettingItem(
                title = privacyTitle,
                content = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showRevokeDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = revokeButtonText)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "预设“家”地址",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = homeAddress,
                onValueChange = { homeAddress = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：北京市海淀区XX路XX号") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (homeAddress.isNotBlank()) {
                            homeAddress = homeAddress.trim()
                            settingsInteractor.saveHomeAddress(homeAddress)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存家地址")
                }
                OutlinedButton(
                    onClick = {
                        homeAddress = ""
                        settingsInteractor.clearHomeAddress()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清除家地址")
                }
            }
        }
    }

    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            title = { Text(text = revokeDialogTitle) },
            text = {
                Text(
                    text = revokeDialogText,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        privacyInteractor.revokeAllPrivacyAgreements()
                        showRevokeDialog = false
                        (context as? ComponentActivity)?.recreate()
                    }
                ) {
                    Text(revokeConfirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = false }) {
                    Text(revokeCancelText)
                }
            }
        )
    }
}

@Composable
private fun SettingItem(title: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(1f)) {
             content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    EarRoveTheme {
        SettingsScreen(navController = rememberNavController())
    }
}
