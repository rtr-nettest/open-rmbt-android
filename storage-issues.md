# Storage / persistence issues

Investigation of how the app stores and retrieves data (SharedPreferences + Room/SQLite),
and the root causes behind three reported symptoms:

1. Terms & Conditions are sometimes requested again although they were accepted before
   (seems to follow an earlier error).
2. Sometimes it is no longer possible to start a test — the control-server URL looks like it
   was "stored and destroyed".
3. Question: when are transactions done (or missing), and do deadlocks occur?

---

## Where data actually lives

| Data | Storage | Class / file |
| --- | --- | --- |
| Control-server URLs (base, V4/V6, check, statistics, share, version) | SharedPreferences `server_settings.pref` | `core/.../data/ControlServerSettings.kt` |
| T&C accepted flag, version, URL | SharedPreferences `terms_and_conditions.pref` | `core/.../data/TermsAndConditions.kt` |
| Client UUID | SharedPreferences `client_uuid.pref` | `core/.../data/ClientUUID.kt` |
| App config (hosts, ports, feature flags) | SharedPreferences `config.pref` | `app/.../config/AppConfig.kt` |
| History rows, TAC content, test data | Room / SQLite `CoreDatabase.db` | `core/.../di/DatabaseModule.kt`, DAOs |

Key point: **settings are NOT in SQLite** — they are plain SharedPreferences whose setters
write through immediately and individually (one `.apply()` per field). There is no atomic
"settings applied" boundary, and they are not part of any DB transaction.

---

## Finding 0 — the amplifier: `io {}` rethrows on the main thread → crash  **[FIXED]**

`util/src/main/java/at/rmbt/util/CoroutineExtensions.kt:29`

```kotlin
fun io(block: suspend CoroutineScope.() -> (Unit)) {
    val catchingBlock: suspend CoroutineScope.() -> (Unit) = {
        try {
            block.invoke(this)
        } catch (e: CancellationException) {
            throw e
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            Handler(Looper.getMainLooper())
                .post { throw throwable }      // ← re-throws on the MAIN thread => app crash
        }
    }
    IO.async(Dispatchers.IO, block = catchingBlock)
}
```

Almost every DB/settings write runs inside `io { }`. Any exception thrown in such a block
(a SQLite error, a `!!` NPE, `list.first()` on an empty response) is **re-posted to the main
thread and crashes the process**. That crash is what leaves the persisted state half-written
— it is the "earlier error" that precedes the other two symptoms.

**Fix direction:** log / route the throwable to an error handler instead of
`post { throw throwable }`.

---

## Finding 1 — control-server URL "stored and destroyed" → cannot start a test  **[FIXED]**

`core/src/main/java/at/specure/data/repository/SettingsRepositoryImpl.kt:63`

```kotlin
private fun emitSettingsRequest(): Maybe<SettingsResponse> {
    ...
    // we must remove ipv4 url before we want to check settings ...
    controlServerSettings.controlServerV4Url = null   // persisted to prefs immediately
    controlServerSettings.controlServerV6Url = null   // persisted to prefs immediately
    val settings = controlServerClient.getSettings(body)   // network call
    return settings
}

override fun refreshSettings(): Boolean {
    val settings = emitSettingsRequest()
    settings.onSuccess { processSettingsResponse(it) }     // only restores URLs on SUCCESS
    return settings.ok                                     // onFailure: URLs stay null
}
```

- The setters (`ControlServerSettings.kt:63-73`) write through to SharedPreferences
  **before** the network request.
- `processSettingsResponse()` (which re-populates the URLs) only runs on **success**. If
  `getSettings` fails (offline / timeout / 5xx), the URLs remain **null** and are never
  restored.
- Downstream consumers of the now-null values:
  - `AppConfig.controlServerCheckPrivateIPv4Host / IPv6Host` (`AppConfig.kt:212/216`) → null
    → IPv4/IPv6 check fails.
  - `ControlServerProviderImpl.statisticsHost` does `statisticsMasterServerUrl!!`
    (`ControlServerProviderImpl.kt:34`) → NPE → crash via Finding 0.
  - In expert / IPv4-only mode `controlServerHost` itself resolves to `controlServerV4Url!!`
    (`AppConfig.kt:197`).
- `refreshSettings()` is triggered from **many** places, so a single flaky-network refresh
  destroys the URLs:
  - history load — `HistoryRepositoryImpl.kt:90` and `:118`
  - after a test — `ResultsRepositoryImpl.kt:204`
  - device sync — `DeviceSyncRepositoryImpl.kt:56`
  - `SettingsWorker.kt:18`, Home (`HomeViewModel.kt`), Settings screen.

