# HeroScript V1 Estimate

Prepared 2026-05-20. Estimating the cost of building HeroScript V1 from zero through a HIPAA-defensible pilot launch (10 clinics + ≥6 pharmacies + 2 weeks sustained ordering, per PRD § 12).

**Relationship to the tech proposal.** This document is the engineering-line-item breakdown that backs the totals in `project-docs/tech-proposal.md`. The tech proposal is the client-facing scope, framing, and pricing summary; this estimate is the source-of-truth for how the totals were assembled, what's in/out of V1, and which design forks Jon picks between. The breakdown and the wireframe mockup are intended to be embedded into the tech proposal at the appropriate sections (see `project-docs/tech-proposal-changes.md`).

**Scope note.** This estimate assumes from-scratch construction. The existing `herxoscript` repo is an exploratory spike that informed the data model and UI direction (see `project-docs/handoff.md`, `project-docs/ui.md`, `project-docs/build-plan.md`) but is treated as research-only — no code reuse, no refactor, no migration. Every screen, endpoint, model, and integration is priced as new work.

**Reading guide.** Engineering work is split across two top-level buckets so the proposal narrative is visible at a glance:

- **Engineering — Feature work** is what makes the product *do* something a user sees.
- **Engineering — Compliance** is what the system needs *because it's HIPAA-regulated*.

The split is the key story: how much of a HIPAA-compliant prescription platform's cost is the regulated-industry tax, separate from product features.

## Methodology

Lightning Kite hackathon point system. 1 point = ~4 real engineer-hours (≈1h prototype + 1h polish + 2h hardening, bugs, tests). Multiply by ~1.2x for unfamiliar territory, 0.8x where the pattern is already established. Points include unit tests but NOT BAA negotiation, third-party vendor lead time, or pen-test scheduling — those are calendar-driven and called out separately.

**Rate.** Lightning Kite bills at **$150/hour**. At 1 point = 4 hours, every point in this estimate represents $600 of engineering delivery.

**On the rate.** Our $150/hr is the engineering rate Lightning Kite charges directly — what reaches the engineer's keyboard. Other vendors typically quote similar base rates but add insurance, recruiter fees, and middle-layer overhead on top before the work starts. This quote is the actual delivery number.

**On the framework head-start.** Lightning Kite owns and maintains the underlying server framework (**Lightning Server**) and UI framework (**KiteUI**) used on this project, plus a working **starter project template** that ships with KMP scaffolding, auth flows, theming, navigation, and deploy pipelines pre-wired. That means auth (proof methods, MFA, session, role-based permissions), REST/CRUD endpoint generation, SDK generation, reactive UI primitives, navigation, AuthComponent UI, and the entire "day-one boot up" are available out of the box. The line items below reflect that head-start — work that would be ~50 points on a from-zero stack (initial setup + auth + CRUD scaffolding) is ~13 points here. The framework value is a real cost reduction we pass through; it's not just a methodology choice.

Markers per line item, appended in parentheses:

- (Required) — must land before V1 pilot launch.
- (Nice-to-have) — defensible to push to V1.x.
- (Deferred) — V1.5 or V2; listed only to explicitly cut.
- Bracketed ranges (e.g. 4–8 points) flag genuine unknowns.

## Locked design decisions

These were called by Joe and are baked into the base scope (no longer offered as alternatives):

- **Mobile-first responsive design.** Every screen designs for narrow viewports first, then adds desktop density. No incremental cost vs desktop-first; clinic staff actually work on phones. The V1 web app is fully usable on phones via responsive design.
- **Formatted-sig picker in OrderEntry.** Data model treats sigs as freehand strings; the UI offers a static client-side catalog of common sig templates that, when tapped, populate the freehand instructions field. Convenience-on-top of the freehand model.
- **Pharmacy state-licensing filter on ship-to state.** Filter keys off `destination.address.state`, not patient residence. The PRD's "patient state" language is a bug to flag in conversation with Jon; pricing the correct version.

**Note on native mobile apps.** PRD § 03 defers native mobile to a later phase. V1 ships as a responsive web app only. Native Android + iOS app deployment (app-store binaries, native polish, native FCM testing) is priced as a separate **add-on** below — not part of V1-Required. The underlying KMP architecture supports adding native apps later with minimal rework.

## Major forks (Lightning Kite recommendation + alternatives)

Each of these is a real fork in V1 scope. The base estimate prices Lightning Kite's recommendation; the alternative deltas are noted inline.

