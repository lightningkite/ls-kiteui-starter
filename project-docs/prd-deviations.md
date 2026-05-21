# HeroScript — Material Deviations from the PRD

Prepared 2026-05-20. Cross-references `project-docs/prd.txt` (V1 PRD, Draft v1.1 May 2026), `project-docs/estimate.md`, and `project-docs/tech-proposal.md` (as amended per `tech-proposal-changes.md`).

## Executive summary

- We are proposing a **mobile-first responsive web app** rather than desktop-only; native apps move from "deferred" to an optional add-on. No cost increase.
- We are recommending **three significant data-model changes** (durable `Prescription` entity, product+variants catalog, and a basket-first `Order/PrescriptionOrder` shape) — together a roughly 19-point swing in the estimate that we'd like Jon to pick the shape on.
- We caught **two PRD bugs** (state-licensing filter keys off patient state instead of ship-to state; F13 says Stripe in the header and Priority Payments in the body) and resolved both in our favor.
- We are adding **~53 points of HIPAA hardening (~22–24% of the bill)** that the PRD names only in the abstract — audit log mechanism, break-glass PHI access, envelope encryption, vulnerability scanning, soft-delete semantics, non-prod PHI scrubbing. This is the regulated-industry tax, and we've itemized it.
- We are proposing **per-Rx ID.me cadence** as the recommended path (DEA baseline) vs the PRD's per-order default. UX gain for prescribers; the estimate prices both.

---

## 1. Scope / platform decisions

### 1.1 Mobile-first responsive web (deviation from PRD § 03 / § 12 desktop-only)

- **PRD says:** § 03 puts "Mobile-responsive UI and native mobile apps" *out of scope* for V1; § 12 lists mobile-responsive UI in V1.5. § 11 names "Desktop web only in V1."
- **We propose:** Mobile-first responsive design baked into V1 from day one; native Android + iOS deployment available as a separate **+$4K add-on** (6.5 points). No incremental cost vs desktop-first.
- **Justification:** Clinical reality. Clinic staff work primarily on phones at the point of care; the discovery transcript and follow-up calls have been clear that the desktop-only directive in the PRD reflects scope-shrink instinct, not user behavior. Mobile-first costs nothing extra when designed in from the start, and the Kotlin Multiplatform architecture makes native apps a small later increment.
- **Cost impact:** Net zero for V1-Required. Add-on +$4K if Jon wants native apps shipped at V1.
- **Action for Jon:** Approval needed. Already covered in `tech-proposal-changes.md` § 1. Joe locked this decision; Jon hasn't yet signed off.

### 1.2 Native mobile apps as an add-on, not deferred outright

- **PRD says:** § 03 / § 12 defer native apps to V2 ("Native mobile apps (only if pilot research justifies)").
- **We propose:** Price native deployment as a $4K add-on the client can pick up at V1 or hold for V1.x.
- **Justification:** Scope expansion with explicit benefit. KMP gives us native apps cheaply; calling that out (vs hiding it) lets Jon decide based on pilot-clinic feedback rather than re-discovering it as scope later.
- **Cost impact:** +$4K if elected; otherwise neutral.
- **Action for Jon:** Decision at proposal meeting.

---

## 2. Data model decisions

### 2.1 Catalog as product + variants, not flat SKUs (deviates from PRD § F1 / § 09)

- **PRD says:** § F1 lists Product attributes as "(name, strength, form, SKU, image, description, controlled-substance flag)" — a scalar-strength, scalar-form model. § 09 mirrors that. Read strictly, each (strength × form × quantity) is its own catalog row.
- **We propose:** One `Product` per drug, owning a `Set<Form>` and N `ProductPharmacyMapping` rows where strength/quantity/form-type live on the mapping. A "Testosterone Cypionate" product has one row; the variant grid lives below it.
- **Justification:** Clinical reality + operational improvement. Compounding pharmacies (Empower, Hone, LifeFile) publish formularies as concentration × vial-size grids, not flat SKU lists. A typical TRT product family flatly expands to ~15–20 catalog rows; product+variants keeps the Ops admin manageable as the Gameday network grows. Confirmed in `questions.md` § 1; locked in `handoff.md` § 3.
- **Cost impact:** Roughly 4–6 points more than flat SKUs upfront, but saves significant ongoing catalog-maintenance cost. Itemized as a fork in `estimate.md` § "Major forks."
- **Action for Jon:** Already approved in our proposal direction; confirm at the proposal meeting.

