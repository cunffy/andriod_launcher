package com.cunffy.launcher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.data.apps.AppInfo

/** Long-press action sheet for an app in the drawer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onInfo: () -> Unit,
    onEdit: () -> Unit,
    onToggleHide: () -> Unit,
    onToggleLock: () -> Unit,
    onAddToHome: () -> Unit,
    onUninstall: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ActionRow(Icons.Rounded.Edit, "Edit") { onEdit() }
            ActionRow(Icons.Rounded.AddToHomeScreen, "Add to home") { onAddToHome() }
            if (app.hidden) {
                ActionRow(Icons.Rounded.Visibility, "Unhide") { onToggleHide() }
            } else {
                ActionRow(Icons.Rounded.VisibilityOff, "Hide") { onToggleHide() }
            }
            if (app.locked) {
                ActionRow(Icons.Rounded.LockOpen, "Unlock") { onToggleLock() }
            } else {
                ActionRow(Icons.Rounded.Lock, "Lock") { onToggleLock() }
            }
            ActionRow(Icons.Rounded.Info, "App info") { onInfo() }
            ActionRow(Icons.Rounded.Delete, "Uninstall") { onUninstall() }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(text = label, modifier = Modifier.padding(start = 20.dp))
    }
}
