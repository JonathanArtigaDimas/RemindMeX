package com.example.remindme.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.remindme.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

fun getGoogleFontFamily(name: String): FontFamily {
    val fontName = GoogleFont(name)
    return FontFamily(
        Font(googleFont = fontName, fontProvider = provider),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = fontName, fontProvider = provider, style = FontStyle.Italic),
    )
}


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    tertiary = Color(0xFF10B981)
)

private val CoffeeMilkColorScheme = lightColorScheme(
    primary = Color(0xFF8B5E3C),
    onPrimary = Color.White,
    secondary = Color(0xFF8D7663),
    onSecondary = Color.White,
    background = Color(0xFFF5E6D3),
    onBackground = Color(0xFF4A3F35),
    surface = Color(0xFFE1D3C2),
    onSurface = Color(0xFF4A3F35),
    onSurfaceVariant = Color(0xFF8D7663),
    outline = Color(0xFFBCADA1),
    tertiary = Color(0xFF00897B)
)

private val NoirMonoColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color.LightGray,
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF111111),
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = Color(0xFF333333),
    tertiary = Color.White
)

private val CyberNeonColorScheme = darkColorScheme(
    primary = Color(0xFFFE007F), // Neon Pink (Guardar, FAB)
    onPrimary = Color.White,
    secondary = Color(0xFF00E5FF), // Electric Cyan (Cancel, Section Titles)
    onSecondary = Color.Black,
    background = Color(0xFF0D0221), // Deep Dark Purple Background
    onBackground = Color.White,
    surface = Color(0xFF190933), // Dark Violet Surface (Cards, TextFields)
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF00E5FF), // Electric Cyan for body text
    outline = Color(0xFF4D1C9E), // Purple Border
    tertiary = Color(0xFF9D00FF) // Vivid Purple for dates
)

private val AestheticColorScheme = lightColorScheme(
    primary = Color(0xFFE68A56), // Terracotta Vibrant
    onPrimary = Color.White,
    secondary = Color(0xFF954535), // Deep Chestnut
    onSecondary = Color.White,
    background = Color(0xFFFEF9F3), // Clean Warm White
    onBackground = Color(0xFF3E2723), // Deep Dark Espresso (High Contrast)
    surface = Color(0xFFF8E3D0), // Soft Peach/Clay Surface
    onSurface = Color(0xFF3E2723),
    onSurfaceVariant = Color(0xFF954535),
    outline = Color(0xFFDBC1AD), // Muted Sand Outline
    tertiary = Color(0xFFD2691E) // Chocolate Orange
)

private val SakuraDreamColorScheme = lightColorScheme(
    primary = Color(0xFFF2A6B3), // Cherry Blossom Pink
    onPrimary = Color.White,
    secondary = Color(0xFFD18E9C), // Muted Rose
    onSecondary = Color.White,
    background = Color(0xFFFFF0F3), // Ultra Light Pink
    onBackground = Color(0xFF70434E), // Dark Rose Brown
    surface = Color.White, // White Surface as requested
    onSurface = Color(0xFF70434E),
    onSurfaceVariant = Color(0xFFD18E9C),
    outline = Color(0xFFFFDDE1), // Soft Pink Border
    tertiary = Color(0xFFFF8A80), // Soft Coral
    error = Color(0xFFE57373) // Soft Red/Pink for Trash
)

@Composable
fun RemindMeTheme(
    themeName: String = "Default",
    fontName: String = "System",
    fontSizeMultiplier: Float = 1.0f,
    isFontBold: Boolean = false,
    isFontItalic: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Coffee Milk" -> CoffeeMilkColorScheme
        "Noir Mono" -> NoirMonoColorScheme
        "Cyber Neon" -> CyberNeonColorScheme
        "Aesthetic" -> AestheticColorScheme
        "Sakura Dream" -> SakuraDreamColorScheme
        else -> DarkColorScheme
    }

    val fontFamily = when (fontName) {
        "Playfair" -> FontFamily.Serif
        "Space Mono" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val baseWeight = if (isFontBold) FontWeight.Bold else FontWeight.Normal
    val baseStyle = if (isFontItalic) FontStyle.Italic else FontStyle.Normal

    val typography = Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = baseWeight, 
            fontStyle = baseStyle,
            fontSize = (16 * fontSizeMultiplier).sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = FontWeight.Bold, 
            fontStyle = baseStyle,
            fontSize = (22 * fontSizeMultiplier).sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = baseWeight, 
            fontStyle = baseStyle,
            fontSize = (11 * fontSizeMultiplier).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = baseWeight, 
            fontStyle = baseStyle,
            fontSize = (14 * fontSizeMultiplier).sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = FontWeight.Bold, 
            fontStyle = baseStyle,
            fontSize = (18 * fontSizeMultiplier).sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = FontWeight.Bold, 
            fontStyle = baseStyle,
            fontSize = (14 * fontSizeMultiplier).sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily, 
            fontWeight = FontWeight.Bold, 
            fontStyle = baseStyle,
            fontSize = (14 * fontSizeMultiplier).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = baseStyle,
            fontSize = (28 * fontSizeMultiplier).sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = baseWeight,
            fontStyle = baseStyle,
            fontSize = (12 * fontSizeMultiplier).sp
        ),
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = baseStyle,
            fontSize = (60 * fontSizeMultiplier).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = baseStyle,
            fontSize = (24 * fontSizeMultiplier).sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = baseStyle,
            fontSize = (14 * fontSizeMultiplier).sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
