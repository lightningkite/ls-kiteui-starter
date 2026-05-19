# UI

## Strategy

For each model in the system, design a:
- Filterable List View
  - Filters double as reports (PRD § F11). No separate "reports" screen — each list view is the report for that data type, with column-toggle and CSV export deferred to V1.5.
- Row Summary View
  - Respects role visibility (Prescribers see pricing; MAs may not — § 03 onboarding).
  - Respects context — known values (e.g. clinic when scoped) are suppressed.
- Detail View with inline Edit mode
  - One screen, "Edit" toggles fields editable; saves in place.
  - Edit visibility/availability respects RBAC (§ 05).

Re-use those views in composed screens: the orders list for a single patient is the same component as the network-wide orders monitor with the patient filter pre-attached and locked. Same pattern for orders-by-prescriber, orders-by-pharmacy, etc. (like the LS admin sublist convention).

## Cross-cutting UI elements

- Mobile-first, extending to desktop. PRD § 03 / § 11 say desktop-only with mobile deferred; we're inverting that. Designing mobile-first and extending up is materially cheaper than the reverse — retrofitting touch targets, single-column flows, and reachable primary actions onto desktop-first layouts costs more than the few extra hours of layout discipline up front. Every screen below is described mobile-first; desktop adds density (multi-column layouts, side-by-side panels, persistent nav) but never restructures the flow.
- Top app bar: HeroScript branding · active-clinic switcher (Prescribers/MAs with membership in >1 clinic — § 03) · notifications bell · user menu. On mobile, branding compresses to a logomark and the clinic switcher becomes a chip below the bar; the rest collapse into the user menu.
- Primary nav: hamburger drawer on mobile, persistent left nav on desktop. Same items either way.
  - Clinic context (`User.role == User`, viewing through a `ClinicMembership`): Dashboard, Orders, Refill Queue, Patients, Catalog (read-only), Clinic Settings.
  - Ops context (`User.role == Admin` or higher): Order Monitor, Clinics, Users, Catalog, Pharmacies, DEA Queue, Invoices. Audit log access path is TBD (the audit-log model itself is being handled outside the canonical models).
  - Role mapping for UI gating: `Admin` ≈ Ops Support (read-mostly), `Developer`/`Root` ≈ Ops Admin (full CRUD on catalog/pharmacies/clinics, can re-route orders). Clinic-level roles (ClinicAdmin / Prescriber / MA) live on `ClinicMembership.role` and gate clinic-context actions independently.
- List ↔ detail pattern: mobile uses master-detail-as-routes (tap row → full-screen detail with back); desktop uses split view (list left, detail right). Same components — the router decides.
- Forms: single-column, full-width inputs on mobile; multi-column gridding on desktop where it doesn't fragment the flow. Order Entry's pharmacy comparison is the one screen that genuinely needs a wider viewport — see that section for the mobile treatment.
- Touch targets ≥ 44pt everywhere; primary actions reachable in the thumb zone (bottom sticky action bar on mobile detail screens). Secondary actions in an overflow menu.
- Audit footer: every detail screen exposes an "Audit" link to the filtered audit history for that entity (§ F14). On mobile this lives in the overflow menu, not pinned. The underlying audit-log mechanism is being handled outside the canonical models — UI just routes to it.
- Empty states: every list/queue has a worded empty state (PRD § 08 Screen 1).
- Permissions: actions hidden, not disabled, when out of role. Pricing line items are role-gated per § 03.
- Version gating: `AppRelease` drives a forced-update interstitial on mobile when `requiredUpdate == true` and the client's version predates the latest required release for its platform. Web has no parallel — refresh loads the current bundle. The interstitial blocks all app interaction until update.
- Accessibility: WCAG 2.1 AA (§ 11) holds across viewports. Browser support: latest two majors of Chrome/Edge/Safari/Firefox plus iOS Safari and Android Chrome.

## Source of truth

`project-docs/planned-models.kt` is the source of truth where it diverges from the PRD. Two specific differences shape the screens:

