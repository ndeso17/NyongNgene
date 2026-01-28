package com.mrksvt.nyongngene.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrksvt.nyongngene.data.repository.BleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val bleRepository: BleRepository
) : ViewModel() {

    private val _scannedPeers: StateFlow<List<com.mrksvt.nyongngene.data.local.PeerEntity>> = bleRepository.nearbyPeers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val peers: kotlinx.coroutines.flow.StateFlow<List<com.mrksvt.nyongngene.data.local.PeerEntity>> = kotlinx.coroutines.flow.combine(
        _scannedPeers,
        bleRepository.activeChannels
    ) { scanned, historyChannels ->
        val scannedMap = scanned.associateBy { it.address }
        val allPeers = scanned.toMutableList()
        
        historyChannels.forEach { profile ->
            if (profile.channelId != "BROADCAST" && !scannedMap.containsKey(profile.channelId)) {
                allPeers.add(
                    com.mrksvt.nyongngene.data.local.PeerEntity(
                        address = profile.channelId,
                        name = profile.displayName, // Use saved name!
                        rssi = 0,
                        lastSeen = 0L
                    )
                )
            }
        }
        allPeers.sortedBy { it.name.lowercase() }  // Sort alphabetically by name
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Channel: null = Contact List, "BROADCAST" = Public, Address = Private
    private val _activeChannel = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val activeChannel: StateFlow<String?> = _activeChannel

    private val _allMessages = kotlinx.coroutines.flow.MutableStateFlow<List<com.mrksvt.nyongngene.data.local.MessageEntity>>(emptyList())

    // Computed flow for current channel messages
    val currentMessages: kotlinx.coroutines.flow.Flow<List<com.mrksvt.nyongngene.data.local.MessageEntity>> = kotlinx.coroutines.flow.combine(
        _allMessages, 
        _activeChannel
    ) { msgs, channel ->
        if (channel == null) emptyList()
        else msgs.filter { it.channelId == channel || (channel == "BROADCAST" && it.isBroadcast) }
             .sortedBy { it.timestamp }
    }

    init {
        bleRepository.start()
        
        viewModelScope.launch {
            bleRepository.messages.collect { msgList ->
                _allMessages.value = msgList
            }
        }
    }

    fun openChannel(channelId: String) {
        _activeChannel.value = channelId
    }

    fun closeChannel() {
        _activeChannel.value = null
    }
    
    fun refreshPeers() {
        bleRepository.refreshPeers()
    }

    fun sendMessage(msg: String) {
        val channel = _activeChannel.value ?: return
        
        if (channel == "BROADCAST") {
             broadcastMessage(msg)
        } else {
             // Private Chat - channel is the peer name
             // Find peer address for BLE connection
             val peer = peers.value.find { it.name == channel }
             val peerAddress = peer?.address ?: channel // fallback if name is an address
             sendPrivateMessage(peerAddress, channel, msg)
        }
    }

    private fun sendPrivateMessage(toAddress: String, peerName: String, msg: String) {
        viewModelScope.launch {
            try {
                val latency = bleRepository.sendMessage(toAddress, msg, isBroadcast = false)
                // Add self message with latency - use NAME as channelId for consistency
                val selfMsg = com.mrksvt.nyongngene.data.local.MessageEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    senderId = "Me",
                    senderName = "Me",
                    content = msg,
                    timestamp = System.currentTimeMillis(),
                    isSent = true,
                    isBroadcast = false,
                    channelId = peerName, // Use NAME as channelId, not address!
                    latencyMs = latency
                )
                 // Save self message
                 bleRepository.saveMessage(selfMsg)
            } catch (e: Exception) {
                 // Save failed message so user sees feedback
                 val failedMsg = com.mrksvt.nyongngene.data.local.MessageEntity(
                     id = java.util.UUID.randomUUID().toString(),
                     senderId = "Me",
                     senderName = "Me",
                     content = "⚠️ Failed: $msg",
                     timestamp = System.currentTimeMillis(),
                     isSent = false, // Mark as NOT sent
                     isBroadcast = false,
                     channelId = peerName, // Use NAME as channelId
                     latencyMs = null
                 )
                 bleRepository.saveMessage(failedMsg)
            }
        }
    }

    private fun broadcastMessage(msg: String) {
        val currentPeers = peers.value
        // Only broadcast to ACTIVE peers (non-zero rssi) and deduplicate by name
        val activePeers = currentPeers.filter { it.rssi != 0 }
        val uniquePeersByName = activePeers.distinctBy { it.name }
        
        if (uniquePeersByName.isNotEmpty()) {
            uniquePeersByName.forEach { peer ->
                // Send to each with isBroadcast=true flag
                viewModelScope.launch {
                    try {
                        bleRepository.sendMessage(peer.address, msg, isBroadcast = true)
                    } catch (e: Exception) {}
                }
            }
        }
        
        // Save to DB
        val selfMsg = com.mrksvt.nyongngene.data.local.MessageEntity(
             id = java.util.UUID.randomUUID().toString(),
             senderId = "Me",
             senderName = "Me",
             content = msg,
             timestamp = System.currentTimeMillis(),
             isSent = true,
             isBroadcast = true,
             channelId = "BROADCAST"
        )
        viewModelScope.launch {
            bleRepository.saveMessage(selfMsg)
        }
    }
}
