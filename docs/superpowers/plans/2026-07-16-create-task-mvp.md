# Create Task MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create and schedule daily, weekday, or weekend launcher tasks from the app.

**Architecture:** Repository computes the durable next trigger; a platform provider supplies launchable apps; ViewModel mediates UI state and persistence; Compose renders the editor.

**Tech Stack:** Kotlin, Room, Coroutines, PackageManager, platform TimePickerDialog, Compose Material 3, JUnit 4.

## Global Constraints

- Query only `MAIN`/`LAUNCHER`; do not add `QUERY_ALL_PACKAGES`.
- Persist UTC epoch milliseconds and use `NextTriggerCalculator`.
- Run Gradle with `JAVA_HOME=D:\develop\AndroidStudio\jbr`.

---

### Task 1: Durable save scheduling

- [x] Add a failing repository test for a daily task saved without `nextTriggerAt`.
- [x] Verify it fails because the task remains unscheduled.
- [x] Calculate and persist the next trigger before scheduling.
- [x] Re-run the test successfully.

### Task 2: Editor and launcher-app source

- [x] Add `LaunchableApp` and a PackageManager provider restricted to Launcher intents.
- [x] Add `CreateTaskViewModel` to load apps and save selected daily/weekday/weekend rules.
- [x] Add Compose editor and replace the home create placeholder with navigation state.
- [x] Build Debug APK and run the repository test.
