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

## Finding 0 — the amplifier: `io {}` rethrows on the main thread → crash

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

## Finding 1 — control-server URL "stored and destroyed" → cannot start a test

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

## Proposed fixes (priority order)

1. **Don't destroy URLs before the request** — override routing in memory for the call only,
   restore on failure, or skip the nulling outside expert mode. (`SettingsRepositoryImpl.kt:81`)
2. **Make `io {}` not crash** — log / route the throwable instead of `post { throw }`.
   (`CoroutineExtensions.kt:29`)
3. **Atomic settings apply** — batch the parsed settings into one commit, only after a fully
   parsed successful response; guard `.first()` and `!!`. (`SettingsRepositoryImpl.kt:89`)
4. **Use `clearInsert()`** for history page 0 and decouple `refreshSettings()` from history
   load. (`HistoryRepositoryImpl.kt:87`)
5. **Replace `runBlocking { channel.send() }`** with a buffered channel / `trySend`.
   (`HistoryLoader.kt:48,54`)
</content>
</invoke>