- **Order model + workflow — two-axis decision, three viable cells.**

    Two independent axes:

    - **Axis A — Durable `Prescription` entity?** YES (Lightning Kite recommendation, not contingent) means Rxs persist across fills; refill tracking is one indexed query; clinical record permanence; per-Rx audit trail. NO means every order regenerates Rx info and refill tracking derives from order history (PRD § 09 shape).
    - **Axis B — Separate `PharmacyOrder` for dispatch / procurement?** YES separates clinical composition from procurement: clinical staff define what's needed (PrescriptionOrders), procurement staff batch approved orders by destination + pick pharmacy at a basket-of-orders level (Procurement Queue screen). NO collapses both into one entity — combined single-screen flow.

    Three viable cells (no-Rx but separate-dispatch is nonsense and is skipped). **The cost spread is meaningful** — these are not interchangeable choices.

    - **Combined Basket (A=YES, B=NO) — `Order + PrescriptionOrder` basket-first, combined workflow.** Priced in base. (~$0 delta)
        - Recommended **IF clinical-shipping aggregation is rare** (direct-to-patient is the dominant flow).
        - Single-screen MA composition; one ID.me submission per basket.
    - **Separated Procurement (A=YES, B=YES) — `Prescription + PrescriptionOrder + PharmacyOrder`, separated workflow.** **+11 points vs base (~+$7K).**
        - Recommended **IF clinical-shipping aggregation is common** (clinics bulk-receive then redistribute to patients) — the Procurement Queue enables real packing + shipping optimization gains that pay back the engineering investment over time.
        - Adds:
            - Procurement Queue screen — grouping by destination, eligible-pharmacy comparison at basket-of-orders level, batch dispatch confirmation modal: 4 points.
            - Procurement workflow + role gating (procurement-operator permissions, two-stage state machine on PrescriptionOrder): 2 points.
            - PharmacyOrder lifecycle as a composed entity (not auto-grouped) — model adjustments, endpoints, change listeners: 2 points.
            - Bulk-shipping packing-optimization affordances on the Procurement Queue (per-pharmacy total across multiple PrescriptionOrders, lead-time visualization, suggested batching): 2 points.
            - Modified Order Entry (no pharmacy step, lighter dispatch flow): −1 point (savings).
            - Additional testing for the two-stage workflow: 2 points.
    - **Flat Order (A=NO, B=NO) — `Order + OrderItem` ultra-flat per PRD § 09.** **−8 points vs base (~−$5K).**
        - Not recommended. Saves cost up front; loses clean refill tracking (must derive from order history), durable clinical record permanence, per-Rx audit trail, and ability to amend Rx authorizations independently of fills.
        - Savings breakdown:
            - Removes `Prescription` model + endpoints: −1 point.
            - Removes Prescription edit + list screens: −3.5 points.
            - Order Entry simplifies (no existing-Rx picker, no two-mode toggle): −1 point.
            - Refill Queue derives from order history rather than indexed Rx lookup: −2.5 points (cheaper to implement, but creates ongoing operational drag — pay back in V1.x maintenance cost).

    **The cost spread, total:** Flat Order to Separated Procurement is a **~19-point swing (~$11–12K)**. Picking the right shape matters.

    **The Axis B decision is a question for Jon:** how common is clinic-bulk-shipping vs direct-to-patient in Gameday's actual ops? If clinics frequently receive shipments of many patients' Rxs together (so the clinic can dispense or redistribute on-site), Separated Procurement's Procurement Queue earns its cost back through packing/shipping efficiency. If most shipments are direct-to-patient, Combined Basket's simpler flow is the better fit.
- **Catalog — product + variants (recommended) vs flat SKUs.**
    - Product + variants: One `Product` owns a set of `Form`s and `ProductPharmacyMapping`s; variant axes (strength, quantity) live on the mapping. Priced in base.
    - Flat SKUs per PRD § F1: Each strength/form/quantity is its own row. Saves ~4–6 points up front; catalog becomes painful to maintain at scale (Gameday network plans to grow well past pilot).
- **Prescriber approval cadence — per-Rx (recommended, DEA-baseline) vs per-order (PRD over-compliance).**
    - Per-Rx: prescriber taps ID.me when writing a new prescription; refills reuse the prior approval. Dramatically better prescriber UX. Priced in base.
    - Per-order: prescriber taps ID.me on every submission including refills (PRD § F4 / § 05 default). Adds ~2–3 points of friction-engineering (refill flow has to round-trip ID.me) and a real UX cost in practice.
- **Audit log strategy — hybrid (recommended) vs DB-only vs external.** Detailed in the Compliance section.
- **Ops admin UI — built screens (recommended at pilot scale) vs scripted onboarding only.** Detailed in the nice-to-have screens section.
- **V1 pharmacy adapter count — 6 (recommended, PRD § 12) vs 3 vs 1.** Detailed in the Pharmacy integrations section.

---

# Engineering — Feature work

What makes the product *do* something a user sees.

## Project setup

Boot-up is fast because Lightning Kite ships a working starter project. The scaffold, framework wiring, default DB, and Firebase pattern come pre-wired in the template; what's left is configuration and pruning.

- Starter project clone + project-specific configuration - 1 point (Required)
    - KMP scaffold (`apps/`, `server/`, `shared/`), KiteUI + Lightning Server wiring, ServerBuilder skeleton, SDK generator task, navigator, reactive primitives — all from the starter template.
    - MongoDB Atlas (prod) + JSON-file (local) + in-memory (test) DB targets — settings.json pattern from the starter.
    - Firebase setup (FCM credentials, JS client wiring) — pattern from the starter.
- Web deploy pipeline - 1.5 points (Required)
    - AWS Lambda + ALB for backend, Vite/CloudFront for web.
    - Native mobile deploy infrastructure (Play Console, Fastlane TestFlight) is priced in the Mobile native add-on section.

## Data models

Built fresh in basket-first shape per the locked decision above. `Order` is the first-class basket entity, `PrescriptionOrder` is a line-item.

