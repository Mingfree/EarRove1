package com.example.earrove.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// As per the UI Interaction Design document:
// Font: Roboto or system default, Bold.
// Size: Min text 24sp, Title 32sp.
val EarRoveTypography = Typography(
    // For titles
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    // For body text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // Ensure other default styles also meet the minimum size requirement.
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
)

val EarRoveVisualAssistTypography = Typography(
    headlineLarge = EarRoveTypography.headlineLarge.copy(
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = EarRoveTypography.headlineMedium.copy(
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    bodyLarge = EarRoveTypography.bodyLarge.copy(
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    bodyMedium = EarRoveTypography.bodyMedium.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    labelLarge = EarRoveTypography.labelLarge.copy(
        fontSize = 26.sp,
        lineHeight = 34.sp
    )
)
