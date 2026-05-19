# HeroScript — Proposal-Stage Questions and Assumptions

We're at the proposal/estimation stage. Bid not yet won. The PRD is reasonable as a starting point; most of its open items don't block estimation. The questions below are the ones that genuinely do.

---

## The three questions

### 1. Catalog shape — flat SKU rows, or base product + variant axes?
PRD § 09 models Product with scalar `name, strength, form, SKU` and ProductPharmacyMapping with a scalar `pharmacy SKU`. Read strictly, every (concentration, vial size, form) combination is its own catalog row — a typical TRT product family expands to ~15–20 separate Product rows in the Ops admin.

Flat is defensible: simplest implementation, strictest "no free-form prescribing" guarantee. The cost is Ops-admin bloat as the product line expands, and divergence from how compounding pharmacies actually publish their formularies — Empower, Hone, and similar publish concentration × vial-size grids rather than flat SKU lists.

> *"For a product like Testosterone Cypionate offered across multiple concentrations and vial sizes, do you envision the catalog as ~20 separate rows in Ops admin, or one base row with two selectable axes? The PRD as written implies the former."*

Affects the catalog data model, the Ops admin UX, and how pharmacy adapters translate orders. Roughly 10–15% of the catalog/admin estimate depending on the answer.

### 2. Is the PRD's V1 scope still the V1 scope?
The PRD was AI-drafted and reviewed by the founder. Things shift between drafting and now. Worth asking, in one open-ended question:

> *"Has anything in the PRD changed since v1.1? Any features you now expect in V1 that the doc currently lists as V1.5 or V2? Anything in V1 you've decided to drop? Are the founder's annotated prototype screenshots ready, and do they introduce anything the PRD doesn't describe?"*

Most likely answer: "no, scope is stable." But if EMR integration has crept into V1, or the patient portal is suddenly wanted, that's material to the proposal. Cheap to ask, expensive to miss.

### 3. Sig structure — fixed strings or parameterized?
PRD § 03 / F1 specifies *"pre-created sigs that providers can use in the prescription."* Read literally, this is a fixed-string menu — Ops authors every sig, prescribers pick.

If real Gameday prescribing matches that (prescribers genuinely pick from a fixed menu per product), the data model and admin work stay simple. If prescribers vary dose/frequency per patient on the same product — common in TRT titration — the catalog needs structured sig parameters, the admin tooling grows, and refill-date math (F10's "dose vs vial size") becomes implementable rather than only-approximated by the 28-day rule.

Concretely:

> *"When a Gameday prescriber writes a script today through one of the existing pharmacy portals, do they pick a fully pre-written sig string, or do they specify dose/frequency themselves? If they vary it per patient, what drives that?"*

Effect on estimate: roughly 15–25% of the catalog/order-entry/admin subsystem depending on the answer. Estimable both ways, but the proposal should quote the right shape.

---

## What we'll assume (in the absence of further input)

Stated explicitly so the client can correct in writing before signing:

- **Catalog:** flat SKU rows per (product, strength, form, vial size), per PRD § 09 (pending question 1).
- **Sigs:** pre-created strings per PRD § 03 / F1 (pending question 3). No parameterization.
- **Ship-to default:** clinic shipping address (Clinic `default shipping address` in § 09); patient address is the per-order override.
- **ID.me cadence:** per-submission (PRD § 05 default). Build supports per-session if legal later permits.
- **Payment processor:** Priority Payments (PRD § 03 dependencies + F13 body), despite F13 header and decisions log saying "Stripe" — flagged below as inconsistency to confirm, but priced the same either way.
- **503A only:** patient-dispensed Rx. No 503B office-stock workflow.
- **Refill math:** 28-day rule for injectables in V1; dose-vs-vial-size deferred unless sig structure (question 3) makes it cheap.
- **Patient data:** manual entry in V1 per PRD § 03 + V1.5 EMR integration. No telehealth-platform integration.
- **Telehealth, patient portal, mobile-responsive, CSV export, insurance, multi-language:** all out of V1 per PRD § 03 / § 12.
- **V1 launch criteria:** 10 pilot clinics + ≥6 pharmacies + 2 weeks sustained ordering, per PRD § 12.

---

## PRD inconsistencies / likely bugs to surface in the proposal (not blocking)

These don't change the estimate. Just flag them so the client knows we read carefully and the build will resolve them.

- **Payment processor:** F13 header says Stripe; body and § 03 dependencies say Priority Payments; decisions log says Stripe. Pick one.
- **State-licensing filter:** PRD § F2 / § F3 say *"licensed in patient's state."* Pharmacy licensing actually follows the ship-to destination state. If ship-to is the clinic in a different state than the patient resides, the filter as written is wrong. Should key off `selectedShipTo.state`.
- **F13 line 69 typo:** *"via credit card or ACH ... or credit card"* — duplicated "credit card." Cosmetic.

---

## How to use this list

Three questions in the proposal-prep call. Everything else is either an assumption we're stating up front, a PRD bug we'll flag in writing, or a build-time decision that doesn't change the estimate magnitude.

Once the bid is won and we receive the requested pharmacy API documentation, a longer open-questions list will be needed for kickoff. This isn't that document.
