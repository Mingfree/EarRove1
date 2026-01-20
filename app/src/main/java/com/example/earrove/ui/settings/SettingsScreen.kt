package com.example.earrove.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.EarRoveTheme

@Composable
fun SettingsScreen(navController: NavController) {
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = "设置", onNavigateUp = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Speech Rate Setting
            SettingItem(
                title = "朗读速度",
                content = {
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.5f..2.0f,
                        steps = 5, // Provides 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0
                        modifier = Modifier.semantics { 
                            contentDescription = "朗读速度调节器，当前值为 ${String.format("%.2f", speechRate)}"
                        }
                    )
                }
            )

            // Haptic Feedback Setting
            SettingItem(
                title = "震动反馈",
                content = {
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = { hapticFeedbackEnabled = it },
                        modifier = Modifier.semantics { 
                            contentDescription = "震动反馈开关，当前为 ${if(hapticFeedbackEnabled) "开启" else "关闭"}"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingItem(title: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
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