- Clinic, User, ClinicMembership, role enums - 1 point (Required)
- Patient + Address + VerifiedAddress + ClinicalEntry - 1 point (Required)
- Product + Form + ProductPharmacyMapping (product + variants) - 1 point (Required)
- Pharmacy + StateInfo (state-licensing matrix) - 1 point (Required)
- PrescriberLicensing + DEA fields + ID.me linkage - 1 point (Required)
- Prescription (template) - 0.5 points (Required)
- Order (basket) + PrescriptionOrder (line item) + Fulfillment + Cancellation - 2 points (Required)
    - First-class basket model: Order owns the patient, destination, prescriber-review, and lines.
- PharmacyOrder + Shipment + ClinicianReview + AcceptDetails - 1 point (Required)
- ClinicInvoice + invoice line items - 1 point (Required)
- AppRelease + FcmToken + notification stubs - 0.5 points (Required)
- KSP @GenerateDataClassPaths wired across all models - 0.5 points (Required)

## Authentication, session, permissions

Lightning Server's auth framework gives us proof methods (email PIN, password, TOTP, backup codes), session machinery, and `requiredProofStrengthFor` policy out of the box; KiteUI's `AuthComponent` gives the login + MFA UI. The line items below are wiring + project-specific configuration, not from-scratch auth.

- Email + password proof + email-PIN MFA wiring - 0.5 points (Required)
- TOTP MFA + backup codes wiring - 0.5 points (Required)
- Invite-link activation flow (clinic member invite → set password + enroll MFA) - 1 point (Required)
- Role-scoped ModelPermissions across every endpoint - 2 points (Required)
    - `ModelPermissions` DSL is Lightning Server; project-specific predicates per model are the work.
    - Clinic-scoped reads, clinic-admin gates on deletes/role-changes, prescriber-only writes.
    - Includes lockdown on Shipment + PrescriberLicensing reads (would otherwise be over-broad).
- Auth caches: RoleCache + ClinicMembershipsCache + CoClinicUsersCache - 1 point (Required)
    - Pattern lifted from Lightning Server demo; 5-minute TTL.
    - Helpers like `clinicIds()`, `clinicAdminIds()`, `prescriberClinicIds()`.
- ID.me real OAuth integration - 3 points (Required)
    - OAuth flow + callback page + `idMeSubjectId` storage — real integration work even with auth scaffolding free.
    - Per-Rx verification cadence per the locked decision; refills reuse prior approval.
    - BAA scope check (workforce-only payload, no patient PHI).

## CRUD endpoints

Lightning Server's `ModelRestEndpoints` gives full CRUD from a single declaration per model; the work is permission predicates, post-submission immutability rules, dual-context (clinic + Ops) read-masks, and change-listener wiring. SDK generation is a Gradle task — essentially free.

- Clinic, User, ClinicMembership endpoints - 1 point (Required)
- Patient endpoints (clinic-scoped) - 0.5 points (Required)
- Prescription + Order + PrescriptionOrder + PharmacyOrder endpoints - 2 points (Required)
    - Post-submission immutability hook; clinical-field write restriction; the most involved permission predicates in the model.
- Shipment endpoints (locked to clinic members via denorm) - 0.5 points (Required)
- Product + ProductPharmacyMapping endpoints - 0.5 points (Required)
- Pharmacy + state-licensing endpoints - 0.5 points (Required)
- ClinicInvoice endpoints (dual-context: clinic + Ops) - 1 point (Required)
- AppRelease + FcmToken endpoints - 0.5 points (Required)
- SDK generation + client-side typed API surface - 0.5 points (Required)
    - Just the Gradle task wiring + a smoke test; Lightning Server handles the codegen.

## Frontend — foundation

KiteUI provides the navigator, routing, reactive primitives, and `AuthComponent` for login + MFA UI. The default `Theme.flat2` is the framework default and will be replaced with a HeroScript-branded theme (called out as its own line item — themes are real visual-design work).

- App shell + navigator + routing wiring - 0.5 points (Required)
    - KiteUI primitives; just configuration.
- **HeroScript theme** - 3 points (Required)
    - Brand colors, typography stack, button variants, semantic color tokens (status / error / warning / success), card styling, spacing scale, input + form styling, dark-mode consideration.
    - Replaces `Theme.flat2` (KiteUI default).
    - Visual-design pass + Kotlin theme declaration. Not free — themes drive perceived product quality.
- Login screen + email-PIN MFA challenge - 0.5 points (Required)
    - Uses KiteUI `AuthComponent`; just project-specific copy and branding.
- Invite activation page (set password + enroll MFA) - 1 point (Required)
- Mobile-first nav: hamburger drawer + persistent left nav on desktop - 2 points (Required)
    - Role-gated items (User.role × ClinicMembership.role).
- Active-clinic switcher chip for multi-clinic prescribers - 1 point (Required)
- Reusable components: PatientPicker, ProductPicker, PharmacyPicker, AddressEditor - 3 points (Required)
- Force-update interstitial via AppRelease - 1 point (Required)

## Frontend — clinic-facing screens

- Clinic Dashboard (role-segmented widgets) - 3 points (Required)
    - Pending submission, refill queue summary, recent activity, quick actions.
