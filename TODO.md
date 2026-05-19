# HeroScript V1 — TODO

State as of 2026-05-19. Models and REST/CRUD are in (`shared/src/commonMain/.../models.kt`, `server/src/main/.../data/*Endpoints.kt`), seed runs, basic permission tests pass. Everything below is what's left between "endpoints exist" and "V1 pilot can launch."

Source-of-truth references:
- PRD: `project-docs/prd.txt`
- Confirmed UI strategy + screen list: `project-docs/ui.md`
- Data model + intent comments: `project-docs/planned-models.kt` (mirrors `shared/.../models.kt`)
- Proposal-stage decisions/assumptions: `project-docs/questions.md`
- Pharmacy sandbox info: `project-docs/lifefile-explanation.md` + `lifefile-sandbox.env`

Order roughly mirrors V1 launch criticality (10 clinics + ≥6 pharmacies + 2 weeks sustained ordering, PRD § 12).

---

## 1. Server gaps (workflow + integrations not covered by CRUD)

### 1.1 Pharmacy adapter layer (PRD § F5 — single biggest unknown)
- [ ] Define `interface PharmacyAdapter` with `placeOrder`, `fetchStatus`, `cancelOrder`, `fetchCatalog` per PRD § F5.
- [ ] Wire `PharmacyAdapterType` (`LifeFile` / `Empower` / `Proprietary`) → concrete adapter via Server runtime registry.
- [ ] LifeFile adapter — first concrete impl. Sandbox creds in `project-docs/lifefile-sandbox.env`; assume Hallandale = same shape per `lifefile-explanation.md`. Translate `Patient` + `Prescription` + `ProductPharmacyMapping` into LifeFile's order payload (clinical[] mirrors our `ClinicalEntry`, see model comments).
- [ ] Empower adapter — stub interface, real impl when API docs arrive.
- [ ] Proprietary adapter — stub interface; one per pharmacy as docs arrive (target ≥6 live, ramp to ~12).
- [ ] Submission dispatch task: triggered when `PrescriptionOrder.clinicianReview.approved == true`. Idempotency key on the order id. Retries with backoff. Writes `PrescriptionOrder.Fulfillment` + creates/updates `PharmacyOrder`. Failed dispatch → dead-letter queue + Ops alert (PRD § F5).
- [ ] Status polling / webhook ingestion: maps pharmacy response → `PharmacyOrder.accepted` / `totalRejection` / per-line `Fulfillment.accept|reject` and creates `Shipment` records on Shipped events. Webhook auth (per-pharmacy shared secret) lives behind a `secretsRef` lookup, NOT in the DB.
- [ ] Catalog ingestion job (Ops-triggered): pulls pharmacy formulary → bulk upserts `ProductPharmacyMapping` rows. Ops admin button in Pharmacy Detail.

### 1.2 Permissions tightening
Several endpoints currently allow `read = Condition.Always` to any authenticated user. Audit and lock down where appropriate:
- [ ] `ProductEndpoints` — fine to keep open read (clinic users browse catalog).
- [ ] `PharmacyEndpoints` — same; clinic users see pharmacies they're licensed to order from.
- [ ] `ProductPharmacyMappingEndpoints` — but **pricing must be role-gated in UI** (PRD § 03; MAs may not see pricing). Consider serializer-level field masking OR a derived read-only DTO.
- [ ] `ShipmentEndpoints` — currently allowAuth-read. Verify this is OK; PHI-wise the tracking number alone is the same the carrier exposes, but the link to a specific `PrescriptionOrder` is sensitive. Consider locking to clinic members of the linked order's clinic.
- [ ] `PrescriptionEndpoints` / `PrescriptionOrderEndpoints` / `PatientEndpoints` — already clinic-scoped; double-check `updateRestrictions` cover all immutable post-submit fields (e.g. once `clinicianReview != null`, almost everything should be locked).
- [ ] `ClinicInvoiceEndpoints` — clinic-scoped read; writes Ops-only.

### 1.3 Order-submission workflow service
- [ ] Server-side validator before allowing `clinicianReview` write (mirrors UI checks): prescriber DEA active + verified, controlled-substance guard if `Product.controlled`, ship-to verified, pharmacy licensed in `destination.address.state`, mapping exists for `(product, form, strength)`. Currently UI-only — must also enforce server-side per "fail fast" and HIPAA.
- [ ] On `clinicianReview.approved` flip, enqueue pharmacy dispatch task (see 1.1).
- [ ] Cancellation flow: if `cancellation` set and pharmacy already accepted, call `adapter.cancelOrder` and write outcome.

