# HeroScript apps/ build plan

Plan to implement the UI sketched in `project-docs/ui.md` against the models in `project-docs/planned-models.kt`. Read both before assigning work.

## Ground rules (every screen, every agent)

- **Theme**: use the default `Theme.flat2("default", Angle(0.55f))` declared in `apps/.../App.kt`. No custom theme, no semantic tokens, no color overrides — yet. Variance comes later.
- **Spacing**: no custom gaps, paddings, or margins unless there is a sensible, stated reason (e.g. "the icon collides with the label at default spacing"). The theme's defaults are the right answer 95% of the time.
- **Mobile-first**: every screen designed for narrow viewports first, then add desktop density. Don't introduce `if (sidewise)` branches until the mobile layout actually composes. Touch targets ≥ 44pt.
- **Reference, don't copy blindly**: `/Users/jivie/Projects/usbe-ar26299/apps/src/commonMain` has high-quality patterns. Read the equivalent screen there before writing a new one here. The package names and some project-specific helpers (`PageWithParent`, `PageWithFilter`, `sidewiseMode`, `slightCard`, etc.) won't exist here yet — we'll build the ones we need as we go, not all up-front.
- **No premature abstraction**: build the third instance of a pattern before extracting a helper. Two screens with similar layout is fine; three is when it deserves a function.
- **Source of truth**: `project-docs/ui.md` for what each screen does. `project-docs/planned-models.kt` for the data shape. PRD only as background context — `ui.md` already reconciles deviations.
- **Sigs are freehand**, **catalog is product+variants**, **ship-to drives pharmacy licensing** (not patient state), **mobile-first**. These are documented deviations from the PRD; do not "fix" them.

## Directory layout

```
apps/src/commonMain/kotlin/com/heroscript/
├── App.kt                        # nav, theme, root
├── theme/                        # only as needed (semantics, dimensions)
├── extensions/                   # reusable lenses, formatters, predicates
├── sdk/                          # SDK config (already partially present)
├── utils/                        # fcm setup, etc. (already present)
└── views/
    ├── auth/                     # Login, MFA, invite activation
    ├── dashboard/                # Clinic Dashboard
    ├── components/               # cross-screen widgets (chips, address editor, etc.)
    ├── orders/                   # Order list / Entry / Detail
    ├── refills/                  # Refill Queue
    ├── patients/                 # Patient list / detail / new
    ├── catalog/                  # Product list / detail (read-only + Ops edit)
    ├── pharmacies/               # Pharmacy list / detail (Ops)
    ├── clinics/                  # Clinic list / detail (Ops) + Clinic Settings (ClinicAdmin)
    ├── users/                    # User list / detail (Ops) + DEA queue
    ├── profile/                  # current-user profile + DEA management
    ├── ops/                      # Network Order Monitor
    └── invoices/                 # ClinicInvoice list / detail
```

The package will likely move from `com.lightningkite.lskiteuistarter` to `com.heroscript` early on. Don't author new code in the old package — first move, then build.

## Phase plan

Order is sequenced so each phase makes the next testable end-to-end. Phases inside a row can run in parallel; phases across rows must be sequential.

### Phase 0 — Project setup (single agent, sequential)

Most of the renaming is automated by `personalize.main.kts` at the repo root.

1. Edit the bottom of `personalize.main.kts` to set:
   - `appName = "HeroScript"`
   - `packageName = "com.heroscript"`
   - `rootUsers = setOf( /* the founder's email(s) */ )`
   - `appStoreTesterEmail = "appstoretester@heroscript.com"` (or whatever)
2. Commit current state (the script warns it modifies many files). Run `kotlinc -script personalize.main.kts`. Verify `git status` looks right and the project compiles.
3. Move `project-docs/planned-models.kt` → `shared/src/commonMain/kotlin/com/heroscript/models.kt`. Re-run KSP if the script didn't pick it up: `./gradlew :shared:kspKotlinJs`. Verify `@GenerateDataClassPaths` generates path files.
4. Replace the starter models (`User`, `UserRole`, `FcmToken` from the starter) wherever they conflict — the planned models redefine `User`, `UserRole`, etc. Keep `FcmToken` (still useful). Keep `AppRelease`.
5. Stub `server/` endpoints for every model — `ModelRestEndpoints` with permissive permissions for now. Tighten as we go.
6. `./gradlew :server:generateSdk` succeeds. Commit.
7. Update `App.kt`: swap the default nav placeholder Home for `DashboardPage` (stub OK).

Steps 1-2 are the script's job. Steps 3-7 are manual but small.

### Phase 1 — Auth + nav foundation (single agent)

6. Login screen: email + password + MFA challenge. Replace existing `LoginScreen` content with HeroScript-specific copy + flows per `ui.md`.
7. Invite-link activation page: lands users from `ClinicMembership` invite emails, sets password + enrolls MFA.
8. `App.kt` nav: hamburger drawer on mobile, persistent left nav on desktop. Items role-gated per `User.role` and active `ClinicMembership.role`. Active-clinic switcher chip below top bar when membership count > 1.

