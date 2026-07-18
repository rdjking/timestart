# Home Task List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render persisted tasks on the Compose home screen and toggle their enabled state through the repository.

**Architecture:** DAO exposes a Room Flow for every task; Repository preserves the data boundary; ViewModel owns collection and IO writes. `HomeTaskItem` is a pure presentation mapper consumed by Compose.

**Tech Stack:** Kotlin, Room Flow, Coroutines, AndroidX Lifecycle ViewModel 2.9.4, Compose Material 3, JUnit 4.

## Global Constraints

- Preserve disabled tasks in the UI; only the scheduler decides alarm registration.
- Display a launch request as a request, never as proof a third-party app is foregrounded.
- Run Gradle with `JAVA_HOME=D:\develop\AndroidStudio\jbr`.

---

### Task 1: Reactive task data and presentation mapping

**Files:**
- Modify: `app/src/main/java/com/example/timestart/data/local/TaskDao.kt`
- Modify: `app/src/main/java/com/example/timestart/data/repository/TaskRepository.kt`
- Create: `app/src/main/java/com/example/timestart/ui/home/HomeTaskItem.kt`
- Create: `app/src/test/java/com/example/timestart/ui/home/HomeTaskItemTest.kt`

- [x] Write a failing test mapping a daily 18:05 `TaskEntity` to an enabled item with `18:05` and `每天`.
- [x] Run the targeted test and observe the missing mapper failure.
- [x] Add DAO/Repository Flow and the task presentation mapper.
- [x] Run the targeted test and verify it passes.

### Task 2: Home ViewModel and Compose list

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/timestart/TimeStartApplication.kt`
- Create: `app/src/main/java/com/example/timestart/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/example/timestart/MainActivity.kt`
- Modify: `app/src/main/java/com/example/timestart/ui/home/HomeScreen.kt`

- [x] Add lifecycle ViewModel dependency.
- [x] Expose task items and `setTaskEnabled(taskId, enabled)` from `HomeViewModel`.
- [x] Render a `LazyColumn` of cards with Material Switch controls and keep the empty state when the list is empty.
- [x] Build Debug APK and run the new mapper test.