### 1.4 Refill calculation
- [ ] Background job — daily — that scans open `Prescription`s, computes refill-due = last `PrescriptionOrder.createdAt + willLastDays`, and emits dashboard counts. (No new model; query-on-read is fine for V1 at pilot scale — defer materialization.)
- [ ] System emails to Prescribers/MAs when refill window opens (PRD § F10 — configurable cadence; respect Profile notification opt-out).

### 1.5 DEA + state-license expiration monitor
- [ ] Daily scheduled task: scan `User.prescriber.deaExpiration` and each `stateLicenses[].expiration`; send 60/30/7-day reminder emails to Prescriber + ClinicAdmin (PRD § F9).
- [ ] On expiration day: NO data change needed — `PrescriberLicensing.isDeaExpired` is derived. Server-side submission validator (1.3) reads it.
- [ ] Reminder-log model OR rely on Email send records — decide. UI surfaces this in Profile (`ui.md`).

### 1.6 Notifications (out-of-model per `ui.md`)
- [ ] Decide model: standalone `Notification` table referencing `PrescriptionOrder` / `Shipment`, OR external store. UI assumes lookups by `prescriptionOrder` and `shipment` id (see `ui.md` § "Open / handled elsewhere").
- [ ] Twilio HIPAA-tier SMS dispatch on Shipment Shipped (PRD § F7). Template per `ui.md` Screen 7 / PRD Screen 7 — single-shipment and N-of-M variants. No PHI in body.
- [ ] Optional: SendGrid HIPAA-tier email on same trigger (PRD § F7 — "if not significantly more scope").
- [ ] Twilio "STOP/HELP" wording appended; respect `Patient.smsConsent` (block sends when null) and a future opt-out store.
- [ ] BAA paperwork tracked outside repo; flag in onboarding doc.

