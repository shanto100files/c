package com.nuvio.app.features.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface

/**
 * Share one title with a friend or a group.
 *
 * The list itself comes from the server-backed [FriendsRepository]: the
 * dialog simply posts the share and reads the repository's error, so a failed
 * call replaces the success copy instead of pretending the share happened.
 * A guest is told to sign in rather than shown an empty chooser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWithFriendsDialog(
    metaId: String,
    metaType: String,
    title: String,
    poster: String?,
    onDismiss: () -> Unit,
) {
    FriendsRepository.ensureLoaded()
    val uiState by FriendsRepository.uiState.collectAsStateWithLifecycle()
    var selectedRecipientId by remember { mutableStateOf<String?>(null) }
    var shareRequested by remember { mutableStateOf(false) }

    val errorMessage = uiState.lastError
    val nothingToAdd = uiState.friends.isEmpty() && uiState.groups.isEmpty()
    val succeeded = shareRequested && errorMessage == null && !uiState.isSaving

    val surfaceMessage = when {
        !uiState.signedIn -> "Sign in with your CinePiX account to share with friends."
        succeeded -> "Successfully shared with your network!"
        shareRequested && errorMessage != null -> errorMessage
        nothingToAdd -> "No friends or groups yet — add a friend below to share with them."
        else -> "Choose a friend or group to share this content with:"
    }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = "Share “$title”",
        message = surfaceMessage,
    ) {
        when {
            !uiState.signedIn || (shareRequested && succeeded) -> {
                DialogButtons {
                    Button(onClick = onDismiss) { Text("Done") }
                }
            }

            nothingToAdd -> {
                AddFriendInlineForm(
                    isSaving = uiState.isSaving,
                    lastError = uiState.lastError,
                    onAdd = FriendsRepository::addFriend,
                )
                DialogButtons {
                    Button(onClick = onDismiss) { Text("Cancel") }
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                        if (uiState.friends.isNotEmpty()) {
                            item {
                                Text("Friends", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            items(uiState.friends, key = { "friend_${it.id}" }) { friend ->
                                Surface(
                                    onClick = { selectedRecipientId = friend.id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedRecipientId == friend.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AvatarImage(url = friend.avatar, size = 28.dp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(friend.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = friend.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (selectedRecipientId == friend.id) {
                                            Text("Selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                        if (uiState.groups.isNotEmpty()) {
                            item {
                                Text("Groups", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            items(uiState.groups, key = { "group_${it.id}" }) { group ->
                                Surface(
                                    onClick = { selectedRecipientId = group.id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedRecipientId == group.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(group.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        if (selectedRecipientId == group.id) {
                                            Text("Selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                DialogButtons {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        enabled = selectedRecipientId != null && !uiState.isSaving,
                        onClick = {
                            val recipientId = selectedRecipientId ?: return@Button
                            val friend = uiState.friends.firstOrNull { it.id == recipientId }
                            val group = uiState.groups.firstOrNull { it.id == recipientId }
                            val name = friend?.name ?: group?.name ?: "Unknown"
                            val isGroup = group != null
                            shareRequested = true
                            FriendsRepository.shareWithRecipient(
                                recipientId = recipientId,
                                recipientName = name,
                                isGroup = isGroup,
                                metaId = metaId,
                                metaType = metaType,
                                title = title,
                                poster = poster,
                            )
                        },
                    ) { Text(if (uiState.isSaving) "Sharing…" else "Share Now") }
                }
            }
        }
    }
}

/**
 * Inline "add a friend" form shown inside the share dialog when the account has
 * no friends or groups yet, so a first-time user can add someone and share in
 * one place instead of being sent away to Settings. Adding is optimistic via
 * [FriendsRepository.addFriend], so the new friend appears in the chooser the
 * moment the tap lands.
 */
@Composable
private fun AddFriendInlineForm(
    isSaving: Boolean,
    lastError: String?,
    onAdd: (name: String, email: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val canAdd = name.isNotBlank() && email.isNotBlank() && !isSaving

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Add a friend",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        lastError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            enabled = canAdd,
            onClick = {
                onAdd(name, email)
                name = ""
                email = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isSaving) "Adding…" else "Add friend")
        }
    }
}
