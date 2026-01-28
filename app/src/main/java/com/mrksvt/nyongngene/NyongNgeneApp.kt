package com.mrksvt.nyongngene

import android.app.Application
import com.mrksvt.nyongngene.di.AppModule

class NyongNgeneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.provideContext(this)
        // Initialize LoRa / BLE services here later
    }
}
