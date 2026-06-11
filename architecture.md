# Signal / Coverage Measurement Service — Control-Flow Analysis

> How `SignalMeasurementService` (signal/coverage/fences measurement) is started,
> stopped, checked and resumed — and which code paths can cause the observed bugs:
> (1) two app instances (PiP + fullscreen), (2) measurement that cannot be stopped /
> restarts itself, (3) notification says "measurement ongoing" while the app shows
> the start screen.
> Analysis date: 2026-06-11, branch `master` @ 6996535e.

---

## 1. Components

| Component | Location | Role |
|---|---|---|
| `SignalMeasurementService` | `core/.../measurement/signal/SignalMeasurementService.kt` | Foreground service (notification ID 3). Holds wake lock; exposes binder `Producer`. Both *started* and *bound*. |
| `SignalMeasurementProcessor` | `core/.../measurement/signal/SignalMeasurementProcessor.kt` | **@Singleton**. Holds the real in-memory active flag `_isActive` + `activeStateLiveData`. Forwards location/signal updates into the coverage layer. |
| `RtrCoverageMeasurementProcessor` | `core/.../measurement/coverage/RtrCoverageMeasurementProcessor.kt` | **@Singleton**. Coverage session lifecycle (fences, pings, timers, termination causes). |
| `RtrCoverageLoopManager` | `core/.../measurement/coverage/RtrCoverageLoopManager.kt` | **@Singleton**. Creates/continues sessions, registration retry (up to 100×2 s), emits `CoverageMeasurementEvent`s. |
| `CoverageMeasurementDataStateManager` | `core/.../coverage/presentation/...` | **@Singleton** `StateFlow<CoverageMeasurementData>` — session state (`IDLE/CREATED/RUNNING/PAUSED/FINISHED_LOOP_CORRECTLY`). |
| `CoverageMeasurementSettings` | `core/.../data/CoverageMeasurementSettings.kt` | SharedPreferences: `signalMeasurementIsRunning`, `signalMeasurementShouldContinueInLastSession`, `signalMeasurementLastMeasurementId` — **persisted across process death**. |
| `HomeViewModel` | `app/.../viewmodel/HomeViewModel.kt` | Service client: `attach()`/`detach()` = bind/unbind; `startSignalMeasurement` / `stopSignalMeasurement` via binder `producer`. **Not shared** — each Activity/Fragment gets its own instance. |
| `HomeFragment` | `app/.../ui/fragment/HomeFragment.kt` | Start screen. Binds in `onStart`, unbinds in `onStop`. Auto-opens `SignalMeasurementActivity` when active-state LiveData turns true. |
| `SignalMeasurementActivity` | `app/.../ui/activity/SignalMeasurementActivity.kt` | Measurement screen (map). Own `HomeViewModel` + `CoverageResultViewModel`. PiP-capable, `launchMode=singleTask`. Auto-starts measurement in `onStart` after 1 s delay. |
| `MeasurementService` | `core/.../measurement/MeasurementService.kt` | Speed-test service. Also binds `SignalMeasurementService` (`BIND_AUTO_CREATE`) in `onCreate` to pause signal measurement during tests. |
| `NotificationProviderImpl` | `app/.../di/NotificationProviderImpl.kt` | Builds the signal-measurement notification. |

Three independent clients bind the same service: HomeFragment's VM, SignalMeasurementActivity's VM, and MeasurementService.

## 2. Control flow

### 2.1 Start
1. Home screen coverage button → `SignalMeasurementTermsActivity` (its result is ignored; the start code in the activity-result callback is commented out).
2. The actual start is `SignalMeasurementActivity.onStart()`:
   ```
   onStart() {
       viewModel.attach(this)                       // bindService (async!)
       lifecycleScope.launch { delay(1000)          // workaround for async bind
           viewModel.startSignalMeasurement(DEDICATED) }
   }
   ```
   Guard: `shouldStartDedicatedMeasurementStateChecker()` = `CoverageResultViewModel.shouldRunCoverageMeasurement()` — returns **true unless** singleton state == `FINISHED_LOOP_CORRECTLY` (also true for `null`, `IDLE`, `RUNNING`, `PAUSED`!).
