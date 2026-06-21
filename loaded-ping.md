# Loaded UDP ping (during the speed measurement)

Adds a UDP "loaded ping" that runs for the **whole** speed measurement (start → end), shows the
latest value on the measurement screen as **`UDP-Ping: xxx ms`**, and submits every sample with the
`/testresult` request. Sampled every **100 ms** (one ping per 100 ms), reusing the existing UDP-ping
client that already powers the coverage/signal measurement.

Branch: `feeature/dz03`.

## How it works

### 1. Token — the v2 token *is* the UDP-ping token
The control server issues a combined token in `test_token`:

```
<v1 token>_#v2#<base64 v2 token>
```

The part after the `#v2#` marker is the v2 token (`open-rmbt-udp-ping` schema, 16 bytes → 24 base64
chars) and authenticates the UDP-ping server (see the server's `token-migration.md`). The client
extracts it:

```
test_token.substringAfter("#v2#")   // null if no marker
```

If the control server later sends a dedicated `ping_token` (see below) that is preferred.

### 2. UDP-ping endpoint
- **Host/port/token preferred from `/testRequest`** when present: `ControlServerConnection` now parses
  optional `ping_host`, `ping_port`, `ping_token` (currently not sent by the control server — prepared
  for when the backend adds them).
- **Prototype fallback** (until the backend is upgraded): host `udpv4.netztest.at` for an IPv4
  connection / `udpv6.netztest.at` for IPv6 (decided from `client_remote_ip`), port **444**, token =
  the v2 token from `test_token`.

### 3. The ping itself
Reuses `UdpHmacPingFlow` + `PingClientConfiguration` from the `pingClient` module (the same client the
coverage/signal measurement uses), with the established protocol headers `RP01` / `RR01` / `RE01`,
**100 ms interval**. The wire packet is `protocolId(4) ‖ seq(4) ‖ token(16)`.

**Timeout = 3000 ms (loaded ping).** A packet is sent every 100 ms regardless of load (verified: the
sender doesn't block on a saturated uplink). **Every** ping is recorded exactly once, keyed by its
send time:
- response within 3 s → recorded with its round-trip value;
- no response within 3 s → recorded as **lost** (`value_ms = null`).

Results arrive out of order (a lost ping is only known after the 3 s timeout), so the list is sorted
by send time before submission. The running count covers *all* pings, not just the returned ones.

The ping flow is started in `TestControllerImpl` right after `onClientReady` and cancelled in the
final-state cleanup (and in `reset()`), so it spans the entire measurement. Each `PingResult.Success`:
- updates the UI (`onUdpPingChanged(ms)`), and
- is recorded as `(t_ns since measurement start, value_ms)`.

### 4. UI — `UDP-Ping: 12,3 ms (#12)` / `UDP-Ping: lost (#12)`
A line below the Ping/Down/Up row in `measurement_bottom_view`, bound via `app:udpPingMs` +
`app:udpPingCount`. Note there are **two** copies of this layout — `layout/` and `layout-land/` — so
the line must be added to **both** (it's not actually a shared file). Shows the latest ping and the
running count of all pings — e.g. `UDP-Ping: 12,3 ms (#12)` (one decimal, device-locale formatted),
or **`UDP-Ping: lost (#12)`** when that ping was lost. Hidden until the first ping.

Plumbing: `TestProgressListener.onUdpPingChanged` → `MeasurementService` (testListener +
`ClientAggregator`) → `MeasurementClient.onUdpPingChanged` → `MeasurementViewModel` →
`MeasurementViewState.udpPingMs` → layout. Also exposed on `MeasurementProducer` so a re-bound
activity restores the last value.

### 5. Submission — array in `/testresult` (prototype shape)
`TestResultBody` gains an optional field:

```json
"udp_pings": [
  { "t_ns": <long, ns from measurement start (send time)>, "value_ms": <float, ms> },   // returned
  { "t_ns": <long>, "value_ms": null }                                                  // lost
]
```

`value_ms` is `null` for a lost ping. Gson omits null fields by default, so a small local
`UdpPingBodySerializer` (registered only for `UdpPingBody`) forces an explicit `"value_ms": null`
without changing null handling for any other request.

Because the backend isn't upgraded yet, the samples are handed over **in memory** (no DB schema
change): `TestControllerImpl` writes them to `UdpPingResultStore` keyed by test UUID at the end of the
measurement; `ResultsRepositoryImpl` drains the store and sets `body.udpPings` (via `copy()`) just
before sending. Field names are a prototype choice — change `UdpPingBody`'s `@SerializedName`s to match
the final backend.

**Endpoint.** The array rides along in the normal test-result submission — the `sendTestResultsUrl`,
i.e. `https://<control-server-host>/RMBTControlServer/result` (POST, body = `TestResultBody`). No new
endpoint. Observed on device:

```
POST https://c01v4.netztest.at/RMBTControlServer/result
{ … , "udp_pings":[
    {"t_ns":87575521,"value_ms":14.78},
    {"t_ns":193045365,"value_ms":18.46},
    {"t_ns":310583542,"value_ms":35.59},
    …
] }
```

## Files changed

| File | Change |
|---|---|
| `rmbt-client/.../helper/ControlServerConnection.kt` | parse + expose optional `ping_host`/`ping_port`/`ping_token` |
| `control_client/.../Requests.kt` | `TestResultBody.udp_pings` + new `UdpPingBody` |
| `core/.../test/UdpPingResultStore.kt` | **new** in-memory hand-off (testUUID → samples) |
| `core/.../test/TestProgressListener.kt` | `onUdpPingChanged(Float)` (default impl) |
| `core/.../test/TestControllerImpl.kt` | extract v2 token, start/stop UDP ping over the whole test, collect + push samples |
| `core/.../measurement/MeasurementClient.kt` | `onUdpPingChanged` (default impl) |
| `core/.../measurement/MeasurementProducer.kt` | `udpPingMs: Float?` |
| `core/.../measurement/MeasurementService.kt` | forward UDP ping (testListener + aggregator + producer) |
| `core/.../data/repository/ResultsRepositoryImpl.kt` | attach `udpPings` to the body before send |
| `app/.../viewmodel/MeasurementViewModel.kt` | `onUdpPingChanged` → state; restore on reconnect |
| `app/.../ui/viewstate/MeasurementViewState.kt` | `udpPingMs` observable |
| `app/.../ui/DataBindingAdapters.kt` | `app:udpPingMs` adapter ("UDP-Ping: %.0f ms", hide if null) |
| `app/.../res/layout/measurement_bottom_view.xml` | `textUdpPing` line |
| `app/.../res/values/strings.xml` | `measurement_udp_ping` |

## Verified on device (motorola edge 70)
- v2 token extracted from `test_token` (len 24), endpoint selected by IP version
  (`udpv4.netztest.at:444` on IPv4, `udpv6.netztest.at:444` on IPv6).
- Pings are sent every 100 ms for the whole measurement; on success the value would show as
  `UDP-Ping: xxx ms` and be submitted as `udp_pings`.

## Open item
At the time of testing the prototype endpoint returned **no responses** (all pings `Lost`, no `RE01`
rejection, no socket error) — the **v2 token was not valid for the UDP-ping server** (to be fixed
server-side). Once the token is correct, values will appear and `udp_pings` will be populated; no
client change expected.

### Debug logging (can be removed later)
`TestControllerImpl.startUdpPing` logs the endpoint + token source/length; ping results log
server/client errors (and lost pings at verbose). Useful while validating the token.
