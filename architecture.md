# Open-RMBT Android — Architecture Notes

> Two-part document.
> **Part I** is an onboarding-style overview of the whole codebase: modules, patterns,
> data flow, and the things a new developer must know before touching anything.
> **Part II** is a deep dive into how the signal/coverage (fences) measurement service
> is controlled, and the root causes of three observed lifecycle bugs.
> Analysis date: 2026-06-11, branch `master` @ 6996535e.

---

# Part I — Codebase overview (onboarding guide)

## I.1 What the app does

RTR-Netztest / Open-RMBT is the Austrian regulator's (RTR) network measurement app.
It performs two fundamentally different kinds of measurement:

1. **Speed test** ("Messung"): a full RMBT protocol test against a measurement server —
   ping, jitter/packet loss, download, upload, optional QoS tests (website, VoIP,
   traceroute, …), optionally repeated in **loop mode** (time- or distance-triggered).
2. **Signal/coverage measurement** ("Coverage", historically "fences" or dedicated
   signal measurement): a passive, long-running recording of signal strength + GPS
   fences with periodic UDP pings, drawn live on a map. No bandwidth is consumed.

Both run in foreground services and submit results to RTR's control server.

## I.2 Module map

| Module | Package root | What it is |
|---|---|---|
| `app` | `at.rtr.rmbt.android` | All UI (activities/fragments/viewmodels/databinding), app-level DI, notification building, map wrappers. The only Android *application* module. |
| `core` | `at.specure` | The heart: measurement services, coverage engine, Room database + repositories, device-info watchers (signal/network/cell), persisted settings, WorkManager workers, DI modules. |
| `control_client` | `at.rmbt.client.control` | Retrofit/OkHttp client for the RTR control server, map server and IP check (request/response DTOs, endpoints, interceptors). Pure networking, no Android UI. |
| `rmbt-client` | `at.rtr.rmbt.client` | **Legacy Java** RMBT measurement engine (sockets, threads): `RMBTClient`, `RMBTTest`, QoS test framework (`v2/task/*`), NDT. Largely shared lineage with other RMBT clients. Treat as a black box driven by `core`. |
| `pingClient` | `at.specure.client` | Kotlin UDP ping client (plain + HMAC-authenticated flows) used by the coverage measurement for continuous ping. |
| `location` | `at.specure.location` | Location abstraction: `LocationWatcher`, merging `GPSLocationSource`/`NetworkLocationSource` via `LocationDispatcher`, state watchers. |
| `location-fused` | `at.specure.location` | Google fused-location-provider implementation of the same source interface. |
| `netmonster_core` | `cz.mroczis.netmonster.core` | Vendored fork of the NetMonster library — reads detailed cell info (LTE/NR neighbours, bands, service state) beyond what plain TelephonyManager gives. |
| `util` | `at.rmbt.util` | Tiny helpers: `Maybe`, exception types (`HandledException`, `NoConnectionException`), coroutine `io {}` helper. |
| `private/` | — | RTR-internal build config (signing, flavor overrides, logos). The public build uses the `openRmbt` flavor with an empty Google Maps key placeholder. |

Dependency direction: `app` → `core` → (`control_client`, `rmbt-client`, `pingClient`, `location*`, `netmonster_core`, `util`). UI never talks to Retrofit or the RMBT engine directly — always through `core` repositories/services.

## I.3 Dependency injection — Dagger 2, hand-rolled, no Hilt

