# HeroScript Discovery Call — Meeting Minutes

**Date:** 2026-05-18
**Attendees:**
- **John** (Client) — Game Day clinic owner / HeroScript founder
- **Dan** — Lightning Kite (Business / Operations lead)
- **Joseph Ivey** — Lightning Kite (General Manager / Lead Developer)
- **Eric** — Lightning Kite

---

## Introductions

Dan and Eric introduced themselves. John joined from a building under renovation in downtown Logan that will become his clinic's new home. Joseph Ivey introduced himself as the technical lead at Lightning Kite.

---

## Project Overview

Eric summarized Lightning Kite's understanding of the PRD:

- John's network ("Game Day") includes clinics that order from 6–12 approved pharmacies.
- Pharmacies have varying state-licensing coverage.
- The platform must aggregate data across pharmacies into a single interface where providers can pick the best prescription option (including cost).
- It must consolidate order management and tracking in one place rather than forcing staff to log into multiple pharmacy portals.
- The platform also benefits pharmacies (single payer, reduced account-management overhead) and gives the network volume-based negotiating power.

John confirmed the big-picture understanding is correct. He noted the PRD is directional — feature-level details will be fleshed out over time.

---

## The Operational Problem

John described the current workflow pain:

- A patient asks about an order three days after it was placed.
- Staff must log into one system to find which pharmacy filled it, then log into that pharmacy's portal to check shipping/tracking, then copy-paste a tracking number into a text back to the patient.
- This happens ~300 times/month per clinic, across 400 clinics — completely unscalable.

**Dan's question:** Is this causing customer churn?

**John's response:**
- His clinic is one of the better-run ones, but friction is real.
- Common pattern: patient runs out of meds because a Game Day order was missed or tracking never arrived.
- Stopgap is inviting the patient in for a clinic shot.
- Generates significant phone/text volume — "enough smoke" to justify solving it.

---

## Phase 2: Refill Reminders

Briefly covered in the PRD. Currently John runs a spreadsheet that flags patients ~28 days after their last order. Most clinics only trigger on payment events.

Future opportunity:
- Calculate run-out date from prescription dose and vial size.
- Trigger an automated text to non-recurring patients prompting a refill.
- Treated as a "Gen 2" feature — focus first on getting orders flowing.

---

## HIPAA / PHI Compliance

**Dan:** Will PHI be transmitted on the platform?

**John:** Yes — extensively. The platform handles prescriptions end-to-end:
- Patient name, address, medication, dosing
- Prescriber identity and credentials (DEA, medical licensing)

There is no obfuscated version of this. Full HIPAA compliance is required.

**Dan:** Lightning Kite already deals with HIPAA-adjacent compliance, so the requirement does not change feasibility — it just means the standard path applies.

---

## Prior Prototypes

Two prior attempts exist:

1. **Pure vibe-coded prototype** — non-technical person used Claude to generate features. Available for demo.
2. **Developer-assisted vibe code** — a former technical co-founder used Claude + ChatGPT in tandem and reviewed code before pushing. Built in ~3 weeks. He asked for 40% equity, which killed the arrangement.

**Decision:** John and Joseph agreed neither prototype should be used as a foundation. Both are useful only for visualizing ideas. Build fresh — too much PHI risk to trust vibe-coded code.

---

## API Access

**Joseph's primary concern:** Access to pharmacy and identity APIs is the biggest unknown for estimation.

- John can obtain all APIs within ~a week. One is already in hand; the rest were sent to the previous developer.
- Pharmacies are "pliable" given Game Day's volume — most will hand over API docs readily.
- Quality varies — some APIs will be solid, others rough. Joseph noted Lightning Kite has worked with worse (e.g., Banner).

---

## Monetization Model

**Core model:** Spread on medication price.
- Pharmacies do not enforce MAP pricing — they only care about their wholesale price.
- Spread size depends on market tolerance.

**Future revenue streams:**
- Shipping markup (pharmacies often charge ~$15 for what costs ~$6).
- "Pro" features such as patient texting, billed as access fees.

For now: focus on ordering, cost visibility, and pricing display to clinics.

---

## Billing Flow

**Joseph's question:** Double-invoicing model — clinic gets billed, then Lightning Kite/HeroScript pays the pharmacy?

**John's answer:**
- **Clinic side:** Daily batch billing of all orders placed that day. Auto-collected via card on file or ACH. Card may carry a CC fee.
- **Pharmacy side:** Pharmacies invoice HeroScript directly, typically weekly (some daily). Cross-reference reporting needed but no integrated AP system required.

Dan noted these terms are tighter than typical net-30/net-90. John clarified pharmacies want fast payment because they currently chase thousands of small doctor's-office accounts. A single, reliable payer (HeroScript) is a key value proposition. Terms may stretch from 7 → 30 days over time but unlikely to reach 90.

---

## Identity Verification (ID.me)

**Clarification:** ID.me in current systems verifies the **prescriber**, not the patient — primarily for controlled substances (in their case, mostly testosterone).

**Pain point:** Prescribers re-verify per prescription — ~20 times/day. Cumbersome.

