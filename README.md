# Airplane Mode Control

A concise Android application for controlling Airplane Mode, supporting two control methods: `WRITE_SECURE_SETTINGS` permission and Digital Assistant permission.

For detailed documentation and policy analysis, please visit: https://powerli2002.github.io/posts/appairplane/

## Features

### Core Functions

- **Manual Control**: Toggle Airplane Mode on/off with a single tap.
- **Smart Toggle**: Checks the current state before toggling.
- **Timed Toggle**: Turns on Airplane Mode → waits 2 seconds → turns off Airplane Mode.
- **Automatic Toggle**: Automatically toggles Airplane Mode at a set interval.
- **Permission Check**: Automatically detects and prompts for permission configuration.

### Control Methods

#### 1\. WRITE\_SECURE\_SETTINGS Permission Control

The most direct method. It controls system settings directly after granting the `WRITE_SECURE_SETTINGS` permission via ADB.

**Authorization Command:**

```shell
adb shell pm grant com.example.airplanecontrol android.permission.WRITE_SECURE_SETTINGS
```

**Characteristics:**

- The most direct control with the fastest response time.
- Performs well on the Android Studio emulator.
- May have compatibility issues on real devices.

#### 2\. Digital Assistant Permission Control

This method bypasses system restrictions to control Airplane Mode by leveraging the Android Digital Assistant permission.

**Configuration Steps:**

1.  Open the app and tap the "Configure Permissions" button.
2.  Tap "Open Assistant Settings".
3.  Select "Airplane Mode Control Assistant" as the default digital assistant app.
4.  Return to the app; the permission status should now show as configured.

**Characteristics:**

- Better compatibility, suitable for real devices.
- Uses a system-level permission to control Airplane Mode.
- Configuration is slightly more complex, but it offers high stability.

## Automation Implementation

This is achieved using a workaround: a background service launches a transparent Activity, which then uses the Activity's `showAssist` method to trigger the digital assistant to control Airplane Mode.

### Key Technical Points

1.  **Transparent Activity**: An Activity with no visible UI, used solely to trigger `showAssist`.
2.  **Lifecycle Control**: The `showAssist` method is triggered within the `onWindowFocusChanged` callback.
3.  **Task Stack Management**: Uses a separate task stack to avoid returning to the main UI.
4.  **Background Service**: A foreground service is used to ensure the toggle operation is executed reliably.

## Project Structure

```
app/src/main/java/com/example/airplanecontrol/
├── ui/
│   ├── AirplaneModeActivity.java      # Main UI Activity
│   └── TransparentActivity.java       # Transparent Activity
├── services/
│   ├── AutoTaskService.java           # Automatic Task Service
│   ├── MyInteractionService.java      # Digital Assistant Interaction Service
│   └── MyInteractionSessionService.java # Session Service
├── utils/
│   └── AirplaneModeUtils.java         # Utility class
├── AirplaneControlApplication.java    # Main Application class
├── AppLifecycleObserver.java          # Lifecycle Observer
└── BootCompletedReceiver.java         # Boot Completed Receiver
```

## Permissions Explained

Permissions required by the application:

- `BIND_VOICE_INTERACTION`: For Digital Assistant functionality.
- `WRITE_SETTINGS`: To write to system settings.
- `WRITE_SECURE_SETTINGS`: To write to secure system settings (optional, granted via ADB).
- `FOREGROUND_SERVICE`: To run foreground services.
- `POST_NOTIFICATIONS`: To post notifications.
- `FOREGROUND_SERVICE_DATA_SYNC`: For data synchronization in a foreground service.

## Build and Run

### Environment Requirements

- Android Studio
- minSdkVersion: 24
- targetSdkVersion: 35
- Java 11

### Build Steps

1.  Clone the project.
2.  Open it in Android Studio.
3.  Wait for the Gradle sync to complete.
4.  Connect a device or start an emulator.
5.  Click the "Run" button.

### Permission Configuration

Configure permissions according to your chosen control method:

**Method 1: ADB Grant (Recommended for development/testing)**

```shell
adb shell pm grant com.example.airplanecontrol android.permission.WRITE_SECURE_SETTINGS
```

**Method 2: Digital Assistant Configuration**

1.  Install and open the application.
2.  Tap "Configure Permissions".
3.  Follow the prompts to set the app as the default digital assistant.

## Disclaimer

This project is for learning and research purposes only. Please comply with all relevant laws and regulations.