package com.nuvio.app.features.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatLocalDateTime
import com.nuvio.app.core.site.SiteApi
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.home.MetaPreview

/**
 * Settings -> "CinePiX Social & Friends".
 *
 * Three tabs: my friends, my groups, and the feed split into what others
 * shared with me (tap to open the title) and what I sent. Everything is
 * server-backed through [FriendsRepository]: a signed-out member sees a
 * sign-in gate, a failing call shows a dismissable strip instead of silently
 * losing the tap, and a refresh runs every time the screen opens.
 */
@Composable
fun FriendsSocialScreen(
    onBack: () -> Unit,
    onOpenMeta: ((MetaPreview) -> Unit)? = null,
) {
    FriendsRepository.ensureLoaded()
    LaunchedEffect(Unit) { FriendsRepository.refresh() }
    val uiState by FriendsRepository.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }

    val filteredFriends = remember(uiState.friends, searchKeyword) {
        if (searchKeyword.isBlank()) uiState.friends
        else uiState.friends.filter { it.name.contains(searchKeyword, ignoreCase = true) || it.email.contains(searchKeyword, ignoreCase = true) }
    }

    NuvioScreen(modifier = Modifier.fillMaxSize()) {
        stickyHeader {
            NuvioScreenHeader(
                title = "CinePiX Social & Friends",
                onBack = onBack,
            )
        }

        if (!uiState.signedIn) {
            item { SignedOutCard() }
        } else {
            item {
                ProfileCard(
                    profile = uiState.me ?: MyProfile(email = SiteApi.sessionEmail ?: ""),
                    onEdit = { showProfileDialog = true },
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = { Text("Search friends...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )

                    SecondaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Friends (${uiState.friends.size})") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Groups (${uiState.groups.size})") },
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Feed (${uiState.inbox.size + uiState.sent.size})") },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Your Friend List"
                                1 -> "Your Groups"
                                else -> "Shared Content Feed"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (selectedTab == 0) {
                            Button(onClick = { showAddFriendDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.height(4.dp))
                                Text("Add Friend")
                            }
                        } else if (selectedTab == 1) {
                            Button(onClick = { showCreateGroupDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.height(4.dp))
                                Text("Create Group")
                            }
                        }
                    }
                }
            }

            uiState.lastError?.let { message ->
                item {
                    ErrorStrip(message = message, onDismiss = { FriendsRepository.dismissError() })
                }
            }

            if (uiState.isSyncing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (filteredFriends.isEmpty()) {
                        item {
                            Text(
                                text = if (searchKeyword.isBlank()) {
                                    "No friends added yet. Add friends to share movies & series!"
                                } else {
                                    "No friend matches \"$searchKeyword\"."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    } else {
                        items(items = filteredFriends, key = { it.id }) { friend ->
                            FriendRow(friend = friend, onDelete = { FriendsRepository.removeFriend(friend.id) })
                        }
                    }
                }

                1 -> {
                    if (uiState.groups.isEmpty()) {
                        item {
                            Text(
                                text = "No groups created yet. Create a group to share with multiple friends!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    } else {
                        items(items = uiState.groups, key = { it.id }) { group ->
                            GroupRow(group = group, onDelete = { FriendsRepository.deleteGroup(group.id) })
                        }
                    }
                }

                else -> {
                    if (uiState.inbox.isEmpty() && uiState.sent.isEmpty()) {
                        item {
                            Text(
                                text = "No shared content yet. Open any movie or series and tap Share!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    } else {
                        if (uiState.inbox.isNotEmpty()) {
                            item { FeedSectionLabel("Shared with me") }
                            items(items = uiState.inbox, key = { "inbox_${it.id}" }) { share ->
                                SharedFeedRow(
                                    share = share,
                                    onClick = onOpenMeta?.let { openMeta ->
                                        {
                                            openMeta(
                                                MetaPreview(
                                                    id = share.metaId,
                                                    type = share.metaType,
                                                    name = share.title,
                                                    poster = share.poster,
                                                ),
                                            )
                                        }
                                    },
                                    onDelete = { FriendsRepository.deleteShare(share.id) },
                                )
                            }
                        }
                        if (uiState.sent.isNotEmpty()) {
                            item { FeedSectionLabel("Shared by me") }
                            items(items = uiState.sent, key = { "sent_${it.id}" }) { share ->
                                SharedFeedRow(
                                    share = share,
                                    onClick = onOpenMeta?.let { openMeta ->
                                        {
                                            openMeta(
                                                MetaPreview(
                                                    id = share.metaId,
                                                    type = share.metaType,
                                                    name = share.title,
                                                    poster = share.poster,
                                                ),
                                            )
                                        }
                                    },
                                    onDelete = { FriendsRepository.deleteShare(share.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onAdd = { name, email ->
                FriendsRepository.addFriend(name, email)
                showAddFriendDialog = false
            },
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            friends = uiState.friends,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, memberIds ->
                FriendsRepository.createGroup(name, desc, memberIds)
                showCreateGroupDialog = false
            },
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            profile = uiState.me ?: MyProfile(email = SiteApi.sessionEmail ?: ""),
            onDismiss = { showProfileDialog = false },
            onSave = { name, bio -> FriendsRepository.saveProfile(name, bio) },
            onSaveAvatar = { url -> FriendsRepository.saveAvatar(url) },
        )
    }
}

@Composable
private fun SignedOutCard() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Sign in to use Social & Friends",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = "Your CinePiX account keeps friends, groups and the shared feed in sync across devices. Sign in from Settings -> Account, then come back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorStrip(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FeedSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Round profile picture; a [Person] glyph stands in until the link loads. */
@Composable
internal fun AvatarImage(url: String?, size: Dp) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.55f),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/** My own card at the top of the signed-in screen: what friends see, one tap from editing it. */
@Composable
private fun ProfileCard(profile: MyProfile, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                AvatarImage(url = profile.avatar, size = 44.dp)
                Column {
                    Text(
                        text = profile.displayName.ifBlank { profile.email.ifBlank { "Your profile" } },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = profile.bio.ifBlank { "Add a name, bio & picture" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
    }
}

@Composable
private fun FriendRow(friend: FriendItem, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(url = friend.avatar, size = 40.dp)
                Column {
                    Text(text = friend.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = friend.displayName?.takeIf { it != friend.name }?.let { "$it • ${friend.email}" }
                            ?: friend.email.ifBlank { "No email" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun GroupRow(group: GroupItem, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
                Column {
                    Text(text = group.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = "${group.memberIds.size} members • ${group.description.ifBlank { "No description" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SharedFeedRow(
    share: SharedContentItem,
    onClick: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (!share.poster.isNullOrBlank()) {
                    AsyncImage(
                        model = share.poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 48.dp, height = 72.dp).clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.size(width = 48.dp, height = 72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Column {
                    val counterpart = if (share.isIncoming) {
                        "From ${share.recipientName}"
                    } else {
                        "Shared with ${share.recipientName}"
                    }
                    val context = if (share.isGroup && share.groupName.isNotBlank()) {
                        "$counterpart • ${share.groupName}"
                    } else {
                        counterpart
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (share.isIncoming) {
                            AvatarImage(url = share.fromAvatar, size = 16.dp)
                        }
                        Text(
                            text = context,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = share.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatLocalDateTime(share.timestampEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = "Add Friend",
        message = "Enter their CinePiX account email - shares only reach real accounts.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Friend Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Friend Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DialogButtons {
                Button(onClick = onDismiss) { Text("Cancel") }
                Button(
                    enabled = name.isNotBlank() && email.isNotBlank(),
                    onClick = { onAdd(name, email) },
                ) { Text("Add") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(
    friends: List<FriendItem>,
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>) -> Unit,
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf(setOf<String>()) }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = "Create Group",
        message = "Pick the friends who should receive group shares:",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (friends.isEmpty()) {
                Text(
                    text = "No friends yet - add them in the Friends tab first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 190.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    friends.forEach { friend ->
                        val checked = friend.id in selectedMemberIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMemberIds = if (checked) selectedMemberIds - friend.id else selectedMemberIds + friend.id
                                },
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedMemberIds = if (isChecked) selectedMemberIds + friend.id else selectedMemberIds - friend.id
                                },
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(friend.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    friend.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "${selectedMemberIds.size} of ${friends.size} selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DialogButtons {
                Button(onClick = onDismiss) { Text("Cancel") }
                Button(
                    enabled = groupName.isNotBlank(),
                    onClick = { onCreate(groupName, description, selectedMemberIds.toList()) },
                ) { Text("Create") }
            }
        }
    }
}
