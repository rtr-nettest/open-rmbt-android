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

## Checkpoint 8 — QoS task interfaces (TracerouteService, QoSTask)

Converted the two leaf interfaces that the task layer implements: `TracerouteService` (extends `Callable<List<HopDetail>>`; nested `PingException`/`HopDetail`) and `QoSTask` (extends `Callable<QoSTestResult>, Comparable<QoSTask>`).

Gotcha: Kotlin `List<T>` emits Java `List<? extends T>` (covariant wildcard), which broke the still-Java `TracerouteAndroidImpl.setResultListObject(List<HopDetail>)` override and the `submit(pingTool)` inference in `TracerouteTask`. Fixed with `@JvmSuppressWildcards` on the interface **and** on the `Callable<…>` supertype type argument to force invariant `List<HopDetail>` in the generated Java signatures.

## Checkpoint 9 — AbstractQoSTask (QoS task base)

Converted `AbstractQoSTask` (the abstract base for all concrete QoS tasks). It's Kotlin extending the still-Java `AbstractRMBTTest` and implementing the Kotlin `QoSTask` — works because it doesn't touch the parent's protected fields (uses `nnTest.getRMBTClient()`). Used `@JvmField` on its protected fields (`taskDesc`, `qoSTest`, `id`, `controlConnection`) so the still-Java concrete tasks keep field access; interface methods (`getPriority`, `getTaskDesc`, `compareTo`, …) as `override fun`; constants in companion `const val` (inherited as Java statics by subclasses). Preserved the original's NPE-on-missing-param behavior with `value!!.toLong()`. All concrete Java tasks compile against it.

## Checkpoint 10 — first concrete QoS task (TcpTask)

Converted `TcpTask` (extends the Kotlin `AbstractQoSTask`; uses inherited protected members + an anonymous `ControlConnectionResponseCallback`).

Two cross-cutting fixes needed:
- `ControlConnectionResponseCallback.onResponse` params made nullable (`String?`) — `QoSControlConnection` passes a `readLine()` result that can be null, and impls null-check it. With non-null Kotlin params, the compiler-inserted param null-check would have thrown at runtime.
- `QoSTestResult.resultMap` changed to `HashMap<String, Any?>` — tasks store nullable values (e.g. `readLine` results), matching the original `HashMap<String,Object>`.

---

### Progress
- 102 original Java files → 52 converted + 24 dead removed + 3 app/core handled.
- **23 Java files remain** (all in `rmbt-client`): QoS task layer (`AbstractQoSTask`, `UdpTask`, `VoipTask`, `HttpProxyTask`, `DnsTask`, `TcpTask`, `TracerouteTask`, `NonTransparentProxyTask`, `WebsiteTask`, `QoSControlConnection`, `QoSTask`, `TaskDesc`), VoIP results (`VoipTest`, `VoipTestResult`, `VoipTestResultHandler`), `QualityOfServiceTest`, `BandCalculationUtil`, `helper/{JSONParser, Dig, ControlServerConnection}`, `ndt/UiServicesAdapter` + `net/measurementlab/ndt/UiServices`, `RMBTTestParameter`, `AbstractRMBTTest`, `TracerouteAndroidImpl`, `TestMeasurement`, `TestSettings`, `QoSResultCollector`, `QoSTestResult`, and the giants `RMBTClient` (1,163) + `RMBTTest` (957, convert last).
