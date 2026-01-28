package com.mrksvt.nyongngene.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrksvt.nyongngene.ui.viewmodel.AppViewModelFactory
import com.mrksvt.nyongngene.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    deepLinkChannelId: String? = null,
    viewModel: ChatViewModel = viewModel(factory = AppViewModelFactory.Factory)
) {
    val activeChannel by viewModel.activeChannel.collectAsState()
    
    // Handle deep link
    LaunchedEffect(deepLinkChannelId) {
        if (deepLinkChannelId != null) {
            viewModel.openChannel(deepLinkChannelId)
        }
    }
    
    // Back handler to close channel if open
    androidx.activity.compose.BackHandler(enabled = activeChannel != null) {
        viewModel.closeChannel()
    }

    if (activeChannel == null) {
        ContactListScreen(viewModel)
    } else {
        ConversationScreen(viewModel, activeChannel!!)
    }
}

@Composable
fun ContactListScreen(viewModel: ChatViewModel) {
    val peers by viewModel.peers.collectAsState()
    var showOfflinePeers by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Active peers have non-zero signal (from actual scan)
    val activePeers = peers.filter { it.rssi != 0 }
    val offlinePeers = peers.filter { it.rssi == 0 }
    val displayPeers = if (showOfflinePeers) peers else activePeers
    
    fun doRefresh() {
        if (!isRefreshing) {
            isRefreshing = true
            viewModel.refreshPeers()
            coroutineScope.launch {
                delay(2000) // Give time for scan
                isRefreshing = false
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
             modifier = Modifier.fillMaxWidth().padding(16.dp),
             colors = androidx.compose.material3.CardDefaults.cardColors(
                 containerColor = MaterialTheme.colorScheme.primaryContainer
             )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NyongNgene Chat", style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${activePeers.size} Peers Nearby", style = MaterialTheme.typography.bodyMedium)
                        if (isRefreshing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.padding(start = 8.dp).size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                IconButton(onClick = { doRefresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) androidx.compose.ui.graphics.Color.Gray else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text("Channels", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
            }
            // Broadcast Channel
            item {
                PeerItem(
                    name = "📢 Broadcast Channel",
                    details = "Public Room (Everyone)",
                    isSelected = false,
                    onClick = { viewModel.openChannel("BROADCAST") }
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Direct Messages (Peers)", style = MaterialTheme.typography.labelMedium)
                    if (offlinePeers.isNotEmpty()) {
                        TextButton(onClick = { showOfflinePeers = !showOfflinePeers }) {
                            Text(
                                if (showOfflinePeers) "Hide Offline (${offlinePeers.size})" else "Show Offline (${offlinePeers.size})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            
            if (displayPeers.isEmpty()) {
                item {
                    Text("No nearby peers found.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                }
            } else {
                items(displayPeers) { peer ->
                    PeerItem(
                        name = peer.name,
                        details = if (peer.rssi == 0) "Offline • ${peer.address}" else "${peer.rssi} dBm • ${peer.address}",
                        isSelected = false,
                        onClick = { viewModel.openChannel(peer.name) }  // Open by NAME, not address!
                    )
                }
            }
        }
    }
}
@Composable
fun ConversationScreen(viewModel: ChatViewModel, channelId: String) {
    val messages by viewModel.currentMessages.collectAsState(initial = emptyList())
    val peers by viewModel.peers.collectAsState()
    var messageText by remember { mutableStateOf("") }
    
    // channelId is now the peer name, so just use it directly
    val title = if (channelId == "BROADCAST") "Broadcast Channel" else channelId
    val headerColor = if (channelId == "BROADCAST") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = headerColor),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeChannel() }) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    if (channelId == "BROADCAST") {
                         Text("${peers.size} peers listening", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
             verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
        ) {
            items(messages) { msg ->
                MessageItem(msg)
            }
        }

        // Input
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") }
            )
            IconButton(
                onClick = { 
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageItem(msg: com.mrksvt.nyongngene.data.local.MessageEntity) {
    val align = if (msg.isSent) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isSent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val timeString = timeFormat.format(java.util.Date(msg.timestamp))

    Column(horizontalAlignment = align, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (!msg.isSent) {
             Text(text = msg.senderName, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
        }
        
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Column {
                    Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                        Text(text = timeString, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
                        if (msg.latencyMs != null) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(2.dp))
                            Text(text = "✓ ${msg.latencyMs}ms", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerItem(name: String, details: String, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            Text(text = details, style = MaterialTheme.typography.bodySmall)
        }
    }
}
