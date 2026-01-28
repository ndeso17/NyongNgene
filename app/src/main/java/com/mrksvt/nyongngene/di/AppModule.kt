package com.mrksvt.nyongngene.di

import android.annotation.SuppressLint
import android.content.Context
import com.mrksvt.nyongngene.data.repository.BleRepository
import com.mrksvt.nyongngene.data.repository.LoRaRepository
import com.mrksvt.nyongngene.data.repository.RealBleRepository
import com.mrksvt.nyongngene.data.local.AppDatabase
import com.mrksvt.nyongngene.data.local.MessageEntity
import com.mrksvt.nyongngene.data.local.PeerEntity
import com.mrksvt.nyongngene.data.repository.RealLoRaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

// Mock Implementations (TEMPORARY: Will use Real implementations soon)
class MockLoRaRepository : LoRaRepository {
    override val isConnected = MutableStateFlow(true)
    override val lastPacketSnr = MutableStateFlow(-10)
    override fun sendBroadcast(data: ByteArray) {
        println("LoRa TX: ${data.size} bytes")
    }
}

class MockBleRepository : BleRepository {
    private val _peers = MutableStateFlow(
        listOf(
            PeerEntity("00:11:22:33:44:55", "Node A", -65, System.currentTimeMillis()),
            PeerEntity("AA:BB:CC:DD:EE:FF", "Node B", -80, System.currentTimeMillis())
        )
    )
    override val nearbyPeers: Flow<List<PeerEntity>> = _peers
    
    private val _messages = MutableStateFlow<List<MessageEntity>>(
        listOf(
             MessageEntity(
                 id = "0", 
                 senderId = "System", 
                 senderName = "System", 
                 content = "Welcome to BLE Chat", 
                 timestamp = System.currentTimeMillis()
             )
        )
    )
    override val messages: Flow<List<MessageEntity>> = _messages
    
    override val activeChannels: Flow<List<com.mrksvt.nyongngene.data.local.ChannelProfile>> = MutableStateFlow(
        listOf(com.mrksvt.nyongngene.data.local.ChannelProfile("00:11:22:33:44:55", "Mock Node"))
    )

    override suspend fun sendMessage(to: String, message: String, isBroadcast: Boolean): Long {
        println("BLE TX to $to: $message")
        return 45L // Mock latency
    }
    
    override suspend fun saveMessage(message: MessageEntity) {
        // Mock save
    }
    
    override fun start() {
        // Mock start
    }
    
    override fun refreshPeers() {
        // Mock refresh
    }
}

// Manual DI Container
@SuppressLint("StaticFieldLeak") // Application Context is safe here
object AppModule {
    private lateinit var context: Context

    fun provideContext(appContext: Context) {
        context = appContext
    }

    private val database: AppDatabase by lazy {
        androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nyongngene.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val loRaRepository: LoRaRepository by lazy { 
        RealLoRaRepository(context)
    }
    
    val bleRepository: BleRepository by lazy { 
        RealBleRepository(context, database.messageDao())
    }
}
