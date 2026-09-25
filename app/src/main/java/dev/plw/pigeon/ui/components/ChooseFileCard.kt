package dev.plw.pigeon.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.plw.pigeon.model.SharedFile

@Composable
fun ChooseFileCard(
    modifier: Modifier = Modifier,
    onFilesUpdated: (List<SharedFile>) -> Unit
) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<SharedFile>() }

    LaunchedEffect(files.size) {
        onFilesUpdated(files.toList())
    }

    val totalSizeMb = files.sumOf { it.sizeBytes } / (1024 * 1024)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            getSharedFileFromUri(context, uri)?.let { sharedFile ->
                if (files.none { it.uri == sharedFile.uri }) {
                    files.add(sharedFile)
                }
            }
        }
    }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Server Files",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${files.size} files | ${totalSizeMb}MB",
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                files.forEach { file ->
                    FileItemRow(
                        file = file,
                        onDelete = { files.remove(file) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Add +",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun getSharedFileFromUri(context: Context, uri: Uri): SharedFile? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            val name = cursor.getString(nameIndex) ?: "Unknown"
            val size = cursor.getLong(sizeIndex)
            SharedFile(uri = uri, name = name, sizeBytes = size)
        } else null
    }
}