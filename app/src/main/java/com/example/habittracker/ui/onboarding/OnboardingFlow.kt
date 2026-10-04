package com.example.habittracker.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.preferences.OnboardingState
import com.example.habittracker.preferences.OnboardingStep
import com.example.habittracker.ui.components.RoutineEditorDialog
import com.example.habittracker.ui.screens.settings.PrayerTimeSettingsScreen
import com.example.habittracker.ui.screens.settings.SleepSettingsScreen

@Composable
fun OnboardingFlow(
    state: OnboardingState, habits: List<Habit>, prayerSettings: PrayerTimeSettings?, prayerReminders: List<PrayerReminderConfig>, sleepPlan: SleepPlan?,
    onNext: () -> Unit, onBack: () -> Unit, onComplete: () -> Unit,
    onAddRoutine: (RoutineDraft, (Result<Unit>) -> Unit) -> Unit,
    onSavePrayer: (PrayerTimeSettings, (Result<Unit>) -> Unit) -> Unit,
    onSavePrayerReminder: (PrayerReminderConfig) -> Unit,
    onSaveSleep: (SleepPlan, (Result<Unit>) -> Unit) -> Unit,
) {
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { if (editor != null) editor = null else onBack() }
    if (editor == "prayer" || editor == "prayer_location") {
        PrayerTimeSettingsScreen(prayerSettings, prayerReminders, { editor = null }, onSavePrayer, requestLocationOnStart = editor == "prayer_location", onReminder = onSavePrayerReminder)
        return
    }
    if (editor == "sleep") {
        SleepSettingsScreen(sleepPlan, { editor = null }, onSaveSleep)
        return
    }
    when (state.step) {
        OnboardingStep.WELCOME -> WelcomeStep(onNext)
        OnboardingStep.PRAYER -> PrayerStep(prayerSettings, { editor = "prayer_location" }, { editor = "prayer" }, onNext, onBack)
        OnboardingStep.ROUTINE -> RoutineStep(habits.any { it.active && it.category == HabitCategory.PERSONAL }, onAddRoutine, onNext, onBack)
        OnboardingStep.NOTIFICATIONS -> NotificationStep(onNext, onBack)
        OnboardingStep.SLEEP -> SleepStep(sleepPlan, { editor = "sleep" }, onNext, onBack)
        OnboardingStep.FINISH -> FinishStep(onComplete, onBack)
        OnboardingStep.COMPLETE -> Unit
    }
}

@Composable
private fun StepPage(step: Int, title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) IconButton(onBack, Modifier.align(Alignment.CenterStart)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$step of 6", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 8.dp).semantics { contentDescription = "Onboarding step $step of 6" }, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(6) { index -> Box(Modifier.size(if (index < step) 8.dp else 6.dp).background(if (index < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
                }
            }
        }
        Icon(icon, null, Modifier.padding(top = 30.dp).size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, Modifier.padding(top = 22.dp).semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Text(body, Modifier.padding(top = 12.dp).widthIn(max = 420.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp)); content(); Spacer(Modifier.height(20.dp))
    }
}

@Composable private fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text(label) }
}
@Composable private fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, label: String) {
    OutlinedButton(onClick, modifier.heightIn(min = 52.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text(label) }
}
@Composable private fun TertiaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, label: String) {
    TextButton(onClick, modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text(label) }
}

