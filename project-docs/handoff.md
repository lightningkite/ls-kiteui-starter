# Handoff — HeroScript build session

Written at the end of a long build session (2026-05-19 → 2026-05-20). Future-me reads this first when picking the project back up.

## 1. Read these in order before doing anything

1. `project-docs/prd.txt` — original PRD. Background.
2. `shared/src/commonMain/kotlin/com/heroscript/models.kt` — the actual model. Source of truth for data shape. `project-docs/planned-models.kt` is a stale copy left in place for reference.
3. `project-docs/ui.md` — screen-by-screen design.
4. `project-docs/user-flows.md` — task-by-task: what someone comes into the app to do.
5. `project-docs/build-plan.md` — the build phases AND the prompt template used to dispatch screen-building subagents.
6. `project-docs/hipaa-compliance-todo.md` — 101 TODOs, annotated by a secondary auditor with `[REQUIRED]` / `[ADDRESSABLE]` / `[NOT-REQUIRED — BEST PRACTICE]` / `[NOT-REQUIRED — NICE TO HAVE]` / `[CONDITIONAL]` / `[CITATION ERROR]` markers. Top section has exec summary + corrections; bottom appendix covers CE-vs-BA framing and TN state-law preemption.
7. `project-docs/ux-recommendations.md` — 33 UX recs (13 high / 10 medium / 10 low) + 7 bugs found during audit.
8. `FEEDBACK.md` (repo root) — running log of upstream tooling/library gaps discovered during the build.
9. `project-docs/questions.md` — proposal-stage open questions, mostly resolved.
10. `project-docs/transcript.md` — discovery-call notes from the founder.

## 2. What's built and working

**Phases 0-6 of `build-plan.md` are complete.** Everything compiles green. Verified end-to-end in browser by multiple subagents.

- Phase 0: package renamed to `com.heroscript`, models in shared, endpoints with role-scoped permissions, SDK regenerated, `DashboardPage` placeholder.
- Phase 1: Login + MFA (via KiteUI `AuthComponent`), invite activation, mobile-first nav shell.
- Phase 2: Patients, Catalog read-only, Profile, `AddressEditor` component, `PatientPicker` / `ProductPicker` extracted.
- Phase 3: Orders list, Order Detail with status timeline + sibling-order subpanel + shipment subpanel, Order Entry (the 900+ line central screen — new-Rx + refill modes, prescription composer, pharmacy comparison, ID.me stub modal).
- Phase 4: Refill Queue, Clinic Settings, role-segmented Dashboard.
- Phase 5: Pharmacies, Clinics, Users, Invoices (dual-context), DEA Queue, Network Order Monitor, Catalog Ops edit.
- Phase 6: `AppRelease` forced-update interstitial, active-clinic switcher chip.
- Phase 7 (post-browser-test fixes): Invoices crash fix (reactive loop), Order Monitor list fix, nav role-gating restored via `NavGroup.hidden`, KiteUI debug logger silenced via `consoleFilter.js.kt` monkey-patch.
- Two HIPAA permission bugs fixed: `Shipment` reads scoped to clinic via denorm `clinics: Set<Clinic.ID>`; `UserEndpoints` `readMask` nulls `prescriber` for non-self/non-Ops/non-ClinicAdmin readers.

## 3. Six PRD deviations baked in — do NOT "fix"

These were intentional decisions. Future-me will be tempted to revert one of them on first read; don't.

