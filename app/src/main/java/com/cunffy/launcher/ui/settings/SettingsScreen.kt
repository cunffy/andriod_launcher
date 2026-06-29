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
import androidx.compose.material3.Slider
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
import com.cunffy.launcher.data.prefs.AccentPreset
import com.cunffy.launcher.data.prefs.IconShape
import com.cunffy.launcher.data.prefs.ThemeMode
import com.cunffy.launcher.gesture.GestureAction
import com.cunffy.launcher.gesture.GestureSlot
import kotlin.math.roundToInt

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
            IconShapeRow(current = settings.iconShape, onSelect = viewModel::setIconShape)
            IconPackRow(
                current = settings.iconPackPackage,
                packs = iconPacks,
                onSelect = viewModel::setIconPack,
            )
            ThemeModeRow(current = settings.themeMode, onSelect = viewModel::setThemeMode)
            SwitchRow(
                title = "Dynamic color",
                subtitle = "Use Material You colors from your wallpaper",
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
            if (!settings.dynamicColor) {
                SwitchRow(
                    title = "Use wallpaper colors",
                    subtitle = "Theme from your wallpaper — works with live wallpapers",
                    checked = settings.accentFromWallpaper,
                    onCheckedChange = viewModel::setAccentFromWallpaper,
                )
                if (!settings.accentFromWallpaper) {
                    AccentPresetRow(
                        current = settings.accentPreset,
                        onSelect = viewModel::setAccentPreset,
                    )
                }
            }
            SliderRow(
                title = "Clock size",
                value = settings.clockSizeSp,
                range = 40..96,
                onChange = viewModel::setClockSize,
            )
            SwitchRow(
                title = "At a Glance",
                subtitle = "Weather and next event under the clock",
                checked = settings.showAtAGlance,
                onCheckedChange = viewModel::setShowAtAGlance,
            )
            SwitchRow(
                title = "Now playing card",
                subtitle = "Show media controls on the home screen",
                checked = settings.showMediaCard,
                onCheckedChange = viewModel::setShowMediaCard,
            )
            SliderRow(
                title = "App drawer columns",
                value = settings.drawerColumns,
                range = 3..6,
                onChange = viewModel::setDrawerColumns,
            )
            SliderRow(
                title = "Drawer opacity",
                value = settings.drawerOpacity,
                range = 50..100,
                onChange = viewModel::setDrawerOpacity,
            )
            SliderRow(
                title = "Wallpaper dim",
                value = settings.wallpaperDim,
                range = 0..80,
                onChange = viewModel::setWallpaperDim,
            )
            SliderRow(
                title = "Icon size",
                value = settings.iconSizeDp,
                range = 36..72,
                onChange = viewModel::setIconSize,
            )
            SliderRow(
                title = "Home grid columns",
                value = settings.gridColumns,
                range = 3..6,
                onChange = viewModel::setGridColumns,
            )
            SliderRow(
                title = "Home grid rows",
                value = settings.gridRows,
                range = 4..8,
                onChange = viewModel::setGridRows,
            )
            SwitchRow(
                title = "App labels in drawer",
                subtitle = "Show names under app icons",
                checked = settings.showDrawerLabels,
                onCheckedChange = viewModel::setDrawerLabels,
            )
            SwitchRow(
                title = "App labels on home",
                subtitle = "Show names under home-screen icons",
                checked = settings.showHomeLabels,
                onCheckedChange = viewModel::setHomeLabels,
            )
            SwitchRow(
                title = "Focus search on open",
                subtitle = "Pop up the keyboard when the drawer opens",
                checked = settings.searchAutoFocus,
                onCheckedChange = viewModel::setSearchAutoFocus,
            )
            SwitchRow(
                title = "24-hour clock",
                subtitle = "Show the home clock in 24-hour time",
                checked = settings.clock24h,
                onCheckedChange = viewModel::setClock24h,
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
            GestureRow(GestureSlot.PINCH_IN, stringResource(R.string.settings_gesture_pinch_in),
                settings.gestures[GestureSlot.PINCH_IN] ?: GestureAction.NONE, viewModel::setGesture)
            GestureRow(GestureSlot.PINCH_OUT, stringResource(R.string.settings_gesture_pinch_out),
                settings.gestures[GestureSlot.PINCH_OUT] ?: GestureAction.NONE, viewModel::setGesture)

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
                    if (packs.isEmpty()) {
                        stringResource(R.string.settings_icon_pack_none)
                    } else {
                        currentLabel
                    },
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

private fun IconShape.displayName(): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun IconShapeRow(current: IconShape, onSelect: (IconShape) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Icon shape",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(current.displayName(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            IconShape.entries.forEach { shape ->
                DropdownMenuItem(
                    text = { Text(shape.displayName()) },
                    onClick = { onSelect(shape); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun AccentPresetRow(current: AccentPreset, onSelect: (AccentPreset) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    fun label(p: AccentPreset) = p.name.lowercase().replaceFirstChar { it.uppercase() }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Accent color",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(label(current), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AccentPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(label(preset)) },
                    onClick = { onSelect(preset); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ThemeModeRow(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Theme", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                current.name.lowercase().replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = { onSelect(mode); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SliderRow(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text("$value", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}
