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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.R
import kotlinx.coroutines.launch
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.AppSpacing
import com.example.earrove.ui.theme.EarRoveTheme

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.defaultFactory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeAddressSaveInProgress by viewModel.homeAddressSaveInProgress.collectAsStateWithLifecycle()

    var showRevokeDialog by remember { mutableStateOf(false) }

    val settingsTitle = stringResource(id = R.string.settings_title)
    val ttsSpeedTitle = stringResource(id = R.string.settings_tts_speed_title)
    val hapticTitle = stringResource(id = R.string.settings_haptic_title)
    val privacyTitle = stringResource(id = R.string.settings_privacy_title)

    val revokeButtonText = stringResource(id = R.string.settings_revoke_button)
    val revokeDialogTitle = stringResource(id = R.string.settings_revoke_dialog_title)
    val revokeDialogText = stringResource(id = R.string.settings_revoke_dialog_text)
    val revokeConfirmText = stringResource(id = R.string.settings_revoke_confirm)
    val revokeCancelText = stringResource(id = R.string.settings_revoke_cancel)
    val homeAddressTitle = stringResource(id = R.string.settings_home_address_title)
    val homeAddressPlaceholder = stringResource(id = R.string.settings_home_address_placeholder)
    val homeAddressSave = stringResource(id = R.string.settings_home_address_save)
    val homeAddressClear = stringResource(id = R.string.settings_home_address_clear)
    val homeAddressSavedSnackbar = stringResource(id = R.string.settings_home_address_saved)
    val homeAddressEmptySnackbar = stringResource(id = R.string.settings_home_address_empty_hint)
    val homeAddressInvalidSnackbar = stringResource(id = R.string.settings_home_address_invalid_format)
    val homeAddressNotOnMapSnackbar = stringResource(id = R.string.settings_home_address_not_on_map)
    val homeAddressGeocodeErrorSnackbar = stringResource(id = R.string.settings_home_address_geocode_error)
    val homeAddressFailedSnackbar = stringResource(id = R.string.settings_home_address_save_failed)
    val homeAddressClearedSnackbar = stringResource(id = R.string.settings_home_address_cleared)
    val homeAddressClearNothingSnackbar = stringResource(id = R.string.settings_home_address_clear_nothing)
    val homeAddressClearFailedSnackbar = stringResource(id = R.string.settings_home_address_clear_failed)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showZhSnackbar(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = null,
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
        }
    }

    val a11ySpeechRateDesc = stringResource(id = R.string.a11y_speech_rate_slider_desc, uiState.ttsSpeechRate)
    val a11yHapticToggleDesc =
        if (uiState.hapticEnabled) stringResource(id = R.string.a11y_haptic_toggle_desc_enabled)
        else stringResource(id = R.string.a11y_haptic_toggle_desc_disabled)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        },
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
            SettingItem(
                title = ttsSpeedTitle,
                content = {
                    Slider(
                        value = uiState.ttsSpeechRate,
                        onValueChange = { viewModel.onTtsSpeechRateChange(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        modifier = Modifier.semantics {
                            contentDescription = a11ySpeechRateDesc
                        }
                    )
                }
            )

            SettingItem(
                title = hapticTitle,
                content = {
                    Switch(
                        checked = uiState.hapticEnabled,
                        onCheckedChange = { viewModel.onHapticEnabledChange(it) },
                        modifier = Modifier.semantics {
                            contentDescription = a11yHapticToggleDesc
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
                text = homeAddressTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.homeAddress,
                onValueChange = { viewModel.onHomeAddressDraftChange(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = homeAddressPlaceholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.saveHomeAddressFromDraftAsync { result ->
                            val message = when (result) {
                                HomeAddressSaveResult.Saved -> homeAddressSavedSnackbar
                                HomeAddressSaveResult.Empty -> homeAddressEmptySnackbar
                                HomeAddressSaveResult.InvalidFormat -> homeAddressInvalidSnackbar
                                HomeAddressSaveResult.NotRecognizedOnMap -> homeAddressNotOnMapSnackbar
                                HomeAddressSaveResult.GeocodeError -> homeAddressGeocodeErrorSnackbar
                                HomeAddressSaveResult.Failed -> homeAddressFailedSnackbar
                            }
                            showZhSnackbar(message)
                        }
                    },
                    enabled = !homeAddressSaveInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(homeAddressSave)
                }
                OutlinedButton(
                    onClick = {
                        val message = when (viewModel.clearHomeAddress()) {
                            HomeAddressClearResult.Cleared -> homeAddressClearedSnackbar
                            HomeAddressClearResult.NothingToClear -> homeAddressClearNothingSnackbar
                            HomeAddressClearResult.Failed -> homeAddressClearFailedSnackbar
                        }
                        showZhSnackbar(message)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(homeAddressClear)
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
                        viewModel.revokeAllPrivacyAgreements()
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
