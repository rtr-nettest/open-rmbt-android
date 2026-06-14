# Fix: results screen not shown when app is backgrounded right after measurement start

## Symptom
When a user starts a normal (non-loop) measurement and puts the app into the background
immediately after starting, the app shows the **start screen** on resume. If the user instead
keeps the app in the foreground, the **result details screen** is shown after the measurement
finishes, as expected.

This is unrelated to the lifecycle enhancements on the branch.

## Root cause
Navigation from the measurement screen to the results screen relied on the running test UUID
still being available when the test finished:

- `TestControllerImpl` clears its `testUUID` (`_testUUID = null`) once the test reaches its final
  state.
- **Foreground:** `MeasurementActivity` had captured the UUID earlier (via `onClientReady`) and
  received the `onSubmitted` callback while alive, so it navigated to the results.
- **Backgrounded right after start:** the activity detaches from the service, so it never receives
  `onSubmitted`. On resume the service reports a `null` test UUID (cleared, or a freshly recreated
  service), and `MeasurementActivity.finishActivity()` fell back to `HomeActivity.start()` — the
  start screen.
- Because `HomeActivity` and `MeasurementActivity` are both `launchMode="singleTask"`, resuming
  via the launcher icon clears `MeasurementActivity` and lands directly on `HomeActivity`, so the
  fix also has to be handled there. (Resuming via Recents instead keeps `MeasurementActivity` on
  top.)

## Fix
A persisted "results pending" handoff that survives the controller clearing its UUID and the
activity/service being stopped or recreated.

| File | Change |
| --- | --- |
| `core/.../config/Config.kt` | New `var pendingResultTestUUID: String?`. |
| `app/.../config/AppConfig.kt` | SharedPreferences-backed implementation (`PENDING_RESULT_TEST_UUID`). |
| `core/.../measurement/MeasurementService.kt` | Sets `config.pendingResultTestUUID` the moment results are ready to be shown (`stateRecorder.onReadyToSubmit`, non-loop only). |
| `app/.../ui/activity/MeasurementActivity.kt` | `finishActivity()` consumes the pending UUID (covers the Recents-resume path) and clears it. |
| `app/.../ui/activity/HomeActivity.kt` | `onResume()` consumes the pending UUID (covers the launcher-icon path) and clears it. |
| `app/.../viewmodel/MeasurementViewModel.kt` | Defensive: no longer overwrites a known `testUUID` with `null` on service reconnect. |

Notes:
- `pendingResultTestUUID` is set only when results should actually be shown
  (`shouldShowResults && loopUUID == null`), so it is never set for cancelled or errored tests.
- It is cleared as soon as it is consumed, so backing out of the results screen returns to Home
  without re-opening the results (no navigation loop).

## Verification (on device)
1. Start test → background immediately → resume → **Results screen shown** (previously: start screen).
2. Foreground test → **Results screen shown** (no regression).
3. Back from results → Home, **no re-navigation loop** (pending cleared after use).
