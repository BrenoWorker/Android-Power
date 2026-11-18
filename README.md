# Android Power

An Android-only power and battery manager written in Kotlin with Jetpack Compose.

## Features

- **Battery**: live level, charge status, charger type, health, current (mA), power (W), voltage, temperature, charge counter, estimated capacity and time to full, with a live current chart.
- **Power**: Battery Saver, Doze, thermal throttling, location saver and screen state, plus recommendations and shortcuts to the system power settings.
- **Apps**: every launchable app with its battery optimization status (Optimized or Unrestricted), with search, filters and a shortcut to each app's settings.
- **Monitor**: a background service with a live status notification, battery history with drain and charge rates, and alerts for charge limit, low battery and high temperature. It restarts automatically after a reboot.

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17

## Build

```bash
./gradlew assembleDebug
```

GitHub Actions builds debug and release APKs on every push to `main` and uploads them as the `AndroidPower` artifact.