1. **Catalog is product + variants, not flat SKUs.** `Product` owns `Set<Form>` directly (one Form per FormType per Product, service-layer enforced). `ProductPharmacyMapping` carries concrete strength/quantity, with `null` meaning "customizable — prescriber fills in." PRD § 09 implies flat SKUs.
2. **Sigs are freehand strings**, not pre-created and not templated. Decided twice; the second decision (freehand) is final. PRD § F1 implies pre-created.
3. **Pharmacy licensing filters on `destination.address.state`** (ship-to), not on patient residence. PRD § F2 / § F3 says "patient's state" — the PRD has a bug there.
4. **Mobile-first, extending to desktop.** PRD § 03 says desktop-only with mobile deferred. Inverted.
5. **Default `Theme.flat2`, no custom themes/gaps.** Variance comes later. Don't reach for `Edges(...)` without a stated reason.
6. **Audit log and notification mechanism live outside the canonical models.** User direction. The HIPAA secondary auditor flagged that the audit-log-out-of-models choice fights Privacy Rule § 164.528 (Accounting of Disclosures); future-me should weigh this against whatever the "different way" plan is for those mechanisms.

## 4. Five integration boundaries still stubbed before V1 launch

Every screen's UI is real and works end-to-end. These five external hookups are the actual work between now and pilot:

1. **ID.me real verification** — currently a 1-second `delay()` that always succeeds; writes `clinicianReview.idEvent = "stub-<uuid>"`.
2. **Pharmacy adapter dispatch** — orders land in the DB with `clinicianReview` set, but nothing goes to a pharmacy.
3. **Pharmacy webhook ingestion** of Accepted / In Process / Shipped events — status timeline derives from model fields that aren't yet populated by external traffic.
4. **Twilio SMS dispatch** on shipped events — Notification mechanism out of canonical models.
5. **Smarty / Lob address verification** — `AddressEditor` currently stamps `verifiedAt = now, verificationProvider = "manual"`.

`user-flows.md` ends with these five plus a demo-presentation guide listing what to lead with and what to call out as placeholder.

## 5. Outstanding work, ranked

### Demo-blocker bugs (user paused on these — "stop and review")

From `ux-recommendations.md` bug section, all reproduce in browser:

1. **Clinic Settings completely blank for ClinicAdmin** after reload (entire content area stays empty 6+ seconds and a reload). Breaks the ClinicAdmin role entirely.
2. **Login Submit-PIN button stalls indefinitely** after entering a valid email PIN; backend session succeeds, UI never advances. Blocks Ops-user demo unless using Root token paste.
3. **Pharmacy comparison panel empty on Order Entry** when product/form/strength set against seeded mapping. ⚠️ Contradicts earlier final-fixes verification — either a regression, seed-data drift, or the audit + final-fixes tested different states. Verify before fixing.
4. **Sign-out icon click during dashboard "No active clinic" flash state** causes the clinic to load and the sign-out to not execute. Same root cause as the session+role-cache race below.

### Top-priority UX recommendation (single most impactful)

> Session+role-cache resolution race causes dashboards and detail screens to flash empty headers before content loads. Fix with a top-level ready-gate. UX agent called this out as "lift the polish floor on every screen at once."

### Lower-priority bugs (from audit)

5. Countdown ticks past zero (-3) on "Can send new code in N".
6. Admin Catalog list shows no products despite Prescriber seeing them — permission-filter mismatch.
7. Patients list and Pharmacy state-licensing render empty-state text alongside data — contradictory UI.

### Architectural tensions worth your decision (HIPAA audit)

- **`Patient.allergies/diseases/otherMedications` storage vs "not an EMR" framing** — agent recommends gating storage to whether destination pharmacy's adapter actually requires the data + application-layer envelope encryption.
- **`smsConsent` / `emailConsent` as nullable Instants** conflate "never consented" with "revoked" — recommend paired `*RevokedAt` fields.
- **`PrescriptionOrder` denormalized snapshots are right for medical-record immutability** but complicate § 164.526 right-to-amendment. Document policy: amendments don't retroactively update historical orders.
- **AppStoreTester user with hardcoded password + ram-default `settings.json`** — accidentally landing in production is a workforce-security violation. User skipped fixing this on first pass; it's in HIPAA TODO #3-area.

### HIPAA — 35 V1-launch-blockers