### 2.2 Durable `Prescription` entity (PRD § 09 doesn't have one)

- **PRD says:** § 09 models `Order` + `OrderItem`. There is no standalone Prescription entity; refill cadence and Rx authorization derive from order history.
- **We propose:** A first-class `Prescription` template that persists across fills. Refills reference the prescription, not a prior order; per-Rx audit trail is clean; amending an authorization without re-creating it is possible.
- **Justification:** Clinical reality + regulatory accuracy. A prescription is a separate clinical instrument from an order; conflating them makes refill tracking, BUD/28-day math, and DEA controlled-substance compliance harder than they need to be. Per-Rx ID.me cadence (§ 3.1 below) only works cleanly with a durable Rx entity.
- **Cost impact:** ~5 points more than the flat-Order alternative ($3K). Estimate prices the alternative: "Flat Order (A=NO, B=NO) — `Order + OrderItem` ultra-flat per PRD § 09. **−8 points vs base (~−$5K)**."
- **Action for Jon:** Decision at proposal meeting (one of three viable cells under "Order model + workflow" fork).

### 2.3 Order model — three viable cells; Combined Basket recommended

- **PRD says:** § 09 implies a flat `Order` with N `OrderItem`s. One pharmacy per order, one ID.me submission, one shipment-group.
- **We propose:** Three viable shapes, with **Combined Basket** as the priced default. (a) Combined Basket: durable Rx + basket Order, single composition screen. (b) Separated Procurement: durable Rx + PrescriptionOrder + separate PharmacyOrder for procurement batching (+$7K). (c) Flat Order per PRD (−$5K, not recommended).
- **Justification:** Operational improvement, with the right cell depending on how Gameday clinics actually receive shipments. If most shipments go direct-to-patient, Combined Basket is the right fit; if clinics frequently receive bulk shipments to redistribute on-site, Separated Procurement earns its cost back. The PRD's flat shape works but loses refill-tracking cleanliness.
- **Cost impact:** ~19-point spread across the three options ($11–12K).
- **Action for Jon:** Decision at the proposal meeting. This is one of the three highest-impact decisions in the estimate.

### 2.4 `PrescriberLicensing` embedded on User (vs PRD § 09 separate "Prescriber profile")

- **PRD says:** § 09 lists "Prescriber profile" as a separate entity with DEA number, license image, expiration, ID.me linkage.
- **We propose:** Embed `PrescriberLicensing` directly on `User` as a nullable field. Adds `stateLicenses: Set<StateMedicalLicense>` (PRD doesn't model state licenses, even though they're required to practice).
- **Justification:** Technical practicality + regulatory accuracy. A User is either a prescriber (then they have licensing data) or they're not — the 1:1 relationship is best modeled as embedded. Adding state-level licensing fills a real regulatory gap the PRD silently omits.
- **Cost impact:** Neutral; same point count as the separate-entity shape.
- **Action for Jon:** Implementation detail; no decision needed.

### 2.5 `Pharmacy.StateInfo` embedded (vs PRD § 09 separate `PharmacyStateLicense` entity)

- **PRD says:** § 09 lists `PharmacyStateLicense` as a separate entity (Pharmacy ↔ state, effective date, expiration, notes).
- **We propose:** Embed as `Pharmacy.StateInfo` records on the Pharmacy entity itself (a `Set<StateInfo>` field).
- **Justification:** Technical practicality. State licenses are owned 1:1 by a pharmacy with no cross-entity references; embedding keeps the licensing matrix query as a single document read. Identical data, simpler implementation.
- **Cost impact:** Neutral.
- **Action for Jon:** Implementation detail; no decision needed.

### 2.6 First-class `Notification` entity with structured dispatch tracking

