package md.kursograf.lab4.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KursografColors = lightColorScheme(
    primary = Color(0xFF00695B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F4EB),
    onPrimaryContainer = Color(0xFF0D473E),
    secondary = Color(0xFF49635C),
    secondaryContainer = Color(0xFFDCECE6),
    onSecondaryContainer = Color(0xFF163B32),
    background = Color(0xFFF0F4F5),
    onBackground = Color(0xFF162B32),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF162B32),
    surfaceVariant = Color(0xFFE4EBED),
    onSurfaceVariant = Color(0xFF465C62),
    outline = Color(0xFF71878C),
    outlineVariant = Color(0xFFD4DFE2),
    error = Color(0xFF9C3B32),
)

@Composable
internal fun KursografTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KursografColors, content = content)
}
