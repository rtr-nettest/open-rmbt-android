# Dependency upgrade audit & plan

Versions verified against Maven Central / Google Maven in June 2026.

## Hard constraints (gate everything)

1. **Kotlin is capped at 2.2.x** by the kapt annotation processors. Dagger's bundled
   kotlin-metadata reader (even in the latest 2.56.2) only reads metadata up to 2.2.0, and
   Kotlin 2.4.0 emits 2.4.0 metadata. KSP (the usual escape hatch) latest 2.3.9 also predates
   Kotlin 2.4. So **2.4.0 stays blocked** until Dagger supports it or Dagger is moved to KSP.
2. **compileSdk 36 + AGP + Gradle are coupled.** compileSdk 36 is only officially supported
   from **AGP 8.9+** (8.8.0 warns). Each AGP needs a matching Gradle; AGP 9.x additionally
   needs Gradle 9 and breaks legacy plugins.

## Current → latest

### Build tooling (high impact, coupled)
| Dep | Current | Latest | Recommendation |
| --- | --- | --- | --- |
| Kotlin | 2.2.0 | 2.4.0 | stay 2.2.x (2.4 blocked by Dagger/kapt) |
| AGP | 8.8.0 | 9.2.1 | **8.13.2** (clears compileSdk-36 warning); 9.2.1 = major |
| Gradle | 8.10.2 | 9.5.1 | **8.14.x** (pairs with AGP 8.13; future Kotlin needs 8.14.4); 9.5.1 = major |
| detekt (plugin) | **1.0.1** | 1.23.8 | bump to 1.23.8 — **needs DSL migration** in tools/script-detekt.gradle |
| dokka (plugin) | **0.10.0** | 2.0.0 | 1.9.20 (last old-style) or 2.0.0 (new plugin API) — **needs config rewrite** |

### AndroidX / Google (mostly safe)
| Dep | Current | Latest |
| --- | --- | --- |
| androidx core-ktx | 1.13.1 | 1.19.0 |
| appcompat | 1.7.0 | 1.7.1 |
| material | 1.12.0 | 1.14.0 |
| lifecycle | 2.7.0 | 2.10.0 |
| work-runtime | 2.9.0 | 2.11.2 |
| paging | 3.2.1 | 3.5.0 |
| room | 2.7.2 | 2.8.4 |
| navigation | 2.9.8 | 2.9.8 (latest) |
| play-services-location | 21.2.0 | 21.3.0 |
| play-services-maps (`google_services`) | 20.0.0 | ~current |
| firebase-crashlytics | 18.6.4 | 20.0.6 (use Firebase BoM) |
| core-splashscreen | 1.0.1 | 1.0.1 (latest stable) |
| constraintlayout | 2.1.4 | 2.2.x (verify) |
| preference / swiperefresh / vectordrawable | 1.2.1 / 1.1.0 / 1.2.0 | already latest stable |

### Third-party
| Dep | Current | Latest |
| --- | --- | --- |
| coroutines | 1.8.0 | 1.10.2 |
| dagger | 2.56.2 | 2.56.2 (latest; caps Kotlin at 2.2) |
| retrofit | 2.11.0 | 2.12.0 (3.0.0 = major, needs okhttp 4.12+) |
| okhttp logging-interceptor | 5.0.0-alpha.14 | 5.0.0-alpha.16 / 4.12.0 stable — align with retrofit's okhttp |
| gson | 2.10.1 | 2.14.0 |
| guava | 31.0.1-android | 33.4.8-android |
| joda (net.danlew:android.joda) | 2.12.7 | ~2.13.x (or migrate to java.time + desugaring) |
| dnsjava | 3.5.3 | ~3.6.x |
| timber | 5.0.1 | 5.0.1 (latest) |

### Test
junit-ktx 1.1.5→1.2.1, test_runner 1.5.2→1.6.2, espresso 3.5.1→3.6.1, rules 1.5.0→1.6.1, junit 4.13.2 (latest).

### Pinned / abandoned (no upgrade)
`com.github.zcweng:switch-button` 0.0.3 (dead), `net.sf.jopt-simple` 5.0.4 (last release).
(`io.noties.markwon` 4.6.2 removed — it was dead code: `MarkwonBuilder`/`MarkwonThemePlugin` had no callers.)

## Execution plan

- **Group 1 — runtime deps (safe, no config changes):** coroutines 1.10.2, core-ktx 1.19.0,
  appcompat 1.7.1, material 1.14.0, lifecycle 2.10.0, work 2.11.2, paging 3.5.0, room 2.8.4,
  gson 2.14.0, guava 33.4.8-android, retrofit 2.12.0, test libs.
- **Group 2 — tooling (coordinated):** AGP 8.13.2 + Gradle 8.14.x, plus migrating detekt
  (1.23.8) and dokka if the old plugins break under the newer Gradle.
