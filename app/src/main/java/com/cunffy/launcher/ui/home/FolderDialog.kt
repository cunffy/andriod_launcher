package com.cunffy.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.home.HomeEntry
import com.cunffy.launcher.ui.components.AppIcon

/** Opens a folder's contents in a centered dialog. */
@Composable
fun FolderDialog(
    folder: HomeEntry.Folder,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = folder.folder.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(folder.apps, key = { it.key }) { app ->
                        AppIcon(app = app, onClick = { onAppClick(app) })
                    }
                }
            }
        }
    }
}
