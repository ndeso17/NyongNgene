package com.mrksvt.nyongngene.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

// Data Classes
data class RouteStep(
    val instruction: String,
    val maneuverType: String,
    val modifier: String?,
    val location: GeoPoint,
    val distance: Double,
    val bearingAfter: Int
)

data class RouteData(
    val path: List<GeoPoint>,
    val steps: List<RouteStep>,
    val totalDistance: Double,
    val totalDuration: Double
)

data class CameraLens(
    val cameraId: String,
    val label: String,
    val focalLength: Float
)

enum class TravelMode(val profile: String, val label: String) {
    FOOT("foot", "Jalan Kaki"),
    BIKE("bike", "Motor"),
    CAR("car", "Mobil")
}

enum class JourneyDirection(val label: String) {
    TO_SUMMIT("Menuju Puncak 🏔️"),
    RETURNING("Pulang 🏠")
}

enum class NavigationStatus {
    ON_ROUTE,
    BACKTRACKING,
    OFF_ROUTE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingScreen(
    mapId: String = "",
    onBack: () -> Unit = {},
    onSosClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var destinationPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var routeData by remember { mutableStateOf<RouteData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastMessage by remember { mutableStateOf("") }
    var compassDegree by remember { mutableFloatStateOf(0f) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    // Travel Mode
    var travelMode by remember { mutableStateOf(TravelMode.FOOT) }
    var showTravelModeMenu by remember { mutableStateOf(false) }
    
    // Journey Direction
    var journeyDirection by remember { mutableStateOf(JourneyDirection.TO_SUMMIT) }
    
    // Camera Lenses
    var cameraLenses by remember { mutableStateOf<List<CameraLens>>(emptyList()) }
    var selectedLensIndex by remember { mutableIntStateOf(0) }
    
    // Navigation State
    var currentInstruction by remember { mutableStateOf("Mencari rute...") }
    var nextStepIndex by remember { mutableIntStateOf(0) }
    var targetBearing by remember { mutableFloatStateOf(0f) }
    
    // Coordinate-based tracking
    val visitedCoordinates = remember { mutableStateListOf<GeoPoint>() }
    var navigationStatus by remember { mutableStateOf(NavigationStatus.ON_ROUTE) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Live Stats
    var liveSpeed by remember { mutableFloatStateOf(0f) }
    var totalDistanceTraveled by remember { mutableFloatStateOf(0f) }
    var startTime by remember { mutableLongStateOf(0L) }
    var lastLocation by remember { mutableStateOf<GeoPoint?>(null) }
    
    // TTS
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var lastSpokenWarning by remember { mutableLongStateOf(0L) }
    
    // Initialize
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
            }
        }
        startTime = System.currentTimeMillis()
        
        // Enumerate camera lenses
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val lenses = mutableListOf<CameraLens>()
            
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    if (focalLengths != null && focalLengths.isNotEmpty()) {
                        val focal = focalLengths[0]
                        // Common focal length mapping for smartphones
                        val label = when {
                            focal < 2.0f -> "0.5x"
                            focal < 3.0f -> "0.6x"
                            focal < 4.5f -> "1x"
                            focal < 7.0f -> "2x"
                            focal < 12.0f -> "3x"
                            else -> "5x+"
                        }
                        lenses.add(CameraLens(cameraId, label, focal))
                        Log.d("CameraLens", "Found: $cameraId, focal: $focal, label: $label")
                    }
                }
            }
            
            cameraLenses = lenses.sortedBy { it.focalLength }
            Log.d("CameraLens", "Total lenses: ${cameraLenses.size}")
            