- **Group 3 — deliberate/breaking (not in this pass):** AGP 9 + Gradle 9.5.1, Retrofit 3.0 +
  OkHttp alignment, Firebase BoM/Crashlytics 20.x, dokka 2.0 rewrite, Kotlin 2.4 (blocked).

## Status — Groups 1 & 2 applied and verified (build + device)

**Applied (dependencies.gradle / build.gradle / gradle-wrapper.properties):**

- Kotlin 2.2.0 (from prior task), Dagger 2.56.2, navigation 2.9.8.
- **Group 1 runtime deps:** coroutines 1.10.2, appcompat 1.7.1, material 1.14.0,
  lifecycle 2.10.0, work 2.11.2, paging 3.5.0, room 2.8.4, gson 2.14.0,
  guava 33.4.8-android, retrofit 2.12.0, junit-ktx 1.2.1, test_runner 1.6.2,
  espresso 3.6.1, rules 1.6.1.
  - **androidx core-ktx capped at 1.16.0** (not the latest 1.19.0): core 1.17.0+ require
    **AGP 9.1.0+**, which is beyond the AGP 8.13.2 we target here. 1.19.0 belongs with the
    AGP-9 upgrade (Group 3).
- **Group 2 tooling:** AGP 8.8.0 → **8.13.2** (clears the compileSdk-36 warning),
  Gradle 8.10.2 → **8.14.3**.
  - detekt **1.0.1** and dokka **0.10.0** still *apply* under Gradle 8.14.3 (deprecation
    warnings only, as before), so they were left as-is. Bumping them (detekt 1.23.8 /
    dokka 1.9.20+) needs the DSL rewrites noted above and is deferred to Group 3.

**Verification:** `:app:assembleRmbtDebug` (rmbt flavor) succeeds; app installs, launches, and
Home/History/Statistics/Map navigation works on device with no crashes.

## Status — Group 3 (attempted)

**Applied & verified (build + device):**

- **Minor bumps:** constraintlayout 2.2.1, dnsjava 3.6.3, net.danlew:android.joda 2.13.1,
  play-services-location 21.3.0.
- **Retrofit 2.12.0 → 3.0.0** with **OkHttp alignment**: logging-interceptor
  5.0.0-alpha.14 → **4.12.0** (Retrofit 3.0 uses OkHttp 4.12; the project had been pulling an
  OkHttp 5 *alpha* via the logging interceptor — now consistent on stable 4.12.0).
  Verified: settings request over the network succeeds on device, no crash.

**Blocked — reverted to the Group-2 base (AGP 8.13.2 / Gradle 8.14.3 / core-ktx 1.16.0):**

- **AGP 9.2.1 + Gradle 9.5.1 + core-ktx 1.19.0** is a project-wide migration, not a bump.
  Surfaced blockers:
  - **Gradle 9 removed `org.gradle.util.ConfigureUtil`** → the ancient detekt 1.0.1 and
    dokka 0.10.0 plugins fail to evaluate. (Their Gradle-9-compatible replacements are
    themselves uncertain — detekt 1.23.8 / dokka 2.0.0 predate Gradle 9.)
  - **AGP 9 ships built-in Kotlin** → `Cannot add extension with name 'kotlin'` because every
    module applies `kotlin-android` (KGP). Needs opting out of built-in Kotlin or migrating.
  - **AGP 9 requires `compileSdk`** (project uses the removed `compileSdkVersion`) in every
    module; plus `kotlinOptions` → `compilerOptions`, and kapt-integration questions.
  - core-ktx 1.19.0 (and other AGP-9-only AndroidX) depend on this, so they stay capped.
- **Kotlin 2.4:** still blocked by Dagger/kapt (max metadata 2.2.0).
- **Firebase Crashlytics 20.x:** not attempted — the Firebase/Crashlytics plugins live in
  `private/` and are commented out; a bump needs the Firebase BoM + crashlytics-gradle 3.x +
  google-services plugin re-enabled. Left as a deliberate follow-up.
- **detekt 1.23.8 / dokka 2.0:** not migrated — they still apply fine on Gradle 8.14.3, so
  left as-is; they only *need* migration as part of the AGP-9/Gradle-9 move.

### Net result after Groups 1–3
Kotlin 2.2.0 (K2), AGP 8.13.2, Gradle 8.14.3, Dagger 2.56.2, Room 2.8.4, navigation 2.9.8,
Retrofit 3.0.0 + OkHttp 4.12.0, and latest-compatible AndroidX/material/lifecycle/work/paging
(+ minor bumps). The remaining frontier (AGP 9 / Gradle 9 / Kotlin 2.4 / core-ktx 1.19.0 /
Firebase 20.x) is gated on a dedicated AGP-9 migration and on Dagger catching up to Kotlin 2.4.
</content>
