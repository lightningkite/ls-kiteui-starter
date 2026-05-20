# Feedback / observations during HeroScript build

Running log of things discovered during build that future maintainers, upstream tool authors, or starter-template caretakers may care about. Append, don't rewrite.

## Phase 0 (project setup via `personalize.main.kts`)

### `personalize.main.kts` misses `AutoRoutes` imports

The kiteui-generated `AutoRoutes` symbol lives in `<newPackage>.views` (it's keyed off the package of the `@Routable` classes). Three platform main files reference `AutoRoutes` without an import and depend on the symbol being in scope:

- `apps/src/jsMain/.../Main.kt`
- `apps/src/iosMain/.../App.ios.kt`
- `apps/src/androidMain/.../MainActivity.kt`

The script moves the files but does not inject `import <newPackage>.views.AutoRoutes` into each. Result: `:apps:compileKotlinJs` fails after personalization until a human adds the imports manually. Easy fix to bake into the script — once the new package name is known, write the import into each platform's main file.

### `personalize.main.kts` omits the test source root

`movePackageDirectories` walks `apps/src/{common,android,ios,js}Main/kotlin`, `server/src/main/kotlin`, and `shared/src/commonMain/kotlin`. It does NOT touch `server/src/test/kotlin`. Result: `ServerTest.kt` ends up at the old package directory path even though its `package` declaration is the new package. Kotlin tolerates the mismatch, but the orphaned `com/lightningkite/<oldname>/` tree under `server/src/test/kotlin/` is left dangling and has to be moved + cleaned up manually.

Add `server/src/test/kotlin` to `sourceRoots` in the script.

### `package-lock.json` workspace names aren't updated

After personalize, `package-lock.json` still contained ~18 references to `lskiteuistarter-*` workspace names (`lskiteuistarter-apps`, `lskiteuistarter-shared`, etc.). Cosmetic, but visible to anyone running `npm` against the workspaces. Script could string-replace these in the same pass it does Kotlin files.

### `:server:generateSdk` auto-creates a dev `settings.json`

Running `./gradlew :server:generateSdk` from a clean checkout (no `settings.json`) silently creates one with ram-db, console email, freshly-generated `secretBasis`, etc. This conflicts with the project convention from `CLAUDE.md`: "Do not commit `settings.json` with real credentials." The auto-created one has no real credentials, but it now exists and shows in `git status`, inviting accidental commit. Either:

- Add `settings.json` to `.gitignore` if it isn't already (the starter likely does this; verify); or
- Have the gradle task emit `settings.local.json` instead, and only fall back to creating `settings.json` if neither exists.

## KiteUI gaps surfaced while building screens

### No native Instant/LocalDate picker
Multiple screens need date inputs against `Instant` or `LocalDate` (DEA expiration, state-license expiration, prescription `endsAt`, pharmacy state-info effective/expiration, invoice period filters). KiteUI doesn't expose a native date/time picker primitive that the agents could find, so every date input is currently an ISO-string text input with a label hint. Workable but ugly — adopt or build a `localDateField`/`instantField` primitive.

### `Select.bind` doesn't tolerate reactive lookups in `render`
`Select`'s `render: (T) -> String` is synchronous. When the rendered label needs a `rememberSuspending` lookup (e.g. user name from `User.ID`, clinic name from `Clinic.ID`), `Select` doesn't compose. Agents fell back to card-button-radio patterns instead. A reactive-aware variant (`select { itemContent { user -> text { ::content { user().displayName } } } }`) would remove a lot of awkward UI.

### `icon(Icon, String)` deprecation noise
Every screen produces deprecation warnings on `icon(...)` calls. The message says "Import has moved" but the move hasn't been applied to project files. Single cleanup sweep across the apps module would silence the noise. Unrelated to behavior — purely cosmetic.

### Missing icons in `Icon`
`Icon.chevronDown` doesn't exist in the current kiteui Icon set — the active-clinic switcher chip uses `Icon.menu` as the dropdown affordance instead. Several other "expected to exist" icons (e.g. directional chevrons, common medical glyphs) may be absent; worth a sweep when the icon set is next updated.

### No on/off switch for KiteUI's `Log` → `console.log` plumbing
`com.lightningkite.kiteui.debugger.js.kt`'s `LogRoot` / `PlatformLog` writes every `Log.log(...)` and `Log.info(...)` call straight to `console.log` / `console.info`. Internal loggers — `ScreenStack.bindToPlatform` is the worst — emit dozens of messages per navigation. In our browser-test harness this drove the JS console past 500 messages per page load, destabilized the Chrome MCP extension, and slowed the test loop ~4x.

`Log.interceptors` exists for observation only; it cannot suppress. `debugMode` and `Element.Debugger.debugTarget` gate a different (view-debug) channel.

Worked around in `apps/src/jsMain/.../utils/consoleFilter.js.kt` by monkey-patching `console.log` / `console.info` at app boot to drop messages whose first arg starts with a known kiteui internal tag (`ScreenStack.bindToPlatform`, `WS to `, `[KiteUI Hydration]`, `ElementLeaks`, etc.). Hacky — please add a real level switch (`LogRoot.minLevel = LogLevel.WARN`, or `kiteui { logLevel = "warn" }` in the gradle plugin) so apps can opt out cleanly. When that lands, delete `consoleFilter.js.kt` and the `installKiteUiLogFilter()` call in `Main.kt`.

## Endpoint / query DSL observations

### Nested-collection conditions don't compose
The DEA Verification Queue needs `User.prescriber.stateLicenses.any { it.review == null }`. The generated path DSL doesn't express the `any { … }` predicate cleanly over a `Set<StateMedicalLicense>` embedded inside a nullable struct. The agent fell back to fetching all `prescriber != null` users and filtering client-side. Acceptable at V1 pilot scale (≤100 prescribers); becomes a real problem at ≥10k. Either:
- Add a `Set<T>.anyMatching(predicate)` path operator, or
- Generate denormalized `User.hasPendingDeaReview` / `hasPendingStateLicenseReview` flags as `@Denormalized` bools.

### N+1 lookups on every list screen
Orders, Refills, Pharmacies, Users, Invoices all do per-row lookups (patient name, product name, clinic name, etc.) over their list pages. Acceptable at V1 pilot scale; flagged as TODO in every screen. A batched `session().users.byIds(setOfIds)` (or equivalent) primitive would let lists pre-fetch all referenced records in one round trip. Not blocking V1.

### Several list screens lack a clinic-id filter param
PatientListPage, OrdersListPage, InvoiceListPage do not accept a `clinic` constructor param yet. Cross-screen "View patients in this clinic" buttons from ClinicDetailPage / UserDetailPage land on the unfiltered list. Easy fix per screen; deferred.

## Model observations (no immediate changes required)

### `PrescriptionOrder.assignedTo: User.ID?` is effectively always set
In every save path we wire up, `assignedTo` gets set to the prescriber. Could be tightened to non-nullable in a future model pass. Not blocking.

### `User.updatedAt` is a coarse proxy for prescriber-field-last-changed
DEA Queue displays `updatedAt` as the "submitted-at" for a pending verification. This timestamp also bumps when any other User field changes (name, phone, etc.). If SLA tracking on DEA verifications matters, add a finer-grained `prescriber.updatedAt` field.

### Service-layer enforcement of Product.forms uniqueness
The model comment says "Only one entry per form type"; the UI enforces it on save; the model itself doesn't (it's a `Set<Form>` keyed by the value-class equality of the full `(FormType, strengthUnit, quantityUnit)` triple, so two Forms with the same FormType but different units coexist in the Set). Service-layer validation is the agreed path; flag for whoever owns the server endpoints to add the check.

