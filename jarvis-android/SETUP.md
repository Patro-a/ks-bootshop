# JARVIS Android — Setup Guide

## Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| Android SDK | API 26+ (minSdk) / API 34 (targetSdk) |
| Kotlin | 1.9.x (bundled with AGP) |
| JDK | 17 (bundled with Android Studio) |

---

## 1. Clone & open the project

Open **`jarvis-android/`** as a project in Android Studio.

---

## 2. Add your API keys

Copy the example file and fill in your keys:

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
sdk.dir=/path/to/your/android/sdk        # Android Studio sets this automatically

openai.api.key=sk-...                    # https://platform.openai.com/api-keys
elevenlabs.api.key=...                   # https://elevenlabs.io → Profile → API Keys
```

> **Never commit `local.properties`** — it is in `.gitignore`.

---

## 3. Generate Gradle wrapper (first time only)

If the `gradlew` script is missing, generate it:

```bash
cd jarvis-android
gradle wrapper --gradle-version 8.4
```

Or just open Android Studio — it will download the wrapper automatically.

---

## 4. Build & run

Press **Run ▶** in Android Studio, or:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. First launch

1. Grant **Microphone** permission when prompted.
2. Tap the blue mic button to start listening.
3. Speak naturally — JARVIS will think, then respond in voice.
4. Say "Hey Jarvis" as a prefix or just speak directly after tapping.

---

## 6. Choosing a voice

Tap the mic icon in the toolbar → select from three ElevenLabs voices:

| Voice | ID | Character |
|-------|----|-----------|
| Adam | `pNInz6obpgDQGcFmaJgB` | Deep, authoritative |
| Antoni | `ErXwobaYiN019PkySvjV` | Warm, friendly |
| Rachel | `21m00Tcm4TlvDq8ikWAM` | Clear, professional |

To add more voices, find the voice ID on ElevenLabs and add it to `ElevenLabsService.kt`.

---

## 7. Built-in commands (no AI call needed)

| Say | Action |
|-----|--------|
| "Open WhatsApp / YouTube / Spotify / Instagram / Maps / Settings" | Launches the app |
| "Set alarm for 7 am" | Creates alarm via system clock |
| "Search for climate change" | Google web search |
| "Open browser" | Opens Chrome/default browser |

Everything else goes to GPT-4o-mini for a natural spoken reply.

---

## 8. Background mode

Start `JarvisService` from your code to keep the app alive when minimized:

```kotlin
startForegroundService(Intent(this, JarvisService::class.java))
```

A persistent notification will appear. Tap it to return to the app.

---

## Architecture overview

```
MainActivity
    └── MainViewModel (MVVM)
            ├── VoiceManager      — Android SpeechRecognizer
            ├── AIService         — OpenAI GPT-4o-mini (last 10 messages context)
            ├── ElevenLabsService — TTS via ElevenLabs API + MediaPlayer
            └── CommandHandler    — Device commands (apps, alarms, search)
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "Speech recognition not available" | Ensure Google app is installed; test on a real device not emulator |
| No audio after AI reply | Check ElevenLabs API key in `local.properties`; check logcat for HTTP errors |
| AI response is empty | Check OpenAI API key and account quota |
| App crashes on first run | Grant microphone permission; check minSdk is 26+ |
