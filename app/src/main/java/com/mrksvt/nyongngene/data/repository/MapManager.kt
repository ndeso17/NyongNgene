package com.mrksvt.nyongngene.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class MapManifest(
    val id: String,
    val mountain: String,
    val trail: String,
    val version: String,
    val minZoom: Int,
    val maxZoom: Int,
    val checksum: String
)

class MapManager(private val context: Context) {
    private val mapsDir = File(context.getExternalFilesDir(null), "maps")
    private val _availableMaps = MutableStateFlow<List<MapManifest>>(emptyList())
    val availableMaps = _availableMaps.asStateFlow()

    suspend fun scanLocalMaps() = withContext(Dispatchers.IO) {
        if (!mapsDir.exists()) mapsDir.mkdirs()
        
        val foundMaps = mutableListOf<MapManifest>()
        mapsDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val manifestFile = File(dir, "manifest.json")
                if (manifestFile.exists()) {
                    try {
                        val manifest = parseManifest(manifestFile)
                        // In real app, validate checksum here
                        foundMaps.add(manifest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        _availableMaps.value = foundMaps
    }

    private fun parseManifest(file: File): MapManifest {
        val json = JSONObject(file.readText())
        return MapManifest(
            id = json.getString("map_id"),
            mountain = json.getString("mountain"),
            trail = json.getString("trail"),
            version = json.getString("version"),
            minZoom = json.optInt("min_zoom", 12),
            maxZoom = json.optInt("max_zoom", 17),
            checksum = json.optString("checksum", "")
        )
    }
}