## Build-process observations

### Parallel agent compiles race against each other
When dispatching 3-4 subagents in parallel to build sibling screens, each agent runs `./gradlew :apps:compileKotlinJs` independently. The first-to-finish often reports "build fails on sibling-agent's file" because the sibling's file hadn't been written yet. The final state was always green, but the per-agent reports were misleading. Worth knowing — final orchestrator verify is the source of truth.

### Pattern divergence between parallel agents
Three agents asked to follow the same established pattern produce three slightly different implementations of similar things (e.g. the inline review modal in UserDetailPage vs the inline confirm row in ClinicSettingsPage vs the inline reorder card in RefillQueuePage). Not bad individually; each is reasonable. Over time these will need to converge into a shared `inlineConfirm` / `inlineForm` component. The Phase 6 polish sweep didn't extract these — flag for a future consolidation pass.

## KiteUI library observations

### `::navItems { ... }` reactive lambda binding vs static list with reactive `hidden`
A prior browser-test hot-fix reported that the reactive-lambda-bound `::navItems { ... }` syntax inside `appNav(...) { ... }` "did not propagate" — the nav column never re-rendered after the session loaded. Restoring role-aware nav used a different pattern: keep the list literal (`appNavRef.navItems = listOf(...)`) but use each `NavLink`/`NavGroup`'s built-in `hidden: ReactiveContext.() -> Boolean` parameter to gate individual items. The data-class members `title`, `icon`, `count`, and `children` are all `ReactiveContext.() -> T` lambdas, which `navGroupColumnInner` re-evaluates correctly via `forEach { ::shown { ... } }`. This is the cleaner KiteUI-native idiom — the navItems list is structurally static, only visibility changes — and worked first try.

If the original `::navItems { reactive lambda }` binding is intended to support fully dynamic lists (not just visibility), that's a real KiteUI bug worth investigating upstream. From reading `AppNav.kt` and `data.kt`, the property-delegate Signal pattern *should* support it; we just didn't need it.

### `NavGroup` runtime status
The earlier hot-fix note also reported a stack overflow when `NavGroup` was used inside `appNav(...)`. The current restoration uses two `NavGroup`s and compiles clean against `:apps:compileKotlinJs` and `:apps:compileDevelopmentExecutableKotlinJs`. Runtime behaviour was NOT manually verified by this agent — needs a browser smoke test after this lands. If a stack overflow recurs, the fallback is a flat `NavLink` list with section-header `NavCustom` separators (or just relying on each item's own `hidden` gate without grouping).