- **PRD says:** § 09 lists Notification thinly: "Order / Shipment reference, channel, recipient, status (sent / delivered / failed), provider message ID."
- **We propose:** Notification as a structured entity with retry state, dispatch timestamps, delivery-receipt webhooks, opt-out state machine paired with `*Consent` / `*RevokedAt` field pairs, and an Ops list/filter view. Estimated at 2 points beyond the bare PRD spec, plus dispatch infrastructure.
- **Justification:** Regulatory accuracy. HIPAA's accounting-of-disclosures (§ 164.528) plus Twilio's STOP/HELP semantics require an auditable, queryable record of every notification — not just a send-and-forget log line. Also enables consent revocation that distinguishes "never consented" from "revoked," per the audit findings.
- **Cost impact:** 2 points ($1.2K) over the bare PRD wire-up.
- **Action for Jon:** Already incorporated; no decision needed.

### 2.7 PRD `Order` totals/items vs our `Fulfillment` + `Cancellation` decomposition

- **PRD says:** § 09 puts items, totals, status, ID.me verification ID, and submitted-at all on the `Order` entity.
- **We propose:** Pull post-submission state (`Fulfillment`, `Cancellation`, `ClinicianReview`) into nested types on `Order` / `PrescriptionOrder`. Immutable once written.
- **Justification:** Regulatory accuracy. § 164.526 amendment rights and DEA prescription-record permanence both require that historical order state stays untouched. Decomposing into write-once nested types makes "immutability after submission" enforceable at the model layer rather than as a policy doc.
- **Cost impact:** Neutral.
- **Action for Jon:** Implementation detail; no decision needed.

---

## 3. Workflow / UX decisions

### 3.1 Per-Rx ID.me cadence (recommended) vs PRD § F4 / § 05 per-order default

