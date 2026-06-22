package com.cunffy.launcher.ui.drawer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.data.apps.AppCategory
import com.cunffy.launcher.data.apps.AppInfo

/** Edit an app's display name and category override. */
@Composable
fun AppEditDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onSave: (label: String?, category: AppCategory) -> Unit,
) {
    var label by remember { mutableStateOf(app.label) }
    var category by remember { mutableStateOf(app.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${app.label}") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Text(
                    text = "Category",
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    AppCategory.entries
                        .filter { it != AppCategory.ALL }
                        .forEach { option ->
                            FilterChip(
                                selected = option == category,
                                onClick = { category = option },
                                label = { Text(option.label) },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(label.takeIf { it.isNotBlank() && it != app.label }, category)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
