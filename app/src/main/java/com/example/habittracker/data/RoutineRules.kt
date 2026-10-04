package com.example.habittracker.data

import com.example.habittracker.data.local.entity.RoutineType

data class RoutineDraft(
    val name: String,
    val type: RoutineType,
    val target: Int? = null,
    val unit: String? = null,
    val iconKey: String = RoutineIdentity.DEFAULT_ICON,
    val themeKey: String = RoutineIdentity.DEFAULT_THEME,
    val quantityPerCount: Double? = null,
    val measurementUnit: String? = null,
    val schedule: ScheduleDraft? = null,
)

object RoutineIdentity {
    const val DEFAULT_ICON = "CHECK"
    const val DEFAULT_THEME = "FOREST"
    val iconKeys = setOf("WALK", "WATER", "BOOK", "STUDY", "EXERCISE", "HEART", "MEDITATION", "MEDICATION", "JOURNAL", "SUN", "MOON", "CHECK", "STAR")
    val themeKeys = setOf("FOREST", "SAGE", "OCEAN", "SKY", "INDIGO", "AMBER", "EARTH", "ROSE", "SLATE")
    fun iconOrDefault(key: String?) = key?.takeIf(iconKeys::contains) ?: DEFAULT_ICON
    fun themeOrDefault(key: String?) = key?.takeIf(themeKeys::contains) ?: DEFAULT_THEME
}
data class RoutineProgressUpdate(val previous: Int, val current: Int, val crossedTarget: Boolean)

object RoutineRules {
    const val MAX_NAME_LENGTH = 40
    const val MAX_UNIT_LENGTH = 20
    const val MAX_DURATION_MINUTES = 1_440
    const val MAX_COUNT_TARGET = 1_000_000
    const val MAX_QUANTITY_PER_COUNT = 1_000_000.0
    val measurementUnits = setOf("mL", "L", "g", "kg")

    fun isComplete(type: RoutineType, value: Int, target: Int?, checked: Boolean = false): Boolean = when (type) {
        RoutineType.CHECK -> checked
        RoutineType.DURATION, RoutineType.COUNT -> target != null && target > 0 && value >= target
    }

    fun normalized(draft: RoutineDraft): Result<RoutineDraft> {
        val name = draft.name.trim().replace(Regex("\\s+"), " ")
        val unit = draft.unit?.trim()?.replace(Regex("\\s+"), " ")
        val measurementUnit = draft.measurementUnit?.trim()?.takeIf { it.isNotEmpty() }
        val hasQuantity = draft.quantityPerCount != null
        val hasMeasurementUnit = measurementUnit != null
        val error = when {
            name.isBlank() -> "Name is required"
            name.length > MAX_NAME_LENGTH -> "Name is too long"
            draft.type == RoutineType.DURATION && (draft.target == null || draft.target !in 1..MAX_DURATION_MINUTES) -> "Duration must be between 1 and $MAX_DURATION_MINUTES minutes"
            draft.type == RoutineType.COUNT && (draft.target == null || draft.target !in 1..MAX_COUNT_TARGET) -> "Count must be between 1 and $MAX_COUNT_TARGET"
            draft.type == RoutineType.COUNT && unit.isNullOrBlank() -> "Unit is required"
            unit != null && unit.length > MAX_UNIT_LENGTH -> "Unit is too long"
            draft.iconKey !in RoutineIdentity.iconKeys -> "Choose a valid icon"
            draft.themeKey !in RoutineIdentity.themeKeys -> "Choose a valid theme"
            draft.type != RoutineType.COUNT && (hasQuantity || hasMeasurementUnit) -> "Secondary measurement is only available for count routines"
            hasQuantity != hasMeasurementUnit -> "Enter both quantity and measurement unit"
            hasQuantity && (draft.quantityPerCount?.isFinite() != true || draft.quantityPerCount <= 0.0 || draft.quantityPerCount > MAX_QUANTITY_PER_COUNT) -> "Quantity per count must be greater than zero"
            hasMeasurementUnit && measurementUnit !in measurementUnits -> "Choose a valid measurement unit"
            draft.schedule != null && !ScheduleRules.isValid(draft.schedule) -> "Choose at least one day and a valid schedule"
            else -> null
        }
        return if (error != null) Result.failure(IllegalArgumentException(error)) else Result.success(
            RoutineDraft(
                name, draft.type,
                if (draft.type == RoutineType.CHECK) null else draft.target,
                when (draft.type) { RoutineType.DURATION -> "min"; RoutineType.COUNT -> unit; RoutineType.CHECK -> null },
                draft.iconKey, draft.themeKey,
                draft.quantityPerCount.takeIf { draft.type == RoutineType.COUNT },
                measurementUnit.takeIf { draft.type == RoutineType.COUNT },
                draft.schedule,
            ),
        )
    }

    fun derivedMeasurement(value: Int, target: Int, quantityPerCount: Double?, measurementUnit: String?): String? {
        if (quantityPerCount == null || measurementUnit == null || !quantityPerCount.isFinite() || quantityPerCount <= 0.0) return null
        return "${formatMeasurement(value * quantityPerCount, measurementUnit)} / ${formatMeasurement(target * quantityPerCount, measurementUnit)}"
    }

    fun formatMeasurement(value: Double, unit: String): String {
        val (displayValue, displayUnit) = when {
            unit == "mL" && value >= 1000.0 -> value / 1000.0 to "L"
            unit == "g" && value >= 1000.0 -> value / 1000.0 to "kg"
            else -> value to unit
        }
        val number = java.math.BigDecimal.valueOf(displayValue).stripTrailingZeros().toPlainString()
        return "$number $displayUnit"
    }
}
