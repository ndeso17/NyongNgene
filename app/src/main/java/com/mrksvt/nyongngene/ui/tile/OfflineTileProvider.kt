package com.mrksvt.nyongngene.ui.tile

import android.content.Context
import android.graphics.drawable.Drawable
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.TileWriter
import java.io.File

/**
 * Custom Tile Provider to load exploded tiles from application private storage.
 * Structure: /maps/{mapId}/tiles/{z}/{x}/{y}.png
 */
class OfflineTileProvider(
    private val mapId: String, 
    private val context: Context,
    tileSource: ITileSource
) : MapTileProviderArray(tileSource, null) {

    init {
        // Add a custom module provider that reads from our specific folder
        val mapsDir = File(context.getExternalFilesDir(null), "maps")
        val customModule = object : MapTileModuleProviderBase(
            1, 15 // min/max threads
        ) {
            override fun getTileLoader(): TileLoader {
                return object : TileLoader() {
                    override fun loadTile(pTile: Long): Drawable? {
                        // MapTileIndex is used in newer osmdroid versions
                        val zoom = org.osmdroid.util.MapTileIndex.getZoom(pTile)
                        val x = org.osmdroid.util.MapTileIndex.getX(pTile)
                        val y = org.osmdroid.util.MapTileIndex.getY(pTile)
                        
                        val tileFile = File(mapsDir, "$mapId/tiles/$zoom/$x/$y.png")
                        
                        return if (tileFile.exists()) {
                            try {
                                Drawable.createFromPath(tileFile.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }
            
            override fun getUsesDataConnection(): Boolean = false
            override fun getMinimumZoomLevel(): Int = tileSource.minimumZoomLevel
            override fun getMaximumZoomLevel(): Int = tileSource.maximumZoomLevel
            override fun setTileSource(tileSource: ITileSource?) {
                // No-op
            }
            
            override fun getName(): String = "OfflineTileModule"
            override fun getThreadGroupName(): String = "OfflineTileModule"
        }
        
        mTileProviderList.add(customModule)
    }
}
