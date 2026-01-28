package com.mrksvt.nyongngene

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mrksvt.nyongngene.ui.MainScreen
import com.mrksvt.nyongngene.ui.theme.NyongNgeneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val enableBluetoothLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { }

        fun checkAndEnableBluetooth() {
            val bluetoothManager = getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter?.isEnabled == false) {
                 val enableBtIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                 enableBluetoothLauncher.launch(enableBtIntent)
            }
        }

        val permissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            checkAndEnableBluetooth()
            // Retry repository start now that we have permissions
            com.mrksvt.nyongngene.di.AppModule.bleRepository.start()
        }
        
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        permissionLauncher.launch(permissions)

        setContent {
            NyongNgeneTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val activity = context as? Activity
                    val intent = activity?.intent
                    
                    val deepLinkChannel = intent?.getStringExtra("EXTRA_CHANNEL_ID")
                    
                    // Clear intent extra to avoid reopening on rotation
                    if (deepLinkChannel != null) {
                        intent.removeExtra("EXTRA_CHANNEL_ID")
                    }
                    
                    MainScreen(deepLinkChannelId = deepLinkChannel)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
