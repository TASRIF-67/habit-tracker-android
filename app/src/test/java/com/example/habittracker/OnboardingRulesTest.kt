package com.example.habittracker

import com.example.habittracker.preferences.*
import org.junit.Assert.*
import org.junit.Test

class OnboardingRulesTest {
    @Test fun `fresh installation enters welcome`() {
        assertEquals(OnboardingStep.WELCOME, OnboardingRules.initialStep(false, true, null))
    }

    @Test fun `existing user upgrade bypasses onboarding conservatively`() {
        assertEquals(OnboardingStep.COMPLETE, OnboardingRules.initialStep(false, false, null))
    }

    @Test fun `completed onboarding remains complete`() {
        assertEquals(OnboardingStep.COMPLETE, OnboardingRules.initialStep(true, true, OnboardingStep.COMPLETE))
    }

    @Test fun `interrupted onboarding resumes persisted step`() {
        assertEquals(OnboardingStep.NOTIFICATIONS, OnboardingRules.initialStep(true, false, OnboardingStep.NOTIFICATIONS))
    }

    @Test fun `skip all path can reach completion`() {
        var step = OnboardingStep.WELCOME
        repeat(6) { step = OnboardingRules.next(step) }
        assertEquals(OnboardingStep.COMPLETE, step)
    }

    @Test fun `permission denial cannot block notification step`() {
        assertEquals(OnboardingStep.SLEEP, OnboardingRules.next(OnboardingStep.NOTIFICATIONS))
    }

    @Test fun `back navigation follows onboarding order`() {
        assertEquals(OnboardingStep.PRAYER, OnboardingRules.previous(OnboardingStep.ROUTINE))
        assertEquals(OnboardingStep.WELCOME, OnboardingRules.previous(OnboardingStep.PRAYER))
    }
}
