# Java → Kotlin migration log

Ongoing effort to convert all Java in the repo to Kotlin. Started from **102 Java files / ~19.7K lines**.

Build/verify workflow:
- JDK 21 JBR: `-Dorg.gradle.java.home="C:/Program Files/Android/Android Studio2/jbr"`
- Module check: `./gradlew :rmbt-client:compileDebugSources …` (~5s)
- Full chain: `./gradlew :app:compileRmbtDebugSources …`; on-device: `./gradlew :app:installRmbtDebug …` (device `ZY22M4L3VR`, motorola edge 70)

Conventions applied so the mixed Java/Kotlin codebase keeps compiling at each step:
- `@JvmField` / `@JvmStatic` / `@Throws` to preserve the exact JVM API the still-Java callers depend on.
- For value/holder classes, properties whose getter names already match (e.g. `getMaxDelay`) convert cleanly; otherwise use private backing fields + explicit functions, or rename to avoid auto-accessor clashes.
- Make genuinely-nullable Java fields nullable in Kotlin; fix the (Kotlin) consumers rather than faking non-null.
- Dead-code: verify by IMPORT / fully-qualified reference, not simple class name (name collisions cause false "live" results).

---

## Checkpoint 1 — setup, dead code, first leaves

Set up the conversion workflow, removed dead code, and converted the first batch — all verified green on device.