3. `HomeViewModel.startSignalMeasurement()`: sets persisted `signalMeasurementIsRunning=true`, then `producer?.startMeasurement(...)` — **silent no-op if the binder isn't connected yet** (hence the 1 s delay hack).
4. `SignalMeasurementService.startMeasurement()` (no is-already-running guard at service level):
   - acquire wake lock (timeout = configured duration),
   - `processor.startMeasurement()`,
   - `startForegroundService(intent(this))` (promotes itself from bound-only to started),
   - `startForeground(3, notification)` — notification is built with `stopMeasurementIntent = null` → **the notification has no Stop action**; tapping it opens `SignalMeasurementActivity`.
5. `SignalMeasurementProcessor.startMeasurement()`: idempotency guard `shouldStartCoverage = !_isActive` — only creates a new coverage session when not active. Always (re-)adds location/signal listeners and registers the battery receiver (duplicate registration if called twice).
6. `RtrCoverageLoopManager.startOrContinueInLoop()`: if persisted `signalMeasurementShouldContinueInLastSession && lastMeasurementId != null` → **continues previous session from DB**, else creates new. `onStartMeasurementSession()` sets `shouldContinue=true` + `isRunning=true` (persisted).

### 2.2 Stop
- UI: stop button / close dialog → `HomeViewModel.stopSignalMeasurement()` → persisted `isRunning=false` → `producer?.stopMeasurement(false)` — **silent no-op when `producer == null`** (binder not connected / already detached). No queued-stop mechanism (start has one: `toggleService` flag).
- Service `stopMeasurement()`: release wake lock, cancel alarm, `processor.stopMeasurement()`, `stopForeground(REMOVE)`, `stopSelf()` (service object stays alive while clients are bound; notification is removed immediately).
- `processor.stopMeasurement()`: synchronously sets `_isActive=false` and posts to LiveData, then `rtrCoverageMeasurementProcessor.stopCoverageSession(...)` which:
  - is a **no-op if state already `FINISHED_LOOP_CORRECTLY`**,
  - runs **asynchronously** (DB writes, result sending), only *then* sets state = `FINISHED_LOOP_CORRECTLY` and clears persisted flags via `onStopMeasurementSession()`.
- Coverage-initiated stop (max-loop-time timer, etc.): `endMeasurementLoop()` → emits `MeasurementLoopEnded` → `measurementSessionStoppedCallback` in the processor → `processor.stopMeasurement()` + `context.startService(stopIntent)` → service `ACTION_STOP` → full stop.
- `ACTION_ALARM_STOP` / `setEndAlarm()` / `isUnstoppable` / `shouldEndAfterLoopMode`: **dead code — `setEndAlarm()` is never called**, so the duration alarm never fires.

### 2.3 Check / resume
- Every `bindService` (`BIND_AUTO_CREATE`) **creates** the service (onCreate) without starting measurement; UI state then comes from `processor.activeStateLiveData` through the binder.
- `HomeFragment` listener: `activeSignalMeasurementLiveData.listen { if (it) openSignalMeasurementActivity() }`. The MediatorLiveData re-attaches its source on every `onServiceConnected`, so the latest value (true) is **re-delivered every time the home screen (re)binds** → the measurement screen is re-opened automatically whenever measurement is active.
- `HomeFragment.continueInSignalMeasurementIfShould()` is **dead code** (never called). The persisted-flag check in `shouldOpenSignalMeasurementScreen()` is commented out — resume after process death relies only on the (volatile) LiveData path, which is `false` after a process restart.
- `onStartCommand` returns `START_STICKY` (default). After the process is killed, the system restarts the service with a null intent: nothing resumes, **`startForeground` is not called**, the singleton processor starts fresh (`_isActive=false`) — but the persisted prefs may still say `isRunning=true` / `shouldContinue=true`.

### 2.4 PiP
- `SignalMeasurementActivity.onUserLeaveHint()` → `enterInPictureMode()` **unconditionally** (even when no measurement is running or it already finished).
- Manifest: `supportsPictureInPicture`, `launchMode="singleTask"`, no `taskAffinity` override, `configChanges=screenSize|smallestScreenSize|screenLayout|orientation`.
- When an activity is pinned, the system **moves it to a separate pinned task**; the original task (HomeActivity) comes forward behind it.

## 3. Root causes of the reported symptoms

