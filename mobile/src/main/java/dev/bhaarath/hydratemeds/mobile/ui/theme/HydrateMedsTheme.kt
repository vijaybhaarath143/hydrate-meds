package dev.bhaarath.hydratemeds.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.bhaarath.hydratemeds.shared.R as SharedR

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F2E3),
    onPrimaryContainer = Color(0xFF062F34),
    secondary = Color(0xFF54636B),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7EEEF),
    background = Color(0xFFF3F7F6),
    errorContainer = Color(0xFFFFDAD0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF75D7C7),
    onPrimary = Color(0xFF003B34),
    primaryContainer = Color(0xFF0B514B),
    onPrimaryContainer = Color(0xFFB9F3E8),
    secondary = Color(0xFFB8C8CE),
    surface = Color(0xFF102124),
    surfaceVariant = Color(0xFF263538),
    background = Color(0xFF071517),
    errorContainer = Color(0xFF733527),
)

private val InterFontFamily = FontFamily(
    Font(SharedR.font.inter_variable, weight = FontWeight.Light),
    Font(SharedR.font.inter_variable, weight = FontWeight.Normal),
    Font(SharedR.font.inter_variable, weight = FontWeight.Medium),
    Font(SharedR.font.inter_variable, weight = FontWeight.SemiBold),
    Font(SharedR.font.inter_variable, weight = FontWeight.Bold),
    Font(SharedR.font.inter_variable, weight = FontWeight.ExtraBold),
)

private val BaseTypography = Typography()

private val InterTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
)

@Composable
fun HydrateMedsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = InterTypography,
        content = content,
    )
}
