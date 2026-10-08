# PhoneBridge Privacy & Data Security Policy

PhoneBridge is designed from the ground up as a private, self-hosted personal project. It strictly limits access to user data according to the principle of least privilege.

---

## 1. Zero Contact Scraping
- PhoneBridge **never** uploads, dumps, scans, or synchronizes your address book or contact database.
- The `READ_CONTACTS` permission is utilized solely on-demand when an incoming call arrives: it performs a targeted lookup for the specific ringing phone number via Android's `ContactsContract.PhoneLookup` API to resolve the contact name (e.g. "Rahul").
- If permission is denied, PhoneBridge gracefully falls back to displaying the phone number or "Unknown caller" without crashing or complaining.

---

## 2. No Call Recording or Audio Access
- PhoneBridge does **not** request the `RECORD_AUDIO` or `CAPTURE_AUDIO_OUTPUT` permissions.
- The app cannot listen to, intercept, stream, or record telephone audio.
- Alexa only acts as a remote control trigger for Android's official Telecom framework.

---

## 3. No Third-Party Telemetry or Analytics
- PhoneBridge contains **no** third-party advertising SDKs, tracking libraries, or analytics frameworks (e.g. no Google Analytics, no Facebook SDK, no crashlytics).
- Network requests are sent **strictly** to your own self-hosted backend server URL specified in the app settings.

---

## 4. Secure Transport & Token Isolation
- All communications between Android, your Backend, and Alexa should use HTTPS / WSS.
- Sensitive authentication tokens (`DEVICE_TOKEN`, `ALEXA_CLIENT_SECRET`) are never included in voice responses spoken by Alexa.
- Phone numbers are masked in backend logs (`+91******3210`) to prevent credential and identifier leakage in system journals.