Note: the URL nulling is only needed for routing in expert/IPv4-only mode, but it destroys
the persisted values for **all** users on any failed refresh.

**Fix direction:** do not persist `null` before the request — override routing in memory only
for the call and restore the previous values on failure, or skip the nulling entirely outside
expert mode. Guard the `!!` accesses.

---

## Finding 2 — Terms & Conditions requested again

`core/src/main/java/at/specure/data/repository/SettingsRepositoryImpl.kt:133`

```kotlin
private fun updateTermsAndConditions(tac: TermsAndConditionsSettings?) = tac?.let { terms ->
    if (termsAndConditions.tacUrl != terms.url) {
        termsAndConditions.tacUrl = terms.url
        termsAndConditions.tacAccepted = false        // ← re-prompt
    }
    termsAndConditions.ndtTermsUrl = terms.ndtURL
    if (termsAndConditions.tacVersion != terms.version) {
        termsAndConditions.tacVersion = terms.version
        termsAndConditions.tacAccepted = false        // ← re-prompt
        terms.url?.let { tacDao.deleteTermsAndCondition(it) }
    }
}
```

- `tacAccepted`, `tacVersion`, `tacUrl` are separate SharedPreferences fields
  (`TermsAndConditions.kt`), each written with its own `.apply()`.
- Resetting acceptance on a genuine new version/URL is correct. It becomes **spurious** when:
  - a crash (Finding 0) interrupts `processSettingsResponse` after some prefs were written and
    others were not, so stored `tacVersion` and `tacAccepted` drift out of sync; or
  - a degraded/partial settings response is processed (e.g. a missing/zero `version`),
    producing a mismatch against the stored version.
- There is no atomic "settings applied" boundary around the multi-field update, so there is no
  point at which T&C state is guaranteed consistent.

**Fix direction:** apply parsed settings as one batched commit, only after a fully-parsed
successful response; treat a missing/invalid `version` as "no change" rather than a mismatch.

---

## Finding 3 — transactions & deadlocks

### Missing atomicity (should be one transaction, currently isn't)

`core/src/main/java/at/specure/data/repository/HistoryRepositoryImpl.kt:87` (`loadHistoryRTR`)

```kotlin
return response.map {
    val items = it?.toModelList()
    if (offset == 0 && (filterDevices == null && filterNetworks == null)) {
        settingsRepository.refreshSettings()   // settings refresh entangled with history load
        historyDao.clear()                     // transaction #1
    }
    if (items != null) {
        historyDao.insert(items)               // transaction #2
    }
    ...
}
```

- `clear()` then `insert()` are **two separate transactions**. A failure/crash between them
  leaves history empty. The DAO already exposes an atomic `clearInsert()`
  (`HistoryDao.kt:92`) that is not used here.
- `refreshSettings()` (SharedPreferences, not transactional) is entangled inside the history
  load mapping — a failure in one corrupts the other with no rollback.

### Room configuration

`core/src/main/java/at/specure/di/DatabaseModule.kt:62`

```kotlin
val builder = Room.databaseBuilder(context, CoreDatabase::class.java, "CoreDatabase.db")
builder.fallbackToDestructiveMigration()    // schema bump w/o migration => whole DB wiped
return builder.build()
```

- `fallbackToDestructiveMigration()` wipes the entire DB on any schema-version change without a
  migration. Not a runtime cause, but a data-loss risk on upgrades.
- Journal mode is left at Room's default (WAL), which is fine (1 writer + N readers).

### Deadlock-ish thread blocking

No classic two-lock deadlock was found, but several `runBlocking` patterns can stall like one:

- `HistoryLoader.kt:48,54` — `runBlocking { channel.send() }` on **rendezvous** channels. If
  the consuming view model is gone (fragment destroyed), the `send` suspends **forever**,
  permanently parking that `Dispatchers.IO` thread. Repeated, this exhausts the IO pool and the
  app appears hung.
- `StateRecorder.kt:158` — `runBlocking { … DB writes … }` on the RMBT client thread during
  test startup; if the DB is busy, the test stalls.
- `MapRepositoryImpl.kt:87` — `runBlocking` on the map-tile thread.

**Fix direction:** use buffered channels / `trySend` so a missing consumer cannot park IO
threads; avoid `runBlocking` on client/UI-adjacent threads.

---

## How it ties together

A `refreshSettings()` on a flaky network nulls the URLs → a downstream `!!` / `first()` /
SQLite error throws → `io {}` re-throws it on the main thread → **crash** — leaving:

