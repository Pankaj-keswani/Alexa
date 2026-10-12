package com.example.phonebridge.data

data class JarvisFeature(
    val id: String,
    val name: String,
    val summary: String,
    val shortCommands: List<String>,
    val hinglishCommands: List<String>,
    val spokenFormat: String,
    val tip: String
)

object JarvisFeatureRegistry {
    val features = listOf(
        JarvisFeature(
            id = PreferencesManager.FEATURE_FIND_PHONE,
            name = "Find My Phone",
            summary = "Triggers maximum volume siren alarm, vibration & strobe on your phone.",
            shortCommands = listOf("find phone", "ring", "where is phone", "stop ring", "stop"),
            hinglishCommands = listOf("phone dhundo", "mera phone dhundo", "phone kahan hai", "ring bajao", "phone chup karo", "ring band karo"),
            spokenFormat = "Alexa, ask my jarvis find phone",
            tip = "Works even if your phone was previously on Silent or Vibrate mode!"
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_FLASHLIGHT,
            name = "Flashlight / Torch",
            summary = "Hands-free camera LED torch control with instant on/off toggles.",
            shortCommands = listOf("torch on", "torch off", "flashlight", "light on", "light off"),
            hinglishCommands = listOf("torch on karo", "torch chalu karo", "light jalao", "torch band karo", "light bujha do"),
            spokenFormat = "Alexa, ask my jarvis torch on",
            tip = "Keep it enabled to easily illuminate your room without touching your phone."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_CALL,
            name = "Incoming Call Management",
            summary = "Inquires caller identity, answers call on speakerphone, or rejects hands-free.",
            shortCommands = listOf("who is calling", "caller", "answer", "pick up", "reject", "cut call"),
            hinglishCommands = listOf("kiska phone hai", "kaun call kar raha hai", "kiska call aa raha hai", "call uthao", "phone pick karo", "call cut karo", "phone kato"),
            spokenFormat = "Alexa, ask my jarvis who is calling",
            tip = "Requires PhoneBridge set as Default Dialer for auto-speakerphone answering."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_MAKE_CALL,
            name = "Outgoing Phone Calls",
            summary = "Dials contacts (Mom, Dad, Boss) or spoken phone numbers with auto-speakerphone.",
            shortCommands = listOf("call mom", "call dad", "call rahul", "dial 9876543210"),
            hinglishCommands = listOf("mummy ko phone lagao", "papa ko call karo", "bhai ko phone lagao", "rahul ko call lagao"),
            spokenFormat = "Alexa, ask my jarvis call mom on speaker",
            tip = "Resolves contact names directly from your Android phone contacts."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_VOLUME,
            name = "Sound Profile & Volume",
            summary = "Adjusts ringer mode (Silent, Vibrate, Normal, Max) or precise volume level.",
            shortCommands = listOf("silent", "mute", "vibrate", "normal", "max volume", "volume 50 percent"),
            hinglishCommands = listOf("phone silent karo", "chup karao", "full volume karo", "awaz badhao", "awaz tez karo"),
            spokenFormat = "Alexa, ask my jarvis put phone on silent",
            tip = "Useful before bedtime or meetings to silence your phone from any Echo device."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_BATTERY,
            name = "Battery & Power Status",
            summary = "Reports real-time battery percentage and charging state hands-free.",
            shortCommands = listOf("battery", "battery status", "charge", "battery level"),
            hinglishCommands = listOf("battery kitni hai", "phone kitna charge hai", "battery batao", "charging ho rahi hai kya"),
            spokenFormat = "Alexa, ask my jarvis battery",
            tip = "Check your phone's charging progress from across the room without picking it up."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_DEVICE_STATS,
            name = "Storage & RAM Specs",
            summary = "Reports internal storage capacity, free gigabytes, and available RAM.",
            shortCommands = listOf("storage", "ram", "memory", "device stats", "phone specs"),
            hinglishCommands = listOf("storage kitni hai", "ram kitni bachi hai", "phone me memory kitni hai", "storage check karo"),
            spokenFormat = "Alexa, ask my jarvis storage",
            tip = "Instant diagnostics directly querying the Android OS StatFs and ActivityManager."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_INTERCOM,
            name = "Voice Intercom / TTS",
            summary = "Broadcasts spoken announcements on phone loudspeaker using Text-To-Speech.",
            shortCommands = listOf("speak hello", "announce dinner", "say time to wake up"),
            hinglishCommands = listOf("phone par bol do khana taiyar hai", "phone me bolo hello", "phone par announce karo"),
            spokenFormat = "Alexa, ask my jarvis announce dinner is ready on phone",
            tip = "Acts as a home intercom — call family members downstairs hands-free."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_WHATSAPP,
            name = "Voice WhatsApp",
            summary = "Prepares and dispatches WhatsApp messages hands-free to any contact.",
            shortCommands = listOf("whatsapp mom", "send whatsapp to rahul", "whatsapp saying running late"),
            hinglishCommands = listOf("mummy ko whatsapp karo", "rahul ko whatsapp bhejo", "whatsapp par bolo"),
            spokenFormat = "Alexa, ask my jarvis send WhatsApp to Rahul",
            tip = "Alexa asks for recipient first, then prompts for the message content in a natural two-turn dialog."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_SMS,
            name = "Voice SMS Text",
            summary = "Sends carrier SMS text messages hands-free through your phone's SIM card.",
            shortCommands = listOf("sms dad", "send text to rahul", "text mom"),
            hinglishCommands = listOf("papa ko sms bhejo", "rahul ko text karo", "mummy ko message karo"),
            spokenFormat = "Alexa, ask my jarvis text Dad",
            tip = "Dispatches directly if SEND_SMS permission is granted, or opens pre-filled SMS composer."
        ),
        JarvisFeature(
            id = PreferencesManager.FEATURE_OPEN_APP,
            name = "App Launcher",
            summary = "Launches YouTube, Spotify, Camera, Settings, Maps, and other apps.",
            shortCommands = listOf("open youtube", "open spotify", "open camera", "open maps", "open settings"),
            hinglishCommands = listOf("youtube kholo", "spotify open karo", "camera chalao", "maps kholo", "insta chalao"),
            spokenFormat = "Alexa, ask my jarvis open YouTube",
            tip = "Matches package names dynamically from your installed applications."
        )
    )

    fun getFeatureById(id: String): JarvisFeature? = features.find { it.id == id }
}
