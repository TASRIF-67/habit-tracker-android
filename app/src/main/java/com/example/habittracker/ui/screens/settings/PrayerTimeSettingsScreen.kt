package com.example.habittracker.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.location.CurrentLocationProvider
import com.example.habittracker.location.PrayerLocationResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimeSettingsScreen(
    settings: PrayerTimeSettings?,
    reminders: List<PrayerReminderConfig>,
    onBack: () -> Unit,
    onSave: (PrayerTimeSettings, (Result<Unit>) -> Unit) -> Unit,
    requestLocationOnStart: Boolean = false,
    onReminder: (PrayerReminderConfig) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var location by remember(settings) { mutableStateOf(settings?.locationLabel.orEmpty()) }
    var latitude by remember(settings) { mutableStateOf(settings?.latitude?.toString().orEmpty()) }
    var longitude by remember(settings) { mutableStateOf(settings?.longitude?.toString().orEmpty()) }
    var method by remember(settings) { mutableStateOf(settings?.calculationMethod ?: PrayerCalculationMethod.KARACHI) }
    var asr by remember(settings) { mutableStateOf(settings?.asrMethod ?: AsrMethod.HANAFI) }
    var message by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }
    var locationRequested by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val valid = location.isNotBlank() && latitude.toDoubleOrNull()?.let { it in -90.0..90.0 } == true && longitude.toDoubleOrNull()?.let { it in -180.0..180.0 } == true

    fun obtainCurrentLocation() {
        locating = true
        message = null
        scope.launch {
            val result = runCatching { CurrentLocationProvider(context).obtain() }.getOrElse { PrayerLocationResult.Unavailable }
            when (result) {
                is PrayerLocationResult.Success -> {
                    location = result.fix.label
                    latitude = result.fix.latitude.toString()
                    longitude = result.fix.longitude.toString()
                    onSave(PrayerTimeSettings(locationLabel = result.fix.label, latitude = result.fix.latitude, longitude = result.fix.longitude, calculationMethod = method, asrMethod = asr)) { saved ->
                        locating = false
                        message = saved.exceptionOrNull()?.message ?: "Location updated"
                    }
                }
                PrayerLocationResult.ServicesDisabled -> { locating = false; message = "Location services are turned off. Turn them on or enter a location manually." }
                PrayerLocationResult.Unavailable -> { locating = false; message = "Current location wasn't available. Try again or enter a location manually." }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationRequested = true
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) obtainCurrentLocation()
        else message = "Location permission wasn't granted. You can enter a location manually."
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val permanentlyDenied = locationRequested && !hasPermission && context.findActivity()?.let { activity -> !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION) } == true

    fun requestCurrentLocation() {
        if (hasPermission) obtainCurrentLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var handledInitialLocation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(requestLocationOnStart) {
        if (requestLocationOnStart && !handledInitialLocation) { handledInitialLocation = true; requestCurrentLocation() }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Prayer times") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Prayer location", style = MaterialTheme.typography.titleMedium)
            Text(settings?.locationLabel ?: "Not configured", style = MaterialTheme.typography.titleLarge, color = if (settings == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            Text("Used only for offline prayer-time calculations. Your coordinates stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = ::requestCurrentLocation, enabled = !locating, modifier = Modifier.fillMaxWidth()) {
                if (locating) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(if (settings == null) "Use current location" else "Update location")
            }
            if (permanentlyDenied) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app settings") }
            message?.let { Text(it, color = if (it == "Location updated") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }

            TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide advanced location" else "Advanced · Enter coordinates manually") }
            if (advanced) {
                OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), label = { Text("City or location name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(latitude, { latitude = it }, Modifier.weight(1f), label = { Text("Latitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(longitude, { longitude = it }, Modifier.weight(1f), label = { Text("Longitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedButton(onClick = { onSave(PrayerTimeSettings(locationLabel = location.trim(), latitude = latitude.toDouble(), longitude = longitude.toDouble(), calculationMethod = method, asrMethod = asr)) { result -> message = result.exceptionOrNull()?.message ?: "Prayer time settings saved" } }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save manual location") }
            }

            HorizontalDivider(Modifier.padding(top = 8.dp))
            Text("Calculation", style = MaterialTheme.typography.titleMedium)
            ChoiceMenu("Calculation method", method, PrayerCalculationMethod.entries, { method = it }, ::methodLabel)
            ChoiceMenu("Asr method", asr, AsrMethod.entries, { asr = it }) { if (it == AsrMethod.HANAFI) "Hanafi" else "Shafi" }
            Button(onClick = {
                onSave(PrayerTimeSettings(locationLabel = location.trim(), latitude = latitude.toDouble(), longitude = longitude.toDouble(), calculationMethod = method, asrMethod = asr)) { result -> message = result.exceptionOrNull()?.message ?: "Prayer time settings saved" }
            }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save calculation settings") }

            HorizontalDivider(Modifier.padding(top = 8.dp))
            Text("Prayer reminders", style = MaterialTheme.typography.titleMedium)
            Text("Off by default. Reminders use the calculated prayer times and stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Prayer.entries.forEach { prayer ->
                val config = reminders.firstOrNull { it.prayer == prayer } ?: PrayerReminderConfig(prayer)
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(prayer.displayName); Text(if (config.offsetMinutes == 0) "At prayer time" else "${config.offsetMinutes} min before", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    OffsetMenu(config.offsetMinutes) { onReminder(config.copy(offsetMinutes = it)) }
                    Switch(config.enabled, { onReminder(config.copy(enabled = it)) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceMenu(label: String, value: T, values: List<T>, onSelect: (T) -> Unit, text: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(text(value), {}, Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded, { expanded = false }) { values.forEach { item -> DropdownMenuItem({ Text(text(item)) }, { onSelect(item); expanded = false }) } }
    }
}

@Composable
private fun OffsetMenu(value: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { TextButton(onClick = { expanded = true }) { Text(if (value == 0) "At time" else "-${value}m") }; DropdownMenu(expanded, { expanded = false }) { listOf(0, 5, 10, 15).forEach { offset -> DropdownMenuItem({ Text(if (offset == 0) "At prayer time" else "$offset min before") }, { onSelect(offset); expanded = false }) } } }
}

private fun methodLabel(method: PrayerCalculationMethod) = method.name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

private tailrec fun Context.findActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }
