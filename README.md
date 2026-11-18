# Android Power

An Android-only power and battery manager written in Kotlin with Jetpack Compose.

## Features

- **Battery**: live level, charge status, charger type, health, current (mA), power (W), voltage, temperature, charge counter, estimated capacity and time to full, with a live current chart.
- **Power**: a system optimizer that closes background processes, clears cache and shows memory and storage usage, plus a system check with one-tap fixes. Also shows Battery Saver, Doze, thermal throttling, location saver and screen state, plus recommendations and shortcuts to the system power settings.
- **Apps**: every launchable app with its battery optimization status (Optimized or Unrestricted), with search, filters and a shortcut to each app's settings.
- **Monitor**: a background service with a live status notification, battery history with drain and charge rates, and alerts for charge limit, low battery and high temperature. It restarts automatically after a reboot.
- **Notifications**: control Android Power's own notifications (detailed status, alert sound, quiet hours) and, with notification access, see active notifications from every app, dismiss or snooze them, count notifications per app today and mute noisy apps so their notifications are dismissed automatically.

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17

## Build

```bash
./gradlew assembleDebug
```

GitHub Actions builds the release APK on every push to `main` and uploads them as the `AndroidPower` artifact.
