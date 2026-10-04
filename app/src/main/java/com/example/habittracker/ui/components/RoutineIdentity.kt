package com.example.habittracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.habittracker.data.RoutineIdentity

data class RoutineIconOption(val key: String, val label: String, val icon: ImageVector)

val routineIconOptions = listOf(
    RoutineIconOption("WALK", "Walking", Icons.AutoMirrored.Outlined.DirectionsWalk),
    RoutineIconOption("WATER", "Water", Icons.Outlined.WaterDrop),
    RoutineIconOption("BOOK", "Reading", Icons.AutoMirrored.Outlined.MenuBook),
    RoutineIconOption("STUDY", "Study", Icons.Outlined.School),
    RoutineIconOption("EXERCISE", "Exercise", Icons.Outlined.FitnessCenter),
    RoutineIconOption("HEART", "Wellbeing", Icons.Outlined.FavoriteBorder),
    RoutineIconOption("MEDITATION", "Meditation", Icons.Outlined.SelfImprovement),
    RoutineIconOption("MEDICATION", "Medication", Icons.Outlined.Medication),
    RoutineIconOption("JOURNAL", "Journal", Icons.Outlined.EditNote),
    RoutineIconOption("SUN", "Day", Icons.Outlined.WbSunny),
    RoutineIconOption("MOON", "Night", Icons.Outlined.DarkMode),
    RoutineIconOption("CHECK", "General", Icons.Outlined.CheckCircleOutline),
    RoutineIconOption("STAR", "Favorite", Icons.Outlined.StarBorder),
)

fun routineIcon(key: String?): ImageVector = routineIconOptions.firstOrNull { it.key == RoutineIdentity.iconOrDefault(key) }?.icon
    ?: Icons.Outlined.CheckCircleOutline

fun routineIconLabel(key: String?): String = routineIconOptions.firstOrNull { it.key == RoutineIdentity.iconOrDefault(key) }?.label ?: "General"

data class RoutineThemeOption(val key: String, val label: String)
val routineThemeOptions = listOf("FOREST", "SAGE", "OCEAN", "SKY", "INDIGO", "AMBER", "EARTH", "ROSE", "SLATE").map {
    RoutineThemeOption(it, it.lowercase().replaceFirstChar(Char::titlecase))
}

data class RoutinePalette(val container: Color, val icon: Color, val progress: Color, val border: Color)

@Composable
fun routinePalette(key: String?): RoutinePalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < .3f
    val resolved = RoutineIdentity.themeOrDefault(key)
    val accent = when (resolved) {
        "SAGE" -> if (dark) Color(0xFF9AB89D) else Color(0xFF58775C)
        "OCEAN" -> if (dark) Color(0xFF71B8B6) else Color(0xFF267775)
        "SKY" -> if (dark) Color(0xFF82B5D2) else Color(0xFF397B9E)
        "INDIGO" -> if (dark) Color(0xFFA9A8D8) else Color(0xFF5D5B91)
        "AMBER" -> if (dark) Color(0xFFD3AB65) else Color(0xFF93671E)
        "EARTH" -> if (dark) Color(0xFFC19A7B) else Color(0xFF805C42)
        "ROSE" -> if (dark) Color(0xFFD0A0AA) else Color(0xFF965968)
        "SLATE" -> if (dark) Color(0xFFA5B1AE) else Color(0xFF596966)
        else -> if (dark) Color(0xFF79B794) else Color(0xFF27684F)
    }
    return RoutinePalette(
        container = accent.copy(alpha = if (dark) .16f else .11f),
        icon = accent,
        progress = accent,
        border = accent.copy(alpha = if (dark) .42f else .3f),
    )
}