**Discussion:**
- Legal requirement vs. system requirement is unclear. John flagged it for review.
- Once-per-session or once-per-day verification would likely be acceptable.
- Joseph proposed a queue model: a medical assistant builds a list throughout the day; the prescriber signs off the entire queue with one ID verification at the end.
- John noted prescribers typically don't write their own scripts — assistants queue them already. This pattern fits.

**Decision:** Build identity verification as a configurable variable (per-script vs. per-session vs. per-day) so frequency can be adjusted later. Default toward more secure than less.

**Note on ID.me itself:** Not necessarily the only option — Joseph mentioned Stripe Identity (~$0.50/verification — too expensive at scale). Treat the provider as TBD; the constraint is just "something stronger than a password."

---

## Competitive Landscape

**SureScripts** is the ~20-year-old backbone of e-prescribing — not a system but the underlying transport. Most e-prescribing systems (including "ScriptSure," which John's clinic uses) are built on top.

**Limitations:** Acts like a fax — no verification of prescriber or prescription content. John demonstrated by example: he could technically prescribe "burger and fries" to anyone at a CVS and it would arrive.

**Target market for HeroScript:**
- **Pharmacies:** Compound pharmacies (e.g., Reeds, Hiram, Cook Brothers) — not retail chains.
- **Clinicians:** Wellness clinics — med spas, men's health, injectables.
- **Payment model:** Mostly cash-pay medicine, monthly treatment plans.

This is the gap SureScripts-based platforms don't serve well.

---

## Technical Scope Summary

Joseph's read of the core technical challenge:
- 6+ pharmacy APIs, each with different shapes.
- System must correctly identify which pharmacies can fill a given prescription (state licensing).
- Catalog reconciliation across pharmacies.

**John pushed back on catalog deduplication:** Don't try to merge catalogs. Show each pharmacy's catalog as-is; prescribers can choose. Smart routing is a long-term feature, not v1 — also avoids the awkward optics of HeroScript steering orders.

**Joseph's takeaway:** Removing smart-routing simplifies v1 scope significantly. The hard work is:
1. Building the data system / forms / workflows (Lightning Kite's strength).
2. Integrating the unknown-shape pharmacy APIs.
3. HIPAA compliance.

---

## Pricing Discussion

**Dan floated:** ~$250K, 4 months to launch + 2 months follow-on development.

**John's response:**
- Number itself isn't a blocker — capital is available.
- Other bids are coming in around $100K, with 2–3 month pilot timelines.
- His real fear: hiring a $100K shop, getting 100K in, discovering they can't deliver, and having to start over.

**Joseph's clarification on Lightning Kite's pricing:**
- $150/hour, T&M.
- Most of the project effort is jumping through HIPAA and API compliance hoops, not code volume.
- Code-only estimates from competitors may be accurate for code only — but understate compliance and integration overhead.

**Dan's note on competitors:** Most Utah competitors offshore part of their work, which is where the price gap comes from. Lightning Kite does not offshore.

**Joseph on AI:** He treats AI as roughly equivalent to offshoring for critical/compliance work. May use AI to **audit** code, but not to **build** the compliance-sensitive parts.

---

## Engagement Length

**John's ideal:** Lightning Kite builds and maintains for at least 1 year, with the option to extend to 3.

**Long term:**
- John's personal horizon is 3–5 years.
- Doesn't want to build a large in-house dev team — the company's enterprise value is the network (clinics + pharmacies + volume), not the tech.
- The tech must be "great" but is a given, not the differentiator.

---

## Pilot & Rollout Plan

- Initial pilot: ~10 clinics from Game Day's 400.
- Game Day is fully committed and will tell its franchisees this is the new ordering system.
- After pilot stabilization: announcement to the broader network with a waitlist, then onboard in waves of 10–20.
- Pace controlled by customer-service and pharmacy capacity.
- Success metric: pilot clinics prefer it over current tools.

---

## Equity / Partnership Option

John raised the possibility of Lightning Kite taking equity as a long-term partner.

**Dan's response:**
- Lightning Kite has done this before — typically a discounted dev rate in exchange for equity.
- Not required from Lightning Kite's side — they get protective of projects regardless of ownership.
- Open to discussing.

---

## Next Steps

**John needs by Wednesday/Thursday:**
- 3–5 page proposal (not a 30-page doc).
- Who Lightning Kite is and why they're the right fit.
- Approach to the project.
- Estimated effort (hours, timeline).
- Pricing.

**John's timeline:**
- Wednesday: daughter's high school graduation.
- Thursday: award the project.
- Friday: leaving town for family time before daughter leaves on a mission (Taiwan) in ~2 weeks.

**Decision criteria:** Not lowest bidder. High-stakes pick — whoever wins the contract is the long-term partner. The hour-long discovery call gave John more signal than any written proposal could.

---

## Action Items

| Owner | Action |
|---|---|
| John | Send pharmacy API documentation as it becomes available (within ~1 week). |
| Lightning Kite | Deliver 3–5 page proposal by Wednesday/Thursday. |
| Lightning Kite | Internal regroup on scope, estimate, and pricing approach. |
| Both | Continue dialogue on identity-verification approach (per-script vs. per-session vs. per-day). |
