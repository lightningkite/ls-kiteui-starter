# Tech Proposal — Suggested Changes

This document lists the changes to make to `tech-proposal.md` before it goes to Jon, including specific text edits, decisions on how to insert the engineering estimate (`estimate.md`) and the mockup, and rationale for each.

Prepared 2026-05-20.

## Conflict resolutions (per Joe's call)

### 1. Mobile-first design replaces "desktop only"

**Location:** Section 5 "Non-Functional Requirements & Acceptance Criteria" → "Compliance & Security Posture" → "Optimization" bullet.

**Current:**
> Required desktop web optimization only, utilizing the English-US language layout.

**Replace with:**
> Mobile-first responsive web design. The V1 application is fully usable on phones, tablets, and desktop from a single codebase. Native Android + iOS app deployment is available as a separate add-on (see budget). English-US language layout.

**Rationale:** Clinic staff work primarily on phones at the point of care. Mobile-first design carries no incremental cost vs desktop-first when designed from day one; the Kotlin Multiplatform architecture supports adding native apps later with minimal rework.

### 2. Stripe stays — no change

**Location:** Section "Third-Party Services Integration" table → Stripe row.

**Current row is correct as written.** Stripe is the V1 payment processor decision and overrides PRD § 252's "Priority Payments" language. The estimate has been updated to match.

### 3. ID.me cadence — note the option

**Location:** Section "Third-Party Services Integration" table → ID.me row.

**Current:**
> Prescriber identity-verification hooks, required immediately prior to final order submission/routing.

**Replace with:**
> Prescriber identity-verification. Default cadence is per-order (defensive over-compliance; tech proposal V1). DEA's baseline requirement is identity verification at prescription creation only — refills can reuse the prior approval. The estimate prices both cadences; the recommended path (per-Rx with refill reuse) reduces Prescriber friction substantially and is a kickoff decision point.

**Rationale:** Surfaces the cost-vs-UX trade-off so Jon makes the call with eyes open. Estimate keeps the fork priced; tech proposal acknowledges it exists.

### 4. Ops PHI access via break-glass — clarify

**Location:** Section 3 "V1 MVP Functional Specification Overview" → "Role-Based Access Control (RBAC)" → "HeroScript Ops Admin" bullet.

**Current:**
> Full access across all clinics and operational reporting (excluding direct patient PHI/order details unless required for support).

**Replace with:**
> Full access across all clinics and operational reporting. Direct patient PHI and order-detail access is mediated by a dedicated "break-glass" endpoint that requires a documented reason on every invocation; the request itself becomes the audit record, and a notification is sent to the affected clinic's administrator. Normal Ops admin operations (catalog, pharmacy, clinic, and user management) do not touch patient PHI and require no break-glass invocation.

**Rationale:** Reconciles the proposal's "excluding ... unless required for support" language with the actual access-control implementation. Makes the safeguard concrete. Estimate prices this as a separate endpoint (2 points in the Compliance section).

## Insertion points for the estimate breakdown

The tech proposal has several empty/stub sections that the engineering estimate populates directly.

### Insert into Section 6 "Project Timeline"

Section 6 is currently empty. Replace with the estimate's calendar projection:

> The V1 engineering effort is estimated at 220–233 points (880–932 engineer-hours). At Lightning Kite's $150/hr rate, this is $132K–$140K for V1-Required scope, $137K–$145K including the recommended nice-to-haves (selective notification email, Network Order Monitor filtering, AppRelease management UI, Ops CRUD polish).
>
> Calendar projection from kickoff:
>
> - **2 FTEs** (recommended), well-parallelized: 14–18 weeks including BAA / pen-test overhang.
> - **3 FTEs**, well-parallelized: 10–14 weeks including BAA / pen-test overhang.
> - **1 FTE**: 27–31 weeks (not recommended for this scope).
>
> The foundation portion (project setup → data models → auth → SDK) is fast because Lightning Kite ships a working starter project for Kotlin Multiplatform + Lightning Server + KiteUI — the framework boot-up that would be ~9 points on a from-zero stack is 2.5 points here. The full estimate methodology and line items are in `project-docs/estimate.md`.

### Insert into "Budget and Timeline expectations"

Currently a stub. Add:

> ### Budget (engineering)
>
> | Bucket | Points | Hours | Cost at $150/hr |
> | --- | --- | --- | --- |
> | Feature work | 173–186 | 692–744 | $104K–$112K |
> | Compliance | 47 | 188 | $28K |
> | **V1-Required total** | **220–233** | **880–932** | **$132K–$140K** |
> | Selected nice-to-haves | +8 | +32 | +$5K |
> | Mobile native add-on (optional) | +6.5 | +26 | +$4K |
>
> Compliance is **22–24% of the V1 engineering bill** — the HIPAA tax on a regulated-industry product. A non-regulated platform with the same features would run roughly 76–78% of these numbers.
>
> **Roughly 60% of the engineering bill is NOT the CRUD app.** The core multi-tenant CRUD application (project setup, data models, auth, REST endpoints, all clinic-facing and Ops screens) is ~38–41% of V1-Required. The remaining ~59–62% is integrations (~22%), compliance (~23%), production hardening (~6%), and launch buffer (~9%). This is the right shape for a HIPAA-regulated B2B SaaS — a working multi-tenant app is table stakes; the cost story is in everything around it.
>
> ### Calendar (engineering)
>
> See Section 6.
>
> ### Calendar items (non-engineering, parallel to engineering)
>
> These are calendar-driven items that consume legal/ops effort, not engineering hours. Their cost is paid out-of-band (legal, vendor BAA fees, pen-test firm, training provider).
>
> - **Compliance Risk Analysis** — initial 40-hour engagement to finalize controls and establish the System Security Plan (~$6K if engineering-billed, lower if internal).
> - **BAAs** — AWS, MongoDB Atlas, each pilot clinic, each pharmacy, Twilio, SendGrid, ID.me, Smarty/Lob (4–8 weeks calendar).
> - **Pen test** — qualified healthcare-experienced firm at ~75% completion (~3 weeks including remediation).
> - **Workforce HIPAA training** — annual; completed pre-launch for every workforce member with PHI access.
> - **Paper artifacts** — Incident response plan, breach notification templates, sanction policy, workstation security policy (~1–2 weeks Security Officer time).
>
> Lightning Kite does not directly provide cyber insurance, monthly compliance retainer, or ongoing workforce training; these are recommended pass-throughs handled via your selected vendor.

### Insert into "Itemized Tasks"

Currently a stub. Cross-reference the engineering breakdown:

> The complete engineering breakdown is in `project-docs/estimate.md`. Engineering is split into two top-level buckets so the regulated-industry cost story is visible:
>
> - **Engineering — Feature work** (173–186 points). Project setup, data models, auth, REST/CRUD endpoints, frontend foundation and screens, pharmacy integrations, payment / SMS / address-verification integrations, production hardening, testing, closing buffer.
> - **Engineering — Compliance** (47 points). Audit log (mechanism + instrumentation + Ops UI + retention strategy), DEA controlled-substance Rx PDF generation, patient HIPAA consent flow, session timeout + force-MFA + break-glass, soft-delete semantics + outbound payload signing, Secrets Manager + clinical-field envelope encryption + TLS hardening + production boot assertions, PHI hygiene + audit-driven architectural tensions.
>
> These map directly to the tech proposal's three buckets:
>
> - Tech Proposal Bucket A "Product Development" ↔ Estimate "Feature work"
> - Tech Proposal Bucket B "HIPAA Compliance Engineering" ↔ Estimate "Compliance"
> - Tech Proposal Bucket C "Business Associate Operational Liability" ↔ Estimate's non-engineering critical-path calendar items above

### Insert a new section: "Key design decisions"

Currently the proposal doesn't surface the design forks Jon picks between. Add a new section between Section 3 and Section 4 (or wherever fits the document's narrative). Title it "Key design decisions" or "Decision points for client review."

Surface these forks with Lightning Kite's recommendation:

> The engineering estimate prices six design decisions where Lightning Kite has a recommendation but the call is yours. Each fork is documented with cost deltas in `estimate.md` § "Major forks."
>
> - **Order workflow + data model.** Three viable shapes:
>   - *Combined Basket* (recommended if direct-to-patient is dominant): MA composes a multi-Rx basket, Prescriber approves once, dispatches. Single-screen flow. Priced in base.
>   - *Separated Procurement* (recommended if clinic-bulk-shipping is common): MA composes Rx orders; a separate procurement step batches approved orders by destination and picks pharmacy at the basket-of-orders level. +11 points / +$7K. Enables real packing + shipping optimization for clinic-bulk flows.
>   - *Flat Order* (not recommended): PRD § 09 shape with no durable Prescription entity. −8 points / −$5K but loses refill tracking cleanliness.
> - **Catalog: product + variants** (recommended) vs flat SKUs. Variants are essential at clinic-network scale; saves ~4–6 points to skip but pays back in maintenance cost.
> - **Prescriber approval cadence: per-Rx** (recommended, DEA baseline) vs per-order (PRD over-compliance). Per-Rx is dramatically better Prescriber UX.
> - **Audit log strategy: hybrid** (recommended) — DB primary + CloudWatch Object Lock mirror. Alternatives: DB-only, external service only.
> - **Ops admin UI: built screens** (recommended) vs scripted onboarding. Saves ~15 points to script but creates engineer-supported clinic onboarding.
> - **V1 pharmacy adapter count: 6** (recommended, PRD § 12 acceptance criteria) vs 3 vs 1.

### Insert the mockup

Reference the click-through wireframe in the proposal:

> A click-through wireframe mockup is available at `local/mockup/index.html`. It demonstrates the V1 user experience across all key screens — clinic-facing (Dashboard, Patients, Orders, Order Entry, Refill Queue, Prescriptions, Profile) and Ops-facing (Network Order Monitor, Pharmacies, Clinics, Users, Catalog Ops, Audit Log). The mockup also includes an "Alternative model shapes" comparison page showing the three order-workflow options side by side so the trade-offs are concrete. The mockup is intentionally mid-fidelity — polished enough to communicate flow, deliberately not pixel-perfect — and is included as a discussion artifact, not as a visual-design commitment.

If the proposal supports embedded images, embed thumbnails of: index, dashboard, order-entry, order-detail, model-comparison, procurement-queue. If text-only, link by relative path.

## Things to leave alone (per Joe's direction)

These items in the tech proposal are correct as-is; resist the urge to "improve" them:

- Lightning Kite credibility paragraph (HCP analogy is on-target for HIPAA work).
- Three-bucket framing (Product / HIPAA / BA Operational) — maps cleanly to our split.
- Stripe (Section 2 services table) — V1 decision, end of story.
- Scope exclusions (Section "Scope Exclusions") — these match our V1 assumptions.
- MVP Launch Acceptance Criteria — 10 clinics + 6 pharmacies + 2 weeks sustained matches PRD § 12.
- Risk register — keep the two existing risks; the full 8-risk register in `estimate.md` is engineering-side detail, not client-facing.
- Shared Responsibility Matrix — useful framing for Jon, leave as written.

## Items in estimate that exist for tech-proposal traceability but do NOT need to appear in the tech proposal

The estimate has detail that's useful for engineering execution but would clutter the proposal:

- Per-line-item point breakdowns (the proposal shows bucket totals only).
- Methodology paragraph + framework-head-start narrative (the proposal hints at this in the credibility paragraph; doesn't need to re-explain).
- Full 8-risk engineering risk register (the proposal's 2-risk register is sufficient client-side).
- Detailed alternative-cost analyses on every fork (the proposal lists the forks with recommendation; details live in the estimate).
- Production hardening sub-items (covered under Bucket A / Feature work bucket total).

## After applying these changes

1. The tech proposal has internally consistent scope (no Stripe-vs-Priority-Payments confusion, mobile vs desktop is resolved, ID.me cadence is presented as a decision).
2. Sections 6 (Project Timeline) and "Budget and Timeline expectations" are populated with real numbers.
3. The engineering estimate is referenced but not duplicated.
4. The mockup is linked.
5. Jon sees the six design forks with our recommendation called out.

The tech proposal becomes a 2–3 page client-facing document backed by the longer engineering estimate and the click-through mockup.
