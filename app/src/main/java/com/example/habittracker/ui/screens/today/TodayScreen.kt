package com.example.habittracker.ui.screens.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.habittracker.data.local.dao.*
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.data.repository.PrayerUpdateResult
import com.example.habittracker.ui.components.*
import com.example.habittracker.ui.theme.AppSpacing
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    habits: List<HabitWithStatus>,
    prayerRecords: List<PrayerRecordDetails>,
    reasons: List<PrayerReason>,
    date: LocalDate,
    onToggle: (HabitWithStatus) -> Unit,
    onRecordPrayer: (Prayer, PrayerStatus, Boolean, Long?, (PrayerUpdateResult) -> Unit) -> Unit,
    onClearPrayer: (Prayer) -> Unit,
    onAddReason: (String, (Result<Long>) -> Unit) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val timeState = rememberTimeOfDayVisualState()
    val prayerItems = habits.filter { it.category == HabitCategory.SALAT }.mapNotNull { habit ->
        Prayer.fromHabitName(habit.name)?.let { prayer -> PrayerJourneyItem(habit, prayer, prayerRecords.firstOrNull { it.prayer == prayer }) }
    }
    val goodDeeds = habits.filter { it.category == HabitCategory.GOOD_DEED }
    val personalHabits = habits.filter { it.category == HabitCategory.PERSONAL }
    val prayerCount = prayerItems.count { it.countsAsCompleted }
    val goodDeedCount = goodDeeds.count { it.completed }
    val primaryCompleted = prayerCount + personalHabits.count { it.completed }
    val primaryTotal = prayerItems.size + personalHabits.size

    var feedback by remember { mutableStateOf<PrayerFeedback?>(null) }
    var celebratingJamaah by remember { mutableStateOf<Prayer?>(null) }
    var selectedPrayer by remember { mutableStateOf<PrayerJourneyItem?>(null) }
    var reasonRequest by remember { mutableStateOf<Pair<PrayerJourneyItem, PrayerStatus>?>(null) }
    var showFullPrayerCompletion by remember { mutableStateOf(false) }
    var prayerStateInitialized by remember { mutableStateOf(false) }
    var previousPrayerCount by remember { mutableIntStateOf(0) }
    var celebratingGoodDeedId by remember { mutableStateOf<Long?>(null) }
    var showOptionalCompletion by remember { mutableStateOf(false) }
    var goodDeedStateInitialized by remember { mutableStateOf(false) }
    var previousGoodDeedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(prayerItems.size, prayerCount) {
        if (prayerItems.size == 5) {
            if (!prayerStateInitialized) { previousPrayerCount = prayerCount; prayerStateInitialized = true }
            else { if (previousPrayerCount < 5 && prayerCount == 5) showFullPrayerCompletion = true; previousPrayerCount = prayerCount }
        }
    }
    LaunchedEffect(feedback) { if (feedback != null) { delay(2_300); feedback = null } }
    LaunchedEffect(celebratingJamaah) { if (celebratingJamaah != null) { delay(1_250); celebratingJamaah = null } }
    LaunchedEffect(celebratingGoodDeedId) { if (celebratingGoodDeedId != null) { delay(1_050); celebratingGoodDeedId = null } }
    LaunchedEffect(goodDeeds.size, goodDeedCount) {
        if (goodDeeds.isNotEmpty()) {
            if (!goodDeedStateInitialized) { previousGoodDeedCount = goodDeedCount; goodDeedStateInitialized = true }
            else {
                val justCompletedAll = previousGoodDeedCount < goodDeeds.size && goodDeedCount == goodDeeds.size
                previousGoodDeedCount = goodDeedCount
                if (justCompletedAll) { showOptionalCompletion = true; delay(2_600); showOptionalCompletion = false }
                else if (goodDeedCount < goodDeeds.size) showOptionalCompletion = false
            }
        }
    }

    fun savePrayer(item: PrayerJourneyItem, status: PrayerStatus, inJamaah: Boolean, reasonId: Long?) {
        val newlyInJamaah = inJamaah && !item.inJamaah
        haptics.performHapticFeedback(if (newlyInJamaah) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        onRecordPrayer(item.prayer, status, inJamaah, reasonId) { result ->
            if (newlyInJamaah) celebratingJamaah = item.prayer
            val message = when { inJamaah -> "Prayed in Jama'ah"; status == PrayerStatus.COMPLETED -> "Prayer recorded as completed"; status == PrayerStatus.QAZA -> "Prayer recorded as Qaza"; else -> "Prayer recorded as missed" }
            feedback = PrayerFeedback(item.habit.name, message, result.xpGranted)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.large).padding(top = AppSpacing.xLarge, bottom = AppSpacing.xxLarge)) {
        TodayHero(date, primaryCompleted, primaryTotal, timeState)
        if (prayerItems.isNotEmpty()) {
            Spacer(Modifier.height(AppSpacing.xLarge))
            PrayerJourney(prayerItems, timeState, celebratingJamaah) { selectedPrayer = it }
            PrayerConfirmation(feedback, Modifier.padding(top = AppSpacing.medium))
        }
        if (goodDeeds.isNotEmpty()) {
            SectionHeader("Good deeds", "Optional")
            goodDeeds.forEachIndexed { index, habit ->
                GoodDeedRow(habit, celebratingGoodDeedId == habit.id) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!habit.completed) celebratingGoodDeedId = habit.id; onToggle(habit) }
                if (index < goodDeeds.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            }
            OptionalSectionCompletion(showOptionalCompletion)
        }
        if (personalHabits.isNotEmpty()) {
            SectionHeader("Personal", "${personalHabits.count { it.completed }} / ${personalHabits.size}")
            personalHabits.forEachIndexed { index, habit -> HabitRow(habit) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggle(habit) }; if (index < personalHabits.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)) }
        }
    }

    selectedPrayer?.let { item ->
        PrayerStatusSheet(item, onSelect = { status, jamaah ->
            selectedPrayer = null
            if (status == PrayerStatus.QAZA || status == PrayerStatus.MISSED) reasonRequest = item to status else savePrayer(item, status, jamaah, null)
        }, onClear = { selectedPrayer = null; onClearPrayer(item.prayer); feedback = PrayerFeedback(item.habit.name, "Record cleared") }, onDismiss = { selectedPrayer = null })
    }
    reasonRequest?.let { (item, status) ->
        PrayerReasonSheet(status, reasons, item.record?.reasonId, onSave = { reasonId -> reasonRequest = null; savePrayer(item, status, false, reasonId) }, onAddCustom = onAddReason, onDismiss = { reasonRequest = null })
    }
    if (showFullPrayerCompletion) FullPrayerCompletion(prayerItems.any { it.status == PrayerStatus.QAZA }, timeState) { showFullPrayerCompletion = false }
}
