# Mockup ⇄ Model + Estimate Audit

Audit performed 2026-05-20 against `shared/.../models.kt` and `project-docs/estimate.md`.
Mockup directory: `/Users/jivie/Projects/herxoscript/local/mockup/`.

---

## 1. Critical issues (fix before Jon sees it)

**a. Sigs picker is "recent sigs" — estimate locked-in says "static catalog of common sig templates".**
- `order-entry.html` lines 125, 150: shows `Recent sigs: 0.5 mL IM weekly · 0.25 mL IM 2x/wk · …`
- Estimate § Locked design decisions: *"a static client-side catalog of common sig templates"*.
- These are different products. Recent-from-history is convenient but not what we priced.
- **Fix:** Replace "Recent sigs" with "Common sigs" / "Sig templates" and use generic placeholders (e.g. "Inject {x} mL IM weekly", "Take {n} tab(s) PO daily"). Or flag to Jon that we'd offer both.

**b. ID.me / approval cadence conflicts with estimate's recommended fork.**
- Mockup `index.html` line 99 + `order-entry.html` line 198: "Submit fires ID.me **once for the whole basket**" / "one prescriber approval — one ID.me authorizes the whole order".
- Mockup `profile.html` line 111: "ID.me step-up runs at every controlled-substance Submit by default (per-submission cadence — F4)".
- Estimate § Major forks: recommended cadence is **per-Rx** (prescriber taps ID.me when writing a new prescription; refills reuse prior approval). Per-submission is the PRD over-compliance alternative.
- The mockup commits us to per-order/per-submission — the rejected alternative.
- **Fix:** Either (i) update the mockup to embody per-Rx (new-Rx lines fire ID.me, refill-only lines reuse, basket submit only re-prompts if any line is new); or (ii) explicitly call out to Jon that the mockup shows the PRD-compliant per-submission flow but we recommend per-Rx and the estimate priced per-Rx.

**c. "Order" / "Basket" entity does not exist in the model.**
- All mockup screens treat `Order` as a first-class basket with a header (patient + ship-to + pharmacy + prescriber approval) and N PrescriptionOrder line items.
- `models.kt` has `Prescription` + `PrescriptionOrder` (single-Rx-per-fill) + `PharmacyOrder` (dispatch bundle). There is **no `Order`/basket entity**. `clinicianReview` lives on `PrescriptionOrder`, not on a basket.
- Estimate prices this as a new entity (§ Data models: "Order (basket) + PrescriptionOrder (line item)"), so the proposal is consistent — but Jon may ask "is this already built?" The mockup is showing the *proposed* shape, not what's in the spike repo.
- **Fix:** Either mention in the index intro that the basket model is a proposed change priced in the estimate, or ensure the verbal walkthrough is clear that this is V1-as-spec'd, not V1-as-currently-built.

---

## 2. Inconsistencies (fix if time)

**a. Order IDs.** `ORD-1042`, `ORD-1041`, … sequential numbers. Model uses `Uuid`. No human-friendly order-number field exists. *Fix:* either fine (display abbreviation) or call out as "display-only label" if anyone asks; better, switch to a short hash like `ORD-7c4e`.

**b. "In Process" status pill** appears in `orders.html`, `order-detail.html`, `order-monitor.html`. Not directly modeled — derivable from `fulfilled.accept != null && shipment == null`, but it's a UI invention not a stored field. Fine to display; flag in walkthrough so Jon doesn't assume a DB field.

**c. "Stuck" status pill** (`orders.html`, `order-monitor.html`) is purely a time-elapsed derivation. Same as above — display state, not stored.

**d. `profile.html` notification preferences card** (lines 114–119): "Drafts awaiting my submission", "DEA expiring 60/30/7d", "Daily settlement receipt". No `NotificationPreferences` model exists. Either price an addition or remove these toggles. Currently mockup-only invention.

**e. Index nav (`index.html`) mixes Prescriber and Ops links** in the same bar: Dashboard + Patients + Orders + Ops Monitor in one row. Other screens correctly split (clinic screens hide "Ops Monitor"; `order-monitor.html` shows the Ops nav). Cosmetic but it muddies the role-segmented story.

