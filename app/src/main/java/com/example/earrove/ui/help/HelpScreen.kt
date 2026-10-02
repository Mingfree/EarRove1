package com.example.earrove.ui.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.earrove.R
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.AppSize
import com.example.earrove.ui.theme.AppSpacing
import com.example.earrove.ui.theme.DeepGray
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureWhite

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.large)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            HelpCard(
                icon = Icons.Filled.Navigation,
                title = helpNavTitle,
                content = helpNavContent
            )
            HelpCard(
                icon = Icons.Filled.Description,
                title = helpOcrTitle,
                content = helpOcrContent
            )
            HelpCard(
                icon = Icons.Filled.Vibration,
                title = helpHapticTitle,
                content = helpHapticContent
            )
        }
    }
}

@Composable
private fun HelpCard(icon: ImageVector, title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepGray),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.large)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(AppSize.iconMedium)
                )
                Spacer(modifier = Modifier.width(AppSpacing.small))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PremiumGold
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = PureWhite.copy(alpha = 0.9f)
            )
        }
    }
}
