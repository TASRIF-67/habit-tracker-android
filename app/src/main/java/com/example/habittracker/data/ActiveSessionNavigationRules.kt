package com.example.habittracker.data

sealed interface ActiveSessionNavigationDecision {
    data object Wait : ActiveSessionNavigationDecision
    data object OpenSession : ActiveSessionNavigationDecision
    data object FallBackToToday : ActiveSessionNavigationDecision
    data object Ignore : ActiveSessionNavigationDecision
}

object ActiveSessionNavigationRules {
    fun notificationRequest(request: Int, handled: Int, loaded: Boolean, hasSession: Boolean): ActiveSessionNavigationDecision = when {
        request <= handled -> ActiveSessionNavigationDecision.Ignore
        !loaded -> ActiveSessionNavigationDecision.Wait
        hasSession -> ActiveSessionNavigationDecision.OpenSession
        else -> ActiveSessionNavigationDecision.FallBackToToday
    }
}
