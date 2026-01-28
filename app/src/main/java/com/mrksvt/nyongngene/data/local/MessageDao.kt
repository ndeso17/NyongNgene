package com.mrksvt.nyongngene.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT channelId, senderName as displayName FROM messages WHERE channelId != 'BROADCAST' AND senderId != 'Me' GROUP BY channelId")
    fun getChannelProfiles(): Flow<List<ChannelProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
}