**f. Order Detail "Re-route to alternate pharmacy"** (`order-detail.html` line 148) is marked Ops-only but appears as a visible greyed button to a Prescriber. Per estimate's role model this should be hidden, not disabled (CLAUDE.md: "Hide (don't disable) out-of-role actions").

**g. `order-entry.html` shows the "Prescribing user" picker even though the demo role is Prescriber.** This picker is MA-only behavior (MA drafts on behalf of a Prescriber). For a Prescriber the field should be locked to self. Currently shows a select with multiple prescribers, which implies a Prescriber can draft for another prescriber.

**h. Mobile collapse already covered by CSS** (`@media (max-width: 768px)` and `420px` rules in `styles.css`) — tables convert to data-label cards, nav collapses to drawer, rows go single-column. Confirmed working. No issue.

---

## 3. Missing surface area (would strengthen the proposal)

**a. AddressEditor with verify-now inline UX.** Estimate § Frontend foundation prices "AddressEditor" + "Smarty/Lob inline suggestions". The mockup only *shows* verified state, never the editor. A small inline panel on `patient-detail.html` would land the priced cost visibly.

**b. ID.me step-up modal not shown.** Estimate prices a per-submit modal; mockup just has a "Submit order (ID.me)" button that jumps straight to Order Detail. A modal screenshot/wireframe would make the cost concrete.

**c. Pharmacy state-licensing matrix.** Mockup describes "licensed TN · ships in 2d" inline but doesn't show the matrix editor (estimate § Ops admin: "Pharmacies list + detail with state-licensing matrix editor"). Linked stub in Ops nav but no screen. OK if scope-bound, but worth a sentence on the Ops nav landing.

**d. Patient HIPAA consent UI at order entry.** Estimate § HIPAA prices "Clinic-affirmation UI at order entry" — mockup has a checkbox but no NPP acknowledgment / revocation surface. Mention or stub.

**e. DEA Verification Queue (Ops).** Linked in Ops nav but no screen. Estimate prices 2 points. A stub page would balance the Ops story.

**f. Audit log Ops UI.** Linked in Ops nav, priced 3 points, no screen. Same.

**g. Invoice screen.** Linked, priced 3 points (dual-context), no screen. Important for Jon — billing is a PRD headline.

---

## 4. Mockup-only inventions (would commit us to extra scope)

**a. "Recent sigs" autocomplete** (see Critical 1a). Implies per-user/per-prescription sig history persistence. Not in model. Strip or rename to "Common sigs / templates".

**b. Notification preferences toggles on Profile** (see Inconsistency 2d). No model field. Strip or price as an addition.

**c. "Snooze 7d" on Refill Queue.** Mockup notes "client-only in V1 (per-device, session-scoped)" — OK as flagged, but if Jon clicks it expecting persistence we owe him a server field. Keep the disclaimer.

**d. "View audit history" links** on Patient Detail and Order Detail. Estimate has Ops audit-log UI priced; per-entity audit history isn't separately scoped. Either fold into the Ops audit UI (filter by patient/order) or remove the links.

**e. "Cancel order (with reason)" on Order Detail** — backed by `PrescriptionOrder.Cancellation` (reason field exists). OK. Just confirm role gating: per current model a regular Prescriber cancelling a submitted order needs `updateRestrictions` work that hasn't been spec'd.

**f. "One-click reorder (last Rx)" button on Patient Detail.** Fine — it's just a routing shortcut into Order Entry refill mode. But verify the button text matches the estimate's "one-click reorder routing into Order Entry" line.

---

## 5. What's RIGHT (anchor — don't break)

