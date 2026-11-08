<div align="center">

# 📷 Simple Camera Application

**A modern Android application demonstrating Camera access, runtime permissions, photo capture via FileProvider, orientation correction, state persistence, and Android System Sharing interface using Kotlin and Jetpack Compose.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-2024.2%2B-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white)](https://developer.android.com/studio)
[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-34D399?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)

</div>

---

## 📖 Overview

This repository contains the completed project for the **Mobile Application Development** module assignment. The objective of this project is to build a camera and sharing application utilizing modern Android architecture and Jetpack Compose.

### Required Assignment Features:
1. **Open the device camera**: Launch the default device camera app via Android's camera intent contracts.
2. **Capture a photograph**: Take full-resolution photos and store them securely in the application cache using `FileProvider`.
3. **Display the captured photo**: Render the photograph within a custom Material 3 photo viewport with automatic EXIF orientation correction, high-resolution downsampling, and rotation state preservation.
4. **Share Button**: An animated, tactile share action button enabled dynamically once a photo is captured.
5. **Android System Sharing Interface**: Trigger the native Android share sheet (`Intent.createChooser` with `ClipData` read grants), enabling users to share photos across any available app (e.g., Bluetooth, WhatsApp, Gmail, Telegram, Quick Share).

---

## 🧭 Architecture & Intent Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as 📱 SimpleCameraApp<br/>(Jetpack Compose)
    participant System as ⚙️ Android OS<br/>(Activity Contracts)
    participant CameraApp as 📸 Camera App<br/>(Hardware Interface)
    participant ShareSheet as 📤 System Share Sheet<br/>(Intent Chooser)

    User->>App: Taps "Capture Photo"
    App->>System: Checks Manifest.permission.CAMERA
    alt Permission Not Granted
        App->>System: RequestPermission() contract
        System-->>User: Displays runtime permission prompt
        User->>System: Grants permission
    end
    App->>App: Creates content:// Uri via FileProvider
    App->>System: TakePicture(photoUri) contract
    System->>CameraApp: Opens Camera Viewfinder
    User->>CameraApp: Snaps photo & confirms (✓)
    CameraApp->>App: Writes image to FileProvider URI & returns true
    App->>App: Decodes EXIF orientation, applies downsampling & saves state
    App-->>User: Displays photo with resolution & success badge
    User->>App: Taps "Share Photo"
    App->>System: Intent(ACTION_SEND) + ClipData + createChooser()
    System->>ShareSheet: Displays WhatsApp, Bluetooth, Gmail, etc.
    User->>ShareSheet: Chooses app to send photograph
```

---

## ✨ Features & Highlights

- 📸 **Seamless Camera Access**: Leverages `ActivityResultContracts.TakePicture()` integrated cleanly with modern Jetpack Compose.
- 🔐 **Runtime Permissions**: Robust permission life-cycle checks (`Manifest.permission.CAMERA`) with state banners and toast alerts.
- 📁 **Secure FileProvider Integration**: Uses Android's `FileProvider` to generate compliant `content://` URIs, preventing `FileUriExposedException` on Android 7.0+ (API 24+).
- 🔄 **EXIF Orientation & Memory Efficiency**: Automatically parses image rotation metadata (`ExifInterface`) so captured photos display in the correct upright orientation regardless of hardware sensor position, with memory-safe `inSampleSize` downsampling for high-megapixel sensors.
- 📱 **Rotation State Preservation**: Uses `rememberSaveable` state management and `LaunchedEffect` observers so captured photos persist seamlessly across device screen rotations.
- 📤 **Native System Share Sheet**: Launches `Intent.createChooser()` with `FLAG_GRANT_READ_URI_PERMISSION` and explicit `ClipData` grants, providing one-tap sharing across WhatsApp, Bluetooth, Nearby/Quick Share, Messages, and Gmail.
- 🎨 **Material Design 3 & Tactile Physics**: Styled with custom spring-damping animations, sleek gradients, frosted-glass borders, and reactive button elevation.

---

## 🧠 Key Technical Concepts Explained

### 1. Camera Permissions (Manifest & Runtime)

In modern Android, camera access requires both compile-time declaration in `AndroidManifest.xml` and user confirmation at runtime:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

```kotlin
// In Compose UI:
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    hasCameraPermission = isGranted
    if (isGranted) {
        Toast.makeText(context, "Camera permission granted! ✅", Toast.LENGTH_SHORT).show()
        startCameraCapture()
    }
}
```

### 2. Camera Access via FileProvider

Direct `file://` URIs are restricted across apps. We configure `androidx.core.content.FileProvider` in `AndroidManifest.xml` and `res/xml/file_paths.xml` to generate secure `content://` URIs:

```kotlin
// Creating a secure content:// URI
fun createImageFileUri(context: Context): Uri {
    val photosDir = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
    val photoFile = File(photosDir, "IMG_${timeStamp}.jpg")
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, photoFile)
}
```

### 3. State Persistence & Rotation Handling

Using `rememberSaveable` and `LaunchedEffect` ensures state is preserved across configuration changes such as device rotation:

```kotlin
var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

LaunchedEffect(photoUriString) {
    val uri = photoUriString?.let { Uri.parse(it) }
    photoBitmap = if (uri != null) CameraUtils.loadRotatedBitmap(context, uri) else null
}
```

### 4. Android System Sharing Interface

Clicking the Share button constructs an implicit `Intent(Intent.ACTION_SEND)` wrapped with `Intent.createChooser()` and explicit `ClipData` permissions:

```kotlin
fun sharePhoto(context: Context, photoUri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, photoUri)
        putExtra(Intent.EXTRA_SUBJECT, "Photo from Simple Camera App")
        putExtra(Intent.EXTRA_TEXT, "Captured using Simple Camera Application 📸")
        clipData = android.content.ClipData.newRawUri("photo", photoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, "Share Photo via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooserIntent)
}
```

---

## 📂 Project Structure

```text
simple-camera-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/simplecamera/
│   │   │   │   ├── MainActivity.kt               # Main screen with permission & capture orchestration
│   │   │   │   ├── components/
│   │   │   │   │   ├── AnimatedActionButton.kt   # Spring-physics tactile action buttons
│   │   │   │   │   ├── ConceptBadge.kt           # Course concept pill chips
│   │   │   │   │   └── PhotoPreviewCard.kt       # Card container with photo preview & empty state
│   │   │   │   ├── ui/theme/
│   │   │   │   │   ├── Color.kt                  # Material 3 colors & brand tokens
│   │   │   │   │   ├── Theme.kt                  # Dynamic color theme configuration
│   │   │   │   │   └── Type.kt                   # Typography scale
│   │   │   │   └── utils/
│   │   │   │       └── CameraUtils.kt            # FileProvider, EXIF rotation & Sharing helpers
│   │   │   ├── res/
│   │   │   │   ├── values/                       # strings.xml, colors.xml, themes.xml
│   │   │   │   ├── xml/
│   │   │   │   │   └── file_paths.xml            # FileProvider secure directory permissions
│   │   │   │   └── mipmap/                       # App launcher icons
│   │   │   └── AndroidManifest.xml               # Permissions, FileProvider & Activity declarations
│   │   ├── test/                                 # Local JVM Unit Tests
│   │   └── androidTest/                          # On-device Instrumentation Tests
│   └── build.gradle.kts                          # App-level build configurations & dependencies
├── gradle/
│   ├── libs.versions.toml                        # Version catalog (Compose BOM, Kotlin, AGP)
│   └── wrapper/                                  # Gradle wrapper
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug 2024.2+ recommended)
- **JDK 17 or 21**
- **Android SDK** (Min API: 24, Target/Compile API: 35+)
- Physical Android device (recommended for camera testing) or Android Emulator with webcam emulation enabled.

### Installation & Run Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Rajapakshaminindu/simple-camera-app.git
   ```

2. **Open the project in Android Studio:**
   - Launch Android Studio.
   - Click **Open** and select the cloned `simple-camera-app` directory.
   - Allow Gradle to sync dependencies automatically.

3. **Deploy & Test:**
   - Connect your physical Android phone (enable USB Debugging) or start an Android Emulator.
   - Click **Run ▶** (or press `Shift + F10`).
   - Grant camera permissions when prompted.
   - Tap **Capture Photo** to take a picture.
   - Tap **Share Photo** to open the system share sheet and send your photograph via Bluetooth, WhatsApp, etc.!

---

## 🎯 Learning Outcomes Satisfied

- [x] Implemented explicit and runtime camera permission validation.
- [x] Configured secure file sharing boundaries with `androidx.core.content.FileProvider`.
- [x] Invoked external camera applications using Android Activity Result contracts.
- [x] Corrected raw EXIF orientation data for accurate Jetpack Compose rendering.
- [x] Handled memory optimization (`inSampleSize`) and orientation state persistence (`rememberSaveable`).
- [x] Implemented the Android System Sharing interface (`Intent.ACTION_SEND` and `Intent.createChooser()`).

---


<div align="center">

*Developed for the Mobile Application Development Module*

</div>
