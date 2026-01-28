package com.mrksvt.nyongngene.data.local

data class PeerEntity(
    val address: String,
    val name: String,
    val rssi: Int,
    val lastSeen: Long
)
