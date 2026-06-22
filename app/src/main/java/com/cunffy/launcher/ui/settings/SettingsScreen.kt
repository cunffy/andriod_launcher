package com.cunffy.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.R
import com.cunffy.launcher.gesture.GestureAction
import com.cunffy.launcher.gesture.GestureSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenHiddenApps: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val iconPacks by viewModel.iconPacks.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::writeBackup) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::readBackup) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_appearance))
            SwitchRow(
                title = stringResource(R.string.settings_themed_icons),
                subtitle = stringResource(R.string.settings_themed_icons_summary),
                checked = settings.themedIcons,
                onCheckedChange = viewModel::setThemedIcons,
            )
            IconPackRow(
                current = settings.iconPackPackage,
                packs = iconPacks,
                onSelect = viewModel::setIconPack,
            )

            SectionHeader(stringResource(R.string.settings_badges))
            SwitchRow(
                title = stringResource(R.string.settings_badges),
                subtitle = stringResource(R.string.settings_badges_summary),
                checked = settings.badgesEnabled,
                onCheckedChange = viewModel::setBadges,
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) { Text(stringResource(R.string.settings_notification_access)) }

            SectionHeader(stringResource(R.string.settings_gestures))
            GestureRow(GestureSlot.SWIPE_UP, stringResource(R.string.settings_gesture_swipe_up),
                settings.gestures[GestureSlot.SWIPE_UP] ?: GestureAction.NONE, viewModel::setGesture)
            GestureRow(GestureSlot.SWIPE_DOWN, stringResource(R.string.settings_gesture_swipe_down),
                settings.gestures[GestureSlot.SWIPE_DOWN] ?: GestureAction.NONE, viewModel::setGesture)
            GestureRow(GestureSlot.DOUBLE_TAP, stringResource(R.string.settings_gesture_double_tap),
                settings.gestures[GestureSlot.DOUBLE_TAP] ?: GestureAction.NONE, viewModel::setGesture)

            SectionHeader(stringResource(R.string.settings_hidden_apps))
            Text(
                text = stringResource(R.string.settings_hidden_apps),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenHiddenApps)
                    .padding(16.dp),
            )

            SectionHeader(stringResource(R.string.settings_backup))
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Button(onClick = { exportLauncher.launch("launcher-backup.json") }) {
                    Text(stringResource(R.string.settings_backup_export))
                }
            }
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                    Text(stringResource(R.string.settings_backup_import))
                }
            }

            SectionHeader(stringResource(R.string.settings_updates))
            OutlinedTextField(
                value = settings.updateUrl.orEmpty(),
                onValueChange = viewModel::setUpdateUrl,
                label = { Text(stringResource(R.string.settings_update_url)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Button(onClick = {
                    viewModel.checkForUpdates { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.settings_check_updates)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IconPackRow(
    current: String?,
    packs: List<com.cunffy.launcher.data.icons.IconPackInfo>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = packs.firstOrNull { it.packageName == current }?.label
        ?: stringResource(R.string.settings_icon_pack_system)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_icon_pack), style = MaterialTheme.typography.bodyLarge)
                Text(
                    currentLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_icon_pack_system)) },
                onClick = { onSelect(null); expanded = false },
            )
            packs.forEach { pack ->
                DropdownMenuItem(
                    text = { Text(pack.label) },
                    onClick = { onSelect(pack.packageName); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun GestureRow(
    slot: GestureSlot,
    label: String,
    action: GestureAction,
    onSelect: (GestureSlot, GestureAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(action.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GestureAction.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onSelect(slot, option); expanded = false },
                )
            }
        }
    }
}