- Catalog is product + variants, not flat SKUs. One `Product` (e.g. "Testosterone Cypionate") owns a `Set<Product.Form>` directly — each Form is a `(FormType, strengthUnit, quantityUnit)` triple (e.g. injectable vial, mg/mL, mL). One Form per FormType per Product (enforced in service layer, see comment in model). Each pharmacy offers it via one or more `ProductPharmacyMapping`s keyed by `Product.FormType` and carrying concrete `strength` and `quantity` (or `null` for either, meaning "customizable — prescriber fills in"). Order Entry pivots around this hierarchy, not around flat SKUs.
- Sigs are freehand strings. `Prescription.instructions` is a free-text field the prescriber writes themselves. No catalog-side template, no variable schema, no fixed-string menu — sigs in this space (especially TRT titration) get weird enough that any structured representation creates more failure modes than it removes. Optional convenience: surface "last 5 sigs used by this prescriber for this product" as click-to-prefill suggestions, but they're plain strings copied into a plain textarea.

## Open strategy ideas

- Sublists are the same screen as the top-level list with a pre-attached, locked filter (LS Admin convention). E.g. "Orders for Patient X" === Orders list with `patient = X` locked.
- Treat ID.me step-up as a single reusable modal triggered by `submit` actions on any controlled-substance flow (order submit, reorder, draft-resubmit). Reduces duplication and keeps the cadence policy in one place (§ F4).
- The "draft → review → submit" pairing on `PrescriptionOrder` is the only place two roles touch the same record. Build it as a single screen with role-dependent footer actions rather than two screens.
- `Prescription` is the long-lived Rx record (with optional `endsAt`); a `PrescriptionOrder` is one fill against it. Refills = new order against the same prescription (so the refill flow is "pick existing Rx, set quantity + ship-to, submit" rather than re-composing the prescription). New medications create a new `Prescription` first, then immediately the first order.

## Screen List

- Log in
  - Email + password + MFA (§ 03, § 05). No SSO in V1.
  - Forgot password → email reset link.
  - First-time activation flow (invite-link landing) sets password + enrolls MFA.

- Clinic Dashboard (PRD Screen 1)
  - Role-segmented widgets; empty states clearly worded.
  - As any clinic user:
    - Active-clinic indicator + switcher when membership count > 1.
    - "Recent activity" — last N status changes on orders this user touched.
    - System announcements banner (Ops-broadcast; deferred model, not in V1 unless cheap).
  - As Prescriber:
    - Drafts awaiting my submission (drafts created by MAs in my clinic — § F3).
    - DEA / state-license expiration warnings at 60 / 30 / 7 days (§ F9).
    - ID.me linkage status (link/relink CTA if missing).
  - As MedicalAssistant:
    - My drafts in progress.
    - Refill queue summary (count due in 7 days, count overdue).
  - As ClinicAdmin:
    - Pending user invites (sent / not yet accepted).
    - Expiring prescriber DEA/state licenses across the clinic.
    - Open invoices / last settlement.
  - Quick actions: Start new order · Add patient.