- `App.onCreate` builds `DaggerAppComponent` and stores it in **two global service-locator objects**: `Injector` (app scope) and `CoreInjector` (core scope, same component instance). `AppComponent extends CoreComponent`.
- Services and workers are *not* constructor-injected; they call `CoreInjector.inject(this)` in `onCreate` (classic field injection — remember this when wondering where a service's `lateinit var` comes from).
- ViewModels use a `ViewModelFactory` + `@ViewModelKey` multibinding; screens obtain them with the project-specific `by viewModelLazy()` extension (`ViewModelLazy.kt`), which also auto-subscribes the screen to the VM's `errorLiveData`.
- **Scoping trap:** `viewModelLazy` scopes the VM to *that* activity/fragment. The same VM class used in two screens (e.g. `HomeViewModel` in both `HomeFragment` and `SignalMeasurementActivity`) means two *independent instances* — shared state must live in `@Singleton`s, prefs, or the DB, not in the VM.

## I.4 UI layer

- **Single-activity it is not.** `HomeActivity` (launcher, `singleTask`) hosts a `NavHostFragment` + bottom navigation with the four tabs: `HomeFragment` (start screen), `HistoryFragment`, `StatisticsFragment`, `MapFragment`. Everything else — measurement screens, results, settings, loop config, terms — is a **separate Activity** (see `AndroidManifest.xml`).
- Pattern per screen: XML layout with **Android Data Binding** bound to a `*ViewState` class (`ui/viewstate/`) full of `ObservableField`s, plus a ViewModel exposing LiveData. Fragments/activities wire LiveData → state fields with the `listen(this) {}` extension (`util/LiveDataExtensions`).
- `BaseActivity`/`BaseFragment` provide `bindContentView`, error-dialog plumbing, state save/restore across the VM list, and `enterInPictureMode()` (PiP) used by the two measurement screens.
- Maps: Google Maps via a thin wrapper layer (`map/wrapper/MapWrapper` etc.) so map tech could be swapped; `MapFragment` (the tab) renders RTR's tile overlays from the map server, `SignalMeasurementActivity` draws live coverage fences as circles.
- Two measurement UIs mirror the two services: `MeasurementActivity` (speed test, PiP-capable, `singleTask`) and `SignalMeasurementActivity` (coverage, PiP-capable, `singleTask`).

## I.5 The speed-test pipeline (the "other" service)

```
HomeFragment (btnSignalLevel)
  └─ MeasurementService.startTests(ctx)        // startForegroundService + ACTION_START_TESTS
       └─ MeasurementService (core, notification ID 1)
            ├─ TestController(Impl)             // drives the legacy engine
            │    └─ RMBTClient / QualityOfServiceTest   (rmbt-client, Java, blocking sockets)
            ├─ StateRecorder                    // RMBTClientCallback: persists everything to Room
            │    (locations, signals via watchers, cells via netmonster, speed graph items…)
            ├─ loop mode: CountDownTimer + distance trigger (stateRecorder.onLoopDistanceReached)
            └─ ResultsRepository.sendTestResults → control server
                 └─ on NoConnectionException → WorkLauncher.enqueueDelayedDataSaveRequest (SendDataWorker)
MeasurementActivity + MeasurementViewModel
  └─ bindService(MeasurementService) → Producer binder → progress LiveData → UI
HomeActivity
  └─ isTestsRunningLiveData → auto-opens MeasurementActivity when a test runs
```

Things worth knowing:
- `MeasurementService` watches the engine's liveness itself (`planInactivityCheck`, 120 s threshold) and kills a stuck test.
- Loop mode logic (scheduling next test by timer *and* movement distance, pending-test flags to avoid double starts) lives entirely inside `MeasurementService` + `StateRecorder` — it is intricate and historically bug-prone; read it fully before touching it.
- `MeasurementService` also binds `SignalMeasurementService` (to pause a signal measurement while a speed test runs) — yet another client of the signal service (relevant for Part II).
- The same auto-open pattern as the coverage screen: `HomeActivity` listens to `isTestsRunningLiveData` and `startActivity(MeasurementActivity)` when true.

## I.6 Device-info stack (watchers)

`core/at.specure.info.*` + the `location` modules form a reactive "what is the phone seeing" layer used by *both* measurement types and the home screen:

- `SignalStrengthWatcher`/`SignalStrengthLiveData` — current signal (all radio types; classes per tech: `SignalStrengthInfoLte/Nr/Gsm/WiFi`).
- `ActiveNetworkWatcher`/`ActiveNetworkLiveData` — which transport is active (cell/wifi/eth/vpn/bluetooth), with `DetailedNetworkInfo` for cellular incl. secondary 5G cells.
- `CellInfoWatcher` — uses **netmonster_core** for detailed cell identity/bands; this is where 4G/5G classification subtleties (NSA vs SA, `NRConnectionState`) live.
- `LocationWatcher` — merges GPS/network/fused sources through `LocationDispatcher`; exposes both LiveData and listener APIs, plus `LocationStateWatcher` (ENABLED / DISABLED_APP / DISABLED_DEVICE) which the UI uses for permission prompts.

Pattern: each watcher is a `@Singleton` with `addListener/removeListener` *and* a LiveData facade. Listeners must be removed symmetrically — several past bugs came from doubled listener registration (see Part II).

## I.7 Data layer

- **Room** `CoreDatabase` (~30 entities, schema **version 174**) with `fallbackToDestructiveMigration()` — **a schema bump wipes all local data** (history is re-fetchable from server; unsent results are not). Bump `version` on any entity change, and know what you're deleting.
- Entities split roughly into: test results (`TestRecord`, `SpeedRecord`, `PingRecord`, `SignalRecord`, graph items, QoS records), history (`History`, `HistoryReference`, medians), map markers, loop mode (`LoopModeRecord`), and coverage (`CoverageMeasurementSession`, `CoverageMeasurementFenceRecord`, `FencesResultItemRecord`).
- **Repositories** (`core/data/repository/`) are interface + `Impl` pairs provided by `DatabaseModule`/`CoreModule`; they are the only place where control-server DTOs are mapped to/from entities (`RequestMappers.kt`, `ResponseMappers.kt` — both large and central).
- **Persisted key-value state** lives in several small SharedPreferences-backed singletons: `Config`/`AppConfig` (server-driven + user settings, the interface is in core, impl in app `config/`), `CoverageMeasurementSettings`, `ControlServerSettings`, `ClientUUID`, `TermsAndConditions`, `MeasurementServers`, `HistoryFilterOptions`.
- **WorkManager** (`worker/WorkLauncher`) handles everything that must survive the process: settings refresh on app start, delayed result submission (`SendDataWorker`), coverage registration/result chunks (`CoverageMeasurementWorker`, `SignalMeasurement*Worker`), and `CoverageSyncWorker` (re-sends unsynced coverage; checks `signalMeasurementIsRunning` so it doesn't race a live session). Workers get dependencies via `CoreInjector`.

## I.8 Network layer

- `control_client` builds Retrofit services in `ControlServerModule`: `ControlServerApi` (settings check, news, test request/registration, result submission, history, signal/coverage requests), `MapServerApi` (tiles/markers/filters), `IpApi` (IPv4/IPv6 reachability — home screen indicators).
- URLs are *not* hardcoded per request — endpoint providers (`ControlEndpointProvider`, implemented in core `config/ControlServerProviderImpl`) assemble them from `Config`, so the control server can be switched in developer settings.
- `ControlServerModule.onResponseInterceptor` is a hook the app uses to trigger coverage re-sync after any successful response (`HomeViewModel.syncCoverageOnRequests`).
- Client identity = `ClientUUID`, assigned by the server at first settings check; nearly every request body carries it.

## I.9 Notifications & foreground services

| ID | Service | Meaning |
|---|---|---|
| 1 | `MeasurementService` | speed test / loop mode progress (throttled to ≥700 ms updates so the cancel button stays clickable) |
| 2 | `MeasurementService` | loop mode finished |
| 3 | `SignalMeasurementService` | coverage measurement ongoing (currently has **no stop action** — see Part II) |

All notification building is in `app/di/NotificationProviderImpl` behind the `NotificationProvider` interface (core defines, app implements — core never touches UI resources).

## I.10 Conventions & gotchas checklist for new developers

1. **Find the state holder first.** State is spread across: `@Singleton` processors (in-memory), SharedPreferences settings classes (persistent), Room (persistent), per-screen ViewModels (volatile), and the OS service/notification state. Before changing behavior, list which of these your feature reads/writes — most lifecycle bugs here are two holders disagreeing (Part II is one long example).
2. **Services are bound *and* started.** Both measurement services use the binder for UI communication and `start(Foreground)Service` for lifetime. `stopSelf()` doesn't kill a service that still has bound clients.
3. **Field injection in services/workers** via `CoreInjector.inject(this)` — `lateinit` crashes usually mean an injection wiring gap in `CoreComponent`/`Injector`.
4. **`by viewModelLazy()` ≠ shared ViewModel** (see I.3).
5. **Data Binding everywhere** — behavior often hides in XML expressions and `DataBindingAdapters.kt`, not in the fragment.
6. **Legacy Java engine** (`rmbt-client`) uses raw threads & blocking IO; everything around it (`TestControllerImpl`) bridges to coroutines. Don't introduce coroutine cancellation into the engine itself.
7. **Room destructive migration** — version bump = data wipe (I.7).
8. **Auto-open listeners**: both `HomeActivity` (speed test) and `HomeFragment` (coverage) auto-launch the measurement screen from LiveData; any change to measurement state propagation can cause screens to pop up unexpectedly.
9. **Dead code is common** (commented-out blocks, unused methods like `setEndAlarm`, `continueInSignalMeasurementIfShould`). Don't assume code you read is actually executed — check call sites.
10. **Flavors/branding**: public `openRmbt` flavor; RTR builds use `private/` configs. Maps API key comes from a manifest placeholder, empty in public builds (map tiles then come from RTR's map server but Google base map is blank).
11. **Logging**: Timber everywhere; coverage code logs with greppable prefixes (`SMS1`, `HVM1/2`, `LPT`, `SDT` and emoji for connectivity events) — use them when tracing measurement flows on a device.

---

# Part II — Deep dive: signal / coverage measurement service control

> How `SignalMeasurementService` (signal/coverage/fences measurement) is started,
> stopped, checked and resumed — and which code paths can cause the observed bugs:
> (1) two app instances (PiP + fullscreen), (2) measurement that cannot be stopped /
> restarts itself, (3) notification says "measurement ongoing" while the app shows
> the start screen.

## II.1 Components

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

## II.2 Control flow

### II.2.1 Start
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

### II.2.2 Stop
- UI: stop button / close dialog → `HomeViewModel.stopSignalMeasurement()` → persisted `isRunning=false` → `producer?.stopMeasurement(false)` — **silent no-op when `producer == null`** (binder not connected / already detached). No queued-stop mechanism (start has one: `toggleService` flag).
- Service `stopMeasurement()`: release wake lock, cancel alarm, `processor.stopMeasurement()`, `stopForeground(REMOVE)`, `stopSelf()` (service object stays alive while clients are bound; notification is removed immediately).
- `processor.stopMeasurement()`: synchronously sets `_isActive=false` and posts to LiveData, then `rtrCoverageMeasurementProcessor.stopCoverageSession(...)` which:
  - is a **no-op if state already `FINISHED_LOOP_CORRECTLY`**,
  - runs **asynchronously** (DB writes, result sending), only *then* sets state = `FINISHED_LOOP_CORRECTLY` and clears persisted flags via `onStopMeasurementSession()`.
- Coverage-initiated stop (max-loop-time timer, etc.): `endMeasurementLoop()` → emits `MeasurementLoopEnded` → `measurementSessionStoppedCallback` in the processor → `processor.stopMeasurement()` + `context.startService(stopIntent)` → service `ACTION_STOP` → full stop.
- `ACTION_ALARM_STOP` / `setEndAlarm()` / `isUnstoppable` / `shouldEndAfterLoopMode`: **dead code — `setEndAlarm()` is never called**, so the duration alarm never fires.

### II.2.3 Check / resume
- Every `bindService` (`BIND_AUTO_CREATE`) **creates** the service (onCreate) without starting measurement; UI state then comes from `processor.activeStateLiveData` through the binder.
- `HomeFragment` listener: `activeSignalMeasurementLiveData.listen { if (it) openSignalMeasurementActivity() }`. The MediatorLiveData re-attaches its source on every `onServiceConnected`, so the latest value (true) is **re-delivered every time the home screen (re)binds** → the measurement screen is re-opened automatically whenever measurement is active.
- `HomeFragment.continueInSignalMeasurementIfShould()` is **dead code** (never called). The persisted-flag check in `shouldOpenSignalMeasurementScreen()` is commented out — resume after process death relies only on the (volatile) LiveData path, which is `false` after a process restart.
- `onStartCommand` returns `START_STICKY` (default). After the process is killed, the system restarts the service with a null intent: nothing resumes, **`startForeground` is not called**, the singleton processor starts fresh (`_isActive=false`) — but the persisted prefs may still say `isRunning=true` / `shouldContinue=true`.

### II.2.4 PiP
- `SignalMeasurementActivity.onUserLeaveHint()` → `enterInPictureMode()` **unconditionally** (even when no measurement is running or it already finished).
- Manifest: `supportsPictureInPicture`, `launchMode="singleTask"`, no `taskAffinity` override, `configChanges=screenSize|smallestScreenSize|screenLayout|orientation`.
- When an activity is pinned, the system **moves it to a separate pinned task**; the original task (HomeActivity) comes forward behind it.

## II.3 Root causes of the reported symptoms

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

## II.4 State is stored in five places (the core problem)

| State holder | Scope | Set by | Cleared by |
|---|---|---|---|
| Service foreground + notification | OS / process | `startForeground` | `stopForeground` |
| `SignalMeasurementProcessor._isActive` | singleton, in-memory | `startMeasurement` (sync) | `stopMeasurement` (sync) |
| `CoverageMeasurementDataStateManager.state` | singleton, in-memory | session events (async) | `initData()` / `FINISHED_LOOP_CORRECTLY` (async) |
| `CoverageMeasurementSettings.*` | SharedPreferences (persistent) | `onStartMeasurementSession` | `onStopMeasurementSession` (async) / `stopSignalMeasurement` |
| `HomeViewState.isSignalMeasurementActive` | per-screen ObservableField | LiveData listener | LiveData listener |

There is no single source of truth and no reconciliation on startup; the async stop pipeline plus the auto-start-on-onStart make every transition race-prone.

## II.5 Recommended fixes (ordered by impact)

1. **Remove the unconditional auto-start in `SignalMeasurementActivity.onStart`** (the `delay(1000)` coroutine). Start the measurement exactly once from an explicit user action (terms accepted / start button), and on `onServiceConnected` only *re-attach* to an existing session. This kills the restart-after-stop race and the double-start from duplicate instances.
2. **Make stop robust:** queue a pending stop in `HomeViewModel` when `producer == null` (mirror of `toggleService`), or better: always stop via `context.startService(SignalMeasurementService.stopIntent(ctx))`, which works without a binder.
3. **Single source of truth in the service:** give `SignalMeasurementService.startMeasurement` an `isActive` guard, and have the *service* (not three clients) own the started/stopped decision.
4. **Fix the PiP duplicate instance:** when the home screen wants to show the running measurement, do not blindly `startActivity`; the pinned task must be expanded (e.g. give `SignalMeasurementActivity` its own `taskAffinity` + `FLAG_ACTIVITY_NEW_TASK`, or check `isInPictureInPictureMode` ownership / use `ActivityManager.AppTask.moveToFront`). Also gate `onUserLeaveHint` → `enterInPictureMode()` on `measurement actually running`.
5. **Reconcile state on startup:** on app/service start compare the persisted `signalMeasurementIsRunning` flag, the processor's `_isActive`, and the actual foreground-service/notification state; clean up orphaned notification or resume properly. Return `START_NOT_STICKY` (or handle the sticky null-intent restart by calling `startForeground` then `stopSelf`).
6. **Add a Stop action to the notification** (pass `stopIntent(context)` instead of `null` to `notificationProvider.signalMeasurementService`), so an orphaned service can always be stopped.
7. Delete dead code that suggests behavior that doesn't exist: `setEndAlarm`/`isUnstoppable`/`shouldEndAfterLoopMode`, `continueInSignalMeasurementIfShould`, commented-out persisted-flag check in `shouldOpenSignalMeasurementScreen`.

## II.6 Reproduction recipes (for verification)

- **Duplicate instance:** start coverage measurement → Home button (PiP appears) → launcher icon → app opens → HomeFragment auto-opens `SignalMeasurementActivity` → observe PiP window + fullscreen instance (device/version dependent).
- **Unstoppable:** start measurement → press stop and within ~1 s leave/re-enter the activity (or have a second instance from above) → `onStart`'s delayed auto-start re-creates the session.
- **Stale notification / start screen:** start measurement → kill process from "background process limit"/OOM (or adb `am kill` after backgrounding without removing the notification race) → reopen app: Home shows start screen, prefs still claim running; or press stop in the brief window before the binder connects.