### Phase 2 — Foundational patterns (parallelizable)

These three screens establish the three-screen list/detail/edit pattern. Build them first because everything else copies them.

9. **Patients** (list + detail + new). Lowest-complexity model with meaningful structure (verified addresses, clinical entries). Establishes the list/detail/edit pattern + the `VerifiedAddress` editor (Smarty/Lob).
10. **Catalog → Products** (list + detail, read-only mode for clinic users only; Ops edit deferred to Phase 5). Establishes how nested collections render (Forms editor, Mappings sublist).
11. **Profile** (current user). Establishes the "single record I own" pattern. DEA management subsection.

### Phase 3 — Core clinical flows (sequential within, parallel across the two flows)

12. **Orders list** + **Order Detail** (no entry yet — start by viewing existing orders).
13. **Order Entry** (the central, most complex screen). Depends on Patients + Catalog. Build last in this phase. Includes the new-Rx-vs-refill mode toggle, prescription composer, pharmacy comparison panel, ID.me modal.

### Phase 4 — Refills + clinic-admin features (parallelizable)

14. **Refill Queue** + one-click reorder routing into Order Entry.
15. **Clinic Settings** (ClinicAdmin's scoped view of their own clinic).
16. **Clinic Dashboard** (role-segmented widgets — last in this phase since it consumes data from prior phases).

### Phase 5 — Ops surface (parallelizable)

17. **Catalog Ops edit mode** (Products full CRUD, mappings, forms editor).
18. **Pharmacies** (list + detail with state-licensing matrix).
19. **Clinics** (Ops list + detail + Add-Clinic flow).
20. **Users** (Ops list + detail).
21. **DEA Verification Queue** (filtered view of `PrescriberLicensing`).
22. **Network Order Monitor** (= Orders list with no clinic filter + Ops-only actions + KPI tiles).
23. **Invoices** (clinic + Ops contexts).

### Phase 6 — Polish

24. Validation passes on every form; empty states wired everywhere; force-update interstitial via `AppRelease`; accessibility audit.

## Prompt template for screen-building subagents

Use this template per screen. Fill in every `«…»` placeholder. Leave the constraints section verbatim — those are the rules.

> Note: this template assumes the agent runs in the herxoscript repo and has access to read both reference projects. The orchestrator should pre-verify Phase 0 is complete before launching any Phase 2+ agent.

```
ROLE
You are implementing a single screen in the HeroScript apps module — a Kotlin Multiplatform app
using KiteUI for UI and Lightning Server for the backend. Mobile-first, extending to desktop.

CONTEXT TO READ FIRST
1. project-docs/ui.md — section: «paste the relevant screen's section verbatim from ui.md»
2. shared/src/commonMain/kotlin/com/heroscript/models.kt — the data model is the source of
   truth for fields, IDs, denorms, and lifecycle.
3. /Users/jivie/Projects/usbe-ar26299/apps/src/commonMain/kotlin/edu/usbe/ar26299/views/«analogous reference path» —
   read this fully before writing. It is the canonical example of similar layout/composition.
   Note: package paths and some helpers (PageWithParent, PageWithFilter, slightCard, sidewiseMode)
   don't exist in herxoscript yet — copy the LAYOUT and the data-flow shape, not the imports.
4. apps/src/commonMain/kotlin/com/heroscript/App.kt — nav structure and existing routing.
5. apps/src/commonMain/kotlin/com/heroscript/views/«any sibling screen we've built» — match its
   patterns. If none exist yet, you are establishing the pattern; be extra deliberate.

WHAT TO BUILD
«One paragraph describing the screen. Example: "Patient list screen at @Routable('patients').
Filterable list of Patient records scoped to the active clinic. Each row links to PatientDetailPage.
A '+' action in the top-right opens the new-patient flow (same screen as PatientDetailPage in
edit mode with empty values)."»

FILES TO CREATE
- apps/src/commonMain/kotlin/com/heroscript/views/«module»/«ScreenName».kt
- «additional files if the screen needs a Filters data class, a Components file, etc. —
  follow the pattern from the USBE reference»

CONSTRAINTS (do not deviate)
- Use the existing default theme: Theme.flat2 already configured in App.kt. Do NOT add custom
  themes, custom colors, custom semantic tokens, custom fonts.
- Do NOT introduce custom gaps, paddings, or margins. Use the theme defaults. If you find
  yourself reaching for a custom Edges(...), stop — pick a sibling element/layout that gives the
  right spacing for free. Exception: if there is a specific, stateable reason (e.g. "default
  spacing visibly clips the icon against the label"), include a one-line comment explaining why.
- Mobile-first: design the narrow-viewport layout first. Desktop adds density via responsive
  layout primitives (e.g. row that wraps to col, or `sidewise` when we have it), never via
  a separate codepath.
- No premature abstraction. Inline before extracting. Two similar pieces is fine; extract on
  the third.
- Reactive bindings via `::content { … }`, `::enabled { … }`, etc. — never recompute and reset
  imperatively.
- Permissions hidden, not disabled. If the current user can't do an action, don't render it.
- Don't add fields the model doesn't have. If the UI doc asks for something not in the model,
  pause and surface that to the orchestrator — do not invent fields.
- Don't add error handling, defensive null-coalescing, or fallbacks for cases that can't
  happen given the model's invariants.
- No comments unless something is non-obvious. Code is mostly self-documenting via naming.

DATA ACCESS
- Use currentSession() / sessionToken / sdk patterns already established in apps/.../sdk/.
- For lists: `remember { session().«model»s.query(Query(…))() }`.
- For single records: `remember { session().«model»s[id].awaitNotNull() }`.
- For drafts/edits: follow the Draft pattern from the USBE reference (likely not wired in
  herxoscript yet; if missing, ask the orchestrator before inventing one).

WHEN DONE
- Compile cleanly: `./gradlew :apps:jsBrowserDevelopmentRun` should build. Don't actually start
  the server; just confirm compile.
- Report what you built, what files changed, and what you noticed that should feed back into
  ui.md or the model. Flag any place you had to make a judgment call.
- If you found a model gap (a field the UI clearly needs but the model lacks), do NOT add the
  field. Surface it in the report so the human can decide.

OUT OF SCOPE FOR THIS TASK
«Bullet anything explicitly NOT to do — e.g. "do not implement the create flow on this list;
that's the next screen", "do not wire role-gating beyond what the UI doc specifies",
"do not implement the ID.me modal — placeholder OK".»

REPORT FORMAT
- Files created/modified (paths only)
- Screen behaviors implemented (1-line each)
- Anything skipped with reason
- Open questions for the human
```

## Filling the template per screen

The orchestrator fills three placeholders per screen:

1. **The `ui.md` excerpt** — copy the bulleted block for that screen verbatim, including PRD reference annotations.
2. **The USBE reference path** — pick the closest analog. Mapping table below.
3. **What's explicitly out of scope** for this iteration (the agent will otherwise scope-creep into adjacent screens).

### Screen → USBE reference path mapping

| HeroScript screen | USBE reference (best analog) |
|---|---|
| Patient list | `views/admin/users/UserListPage.kt` |
| Patient detail | `views/admin/users/UserDetailPage.kt` |
| Catalog (Product list, read-only) | `views/courses/courses/CourseListPage.kt` |
| Product detail (Ops edit) | `views/microcredentials/MicrocredentialManagementPage.kt` |
| Orders list | `views/admin/users/UserListPage.kt` (for the list scaffold) + `views/courses/sections/` (for status badges if any) |
| Order Entry | No close analog — see `views/courses/CourseApplicationForm.kt` for multi-section editor pattern |
| Order Detail | `views/microcredentials/MicrocredentialDetailPage.kt` |
| Refill Queue | `views/courses/MyCourses.kt` (similar "due-soon" listing) |
| Pharmacy list/detail | `views/admin/institutions/` |
| Clinic list/detail | `views/admin/schools/` or `views/admin/institutions/` |
| User list/detail (Ops) | `views/admin/users/` |
| Profile | `views/PreferencesScreen.kt` (closest, but small) |
| Network Order Monitor | Orders list reused with no filter |
| Clinic Dashboard | Build cold; reference `views/HomeScreen.kt` for layout primitives only |
| Filter pages | `views/courses/courses/CourseFilterPage.kt` |

### Default "out of scope" boilerplate

Most screen agents should be told:
- Do not implement related-entity creation from this screen (e.g. "create a patient inline from order entry" — that's a separate screen).
- Do not implement audit-log links — the audit mechanism is handled outside the canonical models.
- Do not implement Notification subpanels with live data — placeholder ("Notifications will appear here once the notification mechanism lands") is fine.
- Do not wire ID.me — placeholder modal that auto-succeeds is fine for now.
- Do not implement bulk actions unless the screen explicitly requires them in `ui.md`.

## Things the orchestrator does before kicking off agents

- Confirm the previous phase's screens compile.
- Verify the SDK is regenerated if any server endpoint changed.
- Pick a reference USBE path and read enough of it that you can answer the agent's "what does this widget do?" follow-up if it asks.
- If a screen needs a widget that doesn't exist yet (e.g. address verification, MFA challenge), spawn a component-building agent first with the same template adapted for a single widget.

## Open items the plan doesn't solve yet

- **`Draft` wrapper** for edit-mode pages: USBE uses one, herxoscript doesn't have it. Phase 2 will need it — first agent that needs it should build a minimal version, or we lift the USBE one wholesale.
- **`PageWithParent` + back-navigation pattern**: same — port from USBE on first need.
- **Address-verification widget** (Smarty/Lob): build as a component before Patients in Phase 2.
- **Permission helpers**: extension fns like `User.isOps`, `ClinicMembership.canPrescribe`. Add as needed, not up-front.
- **ID.me modal**: stub now, real integration is a separate vertical.
