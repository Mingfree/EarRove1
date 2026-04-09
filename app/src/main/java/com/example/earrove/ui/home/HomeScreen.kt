package com.example.earrove.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.R
import com.example.earrove.ui.theme.AppSize
import com.example.earrove.ui.theme.AppSpacing
import com.example.earrove.ui.theme.DeepGray
import com.example.earrove.ui.theme.EarRoveTheme
import com.example.earrove.utils.rememberTTSManager
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController) {
    val navTitle = stringResource(id = R.string.home_nav_title)
    val navSubtitle = stringResource(id = R.string.home_nav_subtitle)
    val navA11y = stringResource(id = R.string.a11y_home_nav_entry)
    val ocrTitle = stringResource(id = R.string.home_ocr_title)
    val ocrSubtitle = stringResource(id = R.string.home_ocr_subtitle)
    val ocrA11y = stringResource(id = R.string.a11y_home_ocr_entry)
    val ocrGuideSpeak = stringResource(id = R.string.home_ocr_guide_speak)
    val settingsText = stringResource(id = R.string.home_settings)
    val helpText = stringResource(id = R.string.home_help)

    val ttsManager = rememberTTSManager()
    val scope = rememberCoroutineScope()
    var isOcrGuideSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Navigation Mode Card
        HomeCard(
            modifier = Modifier.weight(1f),
            title = navTitle,
            subtitle = navSubtitle,
            backgroundColor = MaterialTheme.colorScheme.background,
            icon = Icons.Default.Navigation,
            contentDescription = navA11y,
            onClick = { navController.navigate("navigation") }
        )

        // 2. OCR Mode Card
        HomeCard(
            modifier = Modifier.weight(1f),
            title = ocrTitle,
            subtitle = ocrSubtitle,
            backgroundColor = DeepGray,
            icon = Icons.Default.TextFields,
            contentDescription = ocrA11y,
            onClick = {
                if (isOcrGuideSpeaking) return@HomeCard
                isOcrGuideSpeaking = true
                scope.launch {
                    try {
                        ttsManager.speakAndWait(ocrGuideSpeak)
                    } finally {
                        isOcrGuideSpeaking = false
                        navController.navigate("ocr")
                    }
                }
            }
        )

        // 3. Bottom Action Bar
        BottomActionBar(navController, settingsText, helpText)
    }
}

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(AppSpacing.xxLarge)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (backgroundColor == DeepGray) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.fabLarge)
            )
            Spacer(modifier = Modifier.height(AppSpacing.xLarge))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (backgroundColor == DeepGray) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (backgroundColor == DeepGray) Color.Gray else Color.DarkGray
                )
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    navController: NavController,
    settingsText: String,
    helpText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = AppSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionButton(
                icon = Icons.Default.Settings,
                contentDescription = settingsText,
                onClick = { navController.navigate("settings") }
            )
            ActionButton(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = helpText,
                onClick = { navController.navigate("help") }
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .size(AppSize.iconMedium)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    EarRoveTheme {
        HomeScreen(navController = rememberNavController())
    }
}