- **PRD says:** § F4 and § 05 default to **per-submission ID.me** (every order, including refills) as the controlled-substance industry / EPCS norm. § 13 flags it as legally negotiable.
- **We propose:** **Per-Rx cadence** as the recommended path — prescriber taps ID.me when writing a new prescription; refills reuse the prior approval. The estimate prices both; the tech proposal surfaces it as a decision point.
- **Justification:** Regulatory accuracy + operational improvement. DEA's baseline EPCS requirement is identity verification at prescription creation, not every fill. Per-order ID.me is *defensive over-compliance* — the PRD admits this in § 13. Prescriber friction at every refill is a real day-to-day cost. Legal confirmation expected to bless per-Rx for the V1 controlled-substance catalog (testosterone Schedule III).
- **Cost impact:** Per-Rx is ~2–3 points cheaper than per-order (refill flow doesn't need to round-trip ID.me).
- **Action for Jon:** Decision at the proposal meeting. Already surfaced in `tech-proposal-changes.md` § 3.

### 3.2 Freehand sigs with formatted-sig picker UI (deviates from PRD § F1 pre-created menu)

- **PRD says:** § F1: "Each catalog item needs pre-created sigs that providers can use in the prescription."
- **We propose:** Data model treats sigs as **freehand strings** on `Prescription.instructions`; the OrderEntry UI offers a static client-side catalog of common sig templates that, on tap, populate the freehand instructions field. Convenience-on-top of a freehand model.
- **Justification:** Clinical reality. TRT prescribing varies dose/frequency per patient as titration progresses; a fixed-menu sig system that doesn't allow modification doesn't match how prescribers actually work. The formatted-sig picker gives the convenience of pre-authored sigs without forcing them. Confirmed in `questions.md` § 3 and `handoff.md` § 3.
- **Cost impact:** Neutral; sig picker is in-line with Order Entry estimate.
- **Action for Jon:** Approval needed. Already noted; Joe locked the decision.

### 3.3 Default Ops/Developer view anonymization + break-glass endpoint

- **PRD says:** § 05 describes HeroScript Ops Admin as "Highest-privilege role. All actions audit-logged." with no anonymization layer. Tech proposal § 3 RBAC says "Full access ... excluding direct patient PHI / order details unless required for support" — language without a defined mechanism.
- **We propose:** PHI fields (patient name, DOB, phone, email, address, allergies, instructions, clinical entries) are **masked by default** when accessed by `UserRole >= Admin` or Developer. To unmask, Ops invokes a **break-glass endpoint** with a documented reason, generating an audit record + notification to the affected clinic's administrator. Short-TTL elevated context (e.g. 1 hour). Visual indicator on every screen during the elevated window.
- **Justification:** Regulatory accuracy. HIPAA minimum-necessary (§ 164.502(b)) requires that even Ops not have routine PHI access for non-PHI work (catalog, pharmacy, clinic admin); the break-glass pattern is what an OCR auditor expects to see for legitimate emergency access. Reconciles the tech proposal's "unless required for support" hand-wave with a concrete mechanism.
- **Cost impact:** ~3 points ($1.8K) for default-anonymization read-masks + break-glass endpoint + UI affordance.
- **Action for Jon:** Already surfaced in `tech-proposal-changes.md` § 4; surface for explicit approval.

### 3.4 Prescriber Review Queue as a dedicated screen

- **PRD says:** Implied across § F3 / § 08 / § 06 that prescribers see pending drafts on the dashboard. No dedicated review-queue screen named in § 08.
- **We propose:** A dedicated **Prescriber Review Queue** showing orders awaiting the current prescriber's signoff (`assignedTo = me AND clinicianReview == null AND cancellation == null`). One-tap drill into Order Detail for ID.me + approve.
- **Justification:** Operational improvement. Multi-clinic prescribers (PRD § 05 supports this) reviewing across contexts need a single review surface rather than per-clinic dashboards. Estimate adds 2 points.
- **Cost impact:** +2 points ($1.2K) — included in the V1-Required base.
- **Action for Jon:** Already incorporated; no decision needed.

---

## 4. Integration decisions

### 4.1 Stripe (V1) vs PRD § 252 / § 03 Priority Payments

- **PRD says:** § 03 dependencies, § F13 body, and § 252 name **Priority Payments**. § F13 header and § 13 Decisions Log say **Stripe**. PRD is internally inconsistent (flagged in `questions.md` § "PRD inconsistencies").
- **We propose:** **Stripe** for V1 — ACH + card on a single integration, tokenized card capture, daily settlement job.
- **Justification:** Technical practicality + operational improvement. Stripe is BAA-eligible at enterprise tier, has documented HIPAA-safe metadata patterns, and is the integration we know best. Priority Payments is acceptable but adds an unfamiliar vendor for no marginal benefit at V1 volume. Tech proposal already shows Stripe in the services table; `tech-proposal-changes.md` § 2 confirms.
- **Cost impact:** ~6 points (priced in base). Estimate notes Priority would price roughly the same — choice is operational, not cost-driven.
- **Action for Jon:** Already in the tech proposal; confirm at the meeting.

### 4.2 Pharmacy adapter count: 6 (PRD-aligned) vs 3 vs 1

- **PRD says:** § 03 / § 12 acceptance criteria require ≥6 integrated pharmacies for V1.
- **We propose:** Recommend 6 as priced in base; surface 3 and 1 as alternatives with significant cost savings (12–18 points / 25–35 points respectively).
- **Justification:** Scope shrinkage with named trade-off. PRD § 12 explicitly conditions launch on ≥6 pharmacies; cutting violates the founder direction. Surfaced so Jon knows the costs of relaxing it.
- **Cost impact:** Range is meaningful — cutting to 3 saves $7–11K, cutting to 1 saves $15–21K. Both require Jon's sign-off because they redefine "V1."
- **Action for Jon:** Decision at the proposal meeting if budget compression is needed.

---

## 5. Compliance additions (PRD silent or vague)

These items appear under "Compliance — V1" in PRD § 03 only in summary form ("HIPAA-eligible AWS … encryption at rest and in transit … role-based access control … immutable audit log … 6-year retention"). We're itemizing what that summary actually requires. **Total: ~53 points / $32K, ~22–24% of the engineering bill.**

### 5.1 Audit log mechanism, instrumentation, and Ops UI

- **PRD says:** § F14 names "immutable append-only log, 6-year retention, captured fields actor / role / clinic context / action / target / timestamp / source IP / request ID / payload hash." Strategy unspecified.
- **We propose:** **Hybrid strategy** (DB-backed `AuditEvent` table as primary + CloudWatch with Object Lock as immutable mirror). Per-request middleware on every PHI read and every state-changing write (~35 instrumentation points). Ops UI with filter + CSV export + per-patient accounting-of-disclosures query (§ 164.528). 15 points / $9K.
- **Justification:** Regulatory accuracy. PRD names the audit log but doesn't price it; the mechanism is the single largest compliance line item and is what an OCR auditor expects to see. Hybrid is the defensible default.
- **Action for Jon:** Already in base scope; no decision needed.

### 5.2 DEA controlled-substance Rx PDF generation

- **PRD says:** § 03 and § F9 cover DEA license tracking. PRD is silent on actual DEA-compliant Rx PDF generation.
- **We propose:** DEA-compliant prescription PDF, signed by prescriber, stored with the order, transmitted with the adapter payload. 4 points / $2.4K.
- **Justification:** Regulatory accuracy. Testosterone is Schedule III and requires a DEA-compliant Rx record for transmission and recordkeeping. PRD omits this.
- **Action for Jon:** Already in base scope.

### 5.3 Patient HIPAA consent + data export tool

- **PRD says:** § 03 assumes consent is captured implicitly via clinic intake; § 11 names 6-year retention. Silent on patient right-to-access (§ 164.524) workflow.
- **We propose:** SMS/email consent recording paired with `*RevokedAt` fields (distinguishes never-consented from revoked); Ops command that dumps a Patient's full PHI bundle to JSON+PDF, audit-logged. 4 points / $2.4K total.
- **Justification:** Regulatory accuracy. Right-to-access is a Privacy Rule baseline; an out-of-band manual handle is not defensible at scale.
- **Action for Jon:** Already in base scope.

### 5.4 Session timeout + force-MFA + non-prod PHI scrubbing

- **PRD says:** § 05 / § 11 name "MFA for all clinic and Ops users; session idle timeout; short for Ops." Silent on enforcement mechanism and on non-prod PHI handling.
- **We propose:** Server-side session expiration + activity stamping + T-60s client warning modal; block PHI endpoints until `requiredProofStrengthFor >= 20`; non-prod PHI scrubbing tooling so staging/dev never contains real patient data. 4 points / $2.4K.
- **Justification:** Regulatory accuracy. PRD names the requirements; we're pricing the mechanism. Non-prod PHI scrubbing is an OCR-expected control that the PRD doesn't mention.
- **Action for Jon:** Already in base scope.

### 5.5 Soft-delete semantics + outbound payload signing

- **PRD says:** § 11 names 6-year retention; silent on deletion semantics, integrity proofs, and non-repudiation.
- **We propose:** `deactivatedAt` semantics across Patient / Prescription / Order / Shipment / PharmacyOrder / ClinicInvoice / User; SHA-256 integrity hash on every pharmacy dispatch. 3 points / $1.8K.
- **Justification:** Regulatory accuracy. TN state law mandates 10-year medical-record retention (longer than HIPAA's 6-year documentation retention; the PRD conflates these); integrity hashes give non-repudiation on outbound Rx transmissions.
- **Action for Jon:** Already in base scope.

### 5.6 Secrets, encryption, transport hardening

- **PRD says:** § 11 names "encryption at rest and in transit; key management via AWS KMS; secrets in AWS Secrets Manager." Mechanism unspecified.
- **We propose:** AWS Secrets Manager resolver for every external credential; application-layer envelope encryption on `allergies` / `diseases` / `otherMedications` with a separate KMS key (limits blast radius of a DB snapshot leak); TLS 1.2+ + HSTS + security headers; production boot assertion (refuses to start if debug flags are on). 7 points / $4.2K.
- **Justification:** Regulatory accuracy + defense-in-depth. Envelope encryption on clinical fields is the architectural answer to the "we're not an EMR but we hold clinical data" tension flagged in the secondary HIPAA audit.
- **Action for Jon:** Already in base scope.

### 5.7 Supply chain + vulnerability scanning

- **PRD says:** § 11 names "annual penetration test; quarterly internal review." Silent on supply chain / vulnerability scanning.
- **We propose:** Dependabot for server/apps/shared modules; SAST in CI; secrets scanning at git layer; SBOM per release. 2.5 points / $1.5K.
- **Justification:** Operational improvement. HIPAA Security Rule § 164.308(a)(1)(ii)(B) requires "risk management" — concrete supply-chain controls are how an auditor evaluates that requirement.
- **Action for Jon:** Already in base scope.

---

## 6. PRD bugs we caught

These are not "deviations" — they're errors in the PRD that our proposal silently corrects. Surfaced here so Jon knows we read carefully.

### 6.1 State-licensing filter on ship-to state, not patient state

- **PRD bug:** § F2 / § F3 / § 06 say the filter keys off "patient's state" or "patient's shipping-address state." These are not the same thing; pharmacy licensing is about *where the package is delivered*, not where the patient lives. An order can legitimately ship to a clinic in a different state than where the patient resides.
- **Our correction:** Filter keys off `PrescriptionOrder.destination.address.state`. Documented in `Pharmacy.StateInfo` field comment, `handoff.md` § 3 #3, `estimate.md` "Locked design decisions," and `questions.md` "PRD inconsistencies."
- **Action for Jon:** Flag in writing at the proposal meeting. PRD bug to fix; no scope change.

### 6.2 Payment processor inconsistency (Stripe vs Priority Payments)

- **PRD bug:** § F13 header says "Stripe"; § F13 body and § 03 dependencies say "Priority Payments"; § 13 Decisions Log says "Stripe."
- **Our correction:** Stripe. See § 4.1 above.
- **Action for Jon:** Already resolved in tech proposal; flag the PRD inconsistency at the meeting.

### 6.3 F13 duplicated "credit card"

- **PRD bug:** § F13 says "via credit card or ACH ... or credit card" — duplicated phrase. Cosmetic.
- **Our correction:** None needed.
- **Action for Jon:** PRD copy-edit when convenient.

### 6.4 TN state-law retention vs HIPAA retention conflation

- **PRD bug:** § 11 names "PHI and audit logs retained 6 years (HIPAA)." This conflates two different retention regimes — HIPAA's 6-year documentation/audit-log requirement and TN state law's 10-year medical-record retention. Patient clinical data must be kept 10 years post last contact under TN law; documentation and audit logs are 6 years.
- **Our correction:** Soft-delete semantics with retention tagged to the right regime per entity. See § 5.5 above.
- **Action for Jon:** Informational. Caught by the secondary HIPAA audit; documented in `handoff.md` § 5.

---

## Bottom-line summary table

| Category | Net cost impact | Action for Jon |
| --- | --- | --- |
| Mobile-first (vs desktop-only) | $0 | Approve |
| Native apps add-on | +$4K if elected | Decide |
| Catalog: product + variants | +$3K vs flat | Approve |
| Durable Prescription entity | Part of order-model fork | Decide |
| Order model (3 cells, $11–12K spread) | Decide | Decide |
| Embedded PrescriberLicensing + StateInfo | $0 | Informational |
| Structured Notification entity | +$1.2K | Approved |
| Per-Rx ID.me cadence | −$1.5K vs per-order | Decide |
| Formatted-sig picker UI on freehand model | $0 | Approve |
| Ops anonymization + break-glass | +$1.8K | Approve |
| Prescriber Review Queue | +$1.2K (in base) | Informational |
| Stripe (vs Priority Payments) | $0 | Approved |
| Pharmacy count: 6 (PRD-aligned) | Base. 3=−$8K, 1=−$18K | Decide if budget pressure |
| Compliance hardening (audit log, DEA PDF, consent + export, session/MFA, soft-delete, secrets, scanning) | +$32K total / 22–24% of bill | Approve |
| PRD bugs caught (4) | $0 | Flag at meeting |

## The three decisions Jon should make at the proposal meeting

1. **Order model + workflow** (§ 2.2 + § 2.3). Three viable cells with an $11–12K spread. The decision turns on Gameday's real shipment pattern: direct-to-patient (Combined Basket recommended) vs clinic-bulk-shipping (Separated Procurement worth its +$7K). We can't price this confidently without his answer.
2. **ID.me cadence** (§ 3.1). Per-Rx vs per-order is a real prescriber-experience trade-off. Per-Rx is the DEA baseline and dramatically better UX; per-order is defensive over-compliance. Modest cost delta; meaningful UX delta.
3. **Mobile-first and pharmacy count** (§ 1.1, § 4.2). Pair of scope-shape questions. Mobile-first is free upside if approved; pharmacy count is the lever for budget compression if needed (and Jon should know that cutting violates his own PRD § 12 launch criteria).