- Patients list - 1 point (Required)
- Patient Detail + new-patient flow - 3 points (Required)
    - VerifiedAddress editor with Smarty/Lob inline suggestions.
    - Clinical entries (allergies/diseases/otherMedications) section.
- Catalog browse (read-only for clinic users) - 2 points (Required)
    - List + product detail with forms + pharmacy mappings.
- Profile (current user) + DEA management subsection - 2 points (Required)
    - DEA number, license image upload, expiration tracking, ID.me linkage status.
- Orders list - 2 points (Required)
- Order Detail with multi-shipment timeline - 4 points (Required)
    - Status timeline (Submitted → Accepted → In Process → Shipped).
    - Sibling-order subpanel, shipment subpanel, audit footer (Ops only).
- Order Entry (multi-Rx basket from day one) - 10 points (Required)
    - Patient + shipping panel, eligible-pharmacy comparison panel, catalog area, basket summary.
    - New-Rx vs refill modes; prescription composer with freehand sigs.
    - **Formatted-sig picker UI** — static client-side catalog of common sig templates that populate the freehand instructions field on tap.
    - ID.me step-up modal at submit; per-Rx cadence per the locked decision.
    - Pharmacy filter by `destination.address.state` (ship-to drives licensing) per the locked decision.
- Prescriber Review Queue - 2 points (Required)
    - Dedicated screen showing orders awaiting the current prescriber's signoff (`assignedTo = me AND clinicianReview == null AND cancellation == null`).
    - One-tap drill into Order Detail for ID.me + approve flow.
    - Separate from Orders list (all clinic orders) and Refill Queue (refills coming due).
- Refill Queue + one-click reorder routing into Order Entry - 3 points (Required)
- Clinic Settings (ClinicAdmin-scoped view) - 2 points (Required)
    - Clinic addresses, default shipping, members, role management.

## Frontend — Ops admin screens

- Catalog Ops edit mode - 3 points (Required)
    - Products full CRUD + forms editor + pharmacy mappings.
- Pharmacies list + detail (state-licensing matrix editor) - 3 points (Required)
- Clinics admin list + detail + add-clinic flow - 2 points (Required)
- Users admin list + detail - 2 points (Required)
- DEA Verification Queue - 2 points (Required)
    - Filtered view of `PrescriberLicensing` pending verification.
- Network Order Monitor - 3 points (Required)
    - KPI tiles + network-wide filterable table + re-route / contact / cancel actions.
- Invoices (dual-context: clinic + Ops) - 3 points (Required)
- Alternative: scripted onboarding only - saves ~15 points
    - Skip the Ops admin screens; HeroScript Ops manages clinics/users/pharmacies/catalog via direct-DB scripts at pilot scale.
    - Workable at 10 clinics / 6 pharmacies but every clinic onboarding becomes engineer-supported; the screens are needed before the next 10-20 clinics anyway.
    - Not recommended.

## Pharmacy integrations

The biggest unknown bucket. LifeFile API has a draft client in `project-docs/disabled/` informed by the spike; every `ASK:` comment is an unanswered spec question. Per-pharmacy customizations (PRD § 10) are unknowable until each pharmacy is on a call.

- PharmacyDispatcher abstraction - 3 points (Required)
    - Common interface: `placeOrder`, `fetchStatus`, `cancelOrder`.
    - Retry with backoff, idempotency keys, dead-letter queue, audit-hook callout.
- LifeFile adapter build + sandbox-tested - 5 points (Required)
    - Built fresh against the documented LF API; the spike's draft informs but is not carried forward.
    - Resolve ~10 `ASK:` spec ambiguities with LF support.
- LifeFile webhook/poll ingestion - 3–6 points (Required, high uncertainty)
    - LF PDF doesn't document a status-read endpoint.
    - May be webhook-receive or polling depending on LF support's answer.
    - HMAC verification on inbound, audit-logged.
- Per-pharmacy LifeFile customizations - 4–8 points (Required, high uncertainty)
    - PRD § 10: "each LifeFile pharmacy may have customizations."
    - Estimate 4 pharmacies on LifeFile at launch; each ~1–2 points.
- Empower adapter - 4–6 points (Required, medium uncertainty)
    - Assumed different from LifeFile per founder direction.
    - Spec-collection + build + sandbox-tested.
- Proprietary pharmacy adapter #1 - 4–6 points (Required, medium uncertainty)
    - Cold-build assuming reasonable JSON-over-HTTPS API.
- Proprietary pharmacy adapter #2 - 4–6 points (Required, medium uncertainty)
    - Buffer for the 6th launch pharmacy being proprietary (4 LifeFile + Empower + 1 proprietary = 6).
- Pharmacy webhook ingestion hardening - 3 points (Required)
    - Replay protection, idempotent state transitions, malformed-payload handling.
    - Stuck-order alerts feeding the Network Order Monitor.
- Alternative: launch with 3 pharmacies instead of 6 - saves ~12–18 points
    - Defer Empower + proprietary adapters to V1.1.
    - Violates PRD § 12 acceptance criteria — needs founder sign-off.
