# Android & Alexa Platform Limitations Analysis

This document provides a technical and regulatory analysis of the Android Telecom Framework, the Alexa Skills Kit (ASK) architecture, and the boundaries of this personal project.

---

## 1. Can a third-party Android application answer a normal cellular call?

### Android Evolution & Security Architecture
- **Android 1.0 – 7.1 (API 1 – 25)**:
  There was no public, officially supported API for third-party applications to answer cellular phone calls. Developers frequently relied on hidden Java reflection calls (`com.android.internal.telephony.ITelephony.answerRingingCall()`) or simulating media button events (`KeyEvent.KEYCODE_HEADSETHOOK`). Google explicitly deprecated and blocked these hacks via the Android hidden API blacklist starting in Android 9 (Pie).

- **Android 8.0 (API 26)**:
  Google introduced `TelecomManager.acceptRingingCall()`, requiring the `android.permission.ANSWER_PHONE_CALLS` runtime permission.

- **Android 10+ (API 29 to Android 15/16)**:
  Google heavily tightened background execution limits. While `TelecomManager.acceptRingingCall()` remains in the SDK, Android restricts background apps from answering phone calls without an active user interaction or foreground UI unless the application is registered and granted the **Default Phone Dialer Role** (`RoleManager.ROLE_DIALER`).
  
  Attempting to call `acceptRingingCall()` from a background service on modern Android devices (especially Samsung OneUI, Xiaomi MIUI/HyperOS, and Google Pixel) typically throws a `SecurityException` or is silently dropped by the Telecom framework.

### The Official Solution: `InCallService`
To officially, reliably, and legally answer phone calls on modern Android:
1. The app must declare an `InCallService` in `AndroidManifest.xml` protected by `android.permission.BIND_INCALL_SERVICE`.
2. The user must grant PhoneBridge the **Default Phone app** role via `RoleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)`.
3. When set as default dialer, the Android Telecom framework automatically binds to `PhoneBridgeInCallService` on every incoming call and delivers the `android.telecom.Call` object.
4. PhoneBridge can then safely invoke `call.answer(VideoProfile.STATE_AUDIO_ONLY)`.

---

## 2. Can a third-party Android application reject a call?

- **Android 9.0 (API 28)**:
  `TelecomManager.endCall()` was introduced, but its documentation explicitly states: *"This method is only available to the default phone app or applications with the MODIFY_PHONE_STATE permission (signature/system apps only)."*
- **Android 10+**:
  Calling `telecomManager.endCall()` without the Default Dialer role throws a `SecurityException`.
- **The Official Solution**:
  With `InCallService` active, PhoneBridge invokes `call.reject(false, null)` or `call.disconnect()`, which is 100% legal, officially supported, and Google Play Store compliant.

---

## 3. Graceful Fallback Strategy (When Not Default Dialer)

If the user prefers to keep their stock dialer (e.g., Google Phone, Samsung Phone) as default:
1. PhoneBridge continues to detect incoming calls using `TelephonyCallback` / `TelephonyManager`.
2. Resolves contact name from address book (`ContactsContract.PhoneLookup`).
3. Sends `INCOMING_CALL` event to the backend.
4. Alexa answers: *"You have an incoming call from Rahul."*
5. If the user tells Alexa to answer, PhoneBridge attempts `TelecomManager.acceptRingingCall()`. If blocked by the OS, it returns:
   > *"I couldn't answer the call. Your Android phone requires PhoneBridge to have the required call permissions or phone role."*
6. PhoneBridge displays a high-priority Heads-Up Notification on the phone screen with one-tap Answer/Reject action buttons.

---

## 4. Can Alexa Custom Skills trigger the Android app reliably?

### Real-Time Latency
When the user speaks to Alexa, the command flow is:
1. User: *"Alexa, ask Phone Bridge to answer"*
2. Amazon Echo → AVS (Alexa Voice Service) → Alexa Skill Lambda.
3. Alexa Skill Lambda → HTTPS POST to PhoneBridge Backend (`/api/call/answer`).
4. PhoneBridge Backend → WebSocket message to Android phone (`{"command": "ANSWER_CALL"}`).
5. Android Phone → Telecom API execution → WebSocket ACK back to Backend.
6. Backend responds to Lambda → Echo speaks confirmation.

**Total latency**: Under 250–500 milliseconds on standard Wi-Fi / LTE connections. This is fast enough to answer before the carrier timeout (which is typically 30–45 seconds).

### Android Doze Mode & Background Restrictions
To prevent the Android operating system from putting the network socket to sleep:
- PhoneBridge uses an Android **Foreground Service** (`PhoneBridgeForegroundService`) with an ongoing notification.
- Foreground services maintain network connectivity even when the phone screen is turned off.
- The user should disable "Battery Optimization" for PhoneBridge in Android settings (`Settings > Apps > PhoneBridge > Battery > Unrestricted`).

---

## 5. Can Alexa initiate commands when the skill is not open?

- **Alexa Custom Skills** operate on a request-response model. They are triggered when the user speaks the invocation name: *"Alexa, ask Phone Bridge..."*
- **Proactive Announcements**: Alexa does not automatically speak out loud without being asked unless using the **Alexa Proactive Events API** (which requires skill publication and explicit user permission).
- Therefore, in personal development mode, the natural and supported flow is:
  1. Phone rings.
  2. User: *"Alexa, ask Phone Bridge who is calling"*
  3. Alexa: *"You have an incoming call from Rahul."*
  4. User: *"Alexa, ask Phone Bridge to answer"*

---

## 6. Indian Alexa Accounts & Echo Devices (`en-IN`)

- Echo devices sold in India or registered to Amazon.in use the `en-IN` (English India) locale by default.
- If an Alexa skill only has an `en-US` interaction model, Echo devices configured to `en-IN` will fail to find or invoke the skill with the message: *"I couldn't find a skill named phone bridge."*
- **PhoneBridge includes explicit support for `en-IN`** in `alexa-skill/interaction-model/en-IN.json` and `alexa-skill/lambda/index.js`, alongside `en-US`.
- Both your Amazon Developer account and your Alexa Echo device must be logged into the same Amazon account (or test email).
