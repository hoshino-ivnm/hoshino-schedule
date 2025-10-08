package com.misaka.hoshinoschedule.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.misaka.hoshinoschedule.R
import com.misaka.hoshinoschedule.data.model.PeriodDefinition
import com.misaka.hoshinoschedule.data.settings.CourseDisplayField
import com.misaka.hoshinoschedule.data.settings.UserPreferences
import com.misaka.hoshinoschedule.util.termStartDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt

private const val MaxReminderLeadMinutes = 180
private const val MaxDndLeadMinutes = 60
private const val MaxDndReleaseMinutes = 60
private const val MaxDndSkipThresholdMinutes = 180
private const val MaxTotalWeeks = 40
private const val MaxTestNotificationDelaySeconds = 60
private const val MaxTestDndDurationMinutes = 120
private const val MaxDeveloperTestDndGapMinutes = 180
private const val MaxDeveloperTestDndSkipMinutes = 240

enum class SettingsPage { Main, Developer, About }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    page: SettingsPage,
    snackbarHostState: SnackbarHostState,
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit,
    onTimetableNameChange: (String) -> Unit,
    onBackgroundColorSelected: (String) -> Unit,
    onBackgroundImageSelect: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onVisibleFieldsChange: (Set<CourseDisplayField>) -> Unit,
    onReminderLeadChange: (Int) -> Unit,
    onDndConfigChange: (Boolean, Int, Int, Int) -> Unit,
    onTermStartDateChange: (LocalDate?) -> Unit,
    onTotalWeeksChange: (Int) -> Unit,
    onShowNonCurrentWeekCoursesChange: (Boolean) -> Unit,
    onWeekendVisibilityChange: (Boolean, Boolean) -> Unit,
    onHideEmptyWeekendChange: (Boolean) -> Unit,
    onRequestDndAccess: () -> Unit,
    onAddPeriod: () -> Unit,
    onUpdatePeriod: (PeriodEditInput) -> Unit,
    onRemovePeriod: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit,
    onDeveloperNotificationDelayChange: (Int) -> Unit,
    onDeveloperDndDurationChange: (Int) -> Unit,
    onDeveloperAutoDisableDndChange: (Boolean) -> Unit,
    onDeveloperDndGapChange: (Int) -> Unit,
    onDeveloperDndSkipThresholdChange: (Int) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onTriggerTestDnd: () -> Unit,
    onTriggerTestDndConsecutive: () -> Unit,
    notificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenAboutLink: (String) -> Unit
) {
    val preferences = state.preferences
    val zoneId = remember { ZoneId.systemDefault() }
    var showDatePicker by remember { mutableStateOf(false) }
    var totalWeeksText by remember(preferences.totalWeeks) { mutableStateOf(preferences.totalWeeks.toString()) }
    remember { DateTimeFormatter.ofPattern("yyyy/MM/dd") }
    val colorOptions = remember(preferences.backgroundValue) {
        (listOf(preferences.backgroundValue) + listOf(
            UserPreferences().backgroundValue,
            "#FFB300",
            "#FF7043",
            "#8BC34A",
            "#29B6F6",
            "#9C27B0"
        )).distinct()
    }

    val title = when (page) {
        SettingsPage.Main -> stringResource(R.string.settings_title)
        SettingsPage.Developer -> stringResource(R.string.settings_developer_title)
        SettingsPage.About -> stringResource(R.string.settings_about_title)
    }

    val topBarNavigation: @Composable () -> Unit = {
        IconButton(onClick = {
            if (page == SettingsPage.Main) {
                onBack()
            } else {
                onNavigate(SettingsPage.Main)
            }
        }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = topBarNavigation
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when (page) {
            SettingsPage.Main -> SettingsMainPage(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                preferences = preferences,
                periods = state.periods,
                totalWeeksText = totalWeeksText,
                onTotalWeeksTextChange = { totalWeeksText = it },
                onTimetableNameChange = onTimetableNameChange,
                onBackgroundColorSelected = onBackgroundColorSelected,
                onBackgroundImageSelect = onBackgroundImageSelect,
                onClearBackgroundImage = onClearBackgroundImage,
                onVisibleFieldsChange = onVisibleFieldsChange,
                onReminderLeadChange = onReminderLeadChange,
                onDndConfigChange = onDndConfigChange,
                onTermStartDateChange = onTermStartDateChange,
                onShowDatePicker = { showDatePicker = true },
                onTotalWeeksChange = onTotalWeeksChange,
                onShowNonCurrentWeekCoursesChange = onShowNonCurrentWeekCoursesChange,
                onWeekendVisibilityChange = onWeekendVisibilityChange,
                onHideEmptyWeekendChange = onHideEmptyWeekendChange,
                onRequestDndAccess = onRequestDndAccess,
                onAddPeriod = onAddPeriod,
                onUpdatePeriod = onUpdatePeriod,
                onRemovePeriod = onRemovePeriod,
                onExport = onExport,
                onImport = onImport,
                onNavigateToDeveloper = { onNavigate(SettingsPage.Developer) },
                onNavigateToAbout = { onNavigate(SettingsPage.About) },
                colorOptions = colorOptions
            )

            SettingsPage.Developer -> SettingsDeveloperPage(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                preferences = preferences,
                onDeveloperModeChange = onDeveloperModeChange,
                onDeveloperNotificationDelayChange = onDeveloperNotificationDelayChange,
                onDeveloperDndDurationChange = onDeveloperDndDurationChange,
                onDeveloperAutoDisableDndChange = onDeveloperAutoDisableDndChange,
                onDeveloperDndGapChange = onDeveloperDndGapChange,
                onDeveloperDndSkipThresholdChange = onDeveloperDndSkipThresholdChange,
                onTriggerTestNotification = onTriggerTestNotification,
                onTriggerTestDnd = onTriggerTestDnd,
                onTriggerTestDndConsecutive = onTriggerTestDndConsecutive,
                notificationsEnabled = notificationsEnabled,
                onOpenNotificationSettings = onOpenNotificationSettings
            )

            SettingsPage.About -> SettingsAboutPage(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                onOpenLink = onOpenAboutLink
            )
        }
    }

    if (showDatePicker) {
        val initialDateMillis =
            preferences.termStartDate()?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    val selectedDate =
                        millis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
                    onTermStartDateChange(selectedDate)
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsMainPage(
    modifier: Modifier,
    preferences: UserPreferences,
    periods: List<PeriodDefinition>,
    totalWeeksText: String,
    onTotalWeeksTextChange: (String) -> Unit,
    onTimetableNameChange: (String) -> Unit,
    onBackgroundColorSelected: (String) -> Unit,
    onBackgroundImageSelect: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onVisibleFieldsChange: (Set<CourseDisplayField>) -> Unit,
    onReminderLeadChange: (Int) -> Unit,
    onDndConfigChange: (Boolean, Int, Int, Int) -> Unit,
    onTermStartDateChange: (LocalDate?) -> Unit,
    onShowDatePicker: () -> Unit,
    onTotalWeeksChange: (Int) -> Unit,
    onShowNonCurrentWeekCoursesChange: (Boolean) -> Unit,
    onWeekendVisibilityChange: (Boolean, Boolean) -> Unit,
    onHideEmptyWeekendChange: (Boolean) -> Unit,
    onRequestDndAccess: () -> Unit,
    onAddPeriod: () -> Unit,
    onUpdatePeriod: (PeriodEditInput) -> Unit,
    onRemovePeriod: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onNavigateToDeveloper: () -> Unit,
    onNavigateToAbout: () -> Unit,
    colorOptions: List<String>
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard(title = stringResource(R.string.settings_timetable_section)) {
            OutlinedTextField(
                value = preferences.timetableName,
                onValueChange = onTimetableNameChange,
                label = { Text(stringResource(R.string.settings_timetable_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            val termStartDate = preferences.termStartDate()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_term_start_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = termStartDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                            ?: stringResource(R.string.settings_term_start_not_set),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onShowDatePicker) {
                    Text(stringResource(R.string.settings_term_start_pick))
                }
                if (termStartDate != null) {
                    TextButton(onClick = { onTermStartDateChange(null) }) {
                        Text(stringResource(R.string.settings_term_start_clear))
                    }
                }
            }
            OutlinedTextField(
                value = totalWeeksText,
                onValueChange = { value ->
                    val filtered = value.filter { it.isDigit() }.take(2)
                    onTotalWeeksTextChange(filtered)
                    filtered.toIntOrNull()?.takeIf { it in 1..MaxTotalWeeks }
                        ?.let(onTotalWeeksChange)
                },
                label = { Text(stringResource(R.string.settings_total_weeks_label)) },
                supportingText = { Text(stringResource(R.string.settings_total_weeks_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_show_non_current_week_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.settings_show_non_current_week_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.showNonCurrentWeekCourses,
                    onCheckedChange = onShowNonCurrentWeekCoursesChange
                )
            }
        }

        SettingsCard(title = stringResource(R.string.settings_background_label)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                colorOptions.forEach { hex ->
                    ColorOption(
                        colorHex = hex,
                        selected = preferences.backgroundMode == com.misaka.hoshinoschedule.data.settings.BackgroundMode.COLOR && preferences.backgroundValue == hex,
                        onClick = { onBackgroundColorSelected(hex) }
                    )
                }
                TextButton(onClick = onBackgroundImageSelect) {
                    Text(stringResource(R.string.settings_background_pick))
                }
                if (preferences.backgroundMode == com.misaka.hoshinoschedule.data.settings.BackgroundMode.IMAGE) {
                    TextButton(onClick = onClearBackgroundImage) {
                        Text(stringResource(R.string.settings_background_use_color))
                    }
                }
            }
            if (preferences.backgroundMode == com.misaka.hoshinoschedule.data.settings.BackgroundMode.IMAGE) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(preferences.backgroundValue)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
        }

        SettingsCard(title = stringResource(R.string.settings_course_fields_title)) {
            CourseDisplayField.entries.forEach { field ->
                val checked = field in preferences.visibleFields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val updated = preferences.visibleFields.toMutableSet()
                            if (checked) {
                                updated.remove(field)
                                if (updated.isEmpty()) {
                                    updated.add(CourseDisplayField.NAME)
                                }
                            } else {
                                updated.add(field)
                            }
                            onVisibleFieldsChange(updated)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(text = fieldLabel(field))
                }
            }
        }

        SettingsCard(title = stringResource(R.string.settings_reminders_title)) {
            SliderWithValue(
                label = stringResource(R.string.settings_reminder_lead_label_short),
                value = preferences.reminderLeadMinutes,
                range = 0..MaxReminderLeadMinutes,
                valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                onChange = onReminderLeadChange
            )
        }

        SettingsCard(title = stringResource(R.string.settings_dnd_title)) {
            SwitchRow(
                title = if (preferences.dndEnabled) {
                    stringResource(R.string.settings_dnd_enabled)
                } else {
                    stringResource(R.string.settings_dnd_disabled)
                },
                checked = preferences.dndEnabled,
                onCheckedChange = { enabled ->
                    onDndConfigChange(
                        enabled,
                        preferences.dndLeadMinutes,
                        preferences.dndReleaseMinutes,
                        preferences.dndSkipBreakThresholdMinutes
                    )
                }
            )
            if (preferences.dndEnabled) {
                SliderWithValue(
                    label = stringResource(R.string.settings_dnd_enable_before),
                    value = preferences.dndLeadMinutes,
                    range = 0..MaxDndLeadMinutes,
                    valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                    onChange = { lead ->
                        onDndConfigChange(
                            preferences.dndEnabled,
                            lead,
                            preferences.dndReleaseMinutes,
                            preferences.dndSkipBreakThresholdMinutes
                        )
                    }
                )
                SliderWithValue(
                    label = stringResource(R.string.settings_dnd_disable_after),
                    value = preferences.dndReleaseMinutes,
                    range = 0..MaxDndReleaseMinutes,
                    valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                    onChange = { release ->
                        onDndConfigChange(
                            preferences.dndEnabled,
                            preferences.dndLeadMinutes,
                            release,
                            preferences.dndSkipBreakThresholdMinutes
                        )
                    }
                )
                SliderWithValue(
                    label = stringResource(R.string.settings_dnd_keep_enabled),
                    value = preferences.dndSkipBreakThresholdMinutes,
                    range = 0..MaxDndSkipThresholdMinutes,
                    valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                    onChange = { threshold ->
                        onDndConfigChange(
                            preferences.dndEnabled,
                            preferences.dndLeadMinutes,
                            preferences.dndReleaseMinutes,
                            threshold
                        )
                    }
                )
                TextButton(onClick = onRequestDndAccess) {
                    Text(stringResource(R.string.settings_dnd_grant_access))
                }
            }
        }

        SettingsCard(title = stringResource(R.string.settings_weekend_section)) {
            SwitchRow(
                title = stringResource(R.string.settings_show_saturday),
                checked = preferences.showSaturday,
                onCheckedChange = { onWeekendVisibilityChange(it, preferences.showSunday) }
            )
            SwitchRow(
                title = stringResource(R.string.settings_show_sunday),
                checked = preferences.showSunday,
                onCheckedChange = { onWeekendVisibilityChange(preferences.showSaturday, it) }
            )
            SwitchRow(
                title = stringResource(R.string.settings_hide_empty_weekend),
                checked = preferences.hideEmptyWeekends,
                onCheckedChange = onHideEmptyWeekendChange
            )
            Text(
                text = stringResource(R.string.settings_hide_empty_weekend_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsCard(title = stringResource(R.string.settings_periods_title)) {
            periods.sortedBy { it.sequence }.forEach { period ->
                PeriodEditorRow(
                    period = period,
                    onUpdate = onUpdatePeriod,
                    onRemove = onRemovePeriod
                )
            }
            Button(onClick = onAddPeriod) {
                Text(stringResource(R.string.settings_add_period))
            }
        }

        SettingsCard(title = stringResource(R.string.settings_data_section)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onExport) { Text(stringResource(R.string.settings_export)) }
                Button(onClick = onImport) { Text(stringResource(R.string.settings_import)) }
            }
        }

        SettingsNavigationRow(
            title = stringResource(R.string.settings_developer_entry_title),
            subtitle = stringResource(R.string.settings_developer_entry_subtitle),
            onClick = onNavigateToDeveloper
        )

        SettingsNavigationRow(
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_entry_subtitle),
            onClick = onNavigateToAbout
        )
    }
}

@Composable
private fun SettingsDeveloperPage(
    modifier: Modifier,
    preferences: UserPreferences,
    onDeveloperModeChange: (Boolean) -> Unit,
    onDeveloperNotificationDelayChange: (Int) -> Unit,
    onDeveloperDndDurationChange: (Int) -> Unit,
    onDeveloperAutoDisableDndChange: (Boolean) -> Unit,
    onDeveloperDndGapChange: (Int) -> Unit,
    onDeveloperDndSkipThresholdChange: (Int) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onTriggerTestDnd: () -> Unit,
    onTriggerTestDndConsecutive: () -> Unit,
    notificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard(title = stringResource(R.string.settings_developer_mode_section)) {
            SwitchRow(
                title = stringResource(R.string.settings_developer_mode_enable),
                checked = preferences.developerModeEnabled,
                onCheckedChange = onDeveloperModeChange
            )
            if (!notificationsEnabled) {
                Text(
                    text = stringResource(R.string.settings_developer_notifications_disabled),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onOpenNotificationSettings) {
                    Text(stringResource(R.string.settings_developer_notifications_open_settings))
                }
            }
        }

        if (preferences.developerModeEnabled) {
            SettingsCard(title = stringResource(R.string.settings_developer_testing_section)) {
                SliderWithValue(
                    label = stringResource(R.string.settings_developer_test_notification_delay),
                    value = preferences.developerTestNotificationDelaySeconds,
                    range = 0..MaxTestNotificationDelaySeconds,
                    valueSuffix = stringResource(R.string.settings_slider_seconds_suffix),
                    onChange = onDeveloperNotificationDelayChange
                )
                Button(onClick = onTriggerTestNotification) {
                    Text(stringResource(R.string.settings_developer_send_test_notification))
                }

                SwitchRow(
                    title = stringResource(R.string.settings_developer_dnd_auto_disable),
                    checked = preferences.developerAutoDisableDnd,
                    onCheckedChange = onDeveloperAutoDisableDndChange
                )
                SliderWithValue(
                    label = stringResource(R.string.settings_developer_test_dnd_duration),
                    value = preferences.developerTestDndDurationMinutes,
                    range = 1..MaxTestDndDurationMinutes,
                    valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                    onChange = onDeveloperDndDurationChange
                )
                SliderWithValue(
                    label = stringResource(R.string.settings_developer_test_dnd_gap),
                    value = preferences.developerTestDndGapMinutes,
                    range = 0..MaxDeveloperTestDndGapMinutes,
                    valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                    onChange = onDeveloperDndGapChange
                )
                SliderWithValue(
                    label = stringResource(R.string.settings_developer_test_dnd_skip_threshold),
                    value = preferences.developerTestDndSkipThresholdMinutes,
                    range = 0..MaxDeveloperTestDndSkipMinutes,
                    valueSuffix = stringResource(R.string.settings_slider_minutes_suffix),
                    onChange = onDeveloperDndSkipThresholdChange
                )
                Button(onClick = onTriggerTestDnd) {
                    Text(stringResource(R.string.settings_developer_toggle_dnd))
                }
                Button(onClick = onTriggerTestDndConsecutive) {
                    Text(stringResource(R.string.settings_developer_toggle_dnd_consecutive))
                }
            }
        }
    }
}

@Composable
private fun SettingsAboutPage(
    modifier: Modifier,
    onOpenLink: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val versionName = remember {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    ).versionName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }
            }.getOrNull() ?: "1.0"
        }
        SettingsCard(title = stringResource(R.string.settings_about_app_section)) {
            Text(text = stringResource(R.string.settings_about_version, versionName))
            Text(text = stringResource(R.string.settings_about_author))
        }
        SettingsCard(title = stringResource(R.string.settings_about_links)) {
            TextButton(onClick = { onOpenLink(context.getString(R.string.settings_about_repo_url)) }) {
                Text(stringResource(R.string.settings_about_open_repo))
            }
            TextButton(onClick = { onOpenLink(context.getString(R.string.settings_about_license_url)) }) {
                Text(stringResource(R.string.settings_about_view_license))
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.4f
            )
        ),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.35f
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SliderWithValue(
    label: String,
    value: Int,
    range: IntRange,
    valueSuffix: String,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "$label: $value $valueSuffix")
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun ColorOption(colorHex: String, selected: Boolean, onClick: () -> Unit) {
    val color = try {
        Color(colorHex.toColorInt())
    } catch (e: IllegalArgumentException) {
        MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun fieldLabel(field: CourseDisplayField): String = when (field) {
    CourseDisplayField.NAME -> stringResource(R.string.settings_field_name)
    CourseDisplayField.TEACHER -> stringResource(R.string.settings_field_teacher)
    CourseDisplayField.LOCATION -> stringResource(R.string.settings_field_location)
    CourseDisplayField.NOTES -> stringResource(R.string.settings_field_notes)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodEditorRow(
    period: PeriodDefinition,
    onUpdate: (PeriodEditInput) -> Unit,
    onRemove: (Int) -> Unit
) {
    var startMinutes by remember(period.id) { mutableStateOf(period.startMinutes) }
    var endMinutes by remember(period.id) { mutableStateOf(period.endMinutes) }
    var labelText by remember(period.id) { mutableStateOf(period.label.orEmpty()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.settings_period_title, period.sequence),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePickerTextField(
                value = minutesToText(startMinutes),
                label = stringResource(R.string.settings_period_start_hint),
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            )
            TimePickerTextField(
                value = minutesToText(endMinutes),
                label = stringResource(R.string.settings_period_end_hint),
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = labelText,
            onValueChange = { labelText = it },
            label = { Text(stringResource(R.string.settings_period_label_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (startMinutes < endMinutes) {
                        onUpdate(
                            PeriodEditInput(
                                id = period.id,
                                sequence = period.sequence,
                                startHour = startMinutes / 60,
                                startMinute = startMinutes % 60,
                                endHour = endMinutes / 60,
                                endMinute = endMinutes % 60,
                                label = labelText.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = startMinutes < endMinutes
            ) { Text(stringResource(R.string.settings_period_apply)) }
            TextButton(onClick = { onRemove(period.sequence) }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.settings_period_remove))
            }
        }
    }

    if (showStartPicker) {
        TimePickerAlertDialog(
            initialHour = startMinutes / 60,
            initialMinute = startMinutes % 60,
            onDismiss = { showStartPicker = false },
            onConfirm = { hour, minute ->
                showStartPicker = false
                val minutes = hour * 60 + minute
                startMinutes = minutes
                if (minutes >= endMinutes) {
                    endMinutes = (minutes + 5).coerceAtMost(23 * 60 + 55)
                }
            }
        )
    }

    if (showEndPicker) {
        TimePickerAlertDialog(
            initialHour = endMinutes / 60,
            initialMinute = endMinutes % 60,
            onDismiss = { showEndPicker = false },
            onConfirm = { hour, minute ->
                showEndPicker = false
                val minutes = hour * 60 + minute
                endMinutes = minutes
                if (minutes <= startMinutes) {
                    startMinutes = (minutes - 5).coerceAtLeast(0)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerAlertDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        text = {
            TimePicker(state = state)
        }
    )
}

@Composable
private fun TimePickerTextField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = label
                )
            },
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.35f
                ),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
        )
    }
}

private fun minutesToText(minutes: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)