            // Default to 1x if available
            val idx = cameraLenses.indexOfFirst { it.label == "1x" }
            if (idx >= 0) selectedLensIndex = idx
            
        } catch (e: Exception) { 
            Log.e("CameraLens", "Error: ${e.message}")
            e.printStackTrace() 
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { tts?.shutdown() }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }
    
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    val trailName = remember(mapId) {
        mapId.replace("_v1", "").replace("_v2", "").replace("_", " ").replaceFirstChar { it.uppercase() }
    }
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.getExternalFilesDir(null)
            osmdroidTileCache = java.io.File(context.getExternalFilesDir(null), "tiles")
        }
    }
    
    // Location Updates with coordinate tracking
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val newLocation = GeoPoint(location.latitude, location.longitude)
                
                if (startPoint == null) startPoint = newLocation
                
                lastLocation?.let { last ->
                    totalDistanceTraveled += last.distanceToAsDouble(newLocation).toFloat()
                }
                lastLocation = newLocation
                
                // Add to visited coordinates (every 10 meters)
                val shouldRecord = visitedCoordinates.isEmpty() || 
                    visitedCoordinates.last().distanceToAsDouble(newLocation) > 10
                if (shouldRecord) {
                    visitedCoordinates.add(newLocation)
                    // Keep only last 500 points to save memory
                    if (visitedCoordinates.size > 500) {
                        visitedCoordinates.removeAt(0)
                    }
                }
                
                liveSpeed = location.speed
                userLocation = newLocation
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, locationListener)
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { 
                    userLocation = GeoPoint(it.latitude, it.longitude)
                    lastLocation = userLocation
                    if (startPoint == null) startPoint = userLocation
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        onDispose { locationManager.removeUpdates(locationListener) }
    }
    
    // Compass
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        @Suppress("DEPRECATION")
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) { event?.let { compassDegree = it.values[0] } }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Geocode
    LaunchedEffect(mapId) {
        isLoading = true
        try {
            val dest = withContext(Dispatchers.IO) { geocodeLocation("$trailName Indonesia") }
            destinationPoint = dest ?: GeoPoint(-7.242, 109.208)
        } catch (e: Exception) { destinationPoint = GeoPoint(-7.242, 109.208) }
        isLoading = false
    }
    
    // Route
    LaunchedEffect(userLocation, destinationPoint, travelMode, journeyDirection) {
        if (userLocation != null && destinationPoint != null) {
            try {
                val (routeStart, routeEnd) = if (journeyDirection == JourneyDirection.TO_SUMMIT) {
                    userLocation!! to destinationPoint!!
                } else {
                    userLocation!! to (startPoint ?: userLocation!!)
                }
                
                routeData = withContext(Dispatchers.IO) { getRoute(routeStart, routeEnd, travelMode) }
                if (routeData != null && routeData!!.steps.isNotEmpty()) {
                    nextStepIndex = 0
                    val step = routeData!!.steps[0]
                    currentInstruction = buildInstructionWithDistance(step)
                    tts?.speak(currentInstruction, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    // Navigation Logic with coordinate-based detection
    LaunchedEffect(userLocation, routeData) {
        val user = userLocation
        val data = routeData
        
        if (user != null && data != null && data.steps.isNotEmpty()) {
            // Check if user is on route (within 50m of any route point)
            val minDistToRoute = data.path.minOfOrNull { it.distanceToAsDouble(user) } ?: Double.MAX_VALUE
            
            // Check if user returned to previously visited coordinate
            val isRevisiting = visitedCoordinates.dropLast(10).any { prev ->
                prev.distanceToAsDouble(user) < 15 // Within 15m of a previous position
            }
            
            navigationStatus = when {
                isRevisiting -> NavigationStatus.BACKTRACKING
                minDistToRoute > 10 -> NavigationStatus.OFF_ROUTE
                else -> NavigationStatus.ON_ROUTE
            }
            
            // Set warning message and speak
            val now = System.currentTimeMillis()
            when (navigationStatus) {
                NavigationStatus.BACKTRACKING -> {
                    statusMessage = "⚠️ BERBALIK ARAH! Anda kembali ke jalur yang sudah dilalui!"
                    if (now - lastSpokenWarning > 10000) { // Speak every 10 seconds max
                        tts?.speak("Peringatan! Anda kembali ke jalur yang sudah dilalui", TextToSpeech.QUEUE_ADD, null, null)
                        lastSpokenWarning = now
                    }
                }
                NavigationStatus.OFF_ROUTE -> {
                    statusMessage = "❌ TERSESAT! Anda melenceng ${minDistToRoute.toInt()}m dari jalur!"
                    if (now - lastSpokenWarning > 10000) {
                        tts?.speak("Peringatan! Anda melenceng dari jalur resmi", TextToSpeech.QUEUE_ADD, null, null)
                        lastSpokenWarning = now
                    }
                }
                NavigationStatus.ON_ROUTE -> {
                    statusMessage = ""
                }
            }
            
            // Update next step
            if (nextStepIndex < data.steps.size) {
                val nextStep = data.steps[nextStepIndex]
                val distToStep = user.distanceToAsDouble(nextStep.location)
                
                targetBearing = bearingTo(user.latitude, user.longitude, nextStep.location.latitude, nextStep.location.longitude)
                
                // Build instruction with distance
                currentInstruction = "Dalam ${formatDistance(distToStep)}, ${nextStep.instruction}"
                
                if (distToStep < 20) {
                    if (nextStepIndex + 1 < data.steps.size) {
                        nextStepIndex++
                        val newStep = data.steps[nextStepIndex]
                        val newInstruction = buildInstructionWithDistance(newStep)
                        currentInstruction = newInstruction
                        tts?.speak(newStep.instruction, TextToSpeech.QUEUE_FLUSH, null, null)
                    } else {
                        currentInstruction = if (journeyDirection == JourneyDirection.TO_SUMMIT) "🏔️ Anda sampai di puncak!" else "🏠 Anda sampai di titik awal!"
                        tts?.speak(currentInstruction, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
        }
    }
    
    // Stats
    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
    val avgSpeed = if (elapsedSeconds > 0) totalDistanceTraveled / elapsedSeconds else 0.0
    val remainingDistance = routeData?.totalDistance?.minus(totalDistanceTraveled)?.coerceAtLeast(0.0) ?: 0.0
    val etaSeconds = if (avgSpeed > 0.5) remainingDistance / avgSpeed else routeData?.totalDuration ?: 0.0

    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Broadcast") },
            text = { OutlinedTextField(value = broadcastMessage, onValueChange = { broadcastMessage = it }, label = { Text("Pesan") }) },
            confirmButton = { TextButton(onClick = { showBroadcastDialog = false }) { Text("Kirim") } },
            dismissButton = { TextButton(onClick = { showBroadcastDialog = false }) { Text("Batal") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top: 2D Map
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    TopMapView(userLocation, destinationPoint, routeData?.path ?: emptyList(), trailName)
                    
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.White, CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
                    }
                    
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { 
                                journeyDirection = if (journeyDirection == JourneyDirection.TO_SUMMIT) JourneyDirection.RETURNING else JourneyDirection.TO_SUMMIT
                                routeData = null
                            },
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = 0.9f))
                        ) {
                            Text(journeyDirection.label, color = Color.Black, fontSize = 10.sp)
                        }
                        
                        Box {
                            TextButton(
                                onClick = { showTravelModeMenu = true },
                                colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = 0.9f))
                            ) {
                                Text(travelMode.label, color = Color.Black, fontSize = 10.sp)
                            }
                            DropdownMenu(expanded = showTravelModeMenu, onDismissRequest = { showTravelModeMenu = false }) {
                                TravelMode.values().forEach { mode ->
                                    DropdownMenuItem(text = { Text(mode.label) }, onClick = { travelMode = mode; showTravelModeMenu = false; routeData = null })
                                }
                            }
                        }
                    }
                }
                
                // Bottom: AR View
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Camera with selected lens
                    if (hasCameraPermission) {
                        val lensId = cameraLenses.getOrNull(selectedLensIndex)?.cameraId ?: ""
                        CameraPreviewWithLens(cameraId = lensId, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                            Text("Izin Kamera Diperlukan", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    
                    Terrain3DWebView(
                        latitude = destinationPoint?.latitude ?: -7.242,
                        longitude = destinationPoint?.longitude ?: 109.208,
                        trailName = trailName,
                        userLat = userLocation?.latitude,
                        userLon = userLocation?.longitude,
                        compassBearing = compassDegree
                    )
                    
                    // AR Overlay
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Column(modifier = Modifier.align(Alignment.TopCenter)) {
                            // Status Warning
                            if (statusMessage.isNotEmpty()) {
                                OutlinedText(
                                    text = statusMessage, 
                                    fontSize = 14, 
                                    fontWeight = FontWeight.Bold, 
                                    textColor = if (navigationStatus == NavigationStatus.BACKTRACKING) Color.Yellow else Color.Red
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            // Main Instruction with distance
                            OutlinedText(text = currentInstruction, fontSize = 16, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stats Grid
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                StatItem("Total", formatDistance(routeData?.totalDistance ?: 0.0))
                                StatItem("Tersisa", formatDistance(remainingDistance))
                                StatItem("ETA", formatDuration(etaSeconds))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                StatItem("Kecepatan", formatSpeed(liveSpeed))
                                StatItem("Rata-rata", formatSpeed(avgSpeed.toFloat()))
                            }
                        }
                        
                        // AR Arrow
                        val arrowColor = when (navigationStatus) {
                            NavigationStatus.BACKTRACKING -> Color.Yellow
                            NavigationStatus.OFF_ROUTE -> Color.Red
                            NavigationStatus.ON_ROUTE -> Color.Cyan
                        }
                        val arrowRotation = targetBearing - compassDegree
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Direction",
                            tint = arrowColor,
                            modifier = Modifier.align(Alignment.Center).size(80.dp).rotate(arrowRotation)
                        )
                        
                        // Camera Lens Selector
                        if (cameraLenses.size > 1) {
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cameraLenses.forEachIndexed { index, lens ->
                                    val isSelected = index == selectedLensIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.6f))
                                            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                                            .clickable { selectedLensIndex = index }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = lens.label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            FloatingActionButton(onClick = onSosClick, containerColor = Color.Red, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(48.dp)) {
                Icon(Icons.Default.Warning, "SOS", tint = Color.White)
            }
            FloatingActionButton(onClick = { showBroadcastDialog = true }, containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, "Broadcast", tint = Color.White)
            }
        }
    }
}

@Composable
fun OutlinedText(text: String, fontSize: Int = 16, fontWeight: FontWeight = FontWeight.Normal, textColor: Color = Color.White) {
    Box {
        Text(text = text, fontSize = fontSize.sp, fontWeight = fontWeight, color = Color.Black, modifier = Modifier.offset(1.dp, 1.dp))
        Text(text = text, fontSize = fontSize.sp, fontWeight = fontWeight, color = Color.Black, modifier = Modifier.offset((-1).dp, (-1).dp))
        Text(text = text, fontSize = fontSize.sp, fontWeight = fontWeight, color = Color.Black, modifier = Modifier.offset(1.dp, (-1).dp))
        Text(text = text, fontSize = fontSize.sp, fontWeight = fontWeight, color = Color.Black, modifier = Modifier.offset((-1).dp, 1.dp))
        Text(text = text, fontSize = fontSize.sp, fontWeight = fontWeight, color = textColor)
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedText(text = value, fontSize = 14, fontWeight = FontWeight.Bold)
        OutlinedText(text = label, fontSize = 9)
    }
}

@Composable
fun CameraPreviewWithLens(cameraId: String, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    // Key by cameraId to recreate when lens changes
    key(cameraId) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = modifier,
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        // Find matching camera by ID
                        val availableCameras = cameraProvider.availableCameraInfos
                        val targetCamera = availableCameras.find { info ->
                            info.toString().contains(cameraId) || cameraId.isEmpty()
                        }
                        
                        if (targetCamera != null && cameraId.isNotEmpty()) {
                            val selector = CameraSelector.Builder()
                                .addCameraFilter { cameras -> cameras.filter { it == targetCamera } }
                                .build()
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                        } else {
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                        }
                    } catch (e: Exception) {
                        Log.e("Camera", "Bind failed: ${e.message}")
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                        } catch (e2: Exception) { e2.printStackTrace() }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Terrain3DWebView(latitude: Double, longitude: Double, trailName: String, userLat: Double? = null, userLon: Double? = null, compassBearing: Float) {
    val htmlContent = remember(latitude, longitude, userLat, userLon) {
        val userMarkerJs = if (userLat != null && userLon != null) {
            "L.marker([$userLat, $userLon], {icon: L.divIcon({html: '<div style=\"background:#0F0;width:12px;height:12px;border-radius:50%;border:2px solid #fff\"></div>', iconSize: [16, 16]})}).addTo(map);"
        } else ""
        
        """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css"/>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
        <style>body,html{margin:0;padding:0;background:transparent!important}.map-c{width:100vw;height:100vh;perspective:800px;overflow:hidden}#map{width:100%;height:100%;background:transparent!important;transform:rotateX(60deg) scale(1.5);transform-origin:center}.leaflet-container{background:transparent!important}</style>
        </head><body><div class="map-c"><div id="map"></div></div>
        <script>if(typeof L!=='undefined'){var map=L.map('map',{zoomControl:false,attributionControl:false,dragging:false}).setView([$latitude,$longitude],15);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,opacity:0.7}).addTo(map);L.marker([$latitude,$longitude],{icon:L.divIcon({html:'<div style="background:#F00;width:16px;height:16px;border-radius:50%;border:2px solid #fff"></div>',iconSize:[20,20]})}).addTo(map);$userMarkerJs;window.rotateMap=function(b){document.getElementById('map').style.transform='rotateX(60deg) rotateZ('+(-b)+'deg) scale(1.5)'}}</script>
        </body></html>
        """.trimIndent()
    }
    
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(AndroidColor.TRANSPARENT)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = WebViewClient()
        }
    }
    
    LaunchedEffect(compassBearing) { webView.evaluateJavascript("if(window.rotateMap)window.rotateMap($compassBearing);", null) }
    LaunchedEffect(htmlContent) { webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null) }
    
    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun TopMapView(userLocation: GeoPoint?, destinationPoint: GeoPoint?, routePoints: List<GeoPoint>, trailName: String) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); controller.setZoom(13.0) }
    }
    
    LaunchedEffect(userLocation, destinationPoint, routePoints) {
        mapView.overlays.clear()
        userLocation?.let { mapView.controller.setCenter(it); mapView.overlays.add(Marker(mapView).apply { position = it; title = "Anda" }) }
        destinationPoint?.let { mapView.overlays.add(Marker(mapView).apply { position = it; title = trailName }) }
        if (routePoints.isNotEmpty()) mapView.overlays.add(Polyline().apply { setPoints(routePoints); outlinePaint.color = AndroidColor.BLUE; outlinePaint.strokeWidth = 6f })
        mapView.invalidate()
    }
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

