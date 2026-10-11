package com.nuvio.app.features.friends

import com.nuvio.app.core.site.SiteApi
import com.nuvio.app.core.site.SiteCall
import com.nuvio.app.core.site.SiteStorage
import com.nuvio.app.features.downloads.DownloadsClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class SocialSnapshot(
    val me: MyProfile? = null,
    val friends: List<FriendItem> = emptyList(),
    val groups: List<GroupItem> = emptyList(),
    val inbox: List<SharedContentItem> = emptyList(),
    val sent: List<SharedContentItem> = emptyList(),
)

data class FriendsUiState(
    val me: MyProfile? = null,
    val friends: List<FriendItem> = emptyList(),
    val groups: List<GroupItem> = emptyList(),
    val inbox: List<SharedContentItem> = emptyList(),
    val sent: List<SharedContentItem> = emptyList(),
    val signedIn: Boolean = false,
    val isSyncing: Boolean = false,
    val isSaving: Boolean = false,
    val lastError: String? = null,
)

/**
 * The Social & Friends model behind [FriendsSocialScreen] and
 * [ShareWithFriendsDialog].
 *
 * The site is the authority: every list is pulled from `/api/app/social` and
 * every mutation posts there, then the answer is re-pulled so the screen can
 * never drift from the server. The last successful pull is cached (see
 * `SiteStorage.loadSocialCache`) so an offline launch still shows yesterday's
 * friends instead of an empty page. Mutations are optimistic - the local list
 * moves immediately and rolls back with a readable error when the call fails,
 * because a dead network must feel like a hiccup, not a lost tap.
 *
 * A guest (or a stale token) sees a signed-out state instead of data: the
 * social graph belongs to an account, and never to the device.
 */
object FriendsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private var initialized = false

    /** Cache first (instant, offline-safe), then the server (authoritative). */
    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        val snapshot = SiteStorage.loadSocialCache()
            ?.let { raw -> runCatching { json.decodeFromString<SocialSnapshot>(raw) }.getOrNull() }
            ?: SocialSnapshot()
        _uiState.value = FriendsUiState(
            me = snapshot.me,
            friends = snapshot.friends,
            groups = snapshot.groups,
            inbox = snapshot.inbox,
            sent = snapshot.sent,
            signedIn = SiteApi.isSignedIn,
        )
        refresh()
    }

    /** Re-pulls the whole graph. Keeps the cached lists when the network fails. */
    fun refresh() {
        if (!SiteApi.isSignedIn) {
            _uiState.value = FriendsUiState(signedIn = false)
            return
        }
        scope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, signedIn = true)
            val call = SiteApi.socialState()
            when {
                call.ok -> {
                    val snapshot = parseState(call.body?.toString())
                    if (snapshot == null) {
                        _uiState.value = _uiState.value.copy(isSyncing = false, lastError = "Could not read the server response")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            me = snapshot.me,
                            friends = snapshot.friends,
                            groups = snapshot.groups,
                            inbox = snapshot.inbox,
                            sent = snapshot.sent,
                            isSyncing = false,
                            lastError = null,
                        )
                        SiteStorage.saveSocialCache(json.encodeToString(snapshot))
                    }
                }
                call.code == 401 -> {
                    // stale token: show the signed-out state, never someone's list
                    _uiState.value = FriendsUiState(signedIn = false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastError = call.errorText("Could not reach CinePiX"),
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(lastError = null)
    }

    // ---- mutations -------------------------------------------------------------------

    fun addFriend(name: String, email: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank()) return
        mutate(
            failMessage = "Could not add friend",
            apply = {
                val now = DownloadsClock.nowEpochMs()
                copy(
                    friends = friends + FriendItem(
                        id = trimmedEmail.lowercase(),
                        name = trimmedName,
                        email = trimmedEmail.lowercase(),
                        addedAtEpochMs = now,
                    ),
                )
            },
            call = {
                SiteApi.socialAddFriend(email = trimmedEmail, name = trimmedName)
            },
        )
    }

    fun removeFriend(friendId: String) {
        mutate(
            failMessage = "Could not remove friend",
            apply = {
                copy(
                    friends = friends.filterNot { it.id == friendId },
                    groups = groups.map { group ->
                        group.copy(memberIds = group.memberIds.filterNot { it == friendId })
                    },
                )
            },
            call = {
                SiteApi.socialUnfriend(friendId)
            },
        )
    }

    fun createGroup(name: String, description: String, memberIds: List<String>) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        mutate(
            failMessage = "Could not create group",
            apply = {
                copy(
                    groups = groups + GroupItem(
                        id = "grp_local_${DownloadsClock.nowEpochMs()}",
                        name = trimmedName,
                        description = description.trim(),
                        memberIds = memberIds,
                        createdAtEpochMs = DownloadsClock.nowEpochMs(),
                    ),
                )
            },
            call = {
                SiteApi.socialSaveGroup(id = null, name = trimmedName, description = description, memberIds = memberIds)
            },
        )
    }

    fun deleteGroup(groupId: String) {
        mutate(
            failMessage = "Could not delete group",
            apply = {
                copy(groups = groups.filterNot { it.id == groupId })
            },
            call = {
                SiteApi.socialDeleteGroup(groupId)
            },
        )
    }

    fun shareWithRecipient(
        recipientId: String,
        recipientName: String,
        isGroup: Boolean,
        metaId: String,
        metaType: String,
        title: String,
        poster: String?,
    ) {
        mutate(
            failMessage = "Could not share",
            apply = {
                val row = SharedContentItem(
                    id = "share_local_${DownloadsClock.nowEpochMs()}",
                    recipientId = recipientId,
                    recipientName = recipientName,
                    isGroup = isGroup,
                    metaId = metaId,
                    metaType = metaType,
                    title = title,
                    poster = poster,
                    timestampEpochMs = DownloadsClock.nowEpochMs(),
                    isIncoming = false,
                )
                copy(sent = listOf(row) + sent)
            },
            call = {
                SiteApi.socialShare(
                    recipientId = recipientId,
                    isGroup = isGroup,
                    metaId = metaId,
                    metaType = metaType,
                    title = title,
                    poster = poster,
                )
            },
        )
    }

    fun deleteShare(shareId: String) {
        mutate(
            failMessage = "Could not remove the post",
            apply = {
                copy(
                    inbox = inbox.filterNot { it.id == shareId },
                    sent = sent.filterNot { it.id == shareId },
                )
            },
            call = {
                SiteApi.socialDeleteShare(shareId)
            },
        )
    }

    /** Saves my display name + bio. One `me` move optimistically, like any list mutation. */
    fun saveProfile(displayName: String, bio: String) {
        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(lastError = "Display name cannot be empty")
            return
        }
        val trimmedBio = bio.trim()
        mutate(
            failMessage = "Could not save profile",
            apply = { copy(me = (me ?: MyProfile()).copy(displayName = trimmedName, bio = trimmedBio)) },
            call = { SiteApi.profileSave(displayName = trimmedName, bio = trimmedBio) },
        )
    }

    /** Avatar by pasted image link; byte uploads belong to the site's /me page. */
    fun saveAvatar(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(lastError = "Paste a picture link first")
            return
        }
        mutate(
            failMessage = "Could not save picture",
            apply = { copy(me = (me ?: MyProfile()).copy(avatar = trimmed)) },
            call = { SiteApi.profileAvatarUrl(url = trimmed) },
        )
    }

    /**
     * One optimistic mutation: guard the session, move the list at once, post,
     * and on failure roll back to exactly what the screen showed before with
     * the server's own words as the error. Success re-pulls, so the server
     * stays the source of truth.
     */
    private fun mutate(
        failMessage: String,
        apply: FriendsUiState.() -> FriendsUiState,
        call: suspend () -> SiteCall,
    ) {
        if (!SiteApi.isSignedIn) {
            _uiState.value = _uiState.value.copy(lastError = "Sign in to your CinePiX account to use Social & Friends")
            return
        }
        val before = _uiState.value
        _uiState.value = apply(before.copy(lastError = null))
        scope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = call()
            if (result.ok) {
                _uiState.value = _uiState.value.copy(isSaving = false, lastError = null)
                refresh()
            } else {
                _uiState.value = before.copy(isSaving = false, lastError = result.errorText(failMessage))
            }
        }
    }

    // ---- wire format -----------------------------------------------------------------

    @Serializable
    private data class RemoteMe(
        val displayName: String = "",
        val bio: String = "",
        val avatar: String = "",
        val email: String = "",
    )

    @Serializable
    private data class RemoteFriend(
        val id: String,
        val name: String = "",
        val email: String = "",
        val addedAt: Long = 0,
        val displayName: String = "",
        val avatar: String = "",
    )

    @Serializable
    private data class RemoteGroup(
        val id: String,
        val name: String,
        val description: String = "",
        val memberIds: List<String> = emptyList(),
        val createdAt: Long = 0,
    )

    @Serializable
    private data class RemoteShare(
        val id: String,
        val from: String = "",
        val fromName: String = "",
        val to: String = "",
        val isGroup: Boolean = false,
        val groupName: String = "",
        val metaId: String = "",
        val metaType: String = "",
        val title: String = "",
        val poster: String? = null,
        val at: Long = 0,
        val fromAvatar: String = "",
    )

    @Serializable
    private data class RemoteState(
        val me: RemoteMe? = null,
        val friends: List<RemoteFriend> = emptyList(),
        val groups: List<RemoteGroup> = emptyList(),
        val inbox: List<RemoteShare> = emptyList(),
        val sent: List<RemoteShare> = emptyList(),
    )

    private fun parseState(raw: String?): SocialSnapshot? = raw?.let {
        runCatching { json.decodeFromString<RemoteState>(it) }.getOrNull()
    }?.let { state ->
        val myLabel: (String) -> String = { address ->
            state.friends.find { it.id == address }?.name ?: address
        }
        SocialSnapshot(
            me = state.me?.let {
                MyProfile(displayName = it.displayName, bio = it.bio, avatar = it.avatar, email = it.email)
            },
            friends = state.friends.map {
                FriendItem(
                    id = it.id,
                    name = it.name.ifBlank { it.id },
                    email = it.email.ifBlank { it.id },
                    addedAtEpochMs = it.addedAt,
                    avatar = it.avatar.ifBlank { null },
                    displayName = it.displayName.ifBlank { null },
                )
            },
            groups = state.groups.map {
                GroupItem(id = it.id, name = it.name, description = it.description, memberIds = it.memberIds, createdAtEpochMs = it.createdAt)
            },
            inbox = state.inbox.map {
                SharedContentItem(
                    id = it.id,
                    recipientId = it.from,
                    recipientName = it.fromName.ifBlank { it.from },
                    isGroup = it.isGroup,
                    metaId = it.metaId,
                    metaType = it.metaType,
                    title = it.title,
                    poster = it.poster,
                    timestampEpochMs = it.at,
                    groupName = it.groupName,
                    isIncoming = true,
                    fromAvatar = it.fromAvatar.ifBlank { null },
                )
            },
            sent = state.sent.map {
                SharedContentItem(
                    id = it.id,
                    recipientId = it.to,
                    recipientName = myLabel(it.to),
                    isGroup = it.isGroup,
                    metaId = it.metaId,
                    metaType = it.metaType,
                    title = it.title,
                    poster = it.poster,
                    timestampEpochMs = it.at,
                    groupName = it.groupName,
                    isIncoming = false,
                )
            },
        )
    }
}
