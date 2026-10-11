package com.nuvio.app.features.friends

import kotlinx.serialization.Serializable

/** A friend is a site account: [id]/[email] are their CinePiX address, [name] is *my* label. [avatar]/[displayName] are *their* live profile, resolved by the server on every pull. */
@Serializable
data class FriendItem(
    val id: String,
    val name: String,
    val email: String,
    val addedAtEpochMs: Long,
    val avatar: String? = null,
    val displayName: String? = null,
)

/** What the social endpoint reports about *me* - the profile my friends see. */
@Serializable
data class MyProfile(
    val displayName: String = "",
    val bio: String = "",
    val avatar: String = "",
    val email: String = "",
)

/** Share shortcut. [memberIds] hold friend ids (site accounts), re-checked server-side. */
@Serializable
data class GroupItem(
    val id: String,
    val name: String,
    val description: String,
    val memberIds: List<String> = emptyList(),
    val createdAtEpochMs: Long,
)

/**
 * One row of either feed direction. Outgoing rows describe who received the
 * share ([recipientName]), incoming rows who sent it - [isIncoming] decides.
 */
@Serializable
data class SharedContentItem(
    val id: String,
    val recipientId: String = "",
    val recipientName: String = "",
    val isGroup: Boolean = false,
    val metaId: String,
    val metaType: String,
    val title: String,
    val poster: String? = null,
    val timestampEpochMs: Long,
    val groupName: String = "",
    val isIncoming: Boolean = false,
    /** Sender's avatar on incoming rows; blank on outgoing rows (mine). */
    val fromAvatar: String? = null,
)
