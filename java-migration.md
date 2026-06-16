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

---

### Progress
- 102 original Java files → 59 converted + 24 dead removed + 3 app/core handled.
- Checkpoint numbering now matches commit phases (Checkpoint N == Phase N). Latest commit is Phase 12 (HttpProxyTask = Checkpoint 12); Checkpoint 13 (UdpTask) is converted but not yet committed → will be Phase 13.
- **16 Java files remain** (all in `rmbt-client`): `VoipTask`, VoIP results (`VoipTest`, `VoipTestResult`, `VoipTestResultHandler`), `QualityOfServiceTest`, `BandCalculationUtil`, `JitterTest`, `WebsiteTestServiceImpl`, `TracerouteAndroidImpl`, `helper/{JSONParser, Dig, ControlServerConnection}`, and the `AbstractRMBTTest` → `RMBTTest`/`RMBTClient` finale (`RMBTClient` 1,163 + `RMBTTest` 957, last).
