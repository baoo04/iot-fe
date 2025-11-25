asf# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android IoT application for real-time heart rate monitoring using Bluetooth Low Energy (BLE) devices with cloud API integration. Built with Kotlin, Jetpack Compose, and MVVM architecture.

**Package**: `com.ptit.iot` (App ID: `com.ptit.heartrate`)  
**Target SDK**: 35, Min SDK: 26 (Android 8.0+)

## Essential Commands

```bash
# Build and install
./gradlew assembleDebug           # Build debug APK
./gradlew installDebug           # Install debug build to device
./gradlew assembleRelease         # Build release APK

# Testing
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumentation tests

# Maintenance
./gradlew clean                  # Clean build artifacts
```

## Architecture

**Pattern**: MVVM with Repository pattern using StateFlow for reactive UI updates.

**Key Flow**: LoginActivity → Authentication → MainActivity → BLE Scanning → Device Connection → Heart Rate Monitoring → API Sync

### Core Components

1. **Authentication System**
   - Entry point: `LoginActivity` with `AuthNavigation`
   - JWT token-based auth via `RemoteModule`
   - ViewModels: `LoginViewModel`, `RegisterViewModel`

2. **Bluetooth Integration**
   - `BluetoothScanner`: BLE device discovery with Heart Rate Service UUID filtering (0x180D)
   - `MainViewModel`: Handles device connection, GATT operations, and heart rate data parsing
   - Uses standard Heart Rate Service Characteristic (0x2A37)

3. **API Integration**
   - `MainApi`: Retrofit interface for authentication and real-time heart rate endpoints
   - Base URL: `https://hearrate-api-996273229755.us-central1.run.app/`
   - `RemoteModule`: Centralized HTTP client with logging, timeouts, and auth headers

4. **State Management**
   - `Resource<T>` sealed class for API response states (Loading, Success, Error)
   - StateFlow for reactive UI updates across ViewModels
   - Coroutines for all async operations

### Required Permissions

The app requires multiple Bluetooth and location permissions for BLE scanning:
- Bluetooth (SCAN, CONNECT, ADMIN)
- Location (FINE, COARSE) - Android requirement for BLE
- Internet, Network State

### Dependencies

- **UI**: Jetpack Compose with Material3 and Navigation
- **Network**: Retrofit2 + OkHttp3 + Gson  
- **Architecture**: Android Lifecycle components + Kotlin Coroutines
- **Bluetooth**: Native Android BLE APIs (no external libraries)

## Development Notes

- Uses Gradle version catalogs (`gradle/libs.versions.toml`) for dependency management
- No custom ProGuard rules beyond defaults
- Firebase integration present (`google-services.json`)
- Entry point is `LoginActivity` (launcher), then navigates to `MainActivity` for heart rate monitoring

## Role
- You're an expert android kotlin jetpack compose developer, has more than 10 years of experience in develop mobile app in domain IOT