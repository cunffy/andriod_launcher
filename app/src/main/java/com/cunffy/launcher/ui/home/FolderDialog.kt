package com.cunffy.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.home.HomeEntry
import com.cunffy.launcher.ui.components.AppIcon

/** Opens a folder's contents in a centered dialog; the title is editable to rename it. */
@Composable
fun FolderDialog(
    folder: HomeEntry.Folder,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
) {
    var title by remember(folder.folder.id) { mutableStateOf(folder.folder.title) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it; onRename(it) },
                    textStyle = MaterialTheme.typography.titleMedium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
