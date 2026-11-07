package com.example.simplecamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.simplecamera.components.AnimatedActionButton
import com.example.simplecamera.components.ConceptBadge
import com.example.simplecamera.components.PhotoPreviewCard
import com.example.simplecamera.ui.theme.CameraPrimary
import com.example.simplecamera.ui.theme.CameraSecondary
import com.example.simplecamera.ui.theme.RetakeAccent
import com.example.simplecamera.ui.theme.ShareAccent
import com.example.simplecamera.ui.theme.SimpleCameraAppTheme
import com.example.simplecamera.ui.theme.WarningRed
import com.example.simplecamera.utils.CameraUtils

/**
 * Mobile Application Development – Assignment
 * Simple Camera Application using Android Studio (Kotlin & Jetpack Compose).
 *
 * Implements:
 * 1. Open the device camera
 * 2. Capture a photograph
 * 3. Display the captured photo in the application
 * 4. Share button
 * 5. Android System Sharing interface (WhatsApp, Bluetooth, etc.)
 *
 * Core Concepts Demonstrated:
 * - Camera permissions (Manifest + runtime ActivityResultContracts.RequestPermission)
 * - Camera access (FileProvider + ActivityResultContracts.TakePicture)
 * - Intents (Implicit Camera Intent & Implicit Share Intent)
 * - Android Sharing interface (Intent.createChooser with Intent.ACTION_SEND)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCameraAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraAppScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraAppScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // -------------------------------------------------------------
    // Application State (survives screen rotation via rememberSaveable)
    // -------------------------------------------------------------
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoUri = photoUriString?.let { Uri.parse(it) }
    val pendingCameraUri = pendingCameraUriString?.let { Uri.parse(it) }

    // Reload bitmap whenever photoUriString changes or on screen rotation
    LaunchedEffect(photoUriString) {
        val uri = photoUri
        if (uri != null) {
            photoBitmap = CameraUtils.loadRotatedBitmap(context, uri)
        } else {
            photoBitmap = null
        }
    }

    // Permission tracking state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    // -------------------------------------------------------------
    // Activity Result Launchers (Compose-native Intent Contracts)
    // -------------------------------------------------------------

    // 2. Camera Capture Launcher (Takes picture and saves to FileProvider URI)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val currentPendingUri = pendingCameraUri
        if (success && currentPendingUri != null) {
            photoUriString = currentPendingUri.toString()
            pendingCameraUriString = null
            Toast.makeText(context, "Photo captured successfully! 📸", Toast.LENGTH_SHORT).show()
        } else {
            pendingCameraUriString = null
            Toast.makeText(context, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to start picture taking with a new FileProvider URI
    fun startCameraCapture() {
        val newUri = CameraUtils.createImageFileUri(context)
        pendingCameraUriString = newUri.toString()
        takePictureLauncher.launch(newUri)
    }

    // 1. Camera Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            permissionDeniedMessage = null
            Toast.makeText(context, "Camera permission granted! ✅", Toast.LENGTH_SHORT).show()
            startCameraCapture()
        } else {
            permissionDeniedMessage = "Camera permission is required to capture photos."
            Toast.makeText(context, "Camera permission denied ❌", Toast.LENGTH_SHORT).show()
        }
    }

    // Trigger capture flow helper
    fun launchCameraFlow() {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCameraCapture()
        }
    }

    // -------------------------------------------------------------
    // Main UI Layout with Deep Slate / Cyber Cyan Gradient
    // -------------------------------------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D16), // Deep obsidian
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            )
            .then(modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header Pill Badge
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasCameraPermission) ShareAccent else WarningRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasCameraPermission) "Camera Ready" else "Permission Needed",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Title
            Text(
                text = "Simple Camera App",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Capture photographs and share them across apps using Android Intents & System Sharing.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Key Course Concepts Badges
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConceptBadge(label = "Camera Permissions", dotColor = CameraPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                ConceptBadge(label = "Camera Access", dotColor = CameraSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                ConceptBadge(label = "Intents", dotColor = RetakeAccent)
                Spacer(modifier = Modifier.width(6.dp))
                ConceptBadge(label = "Android Sharing", dotColor = ShareAccent)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Permission Warning Card (if permission not granted)
            AnimatedVisibility(
                visible = !hasCameraPermission,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Warning",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Camera Permission Missing",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap 'Grant Camera Permission' or 'Capture Photo' to prompt the Android system dialog.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Central Photo Preview Card
            PhotoPreviewCard(
                photoBitmap = photoBitmap,
                onClearPhoto = {
                    photoUriString = null
                    pendingCameraUriString = null
                    Toast.makeText(context, "Photo cleared", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // -------------------------------------------------------------
            // Action Buttons Section
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Permission Request Button (only visible if permission is not granted)
                if (!hasCameraPermission) {
                    AnimatedActionButton(
                        text = "Grant Camera Permission",
                        icon = Icons.Default.Lock,
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        containerColor = Color(0xFFE11D48),
                        pressedColor = Color(0xFFBE123C),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 1. Capture / Retake Photo Button
                AnimatedActionButton(
                    text = if (photoBitmap == null) "Capture Photo" else "Retake Photo",
                    icon = if (photoBitmap == null) Icons.Default.CameraAlt else Icons.Default.Refresh,
                    onClick = { launchCameraFlow() },
                    containerColor = if (photoBitmap == null) CameraPrimary else RetakeAccent,
                    pressedColor = if (photoBitmap == null) Color(0xFF0891B2) else Color(0xFFD97706),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Share Photo Button (Android System Sharing Interface)
                AnimatedActionButton(
                    text = "Share Photo",
                    icon = Icons.Default.Share,
                    enabled = photoUri != null,
                    onClick = {
                        val currentUri = photoUri
                        if (currentUri != null) {
                            CameraUtils.sharePhoto(context, currentUri)
                        } else {
                            Toast.makeText(context, "Capture a photo before sharing!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = ShareAccent,
                    pressedColor = Color(0xFF059669),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer note
            Text(
                text = "Sharing uses Intent.ACTION_SEND with Intent.createChooser() to allow sharing across Bluetooth, WhatsApp, etc.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
