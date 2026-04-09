package com.example.earrove.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.R
import com.example.earrove.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    val helpCenterTitle = stringResource(id = R.string.help_center_title)
    val helpNavTitle = stringResource(id = R.string.help_nav_title)
    val helpNavContent = stringResource(id = R.string.help_nav_content)
    val helpOcrTitle = stringResource(id = R.string.help_ocr_title)
    val helpOcrContent = stringResource(id = R.string.help_ocr_content)
    val helpHapticTitle = stringResource(id = R.string.help_haptic_title)
    val helpHapticContent = stringResource(id = R.string.help_haptic_content)

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = helpCenterTitle, onNavigateUp = { navController.popBackStack() })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(AppSpacing.large)
                .verticalScroll(rememberScrollState())
        ) {
            HelpSection(
                title = helpNavTitle,
                content = helpNavContent
            )

            HelpSection(
                title = helpOcrTitle,
                content = helpOcrContent
            )

            HelpSection(
                title = helpHapticTitle,
                content = helpHapticContent
            )
        }
    }
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = AppSpacing.xLarge)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = AppSpacing.small)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
