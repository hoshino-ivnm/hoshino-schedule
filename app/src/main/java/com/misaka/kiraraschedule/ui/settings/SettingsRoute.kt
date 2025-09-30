package com.misaka.kiraraschedule.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import com.misaka.kiraraschedule.data.settings.UserPreferences
import com.misaka.kiraraschedule.R
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            viewModel.export(uri) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) context.getString(R.string.settings_export_success) else context.getString(R.string.settings_export_failure))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.import(uri) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) context.getString(R.string.settings_import_success) else context.getString(R.string.settings_import_failure))
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setBackgroundImage(it.toString()) }
    }

    SettingsScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTimetableNameChange = viewModel::setTimetableName,
        onBackgroundColorSelected = viewModel::setBackgroundColor,
        onBackgroundImageSelect = { imagePicker.launch("image/*") },
        onClearBackgroundImage = { viewModel.setBackgroundColor(UserPreferences().backgroundValue) },
        onVisibleFieldsChange = { viewModel.setVisibleFields(it) },
        onReminderLeadChange = viewModel::setReminderLead,
        onWeekendVisibilityChange = viewModel::setWeekendVisibility,
        onDndConfigChange = viewModel::setDndConfig,
        onRequestDndAccess = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        },
        onAddPeriod = viewModel::addPeriod,
        onUpdatePeriod = viewModel::updatePeriod,
        onRemovePeriod = viewModel::removePeriod,
        onExport = { exportLauncher.launch("kirara_schedule.json") },
        onImport = { importLauncher.launch(arrayOf("application/json")) }
    )
}
