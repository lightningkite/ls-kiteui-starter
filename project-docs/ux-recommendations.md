# UX/UI recommendations

Companion to `ui.md`. Findings from a hands-on audit of the running app at `http://localhost:8941`. Recommendations only — no changes applied.

## Methodology

Walked the app as:
- **Prescriber** (`prescriber@example.com`, seeded session) — Clinic Dashboard, Orders list, Order Detail, Order Entry (new-Rx path), Refill Queue, Patients list + detail, Catalog list + detail, Profile, and confirmed (broken) Clinic Settings / Invoices behavior.
- **Admin** (`admin@example.com`, seeded session) — Ops Dashboard, Order Monitor, Clinics list + detail, Users, Pharmacies list + detail, DEA Queue, Invoices.
- **ClinicAdmin** (`clinicadmin@example.com`, seeded session) — Clinic-Admin Dashboard, Clinic Settings (blank — see Bugs).
- **MA**: not directly walked (no seeded plaintext session secret available; the email-PIN login path stalled on the Submit button — flagged in Bugs). MA-specific surfaces inferred from prescriber walkthrough (the prescriber dropdown, the Save-draft action, and pricing visibility were verified through the prescriber's view of Order Entry and Catalog).

Viewports: desktop ~1400×813 (the only viewport actually achievable through the MCP browser — `resize_window` does not constrain the rendered viewport on this host; mobile-specific reflow behavior was therefore not exercised. The "mobile reality check" findings below are inferences from the inline layout choices observed at the wider viewport and from quick CSS-clamping experiments).

Not covered:
- Mobile viewport flow (could not reduce browser viewport below ~1400px).
- MA persona's Save-draft + prescriber-pick flow end-to-end.
- Working order submission (Submit-ID.me modal didn't surface — pharmacy comparison panel stayed empty for the tested product/strength).
- Forced-update interstitial (reviewed source only, did not simulate).

## High-impact (would noticeably improve first-impression for a client demo)

### 1. Dashboards and detail screens flash empty headers before content loads
**Where:** every page that depends on session + role-cache lookup (Clinic Dashboard, Patients, Refill Queue, Orders, Catalog, Order Monitor, Pharmacy Detail, etc.).
**What's there now:** On first navigation to a page, a fully-rendered shell of empty card headers ("Active clinic", "Recent activity", "Drafts awaiting your submission", "License expiration") paints first; then 2–4 seconds later the content fills in. On a direct URL hit (e.g. `/patients`), the page is even worse: it shows "No active clinic" until the membership cache resolves, then re-renders correctly.
**Why it's a problem:** The user sees a screen that looks broken (empty bars labelled "Active clinic" with nothing under them, or contradictory "No active clinic" messaging) before the real data lands. It's the first thing a client sees on the demo. Already called out in `user-flows.md` § Demo notes ("pre-navigate to a different page and back before showing the dashboard") — that's a workaround, not a fix.
**Recommendation:** Hold the shell back until the active-clinic resolution completes (single top-level `ReadyState` gate). Either show a single centered spinner during cold-load, or render only after `activeClinicMembership() != null || sessionResolved`. Most dashboard widgets shouldn't paint their card chrome until their data signal has resolved. Same for list pages — render the filter row + a skeleton row, not card headers above an empty void.
**Effort:** medium.

### 2. Status badges, role pills, and verified flags are uncolored text
**Where:** Orders list, Order Detail, Order Monitor, Patients list ("Verified"), Catalog list ("Controlled"), Users list ("Admin" / "User"), Pharmacies list ("Active"), Invoices ("Unpaid").
**What's there now:** Status is monochromatic text in the same color as the surrounding labels: "Pending submission", "Shipped", "Submitted", "Accepted", "Active", "Verified", "Controlled", "Unpaid", etc. — all visually equivalent.
**Why it's a problem:** Eye can't scan a list of orders and pick out "the one that's rejected" or "the stuck one." Status is the single most-scanned piece of info on every list. Colorless badges defeat the entire purpose of the column.
**Recommendation:** A small typed-badge component that takes a semantic status and applies a colored pill (success / warning / danger / info / neutral). Use it for: order status, controlled-substance flag, license verification state, invoice paid/unpaid, pharmacy active. Five colors total. The theme is locked but the badge component can pull colors from theme variants — KiteUI's `important`/`subtle`/`warning` variants exist.
**Effort:** small (one shared component, swept across ~8 screens).

### 3. ISO timestamps are shown raw everywhere
**Where:** Dashboard "Recent activity" and "Drafts awaiting your submission", Orders list ("Submitted 2026-05-13T01:50:56.016523Z"), Order Detail status timeline, Patient Detail consent timestamps, Profile DEA + state license + ID.me dates, Invoice list (the invoice header IS the date range — see below), Catalog mappings, Pharmacy detail "Created".
**What's there now:** Every datetime is rendered as the unformatted ISO-8601 string.
**Why it's a problem:** Reads like a database dump. A clinic user has to mentally parse `2026-05-20T16:27:03.529Z` into "today, 4:27 PM" every time they scan a row. On Invoices, the *primary row label* is `2026-04-20T01:50:56.016738Z – 2026-05-19T01:50:56.016741Z` — that should be "Apr 20 – May 19, 2026".
**Recommendation:** Single `formatInstant(instant, style)` helper with three styles: relative ("just now", "3 hours ago", "yesterday") for activity feeds, short ("May 20, 2026") for list rows, and long ("May 20, 2026 at 4:27 PM CT") for detail-screen headers. FEEDBACK.md already notes the missing date *picker* primitive — this is the display side of the same gap; both should land together.
**Effort:** small (helper + sweep).

### 4. Order Entry validation list and pharmacy panel sit as silent gaps
**Where:** Order Entry, the area between the Prescription section and the Submit footer.
**What's there now:** A short bulleted list ("• Enter a strength · Enter a sig · Pick a pharmacy · Set a quantity") sits in a card with no styling — it reads as a section, not as guidance. Above it, the Pharmacy Comparison panel renders as an empty dark bar (the same `slightCard` background) with a separate small-text caption "Pick a pharmacy first." When you've already filled in the patient, ship-to, product, form, and strength, the empty panel looks broken, not waiting.
**Why it's a problem:** The two things that block submission are the pharmacy selection and the validation checklist. Both render as silent voids. The prescriber sees the screen and asks "is it loading?"
**Recommendation:** (a) Make the validation list a styled hint-banner (info icon + colored border) that shows ONLY currently-blocking items, removing each as it's resolved. (b) The pharmacy panel should have its OWN worded empty state inside the card ("Choose a product and strength to see eligible pharmacies" → "No pharmacies match this strength in TN — see Catalog mappings"). The mere presence of an empty dark bar above the patient/product/strength summary is the worst possible affordance.
**Effort:** medium.

### 5. Order Entry "Expiration (ISO instant — optional)" leaks developer language
**Where:** Order Entry, Prescription section, end-date field.
**What's there now:** Label reads `Expiration (ISO instant — optional)`. Placeholder `Leave blank for open-ended`. Field accepts a free-text ISO-8601 string.
**Why it's a problem:** Prescribers will not type `2027-05-20T01:50:56.016523Z`. They will leave the field blank, or they will fail and call support. This is the most visible developer-leak in the entire prescriber-facing surface.
**Recommendation:** Label "Prescription end date (optional)". Use a date picker primitive (FEEDBACK.md § "No native Instant/LocalDate picker" already flags the missing primitive — this field is the most user-visible motivation to build it). The label should not mention "ISO" or "instant". Same fix needed for DEA expiration in Profile, state-license expiration, prescription `endsAt`, pharmacy state-info effective/expiration, invoice period filters — they all read as ISO strings today.
**Effort:** medium (depends on the primitive landing first).

### 6. Order Detail "Order 00000000" header is the truncated UUID prefix
**Where:** Order Detail, header.
**What's there now:** Page title reads `Order 00000000` — that's the first 8 characters of the seed UUID `00000000-0000-0000-0000-0000000003ca`. For a real (random) UUID, this would be an arbitrary hex prefix.
**Why it's a problem:** Looks like a placeholder that no-one finished. "Order 00000000" doesn't identify anything; two real orders submitted within hours of each other will have unrelated prefixes that don't sort meaningfully.
**Recommendation:** Either (a) drop the order number from the title entirely — the patient + product + status in the header are the actual identity — or (b) introduce a short, sortable, clinic-scoped sequence (e.g. `ORD-2026-0142`). The PRD doesn't mandate a specific format; ui.md just says "order #". Option (a) is cheapest and works for V1. Option (b) requires a small schema addition (a sequence counter on Clinic).
**Effort:** small (option a). Medium (option b).

### 7. Several list screens render contradictory empty states alongside data
**Where:** Patients list ("No patients match the current filters." displayed below 2 visible patient rows). Pharmacy Detail "State licensing" section ("No state licensing configured." displayed above TN + KY tiles).
**What's there now:** The default-empty placeholder text is unconditionally rendered, regardless of whether the list above it has items.
**Why it's a problem:** Reads as "Here are two patients. There are no patients." Looks broken. Undermines trust on a first impression.
**Recommendation:** Gate the empty-state copy on `list.isEmpty()`. Pattern is identical in both places — looks like a copy-paste of a List+EmptyState component that didn't wire its visibility. Worth a quick sweep across all list screens to catch others.
**Effort:** small.

### 8. Refill Queue is mostly empty void with a stranded "Search prescriber" field
**Where:** Refill Queue (prescriber session).
**What's there now:** Three filter rows packed together; one row has the Product search field on the left and the "Search prescriber" placeholder hanging in the open space to its right with no surrounding card frame. Below the filters: a giant empty dark card with no rows and no empty state.
**Why it's a problem:** It looks like the page failed to render. The Refill Queue is one of the two screens the demo plan calls out explicitly ("Refill Queue → one-click reorder" from `user-flows.md` § Demo notes). If there are no due refills, the empty state should say so confidently ("No refills due within 7 days. Adjust the window to see more."). The orphaned search field amplifies the broken-layout impression.
**Recommendation:** (a) Wrap the filters in a consistent card or single horizontal-flex row matching the Orders list pattern. (b) Add a worded empty state under the (currently blank) results card: "No refills due in this window. Try expanding the date range or clearing filters." (c) Consider showing a count next to the page title (`Refill Queue · 0`) so the user instantly knows whether the screen has data.
**Effort:** small.

### 9. Drafts and License-expiration dashboard cards have no body copy when empty
**Where:** Prescriber Dashboard "Drafts awaiting your submission" (when there are drafts: rows render; when there are none: the card header is a bare bar with nothing below). Same shape on ClinicAdmin "Prescriber licenses expiring".
**What's there now:** Header bar with nothing beneath. No empty-state copy.
**Why it's a problem:** Reads as broken. Adjacent cards ("Announcements", "License expiration") DO have empty-state copy ("No announcements.", "No expirations within 60 days.") — the inconsistency is visible.
**Recommendation:** Every dashboard card gets a worded empty state in the same style. "No drafts awaiting your submission.", "No prescriber licenses expiring soon.", etc. Pattern is already used in some cards; just complete the sweep. Per `ui.md` § Cross-cutting: "Empty states: every list/queue has a worded empty state."
**Effort:** small.

### 10. Active-clinic context is rendered as the largest card on the dashboard
**Where:** Clinic Dashboard (every clinic-user persona).
**What's there now:** The top of the dashboard is a card labelled "Active clinic" with "Gameday Knoxville" rendered at near-h1 weight, full-width, with no surrounding context. It dominates the screen.
**Why it's a problem:** Visual hierarchy is inverted. The active clinic is *context*, not *content*. The user is in this clinic — they don't need it shouted at them. Meanwhile, the *actual content* the prescriber came for (drafts awaiting submission, recent activity) sits below the fold.
**Recommendation:** Per `ui.md` § Cross-cutting: "active-clinic switcher when membership count > 1" — this belongs in a chip below the top app bar, not a full-width card. Demote the clinic-name card to a single-line chip or remove it entirely when the user belongs to only one clinic. Hoist Drafts / Refill summary / Recent activity above the fold.
**Effort:** medium (small if just demoting; medium if introducing the chip pattern).

### 11. Order Detail status timeline reads as a flat list, not a timeline
**Where:** Order Detail status panel.
**What's there now:** Four labeled rows stacked vertically — "Submitted / Accepted / In Process (— em dash for missing) / Shipped" — each with its own timestamp. No connecting line, no indicator of current step, no visual progression.
**Why it's a problem:** The status journey is the entire point of the screen. A flat list of labels doesn't communicate "we're here, this is where we came from, this is what's next." Users have to read each row to figure out where the order stands.
**Recommendation:** A vertical stepper (filled circle for completed, ring for current, empty circle for upcoming; connecting line in between; current step bold). Reuse the colored badge from recommendation #2 for the overall status pill in the header. Even a thin colored line on the left edge of completed-step rows would do most of the work.
**Effort:** medium.

### 12. Tracking URL is displayed as a raw URL string, not a link
**Where:** Order Detail Shipment subpanel.
**What's there now:** `https://www.ups.com/track?tracknum=1Z999AA10123456784` rendered as plain dark-on-darker text. It IS clickable but reads as nothing.
**Why it's a problem:** Looks like data. Looks unfinished. A patient/MA/prescriber wanting to share a tracking link won't realize they can click.
**Recommendation:** Render as "Track package" linked-button with carrier icon (`Icon.externalLink`). The raw URL belongs in a copyable detail row labeled "Tracking URL" or hidden entirely.
**Effort:** small.

### 13. Login → Submit-PIN spinner never resolves and shows negative countdown
**Where:** Login flow, after typing email + receiving PIN + entering PIN.
**What's there now:** After clicking Submit on the PIN field, the button shows a spinner indefinitely. Even after a successful login on the API side, the UI doesn't advance to the dashboard. The "Send new code" button below it later shows the message "Can send new code in -3" (negative seconds — a countdown that went past zero without disabling).
**Why it's a problem:** First-time login is the *first* impression. A spinner that never resolves on the most-used auth path is a demo killer.
**Recommendation:** Investigate why the Submit-PIN spinner stalls — likely an unresolved promise on the proof-to-session step. Independently, fix the countdown: when it hits 0, show "Send new code" alone (no countdown line) instead of letting it tick negative.
**Effort:** medium (the stuck-spinner is likely a real bug; see Bugs section).

## Medium-impact (polish that adds up)

### 14. Order Monitor KPI tiles are smaller than the search bar
**Where:** Order Monitor header.
**What's there now:** Four KPI tiles ("Active 1", "Stuck > 1h 1", "Rejected 24h 0", "Avg ship —"). Each value is rendered at body-text size; the labels are larger than the numbers. The tiles look like decorations.
**Why it's a problem:** The whole point of KPI tiles is at-a-glance scanning. Tiny numbers in tiles below a giant search bar invert the priority.
**Recommendation:** KPI value at h1 weight, label at body size. Stack: value first, label below. Color the value when above a threshold (Stuck > 0 → amber). Tiles should be tappable to apply the matching filter (already specified in ui.md § Network Order Monitor — verify it's wired).
**Effort:** small.

### 15. Order Entry chip-style radio for Ship-to and Form is non-standard
**Where:** Order Entry, "Ship to" chips (Clinic primary / Patient address), Form picker, Prescriber chip.
**What's there now:** Each selection is a pill-style chip with a `•` for selected and `○` for unselected, plus text. The whole pill is clickable.
**Why it's a problem:** Looks like a tag or filter chip, not a single-select. Users may not realize they can switch. The `•`/`○` glyphs are inconsistently spaced and read as bullet-point decoration rather than radio indicators.
**Recommendation:** Either go with standard radio buttons (still single-select, but obviously single-select) OR keep the chip pattern but use a true selected/unselected visual variant (filled background vs outline) instead of leading-glyph variation. The current state — pill + leading glyph — looks like neither.
**Effort:** small.

### 16. "Add patient" link next to "Start new order" button looks like a heading
**Where:** Clinic Dashboard quick actions; Patient detail "Start order for this patient"; Pharmacy Detail "Test connection / Deactivate".
**What's there now:** Primary action ("Start new order") is a filled blue button. The secondary action ("Add patient") is plain text with an icon — no background, no border, looks like a label.
**Why it's a problem:** Secondary actions are still actions. They should look clickable. As text, they read as headings or static info — a user won't try them.
**Recommendation:** Use the theme's secondary/outlined button variant for non-primary actions. Apply consistently: every clickable "do something" element should at minimum have a hover state and an outline. Especially important for Pharmacy Detail's Deactivate (destructive — should be styled as warning/danger) and "Test connection" (an Ops action, currently invisible).
**Effort:** small.

### 17. Catalog mappings show strength/quantity without units
**Where:** Catalog Detail (clinic user view), Pharmacy Detail Catalog mappings, Order Entry pharmacy comparison.
**What's there now:** "Strength: 200 Quantity: 10" with no units. The Form above declares "mg/mL · mL" but the mapping row doesn't pull those units through.
**Why it's a problem:** A clinician reading "200" can't tell whether it's mg, mg/mL, mL, or units. For TRT injectables specifically — where strength is `mg/mL` and the mappings reflect that — leaving units off is a clinical safety smell.
**Recommendation:** Mapping rendering should consume the parent Form's `strengthUnit` and `quantityUnit` and append them ("200 mg/mL · 10 mL"). Same fix needed in the Order Entry pharmacy comparison, the prescription summary at the bottom of Order Entry ("Testosterone Cypionate · Injectable vial · qty 0"), and Order Detail header ("Semaglutide · 2.5 mg/mL").
**Effort:** small.

### 18. Pharmacy state-licensing rows show only the state code
**Where:** Pharmacy Detail State licensing matrix.
**What's there now:** Two rows showing just "TN" and "KY". No effective date, no expiration date, no notes — though the model carries all of them.
**Why it's a problem:** Ops verifying pharmacy state licensing needs the expiration date most of all (per § F2 of the PRD). Currently they'd have to click into each state to see anything.
**Recommendation:** Row shows state · effective–expiration date range · notes (truncated) · status pill (active / expiring soon / expired). Match the Orders list row density. The data is in the model.
**Effort:** small.

### 19. Profile DEA section leaks raw filenames and IDs
**Where:** Profile, Prescriber section.
**What's there now:** "License image: placeholder.pdf" (filename leak). "ID.me Linked: seed-idme-001 at 2026-04-20T01:50:56.010970Z" (raw seed event ID + raw ISO).
**Why it's a problem:** Seed-data identifiers visible to a real user. "placeholder.pdf" is a tell-tale that the file picker isn't real; "seed-idme-001" looks like a developer string.
**Recommendation:** When the file-picker is the V1 stub, show a generic "Image uploaded" caption with an icon, not the filename. For ID.me, render "Linked Apr 20, 2026" — drop the event ID into a small "Linked via ID.me" tooltip or remove it entirely from the prescriber's view.
**Effort:** small.

### 20. Profile MFA "Not enrolled" with "Re-enroll" and "Recovery codes" links is contradictory
**Where:** Profile, MFA card.
**What's there now:** Header reads "Multi-factor authentication". Body line one: "Not enrolled". Below that: two links, "Re-enroll" and "Recovery codes" — both implying you ARE enrolled.
**Why it's a problem:** A user who isn't enrolled can't "re-enroll", and there are no recovery codes to show. The links look broken or confusing.
**Recommendation:** Render the card in two modes. Not enrolled: single "Set up MFA" button. Enrolled: enrolled-at timestamp, "Manage recovery codes" link, "Re-enroll device" link.
**Effort:** small.

### 21. Order Entry Recent-sigs section shows label even when empty
**Where:** Order Entry Prescription section.
**What's there now:** "Recent sigs from this prescriber" label appears above whatever's beneath. When there are no recent sigs, the label still shows with empty space below it.
**Why it's a problem:** Looks like a misfire. Either hide the section or show "No recent sigs for this product yet" — currently the user has to guess.
**Recommendation:** Hide the section header when the recent-sigs list is empty. Per `ui.md` § Cross-cutting: "actions hidden, not disabled, when out of role" — empty-by-data should be the same: hidden, not silently empty.
**Effort:** small.

### 22. Ops Dashboard shows the clinic-context "No active clinic" empty state
**Where:** Admin role logging in lands on `/dashboard`, which still renders the ClinicDashboardPage.
**What's there now:** "No active clinic — Accept a clinic membership invite to get started." The Ops user has no active clinic (they aren't a clinic member).
**Why it's a problem:** Ops users see an instructional empty-state that doesn't apply to them. Reads as an error or onboarding state. The Order Monitor is the right Ops landing page (per `ui.md`), not the clinic dashboard.
**Recommendation:** When User.role >= Admin, route `/dashboard` to Order Monitor (or to a future Ops Dashboard that summarises Order Monitor KPI tiles + recent verifications). At minimum, replace the "Accept a clinic membership invite" copy with role-aware copy ("Open Order Monitor to begin your shift" + link).
**Effort:** small.

### 23. Order row Patient + Product is the same weight as the date below it
**Where:** Orders list rows, Recent-activity dashboard rows, Drafts dashboard rows, Order Monitor rows.
**What's there now:** "Sam Sample · Semaglutide" is rendered at the same font size and weight as "Submitted 2026-05-13T..." in the line below.
**Why it's a problem:** Visual hierarchy within the row is flat. The patient + product is the scan-target, the date is metadata. They should not be equal weight.
**Recommendation:** Patient + product at body-large weight; metadata line in subtle body color. Status pill at right. Match the pattern already in use on the Pharmacies list ("LifeFile Compounding" header + "Active · support@... · 2 states" subtitle).
**Effort:** small.

## Low-impact (microcopy, alignment, minor consistency)

### 24. Grammar: "1 forms", "1 pharmacies", "1 states"
**Where:** Catalog list rows, Pharmacy list rows.
**What's there now:** Hardcoded plural noun; reads "1 forms · 1 pharmacies".
**Why it's a problem:** Looks unpolished.
**Recommendation:** Format-with-plural helper. `pluralize(count, "form")`.
**Effort:** small.

### 25. "Verified (seed)" / "Verified" / "DEA verified" inconsistencies
**Where:** Order Entry ship-to "Verified (seed)" (leaks the verification provider). Patients list "Verified" (no source). Users list "DEA verified" (different label form). Pharmacy Detail state row missing the verification state entirely.
**Recommendation:** Pick one verification-pill component, one label set. Hide the provider string from the user view ("Verified" not "Verified (seed)"). Put consistent badges in the same place across all screens.
**Effort:** small.

### 26. Orders search placeholder is overloaded
**Where:** Orders list, Order Monitor.
**What's there now:** "Search patient, product, pharmacy, prescriber, MA" / "Search patient, product, pharmacy, clinic, prescriber" — five-way match in one input.
**Why it's a problem:** Users don't know if they have to type one of those words, or if it's full-text across all. The placeholder is also long enough to truncate on a narrower viewport.
**Recommendation:** "Search orders" or "Search" with an info tooltip explaining the matched fields. Keep the underlying multi-field match.
**Effort:** small.

### 27. Filter checkbox-button hybrid is unusual ("Controlled", "Active", "Has form", "Unverified only", "Overdue only", "Rx expired")
**Where:** Catalog list, Patients list, Pharmacies list, Refill Queue.
**What's there now:** A bordered rectangle with a check-icon and label that looks like a toggle button, but ALSO like a checkbox.
**Why it's a problem:** Unclear affordance. Is it a toggle (button) or a multi-select (checkbox)? At quick glance, it looks like a button group, but it's actually a single boolean toggle.
**Recommendation:** Use either a true segmented control (clearly button-like) for binary toggles, or a true checkbox-with-label (clearly a checkbox). The current hybrid is the worst of both.
**Effort:** small.

### 28. "Member of clinic (name)" filter input is a bare text field
**Where:** Users list (Ops).
**What's there now:** Plain text input with placeholder "Member of clinic (name)".
**Why it's a problem:** Free-text matching against clinic names is fragile. Should be a select / autocomplete of actual clinics.
**Recommendation:** Convert to clinic-select (autocomplete or dropdown). Matches the "All clinics" select pattern used on Order Monitor.
**Effort:** small.

### 29. Notification-preference rows have label far-left, checkbox far-right
**Where:** Profile, Notification preferences.
**What's there now:** Label hugs the left margin; checkbox hugs the right margin of the page (>1100px gap at desktop). On mobile they'd be on the same line still but with the same horizontal pull.
**Why it's a problem:** Eye doesn't connect the label to its control. Looks like two unrelated columns.
**Recommendation:** Indent the checkbox into the same column-flow as the label, OR move the checkbox to the *left* of the label (standard checkbox-with-label pattern). The current layout is the worst case.
**Effort:** small.

### 30. Order Detail header has multiple empty placeholder bars between sections
**Where:** Order Detail.
**What's there now:** Between the header card and the status timeline, and again between the Shipment subpanel and the Patient Notifications subpanel, there are dark empty rectangles with no labels and no content.
**Why it's a problem:** Looks like skeleton placeholders that never resolved. Probably failed N+1 lookups or empty sibling-order subpanel (which should render "no siblings" or hide).
**Recommendation:** Hide the entire sibling-order subpanel when there are no siblings (M=1). Verify the same for the API-exchange Ops-only subpanel.
**Effort:** small.

### 31. Invoice row primary label IS the raw ISO date range
**Where:** Invoices list.
**What's there now:** Row header reads `2026-04-20T01:50:56.016738Z – 2026-05-19T01:50:56.016741Z`.
**Recommendation:** "Apr 20 – May 19, 2026" or "May 2026 settlement". Subtitle keeps the clinic, status, payment ID (cleaned up — see #32).
**Effort:** small.

### 32. Invoice row "Stripe in_seed_plac" is a truncated developer string
**Where:** Invoices list row subtitle.
**Recommendation:** Hide the Stripe payment ID from list view (move to detail). Or replace with a friendly "Stripe" badge + tooltip.
**Effort:** small.

### 33. "Server" dropdown on the login screen is dev-only chrome
**Where:** Login page (only visible in the DOM, lower in the layout).
**What's there now:** A "Server: Same Server / Local" select sits between the title and the email input.
**Why it's a problem:** Even partially hidden, it's developer chrome. If a client sees it on the demo, they'll ask what it is.
**Recommendation:** Hide unless a dev-mode env flag is on, or move below the fold under a "Debug" disclosure.
**Effort:** small.

## Cross-cutting themes

- **Race condition between page-paint and role/clinic-membership resolution affects almost every page.** First navigations show shells of empty cards; direct URL hits show wrong-context empty states ("No active clinic" for a user who has one); some pages (Clinic Settings for ClinicAdmin, see Bugs) never recover at all. A single "session+role ready" gate at the App level would fix most of it. Worth doing once, centrally.
- **"Empty state" is implemented inconsistently.** Some cards have great worded empty states ("No announcements.", "No expirations within 60 days."). Others show nothing. Others show the empty-state copy alongside data (bug). Single component, applied everywhere.
- **Datetime/units presentation is the single biggest "this looks unfinished" signal.** Every ISO timestamp, every unitless number, every leaked filename / event ID is a "demo killer" microcue that adds up.
- **Status / verification / role pills are everywhere but never typed.** A single colored-badge component would lift the polish floor across ~10 screens for a few hours of work.
- **Secondary actions are styled as plain text.** "Add patient", "Resend tracking SMS", "Test connection", "Deactivate", "Start order for this patient", "Sign out". All look like static labels. They need at least an outline / hover state.
- **Validation messages render as cards or untyped text, not as banners.** "Pick a pharmacy first.", "Pick a product first.", "Pick a form first.", the four-item submission blocker list — all of these are correct content placed in the wrong UI primitive (card body instead of inline-hint or banner).

## Things that are already excellent

- **Patient picker in Order Entry** — typing "Sam" surfaces "Sam Sample · DOB 1985-04-12" cleanly; after pick, collapses to a compact summary with a "Change" affordance. This is the gold standard for the rest of the entity-pickers in the app.
- **Consent-reaffirmation behavior** — picking a patient correctly auto-checks SMS + Email checkboxes from the patient's stored consent. Subtle but right.
- **Recent-sigs prefill** — small delight; deserves the demo-call-out it gets in `user-flows.md`.
- **Worded empty states on Patient Detail clinical card** — "Allergies / Diseases / Other medications" each say "Not asked at intake" when empty. Three-state UI handled exactly as specified.
- **Login email→proof selection→PIN flow** is a clean three-step on a single page (no mid-flow redirects), with the proof method buttons (Email Code, Enter Password, Authenticator, Backup Code) visible after entering the email. Modulo the submit bug (#13), the flow shape is right.
- **Ops nav structure separates Clinic vs Ops contexts clearly** with the section header treatment; correct items appear per role.
- **Pharmacy Detail Recent-orders subpanel** with status pill + external ID is a solid Ops surface. Mostly needs the polish items above; the structure is right.
- **Order Monitor KPI + per-row actions** layout (modulo recommendation #14) is the right shape. KPI tiles, search, filters, then a list with inline ops-only actions.

## Bugs found during audit (not UX)

- **Clinic Settings is completely blank for ClinicAdmin user.** Navigated to `/clinic-settings` as `clinicadmin@example.com`, page header renders ("Clinic Settings"), but the entire content area stays empty. Waited 6+ seconds, reloaded — no change. Same screen is also blank for Prescriber (expected — they shouldn't have access — but the page should hide itself or show a "ClinicAdmin only" message, not render blank).
- **Submit-PIN button stalls indefinitely on Login.** Entered `prescriber@example.com`, received PIN in backend log (`WYMBTZ`), typed it, clicked Submit. Spinner appears on the button and never resolves. Session DID succeed (subsequent localStorage manipulation confirms the API path works), so the UI step that closes the modal / redirects to dashboard is the broken part.
- **"Send new code" countdown ticks negative.** After a Send-new-code click during PIN entry, the "Can send new code in 7" text decrements past zero and reads `-3` while the "Send new code" link is still grouped with it (visually overlapping). Should disable the link until the timer hits 0 and clear the countdown text at 0.
- **`/dashboard` race during first-navigation flashes the wrong-context empty state.** Loading `/dashboard` as a prescriber initially shows "No active clinic — Accept a clinic membership invite to get started." for ~2 seconds before the active-clinic resolves and the real dashboard paints. Also confirmed: Patients list briefly shows "No active clinic — Patients are scoped to a clinic." with the entire nav greyed out before reflowing. These race-flashes are also called out as recommendation #1; flagged here because they're functional regressions in the page-load sequence, not just visual polish.
- **Pharmacy comparison panel stays empty when product + form + strength are set.** Picked Sam Sample → Clinic primary → SMS/Email auto-checked → Testosterone Cypionate → Injectable vial → strength 200 mg/mL. The Pharmacy Comparison panel above the validation list stayed an empty dark bar. There's a seeded mapping `LF-TEST-200-10` at exactly that strength on LifeFile Compounding for TN, so the panel should populate. Not investigated further — may be a state-licensing-filter discrepancy or a strength-match issue.
- **Admin Catalog list shows no products.** Navigated as `admin@example.com` to `/catalog`; products were visible as the prescriber (Semaglutide, Testosterone Cypionate) but rendered empty as admin. Filters render, list does not. Also the navigation regressed to just "Profile" — implies an auth/permission boundary that doesn't show a useful error.
- **Active-clinic resolution side effect during "sign out" click.** Clicking the top-right sign-out icon when the dashboard was in its "No active clinic" flash state caused the active clinic to *load* (Gameday Knoxville appeared) and the sign-out did not execute. Likely the icon was hidden under a competing reactive update; user perceives the sign-out button as non-functional.
