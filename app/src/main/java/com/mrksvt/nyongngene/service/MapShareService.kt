package com.mrksvt.nyongngene.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.application.call
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapShareService : Service() {

    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_SERVER) {
            val mapId = intent.getStringExtra(EXTRA_MAP_ID)
            if (mapId != null) {
                startServer(mapId)
            }
        } else if (action == ACTION_STOP_SERVER) {
            stopServer()
        }
        return START_NOT_STICKY
    }

    private fun startServer(mapId: String) {
        if (server != null) return

        scope.launch {
            try {
                // Locate the map bundle (assuming zip for sharing, or we share the dir?)
                // For simplicity as per plan "Download zip", we assume a zip exists or we zip it.
                // Here we just expose a dummy file or the manifest for proving concept.
                
                val mapsDir = File(getExternalFilesDir(null), "maps")
                val targetFile = File(mapsDir, "$mapId.zip") // Determine what to share
                
                server = embeddedServer(Netty, port = 8080) {
                    routing {
                        get("/download/map") {
                            if (targetFile.exists()) {
                                call.respondFile(targetFile)
                            } else {
                                call.respondText("Map bundle not found", status = io.ktor.http.HttpStatusCode.NotFound)
                            }
                        }
                        get("/status") {
                            call.respondText("READY")
                        }
                    }
                }.start(wait = false)
                
                println("MapShareService: Server started on port 8080")
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        stopSelf()
    }

    companion object {
        const val ACTION_START_SERVER = "com.mrksvt.nyongngene.service.START_SERVER"
        const val ACTION_STOP_SERVER = "com.mrksvt.nyongngene.service.STOP_SERVER"
        const val EXTRA_MAP_ID = "map_id"
    }
}