- control-server URLs null (cannot start a test, NPEs), and
- T&C `version` / `accepted` flags possibly half-written (re-prompt).

Opening History makes it more likely, because history load both calls `refreshSettings()` and
performs a non-atomic clear + insert.

---

## Fixes applied

### Finding 0 — `io {}` no longer crashes the app

`util/src/main/java/at/rmbt/util/CoroutineExtensions.kt`

Removed the `Handler(Looper.getMainLooper()).post { throw throwable }` (and the now-unused
`Handler`/`Looper` imports). A failure inside an `io { }` block is now only logged instead of
being re-thrown on the main thread.

```kotlin
} catch (e: CancellationException) {
    throw e
} catch (throwable: Throwable) {
    // Log the failure instead of re-throwing it on the main thread, which used to
    // crash the whole app for any background DB/network error and leave persisted
    // state half-written (see storage-issues.md, Finding 0).
    Timber.e(throwable)
}
```

`CancellationException` is still rethrown, so coroutine cancellation is unaffected. Background
DB/network errors are now non-fatal.

### Finding 1 — the settings request no longer touches the stored URLs

**Root of the original hack:** `getSettings` routes to `ControlEndpointProvider.checkSettingsUrl`
→ `host = protocol + config.controlServerHost`. In **expert + IPv4-only** mode,
`config.controlServerHost` (`AppConfig.kt:195`) resolves to `controlServerV4Url`. To make the
settings check still reach the *base* host, the old code nulled the stored V4/V6 URLs for the
duration of the call — which destroyed them whenever the request failed, and did nothing useful
in normal mode.

**Fix:** route the settings check to the base host explicitly, so the stored URLs are never
mutated.

- `core/.../config/Config.kt` — new `val controlServerHostForSettings: String`.
- `app/.../config/AppConfig.kt` — implemented as the configured base host (never the IPv4
  override):
  ```kotlin
  override val controlServerHostForSettings: String
      get() = getString(BuildConfig.CONTROL_SERVER_HOST)
  ```
- `core/.../config/ControlServerProviderImpl.kt` — `checkSettingsUrl` now uses it:
  ```kotlin
  override val checkSettingsUrl: String
      get() = "$protocol${config.controlServerHostForSettings}$routePath/${config.controlServerSettingsEndpoint}"
  ```
- `core/.../data/repository/SettingsRepositoryImpl.kt` (`emitSettingsRequest`) — the null-before
  / restore-after block is removed entirely:
  ```kotlin
  val body = deviceInfo.toSettingsRequest(clientUUID, clientUUIDLegacy, config, termsAndConditions)
  // The settings request is routed to the base host via ControlEndpointProvider.checkSettingsUrl,
  // so there is no longer a need to null the stored IPv4/IPv6 URLs here.
  val settings = controlServerClient.getSettings(body)
  return settings
  ```

The test/history/result paths still use `config.controlServerHost` (which keeps the IPv4
override in expert mode), so only the settings-check routing changed.

**On-device verification** (Samsung SM-S928B, rmbt debug build):

1. Stored URLs before: `CONTROL_V4_SERVER_URL=c01v4.netztest.at`, `CONTROL_V6_SERVER_URL=c01v6.netztest.at`.
2. Airplane mode + relaunch → settings request still routed to the base host
   (`Unable to resolve host "c01.netztest.at"`), no crash; stored URLs **unchanged**.
3. Network back on + relaunch → settings request to base host succeeds; stored URLs intact
   (no regression).

## Reference: control-server host resolution (expert IPv4-only vs. custom control-server URL)

Two independent developer/expert features influence which control-server host the app talks to.
They are easy to confuse because both end up affecting `config.controlServerHost`.

### A) Custom control-server URL (developer option)

- Toggle: `controlServerOverrideEnabled` (pref key `IS_CONTROL_SERVER_OVERRIDE_ENABLED`),
  shown in Settings only when developer mode is on (`fragment_settings.xml:225`).
- User-entered values live in `server_settings.pref`:
  `controlServerOverrideUrl`, `controlServerOverridePort` (`ControlServerSettings.kt`).
- `SettingsViewState.setControlServerAddress()` (`SettingsViewState.kt:55`) applies them:
  ```kotlin
  if (controlServerOverrideEnabled && developerModeIsEnabled) {
      appConfig.controlServerHost = controlServerOverrideUrl   // -> writes pref CONTROL_SERVER_HOST
      appConfig.controlServerPort = controlServerOverridePort
  } else {
      appConfig.controlServerHost = BuildConfig.CONTROL_SERVER_HOST.value  // compiled default
      appConfig.controlServerPort = BuildConfig.CONTROL_SERVER_PORT.value
  }
  refreshSettings()
  ```