- **Scope confirmed:** "Everything" — originally 102 Java files / ~19.7K lines.
- **Dead-code removal:** fixpoint reference analysis removed **22 unused files**, incl. the entire NDT subsystem (`NdtTests` alone was 2,028 lines), `RMBTApplet`, the `QoSServerResult*` family, and the `util/tools/*` system-info collectors (~5K lines that didn't need converting).
- **`app`:** `ViewModelKey.java` → Kotlin annotation (`KClass`, `@Target(AnnotationTarget.FUNCTION)`); Dagger/kapt processes it.
- **`core`:** deleted both auto-generated example test stubs (removed deprecated `androidx.test.runner.AndroidJUnit4` usage; they tested nothing real).
- **`rmbt-client`:** converted 12 leaf files (enums, interfaces, exception, `TestResultConst`, `Ping`); added the missing `compileOptions`/`kotlinOptions` (JVM target 17) needed once a module gains Kotlin sources.

## Checkpoint 2 — result models + Config/helpers

17 files converted across two clusters, verified green and running on device.

- **Result-model cluster (6):** `TestResult` (abstract; kept open since the still-Java `QoSTestResult` extends it), `Results`, `ThreadTestResult`, `IntermediateResult`, `SpeedItem`, `TotalTestResult` (speed-calculation logic; statics via `@JvmStatic`). Exposed genuine nullability hidden by platform types → fixed `core/StateRecorder.kt` faithfully (`client_version ?: ""`, `voipTestResult!!`).
- **Config/helper cluster (5):** `Config` (constants-interface → `object` with `const val`), `RevisionHelper` (`object` + static-init), and the three Gson `*Capability` classes (`@SerializedName` preserved).

## Checkpoint 3 — stream/service leaves + ByteUtil

10 more files converted + 2 more dead files removed.

- **Dead code:** name-collision false-negative caught — `MapOptions` and `ServerOption` (340 lines) were actually unused (the `MapOptions` core uses is a *different* class in `control_client`). **24 dead files removed total.**
- **Converted (10):** `InputStreamCounter`/`OutputStreamCounter`, `Helperfunctions`, the three service interfaces (`TrafficService` w/ companion consts, `TestProgressListener`, `WebsiteTestService`), `TrafficServiceImpl`, `AsyncHtmlStatusCodeRetriever`, `LoopModeSettings`, and `ByteUtil` (bit-manipulation util — careful Java byte→int sign extension and shift operators).

## Checkpoint 4 — UDP networking cluster

Converted the **UDP networking cluster (4 files)** — `StreamSender` (generic interface + nested callback/settings), `UdpStreamSender`, `NioUdpStreamSender` (NIO selector loop), `UdpStreamReceiver`. Done as one unit because same-package classes accessed the settings' package-private fields directly while cross-package QoS tasks use getters. The Kotlin `UdpStreamCallback` interface needed `@Throws(IOException)` on its methods so the still-Java anonymous callbacks compile. Cross-language boundary verified.

## Checkpoint 5 — RTP networking cluster

Converted the **entire RTP networking cluster (3 files)** — the most bit-manipulation-heavy code:
- **`RealtimeTransportProtocol`** — RTP header construction + `PayloadType`/`RtpVersion`/`CodecType` enums and nested `RtpException`.
- **`RtpPacket`** — packet parse/serialize with bit-field accessors built on `ByteUtil`.
- **`RtpUtil`** — generic `runVoipStream` (`<T : Closeable>`), jitter/skew/out-of-order **QoS math**, nested result types. Preserved Java numeric-promotion semantics (`(meanJitter + jitter).toLong()`, float casts) for identical results.

Full app + core + rmbt-client chain green; still-Java `VoipTask`/`VoipTest` link correctly.

## Checkpoint 6 — QoS-support leaves (test params/measurement)

Converted 3 interdependent files: `RMBTTestParameter` (immutable test config; `open` because `TaskDesc` extends it), `TaskDesc` (extends it, Gos params map), `TestMeasurement` (traffic measurement + the `TrafficDirection` enum already consumed by `TotalTestResult`).

Gotcha fixed: a Kotlin property `uuid` generates `getUuid()`, but Java callers (`RMBTClient`, `UdpTask`) call `getUUID()` (all-caps). Forced the JVM name with `@JvmName("getUUID")` on the getter — Java sees `getUUID()`, Kotlin still uses `.uuid`.

## Checkpoint 7 — QoS results + NDT UI services + TestSettings

Converted 5 files: `QoSTestResult` (extends the converted `TestResult`; `isFatalError` Boolean prop), `QoSResultCollector` (`toJson()` used by `core/StateRecorder.kt`), `net.measurementlab.ndt.UiServices` (interface + int consts in companion), `UiServicesAdapter` (`@JvmField` on the public `Double?`/`StringBuffer` fields read by Java `ControlServerConnection`; `startTimeNs`/`stopTimeNs` as `var … private set`), and `TestSettings`.

Ripple fixed: `TestSettings.defaultDnsResolvers` — first typed `MutableList`, but `core/TestControllerImpl.kt` assigns a nullable `List<InetAddress>?` from `getDnsServers()`. Matched the original Java `List` type with `List<InetAddress>?` to preserve behavior (Java callers tolerate null via platform types).

> Note: checkpoint numbers below are aligned to the **commit "Phase N"** numbers. Phase 8 bundled two of the original checkpoints (the QoS interfaces + `AbstractQoSTask`), so they are merged into Checkpoint 8 here. Checkpoint N == Phase N.

## Checkpoint 8 — QoS task interfaces + AbstractQoSTask

(Committed together as **Phase 8**.)

Converted the two leaf interfaces the task layer implements: `TracerouteService` (extends `Callable<List<HopDetail>>`; nested `PingException`/`HopDetail`) and `QoSTask` (extends `Callable<QoSTestResult>, Comparable<QoSTask>`). Gotcha: Kotlin `List<T>` emits Java `List<? extends T>` (covariant wildcard), which broke the still-Java `TracerouteAndroidImpl.setResultListObject(List<HopDetail>)` override and the `submit(pingTool)` inference in `TracerouteTask`. Fixed with `@JvmSuppressWildcards` on the interface **and** on the `Callable<…>` supertype type argument.

Also converted `AbstractQoSTask` (abstract base for all concrete QoS tasks): Kotlin extending the still-Java `AbstractRMBTTest` and implementing the Kotlin `QoSTask` (clean because it uses `nnTest.getRMBTClient()`, not the parent's protected fields). `@JvmField` on its protected fields (`taskDesc`, `qoSTest`, `id`, `controlConnection`) so the still-Java concrete tasks keep field access; interface methods (`getPriority`, `getTaskDesc`, `compareTo`, …) as `override fun`; constants in companion `const val`; preserved NPE-on-missing-param behavior with `value!!.toLong()`.

## Checkpoint 9 — first concrete QoS task (TcpTask)

Converted `TcpTask` (extends the Kotlin `AbstractQoSTask`; uses inherited protected members + an anonymous `ControlConnectionResponseCallback`).

Two cross-cutting fixes needed:
- `ControlConnectionResponseCallback.onResponse` params made nullable (`String?`) — `QoSControlConnection` passes a `readLine()` result that can be null, and impls null-check it. With non-null Kotlin params, the compiler-inserted param null-check would have thrown at runtime.
- `QoSTestResult.resultMap` changed to `HashMap<String, Any?>` — tasks store nullable values (e.g. `readLine` results), matching the original `HashMap<String,Object>`.

## Checkpoint 10 — more concrete QoS tasks (Dns, Traceroute, NonTransparentProxy)

Converted `DnsTask` (dnsjava lookups; `when (r) { is MXRecord -> … }` for the record-type dispatch; `lookupDns` kept as `@JvmStatic`), `TracerouteTask` (uses the converted `TracerouteService` + thread pool; the `@JvmSuppressWildcards` on `TracerouteService` makes `submit(pingTool)` return `Future<List<HopDetail>>` cleanly), and `NonTransparentProxyTask` (anonymous `ControlConnectionResponseCallback`). All follow the `TcpTask` pattern.

## Checkpoint 11 — WebsiteTask + QoSControlConnection

Converted `WebsiteTask` (anonymous `RenderingListener`; relaxed `WebsiteTestService.run` param to `String?` since the URL is nullable) and `QoSControlConnection` (Kotlin extends still-Java `AbstractRMBTTest`, implements `Runnable`; `@JvmField` on `isRunning`/`couldNotConnect` since `AbstractQoSTask` reads `couldNotConnect` as a field; accesses inherited protected `reader`/`connect(...)`/`sendMessage(...)`/`params`; nested `ControlConnectionResponseCallbackHolder`).

## Checkpoint 12 — HttpProxyTask

Converted `HttpProxyTask` (MD5-checksum HTTP download task). All its statics/fields/`Md5Result` are internal (no external refs), so they went into the companion / plain properties without `@JvmStatic`/`@JvmField`. Renamed the local `httpGet` connection var to `connection` to avoid clashing with the `httpGet()` method; `while ((n = in.read()) != -1)` → `.also { n = it }` idiom.

## Checkpoint 13 — UdpTask

Converted `UdpTask` (the largest task file, 522 lines): a self-referencing anonymous `ControlConnectionResponseCallback` (`sendCommand(..., this)`), two thread-pool `Callable`s, the nested `UdpPacketData`, and the UDP send/receive `UdpStreamCallback`s. Used `params.uuid!!.toByteArray()` for the UUID payload, smart-cast the `val`-property ports inside the `if (… != null)` guards, and `!!` inside the nested callbacks where smart-cast can't reach. `udpSettings.socket?.close()` on the error path (the settings socket starts null).

## Bugfix — QoS test hung (nullable callback param)

**Symptom:** a full measurement with QoS never terminated — stuck right after `---- Initializing QoS Tests ----`, no individual QoS task ever ran.

**Root cause:** the still-Java orchestrator `QualityOfServiceTest` calls `qoSTestSettings.dispatchTestProgressEvent(ON_CREATED, null, this)` — passing `null` for the `test` argument. My converted `TestSettings.dispatchTestProgressEvent` (Checkpoint 7) declared `test: AbstractQoSTask` **non-null**, so Kotlin's inserted parameter null-check threw an NPE inside the `QualityOfServiceTest` constructor, aborting QoS startup. The overall test then waited forever for a QoS result that never came.

**Fix:** `dispatchTestProgressEvent(event, test: AbstractQoSTask?, qosTest)` — made `test` nullable; the `ON_START`/`ON_END` branches use `test!!` (those events always carry a real task). Verified on device: full test incl. QoS now completes (64/64 → `QOS_END` → results screen).

**Lesson:** any converted Kotlin method still called from Java must mark every parameter the Java side may pass `null` as nullable — the symptom can be a *hang* (not a visible crash) when the NPE happens on a background/worker thread whose failure isn't surfaced.

## Checkpoint 14 — VoipTask

Converted `VoipTask` (RTP/VoIP QoS task). Two anonymous callbacks (control-connection response + RTP receive). Needed to relax `RtpUtil.runVoipStream`'s `socket` param to `T?` (VoipTask passes `null`). Verified end-to-end on device (full QoS test completes — see Bugfix above).

## Checkpoint 15 — VoIP result classes

Converted `VoipTestResult` (689-line Gson data class) to `var` properties with `@SerializedName` (proven to work with Gson — the QoS-result JSON shows the `*Capability` classes serialized correctly the same way); `core` reads the props by Kotlin name via `VoipTestResult.toRecord()`. Dropped the unused builder-style setters/all-args ctor/`createVoipTestResult`; kept the computed `getMeanJitter`/`getMeanPacketLossInPercent`/`format` helpers + `JSON_OBJECT_IDENTIFIER`. Converted `VoipTestResultHandler` (SharedPreferences save/load; the still-Java `RMBTClient` uses `convertResultsToObject`); preserved the original's quirky mis-assignments (e.g. `voip_objective_sample_rate` → `objectiveBitsPerSample`) faithfully.

## Checkpoint 16 — BandCalculationUtil

Converted `BandCalculationUtil` (607-line band/frequency lookup tables) to an `object` with nested `Band`/`LTEBand`/`NRBand`/`UMTSBand`/`GSMBand`/`WifiBand`/`FrequencyInformation`. `core` accesses `getBandFromArfcn(...)?.frequencyDL` as a **property**, so `FrequencyInformation.frequencyDL` is a Kotlin `val` (→ `.frequencyDL` for Kotlin + `getFrequencyDL()` for Java). Used distinct private backing names (`bandNumber`, `bandValue`) to dodge Kotlin getter clashes with the public `getBand()`. Preserved exact map types (`LinkedHashMap` for nrBands, ordered `ArrayList` for gsmBands).

## Checkpoint 17 — helpers + QoS test hierarchy

Converted the independent helpers `Dig` (dnsjava; `DnsRequest` exposes `val response`/`request` for the property access in `DnsTask`), `JSONParser` (`object`, `@JvmStatic`/`@JvmField` for the still-Java `ControlServerConnection`), `TracerouteAndroidImpl` (relaxed `HopDetail.toJson` return to `JSONObject?`), `WebsiteTestServiceImpl` (WebView; relaxed `WebsiteTestService.getHash()` to `String?`), and deleted dead `OptionFunction`.

Then the **QoS-test hierarchy** `QualityOfServiceTest → VoipTest → JitterTest`. The Java code *shadowed* fields in subclasses; in Kotlin I unified to one set of `protected` backing fields in the base that subclasses populate (no shadowing, no accessor overrides). Exposed `progress`/`testSize`/`status`/`testMap`/`testGroupCounterMap` as **Kotlin properties** (backed by renamed atomics like `progressCounter`/`statusRef`) so app/core's `.progress`/`.status` synthetic-property access keeps working *and* Java still gets `getProgress()`/`getStatus()`. `getRMBTClient`/`getTestSettings` stayed functions (only Java/Kotlin-via-function callers). `VoipTest` now only overrides `call()`+`getTestId()`; `JitterTest` collapses to a single `getTestId()` override (its `call()` was identical to VoipTest's). Verified full QoS test on device: 64/64 → `QOS_END` → results.

## Checkpoint 18 — AbstractRMBTTest + ControlServerConnection

Converted the speed-test base `AbstractRMBTTest` and the 702-line `ControlServerConnection`. `AbstractRMBTTest` exposes its shared connection state as `@JvmField protected var` (`in`/`reader`/`out`/`totalDown`/`totalUp`/`chunksize`/`buf`) so the `RMBTTest` subclass and the already-converted `QoSControlConnection`/`TcpTask` keep field access; `connect(testResult)` is `protected open` so `RMBTTest` overrides it; greeting constants live in the companion as `@JvmField protected`. `client.sslSocketFactory` is read into a local before the `isSecure` branch (a property from another class can't be smart-cast). `ControlServerConnection` kept its accessors as **functions** (`getTestUuid()`, `getStartTimeNs()`, …) because `RMBTClient` calls them that way; `core`'s synthetic-property reads (`connection.testUuid`/`loopUuid`/…) were switched to explicit `getX()` calls — exactly what the old Java synthetic-property access compiled to. Objective param maps became `HashMap<String, Any?>` for the JSON-parsed `TaskDesc` objectives.

## Checkpoint 19 — RMBTTest

Converted the 957-line speed-test worker `RMBTTest : AbstractRMBTTest(...), Callable<ThreadTestResult?>` (download/upload/ping, nested `CurrentSpeed` with `@JvmField trans/time`, inner `SingleResult`). Captured the nullable inherited streams into non-null locals (`val rdr = reader!!`, `val outStream = out!!`, `val inStream = \`in\`!!`, `val buffer = buf!!`) at the top of each loop. `call()` returns `ThreadTestResult?`. Reads `client.isEnabledJitterAndPacketLossTest` / `client.taskDescList` as **properties** and calls `RMBTClient.getCommonThreadPool()` statically.

## Checkpoint 20 — RMBTClient (the last Java file)

Converted the 1,163-line orchestrator `RMBTClient : RMBTClientCallback` — **the final Java file in the project**. Interop decisions driven by how `core`/`app` already call it:
- Accessors read with synthetic-property syntax became Kotlin **properties**: `controlConnection`, `status` (a `var` — `core` *assigns* `client.status = QOS_TEST_RUNNING/QOS_END`, so the old `setStatus()` body moved into the property setter), `statusBeforeError`, `provider`, `serverName`, `testUuid`, `publicIP`, `startTimeMillis`, `taskDescList`, `trafficService`, `errorMsg`, `commonCallback` (`@JvmField`), `isEnabledJitterAndPacketLossTest`, `sslSocketFactory` (`private set`).
- `totalTestResult` is exposed **non-null** (`get() = totalResult!!`) because every `core` use is a non-null deref — matching the old Java platform-type access.
- `runTest()`/`shutdown()`/`getIntermediateResult()` stay **functions** (called that way).
- `getCommonThreadPool()`, the `getInstance(...)` overloads, `getTrustingManager()`, `getSSLContext()` are `@JvmStatic` in the companion; the `TASK_*` ids are `const val` so `RMBTClient.TASK_JITTER` etc. resolve.
- Internal `setStatus(x)` → `status = x`; `RMBTTest`'s `client.setStatus(x)` → `client.status = x`; the three QoS-test classes' `client.getTaskDescList()` → `client.taskDescList`.

Deleted `RMBTClient.java`. **Verified end-to-end on device** (motorola edge 70): a full measurement incl. QoS ran through the now-100%-Kotlin stack and reached `ResultsActivity` with real data — QoS **60/64 (93%)** across all categories (DNS 34/34, TCP 16/18, Web/Traceroute 1/1, …), no crashes.

---

### Progress — ✅ MIGRATION COMPLETE
- **0 Java files remain** in `app`, `core`, and `rmbt-client` (`src/main`). The Android app is now 100% Kotlin.
- 102 original Java files → converted to Kotlin or removed as dead code; final batch (Checkpoints 18–20) covered `AbstractRMBTTest`, `ControlServerConnection`, `RMBTTest`, `RMBTClient`.
- Verified: `:rmbt-client:compileDebugSources` ✓, `:app:compileRmbtDebugSources` ✓, installed + full QoS test on device → results screen ✓.

## Checkpoint 21 — VoIP in_port fix + interop-annotation cleanup

**VoIP `in_port` fix** (`RtpUtil.runVoipStream`): the internal `onBind` reported `incomingPort` (null when the server sends only `out_port`) instead of the actual bound socket port, so `voip_objective_in_port` serialized as null. Now reports the real bound port. (This is a latent bug present in the original Java too; it makes the result more complete but the persistent VoIP QoS failure is a **server-side evaluation** issue — the client submits a healthy 100/100-packet result that the netztest.at eval rejects regardless.)

**Interop-annotation cleanup:** removed the now-vestigial Kotlin→Java interop annotations that were only needed during the mixed-migration phase — **240 of 244** stripped from `rmbt-client` (all `@Throws`, plus the Java-only `@JvmStatic`/`@JvmField`/`@JvmName`/`@JvmSuppressWildcards`). Kept **6** that are genuine Kotlin *language* requirements (not Java interop):
- `@JvmStatic` ×2 on `AbstractRMBTTest`'s `protected` companion members `EXPECT_GREETING`/`RMBT_SERVER_PATTERN` (Kotlin disallows non-`@JvmStatic` protected companion members accessed from a subclass).
- `@JvmField` ×4 on `AbstractQoSTask`'s `taskDesc`/`qoSTest`/`id`/`controlConnection` — these are exposed both as fields (subclass access) and via getter methods (`getTaskDesc()` is a `QoSTask` interface override; the others are called as functions); without `@JvmField` the property auto-getter clashes with those methods.

Verified: `:rmbt-client:compileDebugSources` ✓, `:app:compileRmbtDebugSources` ✓, installed + on-device test ✓.