### Symptom 1 — "PiP instance + fullscreen instance at the same time"

Chain:
1. Measurement runs; user leaves with Home button → activity is pinned (PiP) in its own task.
2. User taps the launcher icon → `HomeActivity` (singleTask) comes to foreground; `HomeFragment.onStart` binds; on `onServiceConnected` the mediator re-delivers `active=true`; the listener calls `SignalMeasurementActivity.start(context)` (plain `startActivity`, no flags).
3. The same launch can also come from the **notification content intent** (`PendingIntent.getActivity`, no flags).
4. With the existing instance living in the *pinned* task, `singleTask` resolution is unreliable: depending on Android version/OEM the launch creates a **new instance inside the HomeActivity task** instead of expanding the pinned one → one PiP window + one fullscreen instance. (`singleTask` does not guarantee a single *instance* across a pinned task; the standard fixes are `singleInstance`-like isolation via a dedicated `taskAffinity`, or expanding the existing PiP task instead of `startActivity`.)
5. Each instance has its own `HomeViewModel`/`CoverageResultViewModel`, its own service binding and its own auto-start coroutine — both call `startMeasurement` (the duplicate coverage *session* is prevented only by the processor's `_isActive` guard; service-level side effects run twice).

Aggravators:
- `onUserLeaveHint` enters PiP even when nothing is measured, so a stale PiP window can survive a finished/stopped measurement.
- HomeFragment's auto-open fires on **every** rebind while measurement is active, repeatedly issuing `startActivity` against a pinned instance.

### Symptom 2 — "Measurement cannot be stopped / runs twice"

Several independent causes:

1. **Auto-restart race in `SignalMeasurementActivity.onStart`.** Stop is async: the UI stop sets `_isActive=false` immediately, but `FINISHED_LOOP_CORRECTLY` is only set later inside a coroutine (after DB writes + fence update). Any `onStart` of *any* `SignalMeasurementActivity` instance (exiting PiP, returning from recents, second instance from Symptom 1, tapping the notification) fires the delayed `startSignalMeasurement` after 1 s. Its guard `shouldRunCoverageMeasurement()` passes whenever state is not-yet-`FINISHED_LOOP_CORRECTLY` (including `null`/`IDLE`/`RUNNING`) → a **new session is started right after the user stopped**, notification re-appears → perceived as "cannot stop".
2. **Stop is a silent no-op when the binder is not connected.** `producer?.stopMeasurement(false)` — if pressed before `onServiceConnected` (or after `detach`), nothing stops, yet the persisted `isRunning` flag *is* cleared → state desync. Start has a pending-toggle mechanism; stop has none.
3. **No service-level start guard.** `Service.startMeasurement()` always re-runs `startForegroundService` + `startForeground` + wake-lock logic; only the processor guards the session. With two activity instances + MeasurementService all interacting, repeated start calls are routine.
4. **Persisted continue-flag resurrects sessions.** `onStartMeasurementSession()` persists `shouldContinueInLastSession=true`; it is only cleared in the async `onStopMeasurementSession()`. If the process dies in between (or the stop coroutine fails — e.g. exception before the `finally`, crash, force-stop), the next `startCoverageSession` **silently continues the old session from DB**.
5. The duration alarm (`setEndAlarm`) is never armed — dead code — so a forgotten session never times out by alarm; only the coverage-layer max-seconds timers (if delivered by the server) end it.

### Symptom 3 — "Notification shows ongoing measurement, app shows start screen"

The notification reflects the *service's* foreground state; the start screen reflects the *singleton processor's* `_isActive` via LiveData. They diverge when:

1. **Process death + `START_STICKY` restart**: the service restarts (null intent), `_isActive=false`, no `startForeground` call. On some OEMs/timings the previous notification is re-shown or never removed, while LiveData says inactive → Home shows the start screen. (Restarting a foreground-started service without calling `startForeground` again can also crash with `ForegroundServiceDidNotStartInTimeException` on API 26+.)
2. **Stop pipeline interrupted half-way**: `processor.stopMeasurement()` sets `_isActive=false` first; the service-side `stopForeground` happens in the same call chain *only* for binder/ACTION_STOP paths — but in `measurementSessionStoppedCallback` the order is `processor.stopMeasurement()` → `startService(stopIntent)`. If `startService` throws (background-start restrictions in edge states) or the process dies in between, the notification stays while the app state says "stopped".
3. **The notification has no Stop action** (`signalMeasurementService(null)` — the stop intent parameter is always null), so the user cannot stop the orphaned service from the notification; tapping it opens `SignalMeasurementActivity`, whose `onStart` **starts a brand-new measurement** after 1 s (Symptom 2.1) — turning a stale notification into a real running session.
4. The resume path that should reconcile this (`continueInSignalMeasurementIfShould` / persisted `signalMeasurementIsRunning`) is dead/commented out, so after process death the app has no logic to either re-attach to or clean up an orphaned service/notification.

## 4. State is stored in five places (the core problem)

| State holder | Scope | Set by | Cleared by |
|---|---|---|---|
| Service foreground + notification | OS / process | `startForeground` | `stopForeground` |
| `SignalMeasurementProcessor._isActive` | singleton, in-memory | `startMeasurement` (sync) | `stopMeasurement` (sync) |
| `CoverageMeasurementDataStateManager.state` | singleton, in-memory | session events (async) | `initData()` / `FINISHED_LOOP_CORRECTLY` (async) |
| `CoverageMeasurementSettings.*` | SharedPreferences (persistent) | `onStartMeasurementSession` | `onStopMeasurementSession` (async) / `stopSignalMeasurement` |
| `HomeViewState.isSignalMeasurementActive` | per-screen ObservableField | LiveData listener | LiveData listener |

There is no single source of truth and no reconciliation on startup; the async stop pipeline plus the auto-start-on-onStart make every transition race-prone.

## 5. Recommended fixes (ordered by impact)

1. **Remove the unconditional auto-start in `SignalMeasurementActivity.onStart`** (the `delay(1000)` coroutine). Start the measurement exactly once from an explicit user action (terms accepted / start button), and on `onServiceConnected` only *re-attach* to an existing session. This kills the restart-after-stop race and the double-start from duplicate instances.
2. **Make stop robust:** queue a pending stop in `HomeViewModel` when `producer == null` (mirror of `toggleService`), or better: always stop via `context.startService(SignalMeasurementService.stopIntent(ctx))`, which works without a binder.
3. **Single source of truth in the service:** give `SignalMeasurementService.startMeasurement` an `isActive` guard, and have the *service* (not three clients) own the started/stopped decision.
4. **Fix the PiP duplicate instance:** when the home screen wants to show the running measurement, do not blindly `startActivity`; the pinned task must be expanded (e.g. give `SignalMeasurementActivity` its own `taskAffinity` + `FLAG_ACTIVITY_NEW_TASK`, or check `isInPictureInPictureMode` ownership / use `ActivityManager.AppTask.moveToFront`). Also gate `onUserLeaveHint` → `enterInPictureMode()` on `measurement actually running`.
5. **Reconcile state on startup:** on app/service start compare the persisted `signalMeasurementIsRunning` flag, the processor's `_isActive`, and the actual foreground-service/notification state; clean up orphaned notification or resume properly. Return `START_NOT_STICKY` (or handle the sticky null-intent restart by calling `startForeground` then `stopSelf`).
6. **Add a Stop action to the notification** (pass `stopIntent(context)` instead of `null` to `notificationProvider.signalMeasurementService`), so an orphaned service can always be stopped.
7. Delete dead code that suggests behavior that doesn't exist: `setEndAlarm`/`isUnstoppable`/`shouldEndAfterLoopMode`, `continueInSignalMeasurementIfShould`, commented-out persisted-flag check in `shouldOpenSignalMeasurementScreen`.

## 6. Reproduction recipes (for verification)

- **Duplicate instance:** start coverage measurement → Home button (PiP appears) → launcher icon → app opens → HomeFragment auto-opens `SignalMeasurementActivity` → observe PiP window + fullscreen instance (device/version dependent).
- **Unstoppable:** start measurement → press stop and within ~1 s leave/re-enter the activity (or have a second instance from above) → `onStart`'s delayed auto-start re-creates the session.
- **Stale notification / start screen:** start measurement → kill process from "background process limit"/OOM (or adb `am kill` after backgrounding without removing the notification race) → reopen app: Home shows start screen, prefs still claim running; or press stop in the brief window before the binder connects.