// Helper Functions
private fun formatDistance(meters: Double): String = if (meters >= 1000) String.format("%.1f km", meters / 1000) else String.format("%.0f m", meters)
private fun formatDuration(seconds: Double): String { val h = (seconds / 3600).toInt(); val m = ((seconds % 3600) / 60).toInt(); return if (h > 0) "${h}j ${m}m" else "${m} menit" }
private fun formatSpeed(mps: Float): String = String.format("%.1f km/j", mps * 3.6f)

private fun buildInstructionWithDistance(step: RouteStep): String {
    val distText = formatDistance(step.distance)
    return when (step.maneuverType) {
        "depart" -> "Mulai perjalanan, lurus sepanjang $distText"
        "arrive" -> "Sampai tujuan"
        else -> "Dalam $distText, ${step.instruction}"
    }
}

private fun geocodeLocation(query: String): GeoPoint? {
    val url = "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(query, "UTF-8")}&format=json&limit=1"
    val conn = URL(url).openConnection(); conn.setRequestProperty("User-Agent", "NyongNgene/1.0")
    val json = JSONArray(conn.getInputStream().bufferedReader().readText())
    return if (json.length() > 0) json.getJSONObject(0).let { GeoPoint(it.getDouble("lat"), it.getDouble("lon")) } else null
}

