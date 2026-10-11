package com.nuvio.app.features.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface

/**
 * "Edit profile": display name, bio, and an avatar link.
 *
 * The picture is a **link** on purpose: byte upload needs a platform file
 * picker this dialog does not own, while a pasted link (image host, another
 * profile) works identically on every platform today. File upload already
 * works on the site's `/me` page from any phone browser - the server stores
 * both shapes in the same `avatar` field.
 */
@Composable
fun ProfileDialog(
    profile: MyProfile,
    onDismiss: () -> Unit,
    onSave: (displayName: String, bio: String) -> Unit,
    onSaveAvatar: (url: String) -> Unit,
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatarUrl by remember { mutableStateOf(profile.avatar) }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = "Edit profile",
        message = "Your display name, bio and picture are visible to friends.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AvatarImage(url = avatarUrl.trim().ifBlank { profile.avatar }, size = 48.dp)
                Column {
                    Text(
                        text = displayName.trim().ifBlank { profile.email.ifBlank { "Your name" } },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(160) },
                label = { Text("Bio (${bio.length}/160)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = avatarUrl,
                onValueChange = { avatarUrl = it },
                label = { Text("Picture link") },
                placeholder = { Text("https://... (leave as-is to keep)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DialogButtons {
                Button(onClick = onDismiss) { Text("Cancel") }
                Button(
                    enabled = displayName.isNotBlank(),
                    onClick = {
                        onSave(displayName, bio)
                        val newAvatar = avatarUrl.trim()
                        if (newAvatar.isNotBlank() && newAvatar != profile.avatar) {
                            onSaveAvatar(newAvatar)
                        }
                        onDismiss()
                    },
                ) { Text("Save") }
            }
        }
    }
}