- Alternative: launch with 1 pharmacy (LifeFile-only) - saves ~25–35 points
    - Validates the platform with one integration depth; can't satisfy PRD § 12 launch criteria.
    - Only viable if "V1" is redefined as "soft pilot, single-pharmacy."
- Alternative: adapter SDK + pharmacy onboarding self-serve - +5 points
    - Build a config-driven adapter so new pharmacies are mostly YAML, not code.
    - Pays off at pharmacy #4+; recommend if pipeline beyond V1 launch is real.

## Other integrations

- Notification model + endpoints + dispatch infrastructure - 2 points (Required)
    - Channel-agnostic `Notification` entity (channel, recipient, status, provider message ID, retries, timestamps) per PRD § 09.
    - Dispatch handler with retry/DLQ; webhook receiver for delivery receipts.
    - Ops list/filter view; opt-out state machine paired with `smsConsent`/`emailConsent` revocation.
    - Audit-hook on every dispatch.
- Twilio SMS dispatch on shipment events - 3 points (Required)
    - Twilio HIPAA-tier client integration; uses the Notification entity from the line above.
    - STOP/HELP handling + delivery-receipt webhook.
    - First-name + tracking-link template (no PHI in body).
- Smarty / Lob address verification - 3 points (Required)
    - Inline suggestion UI in AddressEditor.
    - Strip patient name from request payload (BAA compliance-by-design per HIPAA audit item 77).
