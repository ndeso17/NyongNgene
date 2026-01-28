package com.mrksvt.nyongngene.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.mrksvt.nyongngene.di.AppModule
import com.mrksvt.nyongngene.ui.viewmodel.MainViewModel

object AppViewModelFactory {
    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return when (modelClass) {
                MainViewModel::class.java -> MainViewModel(AppModule.loRaRepository) as T
                ChatViewModel::class.java -> ChatViewModel(AppModule.bleRepository) as T
                else -> throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
