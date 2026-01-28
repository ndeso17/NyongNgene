package com.mrksvt.nyongngene.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrksvt.nyongngene.data.repository.LoRaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val loRaRepository: LoRaRepository
) : ViewModel() {

    val isLoRaConnected: StateFlow<Boolean> = loRaRepository.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val snr: StateFlow<Int> = loRaRepository.lastPacketSnr
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}
