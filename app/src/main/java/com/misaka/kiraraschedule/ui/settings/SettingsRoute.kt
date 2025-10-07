package com.misaka.kiraraschedule.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misaka.kiraraschedule.R
import com.misaka.kiraraschedule.data.settings.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var currentPage by remember { mutableStateOf(SettingsPage.Main) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                viewModel.export(uri) { success ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(if (success) R.string.settings_export_success else R.string.settings_export_failure)
                        )
                    }
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                viewModel.import(uri) { success ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(if (success) R.string.settings_import_success else R.string.settings_import_failure)
                        )
                    }
                }
            }
        }

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.setBackgroundImage(it.toString()) }
        }

    val handleBack = {
        if (currentPage != SettingsPage.Main) {
            currentPage = SettingsPage.Main
        } else {
            onBack()
        }
    }

    SettingsScreen(
        state = uiState,
        page = currentPage,
        snackbarHostState = snackbarHostState,
        onNavigate = { currentPage = it },
        onBack = handleBack,
        onTimetableNameChange = viewModel::setTimetableName,
        onBackgroundColorSelected = viewModel::setBackgroundColor,
        onBackgroundImageSelect = { imagePicker.launch("image/*") },
        onClearBackgroundImage = { viewModel.setBackgroundColor(UserPreferences().backgroundValue) },
        onVisibleFieldsChange = viewModel::setVisibleFields,
        onReminderLeadChange = viewModel::setReminderLead,
        onDndConfigChange = viewModel::setDndConfig,
        onTermStartDateChange = viewModel::setTermStartDate,
        onTotalWeeksChange = viewModel::setTotalWeeks,
        onShowNonCurrentWeekCoursesChange = viewModel::setShowNonCurrentWeekCourses,
        onWeekendVisibilityChange = viewModel::setWeekendVisibility,
        onRequestDndAccess = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        },
        onAddPeriod = viewModel::addPeriod,
        onUpdatePeriod = viewModel::updatePeriod,
        onRemovePeriod = viewModel::removePeriod,
        onExport = { exportLauncher.launch("kirara_schedule.json") },
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        onDeveloperModeChange = viewModel::setDeveloperMode,
        onDeveloperNotificationDelayChange = viewModel::setDeveloperTestNotificationDelay,
        onDeveloperDndDurationChange = viewModel::setDeveloperTestDndDuration,
        onDeveloperAutoDisableDndChange = viewModel::setDeveloperAutoDisableDnd,
        onDeveloperDndGapChange = viewModel::setDeveloperTestDndGap,
        onDeveloperDndSkipThresholdChange = viewModel::setDeveloperTestDndSkipThreshold,
        onTriggerTestNotification = {
            viewModel.triggerTestNotification()
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_developer_test_notification_scheduled))
            }
        },
        onTriggerTestDnd = {
            viewModel.triggerTestDnd()
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_developer_test_dnd_scheduled))
            }
        },
        onTriggerTestDndConsecutive = {
            viewModel.triggerTestDndConsecutive()
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_developer_test_dnd_consecutive_scheduled))
            }
        },
        notificationsEnabled = notificationsEnabled,
        onOpenNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        },
        onOpenAboutLink = { url ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    )
}