### 1.7 Audit log (PRD § F14 — "handled outside the canonical models")
- [ ] Pick mechanism (Lightning Server's built-in audit hooks vs custom append-only log). UI assumes queryable view by actor / role / clinic context / action / target entity / timestamp / source IP / request ID / payload hash.
- [ ] Wire all PHI-touching reads and all `PrescriptionOrder` / `User` / `Patient` state-changing writes through the recorder.
- [ ] 6-year retention configured (PRD § F14 / § 11).
- [ ] Raw CSV export for Ops (§ 04 measurement plan).

### 1.8 Address verification (PRD § F8)
- [ ] Choose USPS / Smarty / Lob. `VerifiedAddress.verificationProvider` already accommodates either.
- [ ] Server endpoint: `POST /addresses/verify` — takes `Address`, returns suggestions + `VerifiedAddress` for the picked suggestion. UI uses inline on Patient and Clinic address edits.
- [ ] Re-verify action surfaces here too.

### 1.9 Identity verification (ID.me or alternative — PRD § F4)
- [ ] Pluggable provider: `interface IdentityVerifier { startStepUp(...) ; checkResult(eventId) }` keyed by config — proposal-call note: provider TBD (could be ID.me, Stripe Identity, etc. — see `transcript.md`).
- [ ] Config-driven cadence: per-submission / per-session / per-day (`transcript.md` decision: build cadence as a variable). Server returns "step-up needed?" via session state; client triggers modal accordingly.
- [ ] Persists `ClinicianReview.idEvent` for audit. ID.me subject id sticks on `User.prescriber.idMeSubjectId` once linked.

### 1.10 Billing & payments (PRD § F13)
- [ ] Pick processor — proposal assumed Priority Payments; PRD body inconsistent (Stripe vs Priority — see `questions.md`). Confirm with client before integrating.
- [ ] Daily settlement job: aggregates per-clinic `PharmacyOrder`s into a `ClinicInvoice` for the period; calls processor to charge (ACH preferred / card with optional fee pass-through).
- [ ] Webhook handler for processor settlement results → updates `ClinicInvoice.paidAt`.
- [ ] PCI: card details NEVER stored; `Clinic.stripePaymentId` is the processor-side token only (already modeled this way).
- [ ] Receipt email to `Clinic.billingContactEmail` on settlement.

### 1.11 Permission audit cache (already in place, but missing one variant)
- [x] `RoleCache`, `ClinicMembershipsCache`, `CoClinicUsersCache` are wired in `UserAuth.kt`.
- [ ] Add a `medicalAssistantClinicIds` helper on `ClinicMembershipsCache` if any future endpoint needs to distinguish MA-only access.

### 1.12 Tests to add (alongside endpoints above)
- [ ] Pharmacy adapter contract test (placeOrder roundtrips a known payload to LifeFile sandbox; mocked when sandbox unavailable).
- [ ] Submission validator: rejects expired-DEA prescriber, rejects controlled when prescriber lacks DEA, rejects unverified ship-to, rejects mapping miss.
- [ ] State-licensing filter test: uses `destination.address.state`, NOT `patient.shippingAddress.state` (per `questions.md` PRD-bug note).
- [ ] Refill calculation correctness on a few sample dosing patterns.
- [ ] Audit-log capture for a representative PHI read + a representative write.
- [ ] SMS dispatch idempotency on duplicate Shipped events.

---

## 2. UI screens (per `project-docs/ui.md`)

Pattern (`ui.md` § Strategy): for each model, build (a) filterable list, (b) row summary, (c) detail with inline edit; compose by attaching locked filters. Mobile-first, desktop adds density. Role-gated actions are HIDDEN, not disabled (PRD § 03).

### 2.1 Shell + auth
- [ ] Login screen — email/password + MFA (TOTP + backup codes already wired in `UserAuth`).
- [ ] Forgot password → email reset link (PinHandler-based; pieces exist via `UserAuth.email`).
- [ ] First-time activation flow (invite-link landing) — sets password + enrolls MFA. Routed to from `ClinicMembership` invite email.
- [ ] Top app bar with active-clinic switcher (only when membership count > 1), notifications bell, user menu.
- [ ] Primary nav — hamburger on mobile, persistent left on desktop. Items vary by Clinic context vs Ops context (`ui.md`).
- [ ] Forced-update interstitial driven by `AppRelease` for mobile builds; web no-ops (refresh loads new bundle).
- [ ] ID.me step-up modal — single reusable component triggered by any controlled-substance submit action. Reads cadence config.

### 2.2 Clinic-context screens
- [ ] Clinic Dashboard (PRD Screen 1) — role-segmented widgets, empty states. Drafts-awaiting-me / refill-queue-summary / pending-invites / expiring-licenses / open-invoices per role.
- [ ] Orders list (`PrescriptionOrder` — PRD § F11) — filters: status, date, prescriber, MA, patient, pharmacy, product, controlled. Status derived per `ui.md` Order Detail timeline.
- [ ] Order Entry / Draft (PRD Screen 2 — central workflow). Both entry points (new Rx + refill against existing Rx) land here. Patient card · ship-to selector · consent reaffirm · Prescription composer (product/form/strength/sig/prescriber/endsAt) · Pharmacy comparison panel · quantity+duration · summary · save-draft / submit(ID.me). Hard-blockers per `ui.md`.
- [ ] Order Detail (PRD Screen 3) — multi-shipment tracking, "package N of M" via PharmacyOrder bundle. Status timeline. Shipment subpanel. Sibling-order subpanel. Notifications subpanel. Cancel / Resend tracking SMS actions.
- [ ] Refill Queue (PRD Screen 4 — § F10) — open Prescriptions joined to patient, due-date derived from `lastOrder.createdAt + willLastDays`. One-click reorder → Order Entry refill mode. Per-device snooze (client-only V1).
- [ ] Patients list — clinic-scoped, filters: name, gender, has-recent-order, consent status, has-unverified-address.
- [ ] Patient Detail + edit — identity, shipping address with Smarty/Lob inline, consent re-affirm, clinical card (allergies/diseases/otherMedications — three-state UI), sublists (Prescriptions, Orders, Notifications), quick actions.
- [ ] Add Patient — Patient Detail in edit-empty mode; address-verification prominent.
- [ ] Catalog (clinic read-only view) — `Product` list with form count / pharmacy count.
- [ ] Clinic Settings (ClinicAdmin) — name (read-only), addresses, billing, members list, invite-member flow.
- [ ] Profile — every user — identity, MFA, memberships, Prescriber section (DEA + state licenses + ID.me link), notification opt-outs, sign-out-everywhere.

### 2.3 Ops-context screens (Admin role and above)
- [ ] Network Order Monitor (PRD Screen 5) — KPI tiles, network-wide `PrescriptionOrder` list with alert column, per-row Re-route / Contact / Cancel.
- [ ] Clinics list + Clinic Detail (Ops) — full edit, memberships management, sublists (Users, Patients, Orders, Invoices). Add-clinic onboarding wizard.
- [ ] Users list + User Detail (Ops) — full edit, role promotion gated (Developer/Root only can promote to Admin+), PrescriberLicensing card with Verify modal.
- [ ] Catalog (Ops full CRUD) — `Product` + Forms editor (one Form per FormType enforced) + Pharmacy Mappings sublist with inline edit + bulk-import action.
- [ ] Pharmacies list + Pharmacy Detail (Ops) — general, state-licensing matrix, mappings sublist, orders sublist, test-connection action.
- [ ] DEA Verification Queue (Ops) — filtered view of `PrescriberLicensing` with `deaReview == null` or expiring soon. Click → User Detail with Verify modal.
- [ ] Invoices (Ops) — filterable by clinic / paid / period. Force-regenerate / mark-paid actions.
- [ ] Audit Log viewer (Ops) — once audit mechanism (1.7) is chosen.

### 2.4 Patient-facing surface
- [ ] None. Patients receive SMS only (PRD § 05). Notification template implementation lives in 1.6.

---

## 3. Compliance + operational readiness

- [ ] HIPAA-eligible AWS account + signed BAA (PRD § 11). KMS encryption at rest, TLS in transit.
- [ ] BAAs with every downstream PHI vendor: each pharmacy, Twilio, SendGrid (if used), ID.me (if used), Smarty/Lob/USPS, processor.
- [ ] Secrets in AWS Secrets Manager — pharmacy creds, processor keys, Twilio tokens. `Pharmacy.credentialsSecretRef` already models the pointer.
- [ ] CORS, idle timeouts, stronger MFA for Ops users (PRD § 05 / § 11).
- [ ] Annual penetration test scheduled (post-pilot, PRD § 11).
- [ ] No PHI in logs / URLs / errors / non-HIPAA telemetry — add a lint or runtime guard if cheap.
- [ ] No PHI to AI/LLM systems (project methodology + PRD § 11).

---

## 4. Operational tooling

- [ ] Seed coverage: extend `Seed.kt` to include a `PrescriptionOrder` + `PharmacyOrder` + `Shipment` so Order Detail can be developed end-to-end against local data.
- [ ] `./testing/prepare-browser-test.sh` already starts everything on :8081/:8951 — keep tests using that path. Document any new env keys (`lifefile-sandbox.env` values, etc.) in `testing/README.md`.
- [ ] Lambda packaging task (`:server:lambda`) already exists — verify it produces a working artifact once the adapter layer is in.
- [ ] Mobile build (Android + iOS) sanity check once enough UI exists to log in + view a single screen.

---

## 5. Known open items (not blocking endpoints, do not start without answer)

These are from `project-docs/questions.md` — proposal-stage questions not yet confirmed by client. Surface again before implementing the affected area:

- Catalog shape: model went with **product + variant axes** (Form + mapping-keyed strength/quantity). Differs from PRD § 09 flat-row reading. Confirm with client before catalog UI (2.3 Catalog Ops).
- Sig structure: model went with **freehand strings on `Prescription.instructions`**. Differs from PRD § 03 "pre-created sigs" reading. Confirm with client before Order Entry composer (2.2 Order Entry).
- PRD § F2/F3 bug — state-licensing filter keys off ship-to, not patient residence. Already reflected in this repo (`Pharmacy.states` doc-comment); confirm in the kickoff write-up.
- Payment processor — Stripe vs Priority Payments inconsistency in PRD. Confirm before billing impl (1.10).
- ID.me cadence — legal counsel decides per-submission vs per-session vs per-day. Build cadence as config (1.9 / 2.1 modal).

---

## 6. Cleanup / minor

- [ ] Remove the deleted-but-still-tracked file `apps/src/commonMain/kotlin/com/lightningkite/lskiteuistarter/MembersScreen.kt` reference if anywhere lingering.
- [ ] `personalize.main.kts` still says "LS KiteUI Starter" branding in places; sweep once HeroScript branding lands.
- [ ] `FeatureFlag` enum is empty — populate once gating decisions land (e.g. `IdMePerSessionCadence`, `EmailOnShipped`).
- [ ] `MembersScreen.kt` deletion + `OrganizationEndpoints.kt` / `MembershipEndpoints.kt` deletions are pending in git status; commit when ready.