- **Basket flow on `order-entry.html`** correctly embodies one ship-to + one pharmacy + N lines + one prescriber approval (matches estimate's recommended basket-first fork).
- **Ship-to drives pharmacy licensing** is called out explicitly in `order-entry.html` line 64 and 75 (matches locked decision; correctly contradicts PRD § F2's patient-state language).
- **Catalog as product + form + pharmacy mappings** (`catalog.html`) — strength/quantity per pharmacy mapping, "Customizable" tag — matches the product+variants fork.
- **VerifiedAddress treatment** correctly shows `verifiedAt` + `verificationProvider` ("Verified · 2025-12-04 · Smarty").
- **Three-state clinical card** (null / empty / itemized) on `patient-detail.html` matches Patient.allergies semantics exactly.
- **DEA + state licenses + ID.me subject id** on `profile.html` reflect `PrescriberLicensing` fields one-to-one.
- **Multi-clinic membership chip** + clinic memberships card on Profile matches the `ClinicMembership` many-to-many.
- **Line-level vs bundle-level reject distinction** in Order Detail closing note matches `PharmacyOrder.totalRejection` vs `PrescriptionOrder.Fulfillment.reject`.
- **Refill due = last order + willLastDays** on `refill-queue.html` matches the denormalized `Patient.lastOrderAt` + `PrescriptionOrder.willLastDays` derivation.
- **Mobile-first responsive CSS** (`@media 768px` and `420px`) — table-to-card collapse, drawer nav, single-column rows. Working.
- **SMS body example** ("Hi Sam — package 1 of 2 …") — first name + tracking link, no PHI. Matches PRD § F7 / § 11.

---

## 6. Visual simplifications

Principle: simple beats feature-dense. Each one-liner is a *consider removing/splitting/collapsing*, not a redesign.

**Split pages over packing one page.**
- `catalog.html` is the clearest case: it shows a 4-row product table *and* the Testosterone Cypionate "Product preview" with full forms + pharmacy mappings on the same screen. **Split into Catalog list (product + summary only) and Catalog detail (forms, strengths, mappings, prices, lead times) on a separate route.**
- `patient-detail.html` already has separate list/detail (good) but stacks Identity + Shipping + Consent + Clinical + Prescriptions tab + future Orders/Shipments tabs all on one page. **Move Clinical card behind an "Edit clinical info" sub-page**; keep the visible detail to identity + shipping + consent + Rx-on-file table.
- `order-detail.html` blends summary + timeline + 2 line items + sibling shipments + notifications + actions on one page. **Move "Patient notifications" table to a sub-tab or its own page**; it's reference-only, not act-upon.
- `profile.html` has 7 cards on one screen. **Split DEA + State licenses + ID.me into a "Credentials" sub-page**; keep Profile to identity + MFA + clinic memberships.

**Density / consolidation.**
- `dashboard.html` KPI row: "DEA expiration 47d" already appears in the page-sub line. **Drop the DEA KPI tile.** Three KPIs are calmer than four.
- `order-entry.html` shows totals **twice** (summary table rows for shipping + sticky footer). **Keep only the sticky footer.**
- `order-entry.html` "Pre-submit checks" alert and the basket footer are stacked above-and-below the same submit area. **Inline the checks into the footer as a single line of pass/fail icons.**
- `order-monitor.html` 9-column table is desktop-dense. **Drop "Lines" and "Submitted" from the default columns**; offer a Columns menu.
- `orders.html` has 8 columns + 5 filters. **Collapse "Tracking" into "Status" as a hover/expand**; default to 3 filters (Status, Date, Patient) with "More filters" toggle for the rest.

**Status / pills.**
- `orders.html` row for ORD-1042 shows "In Process" pill + "1 of 2" tracking sub-label. **Pick one — the pill alone reads cleaner**; the line-count belongs in detail.
- `order-detail.html` header has both an "In Process" pill and a "Shipped 1 of 2" line in the third card. **Consolidate; the timeline already shows progress.**

**Nav.**
- Clinic nav strip has 7 items (Index, Dashboard, Patients, Orders, Refill Queue, Catalog, Profile). `index.html` adds Ops Monitor = 8. **Drop "Index" from the runtime nav** (it's a wireframe-only landing); move "Catalog" + "Profile" under a profile avatar menu on desktop. Target: 4 primary items (Dashboard, Patients, Orders, Refill).

**Form density.**
- `order-entry.html` basket header has 4 stacked selects/inputs (Patient, Ship-to, Pharmacy, Prescriber) plus a consent checkbox above the line items. **Group Patient + Ship-to as "Who & where" and collapse Pharmacy + Prescriber under a "Routing" sub-section that opens once a patient is picked.**
- Each prescription line has 7 fields visible. **Hide "Rx ends at (optional)" behind an "Advanced" toggle**; most lines won't set it.

**Top simplification opportunity:** split `catalog.html` into a list page and a detail page — it's the most obvious "two screens crammed into one" and the easiest win to demo to Jon.
