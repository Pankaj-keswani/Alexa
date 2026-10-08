# Android App Setup & Deployment Guide

This guide explains step-by-step how to open, build, and run the **PhoneBridge Alexa** Android app.

---

## 1. Prerequisites

- **Android Studio**: Iguana (2023.2.1), Jellyfish (2024.1.1), Ladybug (2024.2.1), or newer.
- **Java Development Kit (JDK)**: JDK 17 or JDK 22 (configured in Android Studio).
- **Android Device or Emulator**: Android 8.0 (API 26) or higher. An Android device running Android 10+ is recommended to test RoleManager.
- **USB Debugging** enabled on your physical phone (if testing with a physical device).

---

## 2. Opening the Project

1. Launch Android Studio.
2. Select **Open** (or `File > Open...`).
3. Navigate to and select the `android/` directory inside this repository:
   ```
   e:\Code\Projects\alexa\android
   ```
4. Click **OK**.
5. Wait for Android Studio to sync the Gradle project dependencies.

---

## 3. Explaining Requested Permissions

The app requests only permissions strictly necessary for call bridging:

| Permission | Reason |
|---|---|
| `INTERNET` | Communicates with the PhoneBridge Node.js backend REST API and maintains WebSocket connection. |
| `ACCESS_NETWORK_STATE` | Monitors device Wi-Fi/cellular connection state to reconnect if dropped. |
| `READ_PHONE_STATE` | Detects telephony call events (incoming ringing, call active/answered, call ended). |
| `READ_CALL_LOG` | Required on Android 9+ (API 28+) to read incoming phone numbers during calls. |
| `READ_CONTACTS` | Resolves the caller's display name (e.g. "Rahul") from your local address book. Never uploaded or synced. |
| `ANSWER_PHONE_CALLS` | Official Telecom permission for `TelecomManager.acceptRingingCall()`. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_PHONE_CALL` | Keeps the WebSocket and call listener alive in the background with a persistent notification. |
| `POST_NOTIFICATIONS` | Displays the incoming call heads-up notification and quick action buttons on Android 13+. |

---

## 4. Granting the Default Phone Dialer Role (Optional but Recommended)

On Android 10+ (API 29+), third-party background apps cannot answer or reject cellular calls unless granted the Default Phone Role (`RoleManager.ROLE_DIALER`).

### How to Enable:
1. Open the PhoneBridge app.
2. On the main Dashboard, locate the **Role Status** banner.
3. Tap **"Set as Default Phone App"**.
4. An Android system dialog will appear: Select **PhoneBridge** and tap **Set as default**.
5. The status banner will turn green: **"Default Phone Role Active"**.

> **Note**: If you do not wish to set PhoneBridge as your default dialer, you can still use **Test Mode** or use PhoneBridge to announce incoming callers on Alexa, while answering via your normal phone UI!

---

## 5. Configuring the App Settings

1. Tap the **Settings** tab in the bottom navigation bar.
2. **Backend URL**:
   - If using the **Android Emulator**: `http://10.0.2.2:3000`
   - If using a **Physical Device on the same Wi-Fi**: `http://<YOUR_COMPUTER_LOCAL_IP>:3000` (e.g., `http://192.168.1.15:3000`)
   - If using an **HTTPS Tunnel**: `https://<YOUR-TUNNEL>.ngrok-free.app`
3. **Device Authentication Token**:
   - Must match `DEVICE_TOKEN` in `backend/.env` (default: `dev_token_secret_123`).
4. **Device ID**:
   - Unique identifier (e.g. `phone-pixel7` or auto-generated).
5. Tap **Save & Reconnect**.

---

## 6. Building via Command Line (Alternative)

If you prefer building without Android Studio:

```bash
cd android
.\gradlew.bat assembleDebug
```

The compiled APK will be created at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

Install to connected device via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
