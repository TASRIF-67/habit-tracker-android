package com.example.habittracker.ui.screens.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.habittracker.data.local.dao.*
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.data.repository.PrayerUpdateResult
import com.example.habittracker.data.RoutineProgressUpdate
import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.data.StartSessionResult
import com.example.habittracker.data.StartSleepResult
import com.example.habittracker.data.FinishSleepResult
import com.example.habittracker.data.PrayerTimeRules
import com.example.habittracker.ui.components.*
import com.example.habittracker.ui.theme.AppSpacing
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    habits: List<HabitWithStatus>,
    schedules: List<RoutineSchedule>,
    activeSession: ActivitySession?,
    sleepPlan: SleepPlan?,
    activeSleep: SleepSession?,
    latestSleep: SleepSession?,
    prayerRecords: List<PrayerRecordDetails>,
    reasons: List<PrayerReason>,
    prayerTimeSettings: PrayerTimeSettings?,
    date: LocalDate,
    onToggle: (HabitWithStatus) -> Unit,
    onRecordPrayer: (Prayer, PrayerStatus, Boolean, Long?, (PrayerUpdateResult) -> Unit) -> Unit,
    onClearPrayer: (Prayer) -> Unit,
    onAddReason: (String, (Result<Long>) -> Unit) -> Unit,
    onAddRoutineProgress: (HabitWithStatus, Int, (RoutineProgressUpdate) -> Unit) -> Unit,
    onStartSession: (Long, (StartSessionResult) -> Unit) -> Unit,
    onOpenActiveSession: () -> Unit,
    onStartSleep: ((StartSleepResult) -> Unit) -> Unit,
    onFinishSleep: (Boolean, (FinishSleepResult) -> Unit) -> Unit,
    onOpenSleepSettings: () -> Unit,
    onOpenPrayerTimeSettings: () -> Unit,
    onResumeSleepPlan: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val timeState = rememberTimeOfDayVisualState()
    val prayerItems = habits.filter { it.category == HabitCategory.SALAT }.mapNotNull { habit ->
        Prayer.fromHabitName(habit.name)?.let { prayer -> PrayerJourneyItem(habit, prayer, prayerRecords.firstOrNull { it.prayer == prayer }) }
    }
    val goodDeeds = habits.filter { it.category == HabitCategory.GOOD_DEED }
    val personalHabits = habits.filter { it.category == HabitCategory.PERSONAL && it.active }
    val scheduleByHabit = schedules.associateBy { it.habitId }
    val orderedPersonalHabits = personalHabits.sortedWith(compareBy<HabitWithStatus> {
        val schedule = scheduleByHabit[it.id]
        when { schedule?.enabled != true || !ScheduleRules.includes(schedule.daysMask, date.dayOfWeek) -> 2; !it.completed -> 0; else -> 1 }
    }.thenBy { scheduleByHabit[it.id]?.timeMinutes ?: Int.MAX_VALUE })
    var now by remember { mutableStateOf(java.time.ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = java.time.ZonedDateTime.now(); delay(30_000) } }
    val upNext = ScheduleRules.upNext(personalHabits.filter { it.id != activeSession?.habitId }, schedules, now)
    val currentBlock = ScheduleRules.activeNow(personalHabits.filter { it.id != activeSession?.habitId }, schedules, now).firstOrNull()
    val activeHabit = activeSession?.let { session -> personalHabits.firstOrNull { it.id == session.habitId } }
    val prayerCount = prayerItems.count { it.countsAsCompleted }
    val goodDeedCount = goodDeeds.count { it.completed }
    val primaryCompleted = prayerCount + personalHabits.count { it.completed }
    val primaryTotal = prayerItems.size + personalHabits.size
    val calculatedPrayerTimes = remember(prayerTimeSettings, date) {
        prayerTimeSettings?.let { settings -> runCatching { PrayerTimeRules.calculate(settings, date, java.time.ZoneId.systemDefault()) }.getOrNull() }
    }

    var feedback by remember { mutableStateOf<PrayerFeedback?>(null) }
    var celebratingJamaah by remember { mutableStateOf<Prayer?>(null) }
    var selectedPrayer by remember { mutableStateOf<PrayerJourneyItem?>(null) }
    var reasonRequest by remember { mutableStateOf<Pair<PrayerJourneyItem, PrayerStatus>?>(null) }
    var showFullPrayerCompletion by remember { mutableStateOf(false) }
    var prayerStateInitialized by remember { mutableStateOf(false) }
    var previousPrayerCount by remember { mutableIntStateOf(0) }
    var sleepMessage by remember { mutableStateOf<String?>(null) }
    var confirmLongSleep by remember { mutableStateOf(false) }
    var celebratingGoodDeedId by remember { mutableStateOf<Long?>(null) }
    var showOptionalCompletion by remember { mutableStateOf(false) }
    var loggingDuration by remember { mutableStateOf<HabitWithStatus?>(null) }
    var celebratingRoutineId by remember { mutableStateOf<Long?>(null) }
    var goodDeedStateInitialized by remember { mutableStateOf(false) }
    var previousGoodDeedCount by remember { mutableIntStateOf(0) }
    var celebratingDailyProgress by remember { mutableStateOf(false) }
    var conflictingSession by remember { mutableStateOf<ActivitySession?>(null) }

    fun start(habit: HabitWithStatus) { onStartSession(habit.id) { result -> when {
        result is StartSessionResult.Started -> onOpenActiveSession()
        result is StartSessionResult.AlreadyActive && result.session.habitId == habit.id -> onOpenActiveSession()
        result is StartSessionResult.AlreadyActive -> conflictingSession = result.session
    } } }

    LaunchedEffect(prayerItems.size, prayerCount) {
        if (prayerItems.size == 5) {
            if (!prayerStateInitialized) { previousPrayerCount = prayerCount; prayerStateInitialized = true }
            else { if (previousPrayerCount < 5 && prayerCount == 5) showFullPrayerCompletion = true; previousPrayerCount = prayerCount }
        }
    }
    LaunchedEffect(feedback) { if (feedback != null) { delay(2_300); feedback = null } }
    LaunchedEffect(celebratingJamaah) { if (celebratingJamaah != null) { delay(1_250); celebratingJamaah = null } }
    LaunchedEffect(celebratingGoodDeedId) { if (celebratingGoodDeedId != null) { delay(1_050); celebratingGoodDeedId = null } }
    LaunchedEffect(celebratingRoutineId) { if (celebratingRoutineId != null) { delay(900); celebratingRoutineId = null } }
    LaunchedEffect(celebratingDailyProgress) { if (celebratingDailyProgress) { delay(1_150); celebratingDailyProgress = false } }
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
            if (!item.countsAsCompleted && status.countsAsCompleted && primaryTotal > 0 && primaryCompleted + 1 == primaryTotal) { celebratingDailyProgress = true; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            val message = when { inJamaah -> "Prayed in Jama'ah"; status == PrayerStatus.COMPLETED -> "Prayer recorded as completed"; status == PrayerStatus.QAZA -> "Prayer recorded as Qaza"; else -> "Prayer recorded as missed" }
            feedback = PrayerFeedback(item.habit.name, message, result.xpGranted)
        }
    }

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = AppSpacing.xxLarge)) {
            TimeOfDayHero(date, timeState)
            Column(Modifier.padding(horizontal = AppSpacing.large)) {
            DailyProgressCard(primaryCompleted, primaryTotal, celebratingDailyProgress, Modifier.offset(y = (-12).dp))
            if (prayerItems.isNotEmpty()) {
                if (prayerTimeSettings == null) PrayerTimesSetupCard(onOpenPrayerTimeSettings)
                else NextPrayerCard(prayerTimeSettings, onOpenPrayerTimeSettings)
            }
            if (activeSession != null && activeHabit != null) ActiveNowSection(activeHabit, activeSession, onOpenActiveSession)
            else currentBlock?.let { block -> ScheduledActiveNowSection(block, { start(block.habit) }, { onToggle(block.habit) }, { onAddRoutineProgress(block.habit, 1) {} }) }
            upNext?.let { UpNextRoutine(it.habit, it.schedule, it.occurrence, now) { start(it.habit) } }
            if (prayerItems.isNotEmpty()) {
                Spacer(Modifier.height(AppSpacing.large))
                PrayerJourney(prayerItems, celebratingJamaah, calculatedPrayerTimes?.associate { it.prayer to it.time }) { selectedPrayer = it }
                PrayerConfirmation(feedback, Modifier.padding(top = AppSpacing.small))
            }
            if (goodDeeds.isNotEmpty()) {
                GoodDeedsSection(goodDeeds, celebratingGoodDeedId) { habit -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!habit.completed) celebratingGoodDeedId = habit.id; onToggle(habit) }
                OptionalSectionCompletion(showOptionalCompletion)
            }
            MyRoutinesSection(
                habits = orderedPersonalHabits,
                celebratingId = celebratingRoutineId,
                onToggleCheck = { habit -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!habit.completed && primaryTotal > 0 && primaryCompleted + 1 == primaryTotal) celebratingDailyProgress = true; onToggle(habit) },
                onLogDuration = { loggingDuration = it },
                onStartDuration = ::start,
                onAdjustCount = { habit, delta -> onAddRoutineProgress(habit, delta) { if (it.crossedTarget) { celebratingRoutineId = habit.id; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (primaryTotal > 0 && primaryCompleted + 1 == primaryTotal) celebratingDailyProgress = true } } },
            )
            TonightSection(
                plan = sleepPlan,
                activeSession = activeSleep,
                latestCompleted = latestSleep,
                onGoingToBed = { onStartSleep { result -> when (result) { is StartSleepResult.Rejected -> sleepMessage = result.reason; is StartSleepResult.AlreadySleeping -> Unit; is StartSleepResult.Started -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } } },
                onWake = { onFinishSleep(false) { result -> if (result.needsLongConfirmation) confirmLongSleep = true else haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } },
                onOpenSettings = onOpenSleepSettings,
                onResumePlan = onResumeSleepPlan,
            )
            }
        }
        val statusScrimAlpha by animateFloatAsState(if (scrollState.value > 120) .96f else 0f, label = "status bar scroll scrim")
        Box(
            Modifier.fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Brush.verticalGradient(listOf(Color(0xFF071A17).copy(alpha = statusScrimAlpha), Color.Transparent)))
        )
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
    loggingDuration?.let { habit -> DurationLogDialog(habit, onDismiss = { loggingDuration = null }) { minutes ->
        onAddRoutineProgress(habit, minutes) { if (it.crossedTarget) { celebratingRoutineId = habit.id; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (primaryTotal > 0 && primaryCompleted + 1 == primaryTotal) celebratingDailyProgress = true } }
        loggingDuration = null
    } }
    conflictingSession?.let { session -> val name = habits.firstOrNull { it.id == session.habitId }?.name ?: "Another routine"; AlertDialog(onDismissRequest = { conflictingSession = null }, title = { Text("$name is currently active") }, text = { Text("Open the active session before starting another routine.") }, confirmButton = { TextButton(onClick = { conflictingSession = null; onOpenActiveSession() }) { Text("Open session") } }, dismissButton = { TextButton(onClick = { conflictingSession = null }) { Text("Cancel") } }) }
    sleepMessage?.let { message -> AlertDialog(onDismissRequest = { sleepMessage = null }, title = { Text("Can't start sleep") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { sleepMessage = null }) { Text("OK") } }) }
    if (confirmLongSleep) AlertDialog(onDismissRequest = { confirmLongSleep = false }, title = { Text("Check your sleep time") }, text = { Text("This sleep session is over 24 hours. Finish it with the recorded times, or cancel and correct it later.") }, confirmButton = { TextButton(onClick = { confirmLongSleep = false; onFinishSleep(true) {} }) { Text("Finish anyway") } }, dismissButton = { TextButton(onClick = { confirmLongSleep = false }) { Text("Cancel") } })
}

@Composable
private fun DurationLogDialog(habit: HabitWithStatus, onDismiss: () -> Unit, onLog: (Int) -> Unit) {
    var input by remember { mutableStateOf("") }
    val minutes = input.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log time for ${habit.name}") },
        text = { Column { OutlinedTextField(input, { input = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes") }, suffix = { Text("min") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Row(Modifier.fillMaxWidth().padding(top = AppSpacing.small), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) { listOf(5, 10, 15).forEach { value -> AssistChip(onClick = { input = value.toString() }, label = { Text("+$value min") }) } } } },
        confirmButton = { TextButton(onClick = { onLog(minutes!!) }, enabled = minutes != null && minutes > 0) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