The first audit identified 35 V1-launch blockers; the secondary auditor's marker key shows 51 total Required and 26 Addressable across 101 TODOs. The single largest engineering gap (per the agent): **no audit log = breach blindness**, can't satisfy § 164.528 (Accounting of Disclosures) or scope a breach. User's "different way" plan for audit logs needs to produce a queryable, per-patient, 6-year-retained store.

Top three legal corrections by the secondary auditor:

1. The "no hard delete of patient data" rule comes from **state law (TN: 10 years post last contact)**, not HIPAA's 6-year retention. HIPAA's 6-year applies to documentation + audit logs, not clinical records.
2. "HIPAA training for clinic users" is the **clinic's obligation** (the Covered Entity), not HeroScript's (the Business Associate).
3. Numeric thresholds (15-min timeout, 4-hour termination SLA, etc.) where HIPAA names a standard but is silent on the threshold should be best-practice, not Required.

4 new TODOs added (98-101) for items the first audit missed; most consequential is **#101 (§ 164.504(e)(2)(ii)(C))**: a Business Associate must report any impermissible use/disclosure to the Covered Entity, not only breaches.

## 6. Dev environment state at session end

- **Servers**: I left `prepare-browser-test.sh` started — backend on `:8081`, frontend on `:8941`. The frontend uses `apiOption = SameServer` so requests proxy through Vite. The running backend is **stale** (booted before the HIPAA permission-bug fixes landed); restart with `./testing/stop-all.sh && ./testing/prepare-browser-test.sh` to pick up the changes.
- **Vite doesn't auto-rebuild Kotlin source changes.** After any `apps/src/.../*.kt` edit, run `./gradlew :apps:compileDevelopmentExecutableKotlinJs` and then hard-refresh the browser. Considered as a testing-scripts improvement in FEEDBACK.md.
- **Sandbox restriction**: `./gradlew` calls need `dangerouslyDisableSandbox: true` on the Bash tool.
- **Seed data populated** by `Seed.kt` on startup: 1 clinic (Gameday Knoxville, TN), 2 pharmacies (LifeFile Compounding + Empower), 2 products (Semaglutide + Testosterone Cypionate), 2 patients (Sam Sample, Jordan Jones), 3 orders (mix of Pending submission / Shipped / Submitted-stuck), 1 invoice. 4 users: clinicadmin@example.com, prescriber@example.com, ma@example.com, admin@example.com.
- **Admin tokens** in `testing/.admin-token` (Root tokens from the dev backend — bypass email-PIN MFA via localStorage paste; the script's startup log shows the paste snippet).

## 7. The patterns to follow for new screens

Established by Phases 2-5:

- `@Routable` paths match `ui.md`.
- Every detail page extends `PageWithParent` (one-file helper at repo root of apps `common`).
- Inline `Signal<T?>` for edit-state, edit-mode `Signal<Boolean>`. No Draft wrapper ported from USBE; the agent attempts justified skipping it twice. Re-evaluate on the 4th+ edit screen.
- Same-screen list↔detail-as-routes; desktop adds density, never separate codepaths.
- Reactive bindings via `::content { }`, `::enabled { }`, `::exists { }`.
- N+1 lookups on lists are accepted at V1 pilot scale — flagged TODO for batching.
- Pickers extracted in `apps/.../views/components/`: `PatientPicker`, `ProductPicker`, `PharmacyPicker`. Add `PrescriberPicker` if you're going to use it a third time (currently inline in OrderEntryPage + RefillQueuePage).
- Status derivation for PrescriptionOrder is in `apps/.../extensions/PrescriptionOrder.ext.kt`; reuse it.
- Inline confirm rows / inline review modals instead of dialog primitives (sibling agents diverged slightly here; future consolidation pass would help).
- `ModelPermissions.readMask { mask { it.field.mask(value = null, unless = ...) } }` — pattern lifted from `lightning-server/demo/Server.kt` and now used in `UserEndpoints` to mask `prescriber` for non-privileged readers.
- For Address fields, use `AddressEditor` from `views/components/AddressEditor.kt` — handles the `VerifiedAddress` lens and Smarty/Lob stub.
- Cents formatting: `"$%.2f".format(cents / 100.0)`. No money library.
- Don't introduce custom theme tokens, custom colors, or custom gap values. Default `Theme.flat2` only.

## 8. Reference code to look at

- `/Users/jivie/Projects/usbe-ar26299/apps/src/commonMain/kotlin/edu/usbe/ar26299/` — the canonical pattern reference. `views/courses/template.md` is a comprehensive screen-building guide. Don't blindly copy package paths; many USBE-specific helpers (`PageWithFilter`, `sidewiseMode`, `slightCard`, `ToggleForm`, `Draft`) don't exist in herxoscript and were intentionally NOT ported.
- `~/Projects/kiteui/` — local KiteUI sources. Used by the debug-logger silencer agent to find the source of console flood.
- `~/Projects/lightning-server/` — local Lightning Server sources. Useful when looking up `ModelPermissions` patterns.

## 9. Things that bit us — don't repeat

- **Parallel agent compile races.** When dispatching 3+ subagents to build sibling screens, each one's mid-flight `./gradlew compileKotlinJs` may fail because a sibling's file hasn't been written yet. Final orchestrator-level verify is the source of truth, not per-agent reports. Recorded in FEEDBACK.md.
- **15-minute check-in cadence on long-running browser agents.** First browser-test agent went 60 min without a reply to three pings; second one nailed the cadence. The check-ins are useful for "alive?" but the agents prefer not to interrupt for status. Don't take silence as failure.
- **AskUserQuestion can race with task completion.** The "kill the agent" decision raced with the agent completing on its own. The agent's report was excellent and the kill was moot. Try not to leave AskUserQuestion open while critical agents are about to finish.
- **`@QueryParameter` enums need `@Serializable`.** Six pages had this bug; the page transition serialization would crash every navigation. Add it everywhere.
- **`remember { ... }` reading from `rememberSuspending { ... }`** causes synchronous re-entrancy / JS stack overflow because the non-suspending invoke throws `ReactiveLoading` during `notReady → ready` transitions. The fix is to use `rememberSuspending` consistently for the dependent chain.
- **`Select.bind`'s `render: (T) -> String` is synchronous** — doesn't compose with async lookups. Use card-button-radio fallback when you need reactive labels.
- **Vite doesn't auto-rebuild Kotlin** — was a real time-sink for browser agents. Always recompile after edits.
- **KiteUI debug logger floods the JS console** — silenced via `consoleFilter.js.kt` monkey-patch; upstream FR in FEEDBACK.md.

## 10. Tasks state

All 11 tasks marked completed. Tomorrow will be a fresh task list. The main unfinished work is the **demo-blocker bugs** (4 of them — see § 5), the **session+role-cache race** (top UX rec), and the **HIPAA blockers** (51 Required items, audit-log-from-canonical-or-not decision being the biggest).

## 11. Suggested first move tomorrow

Read `hipaa-compliance-todo.md`'s executive summary + the four architectural-tension notes. Decide on the audit-log strategy. That decision unblocks the largest single bucket of HIPAA work.

After that: fix the four demo-blocker bugs in one focused agent (Clinic Settings blank, login PIN stall, pharmacy comparison empty, sign-out race), then dispatch the session+role-cache race fix. After those land, the app is genuinely demo-ready.

The build-plan's prompt template still applies for any new screen work; reuse it.

## 12. The user — Joe

Lightning Kite. Likes:
- Subagents for everything substantive (don't do screen-building inline).
- Tight scope per agent. Long agent runs erode trust.
- Honest assessment. "I noticed X looks wrong, want me to check?" beats "everything is great."
- Asking before destructive/visible actions.
- Pausing for review at meaningful boundaries (not after every screen, but before committing or running a big sweep).
- "Push straight on" when the path is clear.

The user has a global methodology in `~/.claude/CLAUDE.md` worth re-reading.