- Net effect: the **base host** — i.e. `getString(BuildConfig.CONTROL_SERVER_HOST)` — becomes
  the override host when the override is active, or the compiled default otherwise. Changing it
  triggers a fresh settings request to the new host.

### B) Expert mode "IPv4 only"

- Toggles: `expertModeEnabled` (`EXPERT_MODE_ENABLED`) and `expertModeUseIpV4Only`
  (`EXPERT_MODE_IPV4_ONLY`).
- `controlServerV4Url` is **not** user-entered — it is returned by the backend in the settings
  response (`processSettingsResponse`: `urls.ipv4OnlyControlServerUrl`, e.g. `c01v4.netztest.at`)
  and stored in `server_settings.pref`.
- When `expertModeEnabled && expertModeUseIpV4Only && controlServerV4Url != null`, the app routes
  control traffic to that IPv4-only host, which resolves to an IPv4 address only — forcing the
  whole measurement onto IPv4.

### How they combine — `AppConfig.controlServerHost` (`AppConfig.kt:195`)

```kotlin
get() = if (expertModeEnabled && expertModeUseIpV4Only && serverSettings.controlServerV4Url != null) {
    serverSettings.controlServerV4Url!!          // (B) IPv4-only host wins
} else {
    getString(BuildConfig.CONTROL_SERVER_HOST)   // (A) override host, or compiled default
}
```

- **Precedence:** IPv4-only (B) wins over the base/override host (A) for the operational host.
- The IPv4-only URL is itself derived from a settings response fetched from the base/override
  host (A) — so (B) layers on top of (A): "use the IPv4-only endpoint *that this control server
  advertised*".
- This single getter feeds **test request, results, history, sync, news** — everything except
  the settings check.

### The settings check is the exception (and why Finding 1 existed)

The settings/registration check must reach the **base** host (A), never the IPv4-only host (B):
the base host is what advertises the IPv4/IPv6 URLs in the first place. The old code forced this
by temporarily nulling `controlServerV4Url` so the getter above fell through to the base host —
which destroyed the stored URL on failure (Finding 1). The fix routes the settings check to the
base host directly via `config.controlServerHostForSettings`
(= `getString(BuildConfig.CONTROL_SERVER_HOST)`), so it always honours the override (A) and never
touches (B).

### Subtle coupling: client UUID is keyed by host in expert+override mode  **[FIXED]**

`ClientUUID` (`ClientUUID.kt`) stores the UUID under `KEY_CLIENT_UUID + <host>` **only** when
`expertModeEnabled && controlServerOverrideEnabled`; otherwise under a plain `KEY_CLIENT_UUID`.

**The bug:** the host used in the key was `config.controlServerHost`, which resolves to
`controlServerV4Url` when IPv4-only is on. So toggling IPv4-only flipped the key between
`KEY_CLIENT_UUID + c01.netztest.at` and `KEY_CLIENT_UUID + c01v4.netztest.at` — the client then
looked unregistered, cascading into re-registration and a possible T&C re-prompt. IPv4-only is
only a routing preference for the *same* logical server, so it must not change the UUID storage.

**Fix:** key the UUID by `config.controlServerHostForSettings` (the base/override host, which is
independent of the IPv4-only toggle) instead of `config.controlServerHost`. The UUID is now stable
per logical control server while IPv4-only is toggled.

```kotlin
// ClientUUID.kt — all occurrences
preferences.getString(KEY_CLIENT_UUID + config.controlServerHostForSettings, _value)
```

(This is a behaviour change only for expert + custom-control-server users who had IPv4-only on:
their UUID is re-keyed once to the base host, so they may re-register a single time.) The earlier
null-hack also flipped the host mid-request, which the Finding 1 refactor already removed.

## Remaining proposed fixes (priority order)

1. **Atomic settings apply** — batch the parsed settings into one commit, only after a fully
   parsed successful response; guard `.first()` and `!!`. (`SettingsRepositoryImpl.kt:89`)
2. **Use `clearInsert()`** for history page 0 and decouple `refreshSettings()` from history
   load. (`HistoryRepositoryImpl.kt:87`)
3. **Replace `runBlocking { channel.send() }`** with a buffered channel / `trySend`.
   (`HistoryLoader.kt:48,54`)

Done: Finding 0 (`io {}` crash) and Finding 1 (URL destruction) — see "Fixes applied" above.
</content>
</invoke>
