package com.example.habittracker.data

object LocationLabelRules {
    fun label(locality: String?, subAdminArea: String?, adminArea: String?, country: String?): String {
        val place = listOf(locality, subAdminArea, adminArea).firstOrNull { !it.isNullOrBlank() }
        return listOfNotNull(place?.trim(), country?.trim()?.takeIf { it.isNotBlank() && !it.equals(place, true) })
            .distinct()
            .joinToString(", ")
            .ifBlank { "Current location" }
    }
}
