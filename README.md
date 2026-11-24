# VOID PLAYER

Standalone Android local-music-player starter project.

## Included
- Kotlin + Jetpack Compose.
- Violet/purple glass-inspired Home, Search, and Library screens.
- Local audio discovery through Android MediaStore.
- VOID PLAYER package ID: `com.voidplayer.app`.
- The exact user-provided VOID logo wired as Android launcher icon resources.
- GitHub Actions workflow to build a debug APK.

## Current implementation status
This is a starter foundation, not a finished music player yet. The current UI can scan and display local audio. Actual audio playback, MediaSession background playback and lock-screen controls, persistent favorites/playlists, artwork extraction, Telegram bot shortcut, Arabic/English localization, and full device testing remain to be implemented.

## Build
Requires JDK 17 and Android SDK 35. GitHub Actions workflow builds `app-debug.apk`.
