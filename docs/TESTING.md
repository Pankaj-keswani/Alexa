# PhoneBridge Testing & Verification Guide

This guide walks you through verifying the complete pipeline:
```
Android App ⇄ PhoneBridge Backend ⇄ Amazon Alexa Skill ⇄ Echo Device
```

---

## 1. Quick Verification Matrix

| Step | Component | Action | Verification |
|---|---|---|---|
| **1** | Backend | `npm test` in `backend/` | All 4 unit/integration tests pass |
| **2** | Alexa Skill | `npm test` in `alexa-skill/lambda/` | All 6 intent test cases pass |
| **3** | Android App | Run in Emulator or Device | UI shows "Backend Connected" |
| **4** | Simulation | Tap "Simulate Incoming Call" | State becomes RINGING (Rahul) |
| **5** | Alexa Voice | "Alexa, ask Phone Bridge who is calling" | Alexa speaks: "You have an incoming call from Rahul." |
| **6** | Alexa Answer | "Alexa, ask Phone Bridge to answer" | Alexa speaks: "Okay, I answered the call." |
| **7** | Telecom Real | Physical incoming phone call | Call is announced and controlled |

---

## 2. Testing Phase 1: Test Mode Simulation (Zero Cellular Required)

You do **not** need a second phone or active SIM card to verify that the entire architecture works.

### Step-by-Step Simulated Flow:
1. Start the backend:
   ```bash
   cd backend
   npm start
   ```
2. Open the PhoneBridge Android App.
3. Tap the **Test Mode** tab.
4. Keep the default simulated caller:
   - **Caller Name**: `Rahul`
   - **Phone Number**: `+919876543210`
5. Tap **"Simulate Incoming Call"**.
6. Observe:
   - Android App displays: `Incoming call ringing... From: Rahul`
   - Heads-Up notification appears with Answer/Reject buttons.
   - Developer Logs show:
     ```
     [ANDROID] Incoming call detected: Rahul (+919876543210)
     [BACKEND] Incoming call event sent successfully to backend
     ```
7. Open Alexa Developer Console (or speak to your Echo):
   - *"Alexa, ask Phone Bridge who is calling"*
   - Alexa answers:
     > *"You have an incoming call from Rahul."*
8. Then say:
   - *"Alexa, ask Phone Bridge to answer"*
   - Alexa answers:
     > *"Okay, I answered the call."*
   - Android Developer Logs show:
     ```
     [ALEXA] Command received via backend: ANSWER_CALL
     [TELECOM] Executing Answer attempt via TelecomController
     [TELECOM] Call answered successfully
     ```

---

## 3. Testing Phase 2: Real Cellular Phone Calls

### Prerequisites for Real Calls:
1. Ensure the app has the required runtime permissions:
   - `READ_PHONE_STATE`
   - `READ_CALL_LOG`
   - `READ_CONTACTS` (optional for contact names)
   - `ANSWER_PHONE_CALLS`
2. For programmatic call answering/rejecting on Android 10+:
   - Tap **"Set as Default Phone App"** on the PhoneBridge Dashboard.
   - Accept the Android system role prompt.

### Verification:
1. Place a real phone call to your Android device from another phone (e.g. from a friend or landline).
2. The phone starts ringing.
3. Check the PhoneBridge app:
   - State switches to `RINGING`.
   - Contact name is resolved from your contacts or displays the phone number.
4. Speak to Alexa:
   - *"Alexa, ask Phone Bridge who is calling"*
   - Alexa correctly names the caller.
5. Speak to Alexa:
   - *"Alexa, ask Phone Bridge to answer"*
   - The active cellular call is answered via `InCallService`.

---

## 4. Reading Developer Logs

The **Logs** tab in the Android app records all interactions with color-coded tags:

- `[ANDROID]`: Local Android telephony events, permission changes, service lifecycle.
- `[BACKEND]`: REST API calls, WebSocket connection states, ping/pong heartbeats.
- `[ALEXA]`: Inbound voice commands routed from Alexa through the backend.
- `[TELECOM]`: Execution of Telecom answering/rejecting APIs and OS return codes.

> **Privacy Guarantee**: Tokens, authentication headers, and full phone numbers are never stored in plaintext logs.
