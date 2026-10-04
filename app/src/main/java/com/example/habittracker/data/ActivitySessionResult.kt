package com.example.habittracker.data

import com.example.habittracker.data.local.entity.ActivitySession

sealed interface StartSessionResult { data class Started(val session: ActivitySession) : StartSessionResult; data class AlreadyActive(val session: ActivitySession) : StartSessionResult; data class Rejected(val message: String) : StartSessionResult }
