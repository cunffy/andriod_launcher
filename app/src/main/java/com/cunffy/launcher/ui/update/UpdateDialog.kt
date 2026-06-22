package com.cunffy.launcher.ui.update

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Surfaces the in-app update prompt and download progress over the launcher. */
@Composable
fun UpdateHost(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is UpdateViewModel.State.Available -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Update available") },
            text = {
                val notes = s.manifest.notes.ifBlank { "A new version (${s.manifest.versionName}) is ready." }
                Text(notes)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.startUpdate(s.manifest) }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text("Later") }
            },
        )
        UpdateViewModel.State.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Downloading update") },
            text = { CircularProgressIndicator(modifier = Modifier.size(32.dp)) },
            confirmButton = {},
        )
        is UpdateViewModel.State.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Update failed") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text("OK") } },
        )
        UpdateViewModel.State.Idle -> Unit
    }
}
