package com.mrksvt.nyongngene.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isSent: Boolean = false,
    val isBroadcast: Boolean = true,
    val channelId: String = "BROADCAST", // "BROADCAST" or PeerAddress
    val latencyMs: Long? = null
)

data class ChannelProfile(
    val channelId: String,
    val displayName: String
)