- Orders (= `PrescriptionOrder` list — § F11)
  - Filters: status, date range, prescriber, MA (creator), patient, pharmacy, product, controlled flag.
  - Status derived from `clinicianReview` + `fulfilled` + linked `PharmacyOrder` lifecycle (§ F6).
  - Columns: order #, patient, product (+strength), prescriber, pharmacy, submitted, status badge, tracking link if shipped.
  - Bulk: none in V1 (orders submit one-at-a-time per ID.me cadence — § F4).
  - Order Entry / Draft (PRD Screen 2 — the central workflow)
    - Two entry points, both land on the same screen:
      - New prescription: composes `Prescription` then its first `PrescriptionOrder` in one save.
      - Refill / new order against existing Rx: opens with `Prescription` pre-selected (read-only) — only pharmacy, quantity, ship-to, and `willLastDays` are editable.
    - Patient card (top): name, DOB, gender, verified `shippingAddress` with inline Smarty/Lob suggestions when unverified (§ F8). Active-clinic badge. Inline edit for shipping address (saves back to `Patient.shippingAddress`).
    - Ship-to selector: clinic address (from `Clinic.primaryAddress` or `additionalShippingAddresses`) or patient address. Snapshots as `PrescriptionOrder.destination` (`VerifiedAddress`).
    - Patient consent reaffirmation checkbox (SMS / email — § 03 assumption; clinic affirms at order entry).
    - **Prescription composer** (new-Rx mode; collapsed read-only summary in refill mode):
      - Product picker: searches `Product.name`. Single selection. Controlled badge shown.
      - Form picker: radio over `Product.forms` for the chosen product — each option labels with its `FormType` plus the unit conventions ("Injectable vial · mg/mL · mL"). Usually 1–3 options. Selection pins a `Product.FormType` (the units travel with the Product for display).
      - Strength input: numeric + `strengthUnit` suffix from the selected Form (e.g. "200 mg/mL"). Required. Persists as `Prescription.strength`.
      - Sig: freehand textarea saved to `Prescription.instructions`. Above the textarea, a "Recent sigs" dropdown lists this prescriber's last N sigs for this product (query: `Prescription` where `prescribedBy == self && product == selected`, ordered by `createdAt desc`). Click to prefill — still fully editable. Required, non-empty.
      - Prescribing user: defaults to current user when Prescriber; MAs must explicitly pick a Prescriber from the clinic's memberships (saved to both `Prescription.prescribedBy` and `PrescriptionOrder.assignedTo` so the draft routes to that prescriber's dashboard).
      - `endsAt` (optional): date picker for prescription expiration. Empty = open-ended.
    - **Pharmacy comparison** panel — only the pharmacy/order half of the screen:
      - Filter: pharmacies whose `Pharmacy.states` covers `destination.address.state` (NOT patient residence — flagged in questions.md), AND have an active `ProductPharmacyMapping` for the chosen `(product, FormType)`.
      - Desktop: row per eligible pharmacy with matching mappings as cells: `(strength, quantity) → price + shipping + total + leadTimeDays`. Side-by-side comparison is the point.
      - Mobile: vertically stacked pharmacy cards, each card listing its available `(strength, quantity)` options as a vertical list of tappable rows. Sort defaults to total price ascending so the first card is the obvious pick. A "Compare" toggle expands a horizontal-scroll matrix for prescribers who explicitly want side-by-side — opt-in, not the default, since the matrix is hard to read on a narrow screen.
      - Cells/rows with `null` strength or quantity render as "Customizable" and expand to numeric inputs on select. Pricing role-gated.
      - Selecting a cell/row pins `(pharmacy, FormType, strength, quantity)` for the order. Strength here must equal the prescription's strength — if the prescriber's strength matches no mapping exactly and no mapping is strength-customizable, that pharmacy is greyed with reason.
    - Quantity + duration: quantity from the selected mapping (or filled in if customizable). `willLastDays` defaults to 28 for injectables (§ F10 BUD rule) and an empty suggestion otherwise; prescriber sets it explicitly. Editable.
    - Order summary panel: prescription summary + selected pharmacy line + shipping + total.
    - Footer actions:
      - Save draft — MA action; creates `PrescriptionOrder` (and `Prescription` if new-Rx) with `clinicianReview = null` and `assignedTo` set to the picked Prescriber. `createdBy` records the MA. Order appears on the assigned Prescriber's dashboard.
      - Submit (ID.me) — Prescriber action (current user must equal `Prescription.prescribedBy`); opens ID.me modal, on success writes `clinicianReview` and dispatches to pharmacy adapter (§ F4). Also stamps `consentAffirmedAt`.
      - Submit is hard-blocked when: prescriber DEA expired (§ F9), product is controlled and prescriber lacks `canSubmitControlledSubstance`, ship-to address unverified (`destination.verifiedAt == null`), sig empty, or consent reaffirmation unchecked.
    - Validation banner area for all blockers (DEA, address, pharmacy licensing, mapping availability — § F3).
  - Order Detail (PRD Screen 3 — multi-shipment tracking)
    - Header: order ID, patient, pharmacy, submitting prescriber, submitted-at (= `createdAt`), ID.me event ID (`clinicianReview.idEvent`), total, overall status badge.
    - "Package N of M" framing: this is a PharmacyOrder-scoped concept. Given this `PrescriptionOrder.fulfilled.by`, look up all sibling PrescriptionOrders in the same `PharmacyOrder` (`fulfilled.by == thisPharmacyOrder`), collect their distinct `shipment` IDs (M = count of distinct non-null shipments expected; N = count with `shippedAt != null`). When `M > 1`, render "Shipped N of M" with siblings linked from the header so the user can navigate the bundle.
    - Status timeline derivation:
      - Submitted: `clinicianReview != null && clinicianReview.approved`.
      - Accepted: `fulfilled.by`'s `PharmacyOrder.accepted != null` AND no `fulfilled.reject`.
      - In Process: Accepted AND `shipment == null` (or shipment exists but `shippedAt == null`).
      - Shipped: `shipment != null && shipment.shippedAt != null`.
      - Rejected: `fulfilled.reject != null` OR `PharmacyOrder.totalRejection != null` (line vs bundle rejection — see model-notes).
      - Cancelled: `cancellation != null`.
      Each step timestamped from the corresponding model field. Future steps render muted.
    - Shipment subpanel — the single linked `Shipment` (via indexed `PrescriptionOrder.shipment`):
      - Carrier (`Shipment.carrier`), tracking number (`trackingNumber`), tracking link (`shippingUrl` when present; otherwise build from carrier + number).
      - Ship date (`shippedAt`), delivered date (`deliveredAt` when present — V1 patients see delivery via carrier link but the field is captured if returned).
      - Other PrescriptionOrders sharing this shipment listed inline ("This package also contains: …") so it's clear when multiple Rx ship together.
    - Sibling-order subpanel: lists every other `PrescriptionOrder` in the same `PharmacyOrder` (with patient, product, status, link). Renders only when the PharmacyOrder bundle has more than one PrescriptionOrder.
    - Patient notifications subpanel: every SMS/email send for this order, with provider message ID and delivery status. No PHI in the rendered body — we display what was actually sent (§ F7). (Notification model is being handled outside the canonical models; UI assumes a lookup by `prescriptionOrder` or `shipment`.)
    - API exchange subpanel: Ops-only. Last adapter request/response with payload hash (§ F14). Hidden from clinic users.
    - Actions:
      - Cancel (clinic, only when no `shipment` and `fulfilled` is null-or-rejected; opens a modal collecting `cancellation.reason`; writes `PrescriptionOrder.cancellation = Cancellation(at, by = currentUser, reason)`; calls pharmacy adapter to cancel where applicable).
      - Re-route (Ops only; picks alternate eligible pharmacy and submits a fresh PrescriptionOrder against the same Prescription — PRD § 06 Ops intervention. Original is marked cancelled with reason "Re-routed to <pharmacy>").
      - Resend tracking SMS (clinic; re-uses last shipment's payload).

- Refill Queue (PRD Screen 4 — § F10)
  - Filters: due-in-N-days, prescriber, product, overdue-only, prescription-expired (where `Prescription.endsAt` has passed).
  - Rows are open `Prescription`s with at least one prior `PrescriptionOrder`, joined on the patient. Each row shows: patient, prescription (product + strength), prior pharmacy, last-order date, refill-due indicator (overdue in red), `endsAt` if set.
  - Calculation: refill due date = last `PrescriptionOrder.createdAt` + `willLastDays`. `willLastDays` was set at prior order time (28-day default for injectables per § F10, prescriber-overridable). No re-derivation here.
  - Per-row One-click reorder opens Order Entry in refill mode with the Prescription pre-selected and pharmacy/quantity/ship-to pre-filled from the prior `PrescriptionOrder`. Prescriber still submits with ID.me — never bypasses (§ F4).
  - Dismiss row → snooze N days. No model change yet; client-only for V1 (per-device snooze acceptable). Cross-device persistence is a follow-up (would need a `RefillSnooze` entity).

- Patients (= `Patient` list — clinic-scoped)
  - Filters: name search, gender, has-recent-order (uses denormalized `lastOrderAt`), consent status, has-unverified-address.
  - Patient Detail (+ edit mode)
    - Identity: first/last, gender, DOB, phone, email.
    - Shipping address card: inline Smarty/Lob suggestions on edit; shows `verifiedAt` + provider (§ F8). Re-verify action.
    - Consent: SMS / email consent timestamps, with "Re-affirm" action (records new `Instant`).
    - Clinical card (collapsible):
      - Allergies (`ClinicalEntry[]`) — three-state UI: "not asked" (null), "none" (empty list), or itemized list. Each row: description, optional code, source (Doctor/Patient/PatientAgent/Pharmacist), reaction, dates.
      - Diseases — same shape minus reaction.
      - Other medications — same shape minus reaction.
      - Add / edit / remove entry inline.
    - Sublists (same-screen-pattern):
      - Prescriptions on file (`Prescription` list, `patient` locked) — primary clinical view; rows show product, strength, sig, prescriber (`prescribedBy`), `createdAt`, `endsAt`, count of orders against it. Click → Prescription Detail (read-only) with Orders sublist and a "New refill" CTA.
      - Orders for this patient (Orders list, `patient` filter locked).
      - Notification history (every SMS/email sent for this patient's shipments — sourced from the notification mechanism being handled outside the canonical models).
    - Quick actions: Start order for this patient (opens Order Entry in new-Rx mode) · One-click reorder from last (opens Order Entry in refill mode against the most recent open `Prescription`).
  - Add Patient = Patient Detail in edit mode with empty values; address-verification step prominent.

- Catalog (= `Product` list)
  - One row per canonical drug (e.g. "Testosterone Cypionate"). Strength/quantity/SKU live on `ProductPharmacyMapping`, not here.
  - Filters: name, controlled flag, active, has-form, available-at-pharmacy.
  - View is role-gated:
    - Clinic users: read-only; columns name, controlled badge, # of forms, # of pharmacies stocking.
    - OpsAdmin: full CRUD; pricing visible inside mappings.
  - Product Detail (+ edit mode — Ops only)
    - General: name, description, `controlled`, `active`.
    - Forms editor (`Product.forms: Set<Form>`): inline list of `(FormType, strengthUnit, quantityUnit)` rows. Add/remove rows. UI enforces one Form per FormType per Product (the model's stated invariant). Editing a Form's units is a display-only change downstream — mappings key on `FormType` not units — but is still flagged with a warning since prior prescriptions and orders denormalized the old strength interpretation.
    - Pharmacy Mappings sublist (`ProductPharmacyMapping`):
      - Grouped by `FormType` (matched to the Product's `forms` for unit-aware display). Per row: pharmacy, strength (or "Customizable"), quantity (or "Customizable"), pharmacy SKU, price, tax, shipping fee, total, lead-time days, active.
      - Inline edit. Add mapping = pick pharmacy + FormType (constrained to FormTypes the Product owns a Form for), then strength/quantity (or leave null for customizable), pharmacy SKU, pricing, lead time.
      - Bulk-import action (Ops) for ingesting a pharmacy's published price list as mappings.
    - No sig content here — sigs are freehand at prescribe time (§ F1 deviates from PRD here, see Source of truth above). Catalog Ops doesn't author or curate sigs.
  - Add Product = Product Detail with empty values, prompting at least one Form before mappings become available.

- Pharmacies (Ops only — `Pharmacy` list)
  - Filters: adapter type, active, ships-to-state.
  - Pharmacy Detail (+ edit mode)
    - General: name, `adapterType` (LifeFile / Empower / Proprietary), contact email/phone, credentials secret ref (display ARN/key name only — never the secret).
    - State-licensing matrix (`Pharmacy.states`): state, effective date, expiration date, notes. Add/edit/remove rows. (§ F2)
    - Mappings sublist: every `ProductPharmacyMapping` for this pharmacy (Catalog mappings list, pharmacy locked).
    - Orders sublist: `PharmacyOrder` list scoped to this pharmacy.
    - Test connection action (Ops): runs a no-op call against the adapter to validate credentials.
    - Activate / deactivate toggle (sets `deactivatedAt`).
  - Add Pharmacy = Pharmacy Detail with empty values.

- Clinics (Ops only — `Clinic` list)
  - Filters: name, active, state.
  - Clinic Detail (+ edit mode)
    - General: name, logo (`ServerFileWithMetadata`), primary address (`VerifiedAddress` — verification via Smarty/Lob), additional shipping addresses (each `VerifiedAddress`), billing contact name/email, Stripe payment ID + payment type (card / ACH), `createdAt`, `deactivatedAt`.
    - Memberships sublist (`ClinicMembership`): user, role (ClinicAdmin / Prescriber / MA), invited-at, accepted-at, deactivated-at.
      - Add membership → invite-by-email flow; sends activation email.
      - Change role / deactivate inline.
    - Sublists: Users, Patients, Orders, Invoices — all clinic-locked.
    - Activate / deactivate.
  - Add Clinic (Ops onboarding flow — § 06)
    - Step 1: clinic details + addresses + billing.
    - Step 2: provision first ClinicAdmin (email; sends activation).

- Clinic Settings (ClinicAdmin, scoped to active clinic)
  - General: name (read-only — Ops-managed), primary address, additional shipping addresses (add/edit/remove — both go through Smarty/Lob, stored as `VerifiedAddress`).
  - Billing: contact name/email, payment method on file (update via Stripe/Priority redirect), last settlement.
  - Members: same membership list as Ops "Clinic Detail → Memberships" but scoped to this clinic. Restricted to `ClinicRole` values only — ClinicAdmins cannot grant elevated `User.role` (system-level role escalation stays Ops-only).
  - Invite member → email, role (`Prescriber` / `MedicalAssistant`; `ClinicAdmin` only when current user is an existing ClinicAdmin), optional message; creates `ClinicMembership` with `acceptedAt = null` and `invitedBy = currentUser`.

- Users (Ops only — global `User` list)
  - Filters: name/email, `UserRole` (User/Admin/Developer/Root), has-prescriber-licensing, DEA-status (verified/pending/expired), MFA-enrolled, clinic membership.
  - User Detail (+ edit mode)
    - Identity: first/last, email (unique), phone, `role` (UserRole — only Developer/Root can promote to Admin or above).
    - MFA status, last login.
    - PrescriberLicensing card (only when `User.prescriber != null`):
      - DEA number, license image preview, expiration, review status (Verified / Pending / Expired — derived from `isDeaVerified` / `isDeaExpired` helpers).
      - State medical licenses sublist — state, license #, expiration, review status. Add/edit/remove.
      - ID.me linkage: subject ID, linked-at. (Cannot be set from UI — informational only.)
      - Verify action (Ops): opens manual verification modal — approver records `Review { byUser, approved, notes }` (§ F9).
      - Replace license image action.
    - Memberships sublist: every `ClinicMembership` for this user.
    - Sublists: orders prescribed (when prescriber — filtered on `PrescriptionOrder.prescribedBy`), orders drafted (when MA — filtered on `createdBy`).
    - Deactivate user (sets `deactivatedAt`).
  - Add User — typically not used directly; users arrive via clinic invitations. Ops can create Admin/Developer users here.

- DEA Verification Queue (Ops only — § F9 / § F12)
  - Filtered view of `PrescriberLicensing` where `deaReview == null` or any `stateLicenses[].review == null` — pending verifications, plus expiring-soon at 60/30/7 days.
  - Bulk: none; each verification is a deliberate review.
  - Per row: prescriber, clinic(s), DEA #, expiration, license image thumbnail, submitted-at.
  - Click row → User Detail with DEA card focused and Verify modal pre-opened.

- Profile (every user's own)
  - Identity: name, email, phone (editable).
  - MFA: enrolled-at, re-enroll, recovery codes.
  - Active clinic / membership list (Prescribers/MAs).
  - Prescriber section (only when `User.prescriber != null`):
    - DEA: number, image, expiration, status badge, "Replace image" (goes back into Ops queue).
    - State licenses: add/edit (each goes into review).
    - ID.me: link / relink button.
    - Reminder log: 60/30/7-day notifications sent for DEA/state expirations.
  - Notification controls — opt-in/out for system emails (renewal reminders, expiring-license warnings, draft-awaits-you, settlement receipts). Patient-facing notifications are NOT controlled here (they're per-shipment to patients, not users).
  - Sign out everywhere.

- Network Order Monitor (Ops only — PRD Screen 5)
  - KPI tiles: active orders, "Awaiting accept > 1h", rejected today, average ship time (§ 04).
  - Same `PrescriptionOrder` list component used by clinics, with no clinic filter locked.
  - Alert column: stuck (submitted > X h without accept), rejected, address-verification failed.
  - Per-row actions: Re-route (alternate eligible pharmacy), Contact pharmacy (templated outreach — out-of-band; just logs the action), Cancel with note.
  - All actions audit-logged (§ F14).

- Invoices (`ClinicInvoice` — § F13)
  - Clinic context (ClinicAdmin): own clinic's invoices.
    - Filters: paid / unpaid, date range.
    - Detail: period start/end, total, paid-at, Stripe ID, line items = the `PharmacyOrder`s in that invoice's billing window (via `PharmacyOrder.invoice`).
    - Download / email receipt.
  - Ops context: all invoices, filterable by clinic, paid status, period.
    - Force-regenerate / mark-paid (logged).

- Audit Log (§ F14) — mechanism handled outside the canonical models
  - Wherever the underlying audit store lives, the UI presents a global filterable view: actor, role, clinic context, action, target entity, timestamp, source IP, request ID, payload hash.
  - Every detail screen links into this view with the entity filter pre-attached.
  - Ops-only. Read-only. No export in V1 (raw CSV download from Ops admin — § 04 measurement plan — is a separate Ops-only action).

- Help / Support
  - Static content; contact info for HeroScript Ops Support.
  - Out-of-scope to author content in V1; placeholder shell.

## Notes on the model that surfaced while sketching screens

Status as of the latest `planned-models.kt`.

**Resolved (model now supports these screens):**

- `Prescription.prescribedBy`, `PrescriptionOrder.createdBy` / `assignedTo` / `consentAffirmedAt` / `cancellation`, and `Shipment` as a first-class entity with `carrier` + `trackingNumber` are all in place. Order Entry's draft assignment, refill suggestion query, cancellation-with-reason, and the Order Detail shipment subpanel all map cleanly.
- `Clinic.primaryAddress` and `additionalShippingAddresses` are `VerifiedAddress` — clinic ship-to destinations preserve their verification trail through the order snapshot.
- `Product.forms: Set<Form>` (one Form per FormType per Product, enforced in service layer) gives Order Entry's Form picker and the Catalog Forms editor a clean data shape.
- `PrescriptionOrder.shipment: Shipment.ID?` is indexed, so "which orders are in this shipment" stays a fast lookup despite the lack of a reverse pointer on `Shipment`.

**Open / handled elsewhere:**

- Notification records: handled outside the canonical models per direction. UI screens (Order Detail notification subpanel, Patient notification history, Profile notification controls) assume a lookup-by-`prescriptionOrder` or `shipment` exists in that mechanism.
- Audit log: same — mechanism is outside the canonical models, but the UI assumes a queryable view and detail-screen deeplinks.
- System Announcements: dashboard banner is described but no model. Defer to V1.x unless cheap.
- `RefillSnooze`: refill-queue dismissals are client-only in V1 per the Refill Queue note above. Server-side cross-device persistence is a future addition if users complain.

**UX gotchas the model doesn't enforce but the UI must handle:**

- ID.me cadence policy: § F4 says per-submission default with possible per-session reduction. Build the modal trigger to read a config flag rather than hardcoding cadence — the screens don't change either way.
- Order rejection: `PrescriptionOrder.Fulfillment.reject` is line-level; `PharmacyOrder.totalRejection` is bundle-level. The Order Detail status timeline collapses both into a single "Rejected" terminal state, but the Sibling-order subpanel surfaces the distinction (bundle-level rejection means *every* sibling rejected too).
- Ship-to vs patient residence for licensing: questions.md flags this as a PRD bug. Order Entry filters by `destination.address.state`, not `patient.shippingAddress.state` — flagging here so it's intentional.
- Customizable-axis mapping pricing: `ProductPharmacyMapping` with `strength == null` and `quantity == null` (both customizable) has an ambiguous price interpretation (per-strength-unit? flat? tiered?). Model carries no `priceModel` discriminator. UI defaults to treating `price` as flat for the chosen (strength, quantity) and warns Ops authors when saving a double-null mapping.
- Form FormType uniqueness: model has a comment, not a constraint. Catalog Ops UI must prevent adding a second Form with the same FormType to a single Product on save.