- Stripe — ACH + card - 6 points (Required)
    - Single integration covering both rails. Stripe is the V1 decision (overrides PRD § 252's "Priority Payments" language).
    - Tokenized card capture, daily settlement job.
    - Charge-descriptor scrubbing to avoid PHI exposure.
    - Invoice mark-paid flow.
    - Alternative: ACH-only + card-by-invoice-link - 3 points
        - Adds card-on-file in V1.1.
- Notification email (SendGrid HIPAA tier) - 1 point (Nice-to-have, recommend including)
    - Same template policy as SMS. PRD § F7 says "included if not significantly more scope."
    - Costs <1 incremental point once Twilio + SendGrid templates share a renderer.
- FCM push for clinic users on order-state changes - 2 points (Required)
    - Wired into Firebase setup; per-platform integration on Android/iOS/web.

## Production hardening (non-compliance)

Hardening items that are operational quality, not HIPAA-driven. (Encryption verification, secrets management, session timeout, audit retention, etc. live in the Compliance section.)

- Production `settings.json` + deploy pipeline - 3 points (Required)
    - Production AWS account.
    - Build promotion, infrastructure-as-code, environment isolation.
- Monitoring + alerting - 2 points (Required)
    - CloudWatch dashboards.
    - Alarms on stuck orders (PRD § 04 guardrails).
    - Pharmacy adapter error rates.
- Session+role-cache race fix - 1 point (Required)
    - Top-level ready-gate so dashboards don't flash empty (lesson from the spike).
- Test suite — server + integration - 5 points (Required)
    - CRUD happy paths, permission boundaries, adapter dispatch, webhook ingestion.
- Test suite — frontend smoke + screen-level - 3 points (Required)
    - Per-screen smoke pass via the browser-test harness; key user flows scripted.

## Optional / nice-to-have feature screens

- DEA Queue advanced filtering - 0 points (Deferred)
    - Only needed if Schedule II at V1; testosterone is Schedule III so the basic queue suffices.
- Network Order Monitor advanced filtering polish - 2 points (Nice-to-have)
    - Functional at baseline; polish for the V1.1 batch of pharmacies.
- AppRelease management UI - 2 points (Nice-to-have)
    - Direct-DB admin works for V1 with 10 clinics.
- Ops CRUD polish on Pharmacies / Clinics / Users - 3 points (Nice-to-have)
    - CSV import, bulk invite, onboarding-at-scale niceties.

## Mobile native deployment (add-on, not V1-Required)

Per PRD § 03, native mobile is deferred. V1 ships as a responsive web app fully usable on phones (mobile-first design is locked in base scope). This section prices what it costs to ship Android + iOS native apps to their stores instead.

- Mobile deploy infrastructure - 1.5 points (Add-on)
    - Android Play Console + signing keystore.
    - Fastlane match for iOS certs + provisioning profiles.
    - TestFlight / Play Internal Track wiring.
    - One-time per-platform onboarding.
- Mobile native polish - 5 points (Add-on)
    - Android + iOS exercised in real device testing.
    - FCM-on-iOS verify, native push wiring, AppRelease force-update interstitial on native.
    - Address-keyboard polish, native pickers, native nav transitions.
    - App-store binaries + submission, screenshots, store-listing copy.
- Mobile native add-on subtotal - 6.5 points (~26 hours, ~$4K)

The underlying KMP architecture means adding mobile later is incremental, not a re-architecture. The add-on can land in V1.x after web pilot validates.

## Closing buffer

- Soak testing with pharmacy sandboxes - 4 points (Required)
    - 2-week sustained ordering against LF sandbox + Empower sandbox before pilot go-live.
    - Matches PRD § 12 acceptance criteria; real-clock bugs surface here.
- End-to-end integration test harness - 3 points (Required)
    - Spin up: server + LF mock + Twilio test number + Smarty sandbox + ID.me sandbox + Stripe test mode.
    - CI green = launch-eligible.
- Demo + onboarding rehearsal with first 2 pilot clinics - 2 points (Required)
    - Iteration on what surfaces during real-clinic walkthroughs.
- Unknown-unknown buffer — feature work share - 12 points (Required)
    - ~10% of feature-work code-line totals on a project of this risk profile.

## Feature-work subtotal

- Project setup - 2.5 points (down from 9 — starter project ships pre-wired)
- Data models - 10.5 points
- Authentication, session, permissions - 8 points (down from 15 — Lightning Server gives proof methods, MFA, sessions; KiteUI gives AuthComponent)
- CRUD endpoints - 7 points (down from 11.5 — Lightning Server's `ModelRestEndpoints` gives full CRUD per model declaration)
- Frontend — foundation - 12 points (includes new 3-point HeroScript theme line item)
- Frontend — clinic-facing screens - 34 points (adds Prescriber Review Queue)
- Frontend — Ops admin screens - 18 points
- Pharmacy integrations - 30–43 points
- Other integrations (Required only) - 16 points (adds Notification model + endpoints + dispatch infrastructure)
- Production hardening (non-compliance) - 14 points (down from 19 — Mobile native polish moved to add-on)
- Optional feature screens (Required only) - 0 points
- Closing buffer - 21 points
- **Feature-work V1-Required total - 173–186 points**

---

# Engineering — Compliance

What the system needs *because it's HIPAA-regulated and prescribing controlled substances*. Most of these line items would not exist on a non-regulated product.

## Audit logging (single biggest compliance item)

- Audit log mechanism — core build - 6 points (Required)
    - First-class `AuditEvent` model, append-only DB write.
    - Per-request middleware, schema enumerated up front per HIPAA-TODO #14.
    - 6-year retention partition.
- Audit log instrumentation sweep - 4 points (Required)
    - Every PHI read + every state-changing write.
    - Every auth event + every pharmacy adapter dispatch.
    - Every notification dispatch (now first-class via the Notification entity).
    - ~35 instrumentation points across the codebase.
- Audit log Ops UI - 3 points (Required)
    - Filter by actor / role / clinic / action / target / date-range.
    - CSV export (export itself audit-logged).
    - Per-patient accounting-of-disclosures query (§ 164.528).
- Audit log strategy fork (selects one of three; base estimate prices recommended option (c))
    - Option (a) DB-backed `AuditEvent` table only - 12 points across the three audit-log items
        - Queryable per-patient, integrates with Ops UI, simplest implementation, internal-only deps.
        - Risk: tamper-evidence is policy + IAM, not cryptographic.
    - Option (b) external service (CloudWatch immutable / Datadog HIPAA / Splunk) - ~6 points less than (a)
        - Ops UI defers to vendor console.
        - External query latency on § 164.528 requests; vendor BAA required.
        - Breach-scope tooling lives at the vendor; accounting-of-disclosures CSV needs custom export.
        - Saves points; costs operational flexibility.
    - Option (c) hybrid: DB-backed primary + CloudWatch with Object Lock mirror - +2 points on (a) (recommended)
        - What an OCR auditor expects to see.
        - Mirror is also the V1.x answer in `hipaa-compliance-todo.md` § V1.x #45.

## DEA controlled-substance handling

- DEA controlled-substance Rx PDF generation - 4 points (Required)
    - Testosterone is Schedule III; required to ship TRT (the PRD's main V1 catalog item).
    - DEA-compliant prescription PDF, signed by prescriber, stored with the order, transmitted with the adapter payload.

## Patient rights (HIPAA Privacy Rule)

- Patient HIPAA consent / Notice of Privacy Practices acknowledgement - 2 points (Required)
    - SMS/email consent recording + paired `*RevokedAt` fields (distinguishes never-consented from revoked).
    - Clinic-affirmation UI at order entry.
- Patient data export tool - 2 points (Required)
    - HIPAA right-to-access § 164.524.
    - Ops command that dumps a Patient's full PHI bundle to JSON+PDF, audit-logged.

## Session, MFA, access controls (HIPAA Security Rule § 164.312)

- Session idle timeout + auto-logoff - 2 points (Required)
    - Server expiration + activity stamping.
    - Client T-60s warning modal.
- Force MFA enrollment at first login - 1 point (Required)
    - Block PHI endpoints until `requiredProofStrengthFor >= 20`.
- Default Ops/Developer view anonymization - 2 points (Required)
    - Read-masks across Patient / Prescription / PrescriptionOrder / Shipment / ClinicianReview when accessed by `UserRole >= Admin` (Ops) or Developer role.
    - Masked: patient name, DOB, phone, email, address, allergies/diseases, instructions, clinical entries.
    - Affected screens: Network Order Monitor, Catalog Ops, Audit Log viewer, Patients-as-Ops, all detail pages reached without break-glass.
    - Non-PHI Ops work (catalog / pharmacy / clinic / user admin) remains fully accessible without invocation.
- Emergency-access ("break-glass") endpoint - 2 points (Required)
    - Dedicated endpoint Ops/Developer invokes to temporarily unmask PHI for a specific patient/order/window.
    - Mandatory reason field; the request itself becomes the audit record.
    - Short TTL on elevated context (e.g. 1 hour); auto-revert to anonymized views.
    - Notifications: affected clinic's administrator + Security Officer on every invocation.
    - Reconciles with the tech proposal's Ops-Admin scope language ("excluding direct patient PHI / order details unless required for support").
- Emergency-access break-glass UI - 1 point (Required)
    - Request modal/page where Ops/Developer enters reason + target patient/order, requests temporary unmask.
    - Active-elevated-context indicator visible on every screen during the window (visual + persistent).
    - Explicit "Resolved" CTA ends elevated context before TTL.
- Non-production PHI scrubbing - 1 point (Required)
    - Staging/dev databases never contain real PHI.
    - Migration/refresh tooling sanitizes names, DOBs, addresses, contact info, clinical entries when copying prod → non-prod.
    - Synthetic-data seeder for fresh dev environments.

## Data lifecycle (HIPAA retention + integrity)

- Soft-delete semantics across PHI-bearing models - 2 points (Required)
    - `deactivatedAt` semantics on Patient / Prescription / Order / Shipment / PharmacyOrder / ClinicInvoice / User.
    - Filter at every list endpoint; baked into model design rather than retrofitted.
    - Driven by TN 10-year medical-record retention + HIPAA 6-year documentation retention.
- Outbound payload signing + integrity hashes - 1 point (Required)
    - SHA-256 of every pharmacy dispatch stored on Fulfillment for non-repudiation.

## Secrets, encryption, transport

- AWS Secrets Manager wiring - 2 points (Required)
    - Resolver for `Pharmacy.credentialsSecretRef`.
    - Same pattern extended to Twilio, SendGrid, processor key, ID.me client secret, FCM service-account key.
- Clinical-field encryption at application layer - 2 points (Required)
    - Envelope encryption for allergies / diseases / otherMedications with a separate KMS key.
    - Limits blast radius of a database snapshot leak per HIPAA architectural-tension C.
- Encryption-at-rest verification across data stores - 1 point (Required)
    - MongoDB Atlas encryption-at-rest, S3 SSE-KMS with customer-managed key.
    - DynamoDB encryption, CloudWatch Logs KMS encryption.
    - Documented KMS key ARNs inventory.
- TLS 1.2+ enforcement + HSTS + security headers - 1 point (Required)
    - ALB / API Gateway SSL policy, HSTS preload, X-Content-Type-Options, X-Frame-Options, Referrer-Policy.
- Production boot assertion - 1 point (Required)
    - Refuses to start if `general.debug = false` AND any of cache / database / email is `ram` / `console`.
    - No AppStoreTester-style hardcoded credentials path in production.

## PHI hygiene + logging

- PHI-in-URLs/logs guard - 2 points (Required)
    - Structured-logging redaction helper.
    - CI grep for `phoneNumber` / `email` in path patterns.
    - No PHI in error messages or non-HIPAA telemetry.
- Pharmacy webhook HMAC verification - 0 points (counted under Feature work / Pharmacy integrations)
    - Listed here for traceability; HMAC verification is part of the LifeFile webhook ingestion line item.

## Supply chain + vulnerability scanning

- Dependency auditing automation - 1 point (Required)
    - Dependabot (or equivalent) enabled at repo level for server, apps, and shared modules.
    - Weekly review process; security advisories triaged within 7 days, critical within 24 hours.
    - License-compatibility scan included.
- Automated vulnerability scanning - 1.5 points (Required)
    - SAST integrated into CI (per-PR scan; block on critical findings).
    - Secrets scanning enabled at the git layer (pre-commit + server-side push protection).
    - Container/artifact image scanning if containerized; otherwise dependency-CVE scan at build time.
    - SBOM generation per release for supply-chain transparency.

## Architectural tensions resolution (handoff § 5)

Four items the secondary HIPAA audit and UX audit flagged that need explicit resolution rather than drift.

- Allergies / diseases / otherMedications storage gating - covered above by clinical-field encryption + the model gating note.
- smsConsent + emailConsent revocation semantics - covered above in the patient consent line item.
- Denormalization vs § 164.526 amendment rights — policy doc - 1 point (Required)
    - Document that historical snapshots on submitted PrescriptionOrders remain immutable; amendments apply forward only.
    - Lives in the Ops runbook + BAA addendum.
- AppStoreTester removal from production - covered above by the production boot assertion.

## Compliance buffer

- Unknown-unknown buffer — compliance share - 6 points (Required)
    - ~10% of compliance-line totals.
    - HIPAA work tends to surface "and one more thing" items late.

## Compliance subtotal

- Audit logging - 15 points (option (c) hybrid; instrumentation up 1 pt for Notification dispatch coverage)
- DEA controlled-substance handling - 4 points
- Patient rights (HIPAA Privacy Rule) - 4 points
- Session, MFA, access controls - 9 points (adds Ops/Developer anonymization + break-glass UI + non-prod PHI scrubbing)
- Data lifecycle - 3 points
- Secrets, encryption, transport - 7 points
- PHI hygiene + logging - 2 points
- Supply chain + vulnerability scanning - 2.5 points
- Architectural tensions resolution - 1 point
- Compliance buffer - 6 points
- **Compliance V1-Required total - 53.5 points**

---

## Non-engineering critical path (NOT pointed — calendar items)

Launch-blocking compliance work that consumes legal/ops calendar, not engineering hours.

- BAAs — allow 4–8 weeks calendar
    - AWS (free, immediate).
    - Each pilot clinic (10).
    - Each pharmacy (6+); per-pharmacy CE-vs-BA analysis per HIPAA audit item 73.
    - Twilio HIPAA-tier.
    - SendGrid HIPAA-tier (if email notification ships).
    - Stripe enterprise (if processor metadata is rich).
    - Smarty / Lob enterprise (if name is sent with address).
    - ID.me (if patient-linked data is sent).
    - MongoDB Atlas.
- Security Officer + Privacy Officer designation
    - Internal HeroScript HR.
- HIPAA Security Rule risk analysis
    - Required before any PHI lands.
    - ~2 weeks dedicated work for outside consultant or internal Security Officer.
- Workforce HIPAA training
    - Annual; completed pre-launch for every workforce member with PHI access.
- Pre-launch penetration test
    - Qualified healthcare-experienced firm.
    - ~3 weeks calendar including remediation.
- Paper artifacts — ~1–2 weeks Security Officer time
    - Incident response plan.
    - Breach notification templates.
    - Sanction policy.
    - Termination procedure.
    - Workstation security policy.

---

## Combined V1-Required totals

- Engineering — Feature work - 173–186 points
- Engineering — Compliance - 53.5 points
- **V1-Required total - 226.5–239.5 points** (rounds to 227–240)
- Selected nice-to-haves (Notification email + Network Order Monitor filtering + AppRelease UI + Ops CRUD polish) - +8 points
- V1 + selected nice-to-haves - 235–248 points
- Mobile native deployment add-on (separate from V1-Required) - 6.5 points

**Compliance share of engineering cost: 22–24% of V1-Required.** Even with Lightning Kite's framework head-start shrinking the feature-work side, HIPAA compliance still claims ~1/4 of the engineering bill. Read another way: a non-regulated product with the same features would run ~76–78% of what's quoted here.

## Real-hours and dollar conversion

At 1 point = 4 hours and $150/hour:

- V1-Required - 908–960 engineer-hours - **$136K–$144K**.
- V1 + selected nice-to-haves - 940–992 hours - **$141K–$149K**.
- Feature work alone - 692–744 hours - **$104K–$112K**.
- Compliance alone - 214 hours - **$32K**.
- Mobile native add-on - 26 hours - **~$4K**.

Dollar figures rounded to nearest $1K.

## Calendar projection

Treating an engineer-day as 7 productive hours after meetings and context-switch overhead.

- 1 FTE engineer, focused - 124–132 working days ≈ 25–27 weeks (~6 months).
- 2 FTEs, focused, well-parallelized - 12–14 weeks (~3 months).
    - Pharmacy integrations and most screens parallelize cleanly.
    - Foundation (starter project → data models → auth/permissions → SDK) is fast since the scaffold is pre-wired; serial portion is ~1 week, not 3.
    - Compliance work parallelizes well with feature work after audit log lands.
- 3 FTEs - 8–10 weeks once the foundation is past the serial portion.
- Add 2–4 weeks for BAA / pen-test calendar
    - Overlaps with engineering but cannot fully overlap; pen test wants a near-final build.
- Realistic V1 pilot launch window
    - 2 FTEs - 14–18 weeks from kickoff including BAA/pen-test overhang.
    - 3 FTEs - 10–14 weeks from kickoff.
    - 1 FTE - 27–31 weeks (not recommended for this scope).

## Risk register

- Per-pharmacy LifeFile customization explodes (high end of the pharmacy integrations range)
    - Likelihood - medium.
    - Impact - +1–2 weeks.
    - Mitigation - front-load LF support calls week 1; cap proprietary pharmacy scope at 1 if needed.
- Audit log path chosen poorly and has to be redone
    - Likelihood - low.
    - Impact - +1 week.
    - Mitigation - recommend hybrid option (c); it's the OCR-defensible default; don't optimize.
- Stripe integration scope creep
    - Likelihood - medium.
    - Impact - +0.5–1 week.
    - Mitigation - lock processor side of the integration contract before code starts.
- BAA calendar pushes past engineering completion
    - Likelihood - high.
    - Impact - +2 weeks of idle.
    - Mitigation - start BAA process week 1, parallel to engineering.
- Pen test findings require non-trivial remediation
    - Likelihood - medium.
    - Impact - +1–2 weeks.
    - Mitigation - schedule pen test at ~75% completion so remediation is parallel.
- HIPAA Security Officer / Privacy Officer not staffed in time
    - Likelihood - medium.
    - Impact - blocking.
    - Mitigation - identify Security Officer in week 1; the policy work has to start then.
- Foundation work runs longer than estimated (first 3 weeks serial)
    - Likelihood - medium.
    - Impact - +1 week per FTE waiting.
    - Mitigation - have the most senior engineer drive setup → models → auth in week 1; don't try to parallelize prematurely.
- Compliance items surface late ("and one more § 164.x requirement")
    - Likelihood - medium.
    - Impact - +0.5–1 week.
    - Mitigation - compliance buffer of 6 points absorbs this; pen test catches anything material.