private fun getRoute(start: GeoPoint, end: GeoPoint, mode: TravelMode): RouteData? {
    try {
        val profile = mode.profile
        val url = "https://router.project-osrm.org/route/v1/$profile/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?geometries=geojson&overview=full&steps=true"
        val conn = URL(url).openConnection(); conn.setRequestProperty("User-Agent", "NyongNgene/1.0")
        val json = JSONObject(conn.getInputStream().bufferedReader().readText())
        
        if (json.getString("code") == "Ok") {
            val route = json.getJSONArray("routes").getJSONObject(0)
            val path = route.getJSONObject("geometry").getJSONArray("coordinates").let { coords ->
                (0 until coords.length()).map { coords.getJSONArray(it).let { c -> GeoPoint(c.getDouble(1), c.getDouble(0)) } }
            }
            val steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps").let { arr ->
                (0 until arr.length()).map { i ->
                    val s = arr.getJSONObject(i); val m = s.getJSONObject("maneuver"); val loc = m.getJSONArray("location")
                    val mod = if (m.has("modifier")) m.getString("modifier") else null
                    val name = if (s.has("name") && s.getString("name").isNotEmpty()) s.getString("name") else null
                    RouteStep(buildDirection(m.getString("type"), mod, name), m.getString("type"), mod, GeoPoint(loc.getDouble(1), loc.getDouble(0)), s.getDouble("distance"), if (m.has("bearing_after")) m.getInt("bearing_after") else 0)
                }
            }
            return RouteData(path, steps, route.getDouble("distance"), route.getDouble("duration"))
        }
    } catch (e: Exception) { e.printStackTrace() }
    return null
}

private fun buildDirection(type: String, mod: String?, street: String?): String {
    val dir = when (mod) { "left" -> "belok kiri"; "right" -> "belok kanan"; "sharp left" -> "belok tajam kiri"; "sharp right" -> "belok tajam kanan"; "slight left" -> "agak kiri"; "slight right" -> "agak kanan"; "straight" -> "lurus"; "uturn" -> "putar balik"; else -> null }
    val act = when (type) { "depart" -> "Mulai"; "arrive" -> "Sampai"; "turn" -> dir?.replaceFirstChar { it.uppercase() } ?: "Belok"; "new name" -> "Lanjut"; "roundabout" -> "Bundaran, $dir"; "fork" -> "Percabangan, $dir"; "end of road" -> "Akhir jalan, $dir"; else -> dir?.replaceFirstChar { it.uppercase() } ?: "Lurus" }
    return if (street != null && type != "depart" && type != "arrive") "$act ke $street" else act
}

private fun bearingTo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) - sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return (((Math.toDegrees(atan2(y, x))) + 360) % 360).toFloat()
}