@Composable private fun WelcomeStep(next: () -> Unit) = StepPage(1, "HabitTracker", "Build a calmer daily rhythm with routines, prayer, activity and sleep.", Icons.Outlined.Spa) {
    Text("Private by design. Your tracking data stays on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    PrimaryButton(next, Modifier.fillMaxWidth().padding(top = 28.dp), label = "Get started")
}

@Composable private fun PrayerStep(settings: PrayerTimeSettings?, location: () -> Unit, manual: () -> Unit, skip: () -> Unit, back: () -> Unit) = StepPage(2, "Prayer times", "Calculate prayer times for your location. Your location stays on this device.", Icons.Outlined.Mosque, back) {
    if (settings != null) Text("Configured for ${settings.locationLabel}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    PrimaryButton(if (settings == null) location else skip, Modifier.fillMaxWidth().padding(top = 18.dp), label = if (settings == null) "Use current location" else "Continue")
    SecondaryButton(manual, Modifier.fillMaxWidth().padding(top = 10.dp), label = if (settings == null) "Set up manually" else "Review prayer settings")
    if (settings == null) TertiaryButton(skip, Modifier.padding(top = 6.dp), label = "Skip")
}

@Composable private fun RoutineStep(hasRoutine: Boolean, add: (RoutineDraft, (Result<Unit>) -> Unit) -> Unit, next: () -> Unit, back: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }; var custom by rememberSaveable { mutableStateOf(false) }; var saving by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val suggestions = listOf(
        RoutineDraft("Walking", RoutineType.DURATION, 30, "min", "WALK", "FOREST"), RoutineDraft("Water", RoutineType.COUNT, 4, "bottles", "WATER", "OCEAN"),
        RoutineDraft("Reading", RoutineType.CHECK, iconKey = "BOOK", themeKey = "SAGE"), RoutineDraft("Study", RoutineType.DURATION, 60, "min", "STUDY", "INDIGO"), RoutineDraft("Exercise", RoutineType.DURATION, 30, "min", "EXERCISE", "AMBER"),
    )
    StepPage(3, "Create your first routine", if (hasRoutine) "You already have an active routine. You can continue or add another." else "What would you like to make part of your day?", Icons.Outlined.Checklist, back) {
        if (!hasRoutine) suggestions.forEach { draft -> FilterChip(selected == draft.name, { selected = draft.name; error = null }, { Text(draft.name) }, modifier = Modifier.padding(horizontal = 3.dp)) }
        selected?.let { name ->
            val draft = suggestions.first { it.name == name }
            Text(when (draft.type) { RoutineType.CHECK -> "Done / not done · Anytime"; RoutineType.DURATION -> "Track time · ${draft.target} min · Anytime"; RoutineType.COUNT -> "Track quantity · ${draft.target} ${draft.unit} · Anytime" }, Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            PrimaryButton({ saving = true; add(draft) { result -> saving = false; result.onSuccess { next() }.onFailure { error = it.message } } }, Modifier.fillMaxWidth().padding(top = 14.dp), enabled = !saving, label = "Create ${draft.name}")
        }
        SecondaryButton({ custom = true }, Modifier.fillMaxWidth().padding(top = 12.dp), label = "Create a custom routine")
        TertiaryButton(next, Modifier.padding(top = 6.dp), label = if (hasRoutine) "Continue" else "Skip")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
    }
    if (custom) RoutineEditorDialog(onDismiss = { custom = false }) { draft, report -> add(draft) { result -> result.onSuccess { custom = false; next() }.onFailure { report(it.message ?: "Couldn't create routine") } } }
}

@Composable private fun NotificationStep(next: () -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; requested = true }
    StepPage(4, "Stay on track", "HabitTracker can remind you about routines, prayer times and bedtime. You control which reminders are enabled.", Icons.Outlined.Notifications, back) {
        if (granted) { Text("Notifications are allowed", color = MaterialTheme.colorScheme.primary); PrimaryButton(next, Modifier.fillMaxWidth().padding(top = 18.dp), label = "Continue") }
        else { PrimaryButton({ if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else next() }, Modifier.fillMaxWidth(), label = "Allow notifications"); if (requested) Text("Notification permission was not granted. You can enable it later in Android Settings.", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center); TertiaryButton(next, Modifier.padding(top = 6.dp), label = "Not now") }
    }
}

@Composable private fun SleepStep(plan: SleepPlan?, setup: () -> Unit, next: () -> Unit, back: () -> Unit) = StepPage(5, "Plan your night", "Set a usual bedtime and wake time to make Tonight useful.", Icons.Outlined.Bedtime, back) {
    if (plan != null) Text("Sleep schedule configured", color = MaterialTheme.colorScheme.primary)
    PrimaryButton(if (plan == null) setup else next, Modifier.fillMaxWidth().padding(top = 18.dp), label = if (plan == null) "Set up sleep" else "Continue")
    if (plan != null) SecondaryButton(setup, Modifier.fillMaxWidth().padding(top = 10.dp), label = "Review sleep schedule")
    if (plan == null) TertiaryButton(next, Modifier.padding(top = 6.dp), label = "Skip")
}

@Composable private fun FinishStep(complete: () -> Unit, back: () -> Unit) = StepPage(6, "You're ready.", "Your day is set up. You can change everything later in Settings.", Icons.Outlined.CheckCircle, back) {
    PrimaryButton(complete, Modifier.fillMaxWidth().padding(top = 18.dp), label = "Go to Today")
}
