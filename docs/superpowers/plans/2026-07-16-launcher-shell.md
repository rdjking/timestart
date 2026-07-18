# Launcher Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a launchable Compose home shell that exposes the notification-permission entry point.

**Architecture:** `MainActivity` owns Android lifecycle and permission refresh. `HomeScreenState` is a small pure UI model and `HomeScreen` renders it with Material 3; this keeps permission presentation testable without coupling UI tests to Android system dialogs.

**Tech Stack:** Kotlin, AndroidX Activity Compose 1.12.1, Compose BOM 2026.06.01, Material 3, JUnit 4.

## Global Constraints

- `minSdk=26`, `compileSdk=36`, `targetSdk=36`.
- Do not request notification permission from any receiver or automatically on entry.
- Do not state that a third-party app launch request means it is visible in the foreground.

---

### Task 1: Home state and shell

**Files:**
- Create: `app/src/main/java/com/example/timestart/ui/home/HomeScreenState.kt`
- Create: `app/src/main/java/com/example/timestart/ui/home/HomeScreen.kt`
- Create: `app/src/test/java/com/example/timestart/ui/home/HomeScreenStateTest.kt`

- [x] Write a failing unit test that expects `HomeScreenState.from(requiresRuntimePermission = true, hasNotificationPermission = false).showNotificationPermissionCard` to be true and the same value to be false after permission is granted.
- [x] Run `:app:compileDebugUnitTestKotlin --offline --no-daemon`; it must fail because `HomeScreenState` is absent.
- [x] Implement the immutable state model and render title, empty state, notification explanation and a user-triggered permission button.
- [x] Re-run the compilation command and verify it succeeds.

### Task 2: Android activity integration

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/timestart/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [x] Enable Compose and add Activity Compose plus Compose BOM/Material 3 dependencies.
- [x] Implement `MainActivity` with `setContent`, refresh permission state in `onResume`, and call `NotificationPermission.requestFrom(this)` only from the visible button callback.
- [x] Declare `MainActivity` as the exported `MAIN`/`LAUNCHER` activity.
- [x] Run `:app:assembleDebug --no-daemon` and verify the APK is produced.
