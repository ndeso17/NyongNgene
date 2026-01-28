package com.mrksvt.nyongngene.data.repository

import com.mrksvt.nyongngene.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

interface LoRaRepository {
    val isConnected: Flow<Boolean>
    val lastPacketSnr: Flow<Int>
    fun sendBroadcast(data: ByteArray)
}

interface BleRepository {
    val nearbyPeers: Flow<List<com.mrksvt.nyongngene.data.local.PeerEntity>>
    val messages: Flow<List<MessageEntity>>
    val activeChannels: Flow<List<com.mrksvt.nyongngene.data.local.ChannelProfile>>
    suspend fun sendMessage(to: String, message: String, isBroadcast: Boolean): Long // Returns latency in ms
    suspend fun saveMessage(message: MessageEntity)
    fun start()
    fun refreshPeers()  // Restart BLE scan to refresh peer list
}
