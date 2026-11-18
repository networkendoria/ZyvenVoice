# ZYVEN VOICE

Android voice-processing prototype built with Kotlin and Jetpack Compose.

## Features in this build
- White and purple interface
- Deep Voice and Custom Voice profiles
- Microphone permission and in-app live audio monitor
- Adjustable gain and low-frequency emphasis while monitoring

## Android limitation
This app does **not** provide a system-wide virtual microphone and cannot replace the microphone input used by Discord, WhatsApp, games, or other apps on a standard non-rooted Android device. Android does not expose a normal third-party API for injecting processed microphone PCM into another app's microphone capture path.

The processed audio is played inside this app only. Use headphones to reduce acoustic feedback. Deep Voice in this prototype is a basic tone adjustment, not a professional pitch-shifting voice changer.

## Build
The GitHub Actions workflow builds a debug APK on every push to the main branch. Open the Actions tab and download the `ZyvenVoice-debug-apk` artifact from a successful run.
