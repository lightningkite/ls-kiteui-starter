# User flows

Companion to `ui.md` (which is screen-by-screen). This document describes **what tasks a user comes into HeroScript to perform** and the path through the app for each one. Use this when explaining the app to a client, onboarding a new developer, or sanity-checking that every screen earns its keep against a real task.

For each task: trigger, entry point, path through the app, what gets persisted, and what's still a placeholder in V1.

Tasks are organized by role. A few cross-role flows (order lifecycle, clinic onboarding) appear at the end.

---

## Sign-in & activation (every role)

### First-time activation
**Trigger:** Receives an email with an invite link (from a ClinicAdmin invite or HeroScript Ops onboarding).

**Path:**
1. Click invite link → `/activate/{token}` (`ActivationPage`).
2. Set password.
3. Enroll MFA (TOTP QR + recovery codes) — or defer with "I'll set up MFA later."
4. Land on Dashboard.

**Persists:** User password + MFA enrollment. ClinicMembership's `acceptedAt` is stamped (for clinic-user invites).

**Stubs in V1:** TOTP QR rendering is a placeholder; recovery codes list is a placeholder. The flow shape is right but the cryptographic plumbing isn't wired.

### Sign in
**Trigger:** Returning user opening the app.

**Path:**
1. `/` (LandingPage) decides session → redirects to LoginPage if not authenticated.
2. Email + password.
3. If MFA enrolled, second step: TOTP code (or backup code / email PIN as fallbacks).
4. Land on Dashboard.

**Persists:** Session token in localStorage; `User.lastLoginAt` updated.

**Stubs:** None — MFA proofs route to real Lightning Server `UserAuth` endpoints via KiteUI's `AuthComponent`.

### Sign out
**Trigger:** Anywhere — from the user menu or the Profile screen.

**Path:**
1. Top-bar user menu (or Profile → "Sign out").
2. Server terminates session.
3. Active-clinic context cleared.
4. Redirect to LoginPage.

---

## ClinicAdmin

The clinic's billing-and-team owner. One per clinic.

### Invite a Prescriber or MA
**Trigger:** Adding a new clinician or MA to the team.

**Path:**
1. Dashboard → Clinic Settings (or hamburger → Clinic Settings).
2. Members card → "Invite member" expands the inline form.
3. Enter email, pick role (Prescriber or MedicalAssistant), optional message.
4. "Send invite" — creates a `ClinicMembership` with `acceptedAt = null` and `invitedBy = self`. If the email doesn't match an existing User, creates a User shell as well.
5. Invitee gets an email with the activation link (server-side dispatch — see Stubs).

**Persists:** ClinicMembership, possibly a new User.

**Stubs in V1:** Activation email dispatch is server-side and currently placeholder text — verify the email-out path before demo.

### Update billing info
**Trigger:** Switching from credit card to ACH, or contact-person change.

**Path:**
1. Clinic Settings → Edit.
2. Update `billingContactName`, `billingContactEmail`, `stripePaymentType`, `stripePaymentId`.
3. Save.

**Persists:** Clinic record.

**Stubs:** Real Stripe / Priority redirect for payment-method updates is a placeholder — the field is just a string. The client can demo "Update payment method" and see the placeholder.

### Update clinic shipping addresses
**Trigger:** Opening a second clinic location, or correcting an address.

**Path:**
1. Clinic Settings → Edit.
2. General section → Primary address (uses `AddressEditor` with Smarty/Lob stub).
3. Additional shipping addresses → add/remove rows.
4. Each address has a "Verify address" button that currently stamps `verifiedAt` manually. (Smarty/Lob integration is the real one.)
5. Save.

**Persists:** Clinic's address fields, each as a `VerifiedAddress`.

**Stubs:** Smarty/Lob address verification — currently stamps `verifiedAt = now, verificationProvider = "manual"`. The verification UX works; the actual API call is the stub.

### Change a member's role or deactivate
**Trigger:** Promoting an MA to Prescriber (with DEA upload to follow), or removing a departing member.

**Path:**
1. Clinic Settings → Members card.
2. For each member row: "Change role to …" dropdown + "Apply"; or "Deactivate" button.
3. UI blocks demoting / deactivating the last active ClinicAdmin (would orphan the clinic).
4. Confirmation toast on success.

**Persists:** ClinicMembership.role and/or .deactivatedAt.

### View invoices
**Trigger:** Daily settlement notification, or end-of-month reconciliation.

**Path:**
1. Top nav → Invoices (clinic-context view).
2. Filter by paid/unpaid, period.
3. Click a row → InvoiceDetailPage with header, line items (each line is a `PharmacyOrder` in the billing window), receipt button.

**Persists:** Nothing on view. "Download receipt" is currently a placeholder.

**Stubs:** Receipt generation/email is placeholder.

---

## Prescriber

DEA-licensed clinician. Submits orders.

### First-time setup (after activation)
**Trigger:** First time signing in after accepting a ClinicMembership invite as a Prescriber.

**Path:**
1. Activate (set password + MFA as above).
2. Dashboard prompts: "Upload your DEA license to begin prescribing."
3. Profile → Prescriber section → upload DEA license image, enter DEA number, expiration.
4. (Optionally) add State Medical Licenses for each state you practice in.
5. (Optionally) link ID.me.
6. Wait for HeroScript Ops to verify DEA (DEA Verification Queue flow on the Ops side).
7. Once verified, the prescribe-controlled-substances gate opens.

**Persists:** `User.prescriber: PrescriberLicensing` populated, with `deaReview = null` (pending).

**Stubs:** File picker for the DEA license image is a placeholder — currently shows the ServerFile location as text rather than an inline preview. ID.me linkage is a "Coming soon" toast.

### Submit an MA's drafted order
**Trigger:** Dashboard "Drafts awaiting my submission" widget surfaces orders assigned to me by an MA.

**Path:**
1. Dashboard → Click a draft row → OrderDetailPage.
2. Review patient, product, strength, sig, pharmacy, ship-to.
3. Click **Submit (ID.me)** in the action bar.
4. ID.me modal: 1-second stub spinner → success.
5. Status timeline advances Submitted → Accepted (when pharmacy adapter returns acceptance).
6. Return to Dashboard.

**Persists:** `PrescriptionOrder.clinicianReview = ClinicianReview(...)` + `consentAffirmedAt`.

**Stubs:** ID.me is fake-success after 1 second. Pharmacy adapter dispatch is not wired (the order lands in the DB with `clinicianReview` set but no real submission is sent out).

### Create a prescription + order from scratch
**Trigger:** Seeing a new patient who needs a new med, with no MA in the loop.

**Path:**
1. Top nav → Orders → "+" button (or from Patient Detail → "Start order").
2. OrderEntryPage in new-Rx mode.
3. Pick patient (existing or "Add patient" — see MA flow for the patient-creation path).
4. Pick ship-to (clinic primary, additional, or patient address).
5. Reaffirm consent (SMS / Email checkboxes, pre-checked from `Patient.smsConsent` / `emailConsent`).
6. Pick product → pick form → enter strength → enter freehand sig (or click a "Recent sigs" suggestion to prefill).
7. Pick pharmacy from comparison panel (rows sorted by total ascending; pricing visible).
8. Confirm quantity + `willLastDays` (defaults to 28 for injectables; editable).
9. Click **Submit (ID.me)** → 1-second stub → order written.
10. Land on OrderDetailPage with status timeline.

**Persists:** `Prescription` (new) + `PrescriptionOrder` (with `clinicianReview` and `consentAffirmedAt` stamped).

**Stubs:** ID.me stub; pharmacy adapter dispatch.

### Refill an existing prescription
**Trigger:** Patient is due — either Dashboard Refill summary or proactive review.

**Path:**
1. Top nav → Refill Queue.
2. Find patient row (overdue ones first; click "Reorder").
3. OrderEntryPage opens in refill mode: prescription composer is read-only (product/form/strength/sig pre-locked); only pharmacy / quantity / ship-to / `willLastDays` are editable.
4. Confirm or change selections.
5. **Submit (ID.me)** — same as fresh order.

**Persists:** New `PrescriptionOrder` against the existing `Prescription`.

### Replace DEA license image (e.g. renewal)
**Trigger:** 60/30/7-day expiration reminders, or manual update.

**Path:**
1. Profile → DEA card → "Replace image" button.
2. Upload new image + update expiration date.
3. Save → `prescriber.deaReview = null` (back to pending).
4. Wait for Ops to re-verify (DEA Queue flow).

**Persists:** New DEA license image + expiration, reset review status.

**Stubs:** File picker is a placeholder — currently just edits the location string. Replace with a real upload before demo.

### Add a state medical license
**Trigger:** Beginning to practice in a new state.

**Path:**
1. Profile → State medical licenses sublist → "Add license."
2. Enter state, license number, expiration.
3. Save → `review = null` (pending).
4. Ops verifies via DEA Queue (it surfaces state-license-pending rows too).

**Persists:** Appended to `PrescriberLicensing.stateLicenses`.

---

## Medical Assistant

Drafts orders on behalf of a Prescriber. Cannot submit.

### Draft an order for a new patient
**Trigger:** Patient walks in (or telehealth intake) and the chart says "needs Rx X."

**Path:**
1. Top nav → Patients → "+ Add patient."
2. Fill out PatientDetailPage in new-patient mode: name, gender, DOB, phone, email, verified shipping address, optional clinical entries (allergies / diseases / other meds).
3. Save → Patient persisted with `clinic = activeClinic()`, `createdBy = self.id`.
4. From Patient Detail's "Start order" quick action → OrderEntryPage with patient pre-selected.
5. Walk the Prescriber's "Create from scratch" path EXCEPT click **Save draft** instead of Submit.
6. Pick a Prescriber from the dropdown (which routes the draft to that prescriber's Dashboard).
7. Draft appears on the chosen Prescriber's "Drafts awaiting my submission" widget.

**Persists:** Patient + Prescription (new) + PrescriptionOrder (with `clinicianReview = null`, `assignedTo = pickedPrescriber.id`, `createdBy = self.id`).

**Stubs:** Same as Prescriber's create flow.

### Draft an order for an existing patient
**Trigger:** Returning patient who needs a refill or new Rx.

**Path:**
1. Top nav → Patients → click patient row.
2. "Start order for this patient" quick action.
3. Walk the create-from-scratch path; click **Save draft** at the end.

### Update a patient's address
**Trigger:** Patient moves, or address verification flags a mismatch.

**Path:**
1. Patients → click row → Patient Detail → Edit.
2. Shipping address card → AddressEditor opens with the existing address pre-filled.
3. Edit and "Verify address" → re-stamps `verifiedAt`.
4. Save.

### Triage the refill queue
**Trigger:** Morning routine — clear out due/overdue refills.

**Path:**
1. Top nav → Refill Queue.
2. Filter by due-in-N-days, prescriber, product, overdue-only.
3. For each row: click "Reorder" → OrderEntryPage in refill mode → Save draft → assigns to original Prescriber.
4. Or "Snooze" to dismiss for the session (no server-side persistence per V1 — client-only).

**Persists:** New PrescriptionOrder drafts.

### Edit a patient's clinical entries
**Trigger:** Patient reports a new allergy, or a chart update.

**Path:**
1. Patient Detail → Edit → Clinical card.
2. Three subsections (allergies, diseases, other meds), each with three-state UI:
   - "Not asked at intake" (null) → click "Ask now" to convert to empty list.
   - "Patient reported none" (empty list) → click "Mark unasked" or "Add entry."
   - List with items → add/edit/remove rows inline.
3. Each entry: description (required), code (optional — RxNorm/SNOMED-CT/ICD-10 per container), source (Doctor / Patient / PatientAgent / Pharmacist), reaction (allergies only), dates.
4. Save.

---

## HeroScript Ops (Admin / Developer / Root)

Internal team. Onboards clinics + pharmacies, manages the catalog, monitors orders, verifies DEA licenses, handles billing exceptions.

### Onboard a new clinic
**Trigger:** New clinic signs up via the sales pipeline.

**Path:**
1. Ops nav → Clinics → "+" button.
2. ClinicDetailPage in new-clinic mode.
3. Step 1 fields: name, primary address (verified), billing contact, payment info.
4. Save → clinic created → inline "Provision first ClinicAdmin" form expands (Step 2).
5. Step 2: enter email and optional name for the first ClinicAdmin → "Send invite" creates a User if needed + ClinicMembership(role=ClinicAdmin, acceptedAt=null, invitedBy=self).
6. The new admin receives the activation email and walks the first-time activation flow.

**Persists:** Clinic + User (sometimes) + ClinicMembership.

**Stubs:** Activation email dispatch is placeholder.

### Onboard a new pharmacy
**Trigger:** New pharmacy partner signed.

**Path:**
1. Ops nav → Pharmacies → "+" button.
2. PharmacyDetailPage in new-pharmacy mode.
3. Fill name, adapterType (LifeFile / Empower / Proprietary), credentialsSecretRef (ARN pointer — never the secret itself), contactEmail/phone.
4. State-licensing matrix — add rows for each state the pharmacy is licensed to ship to, with effective/expiration dates.
5. Save → pharmacy created.
6. From Catalog → product detail → add ProductPharmacyMapping rows for each product this pharmacy stocks (or use "Bulk import" placeholder when implemented).
7. "Test connection" placeholder runs a no-op adapter ping.

**Persists:** Pharmacy + Pharmacy.StateInfo rows + ProductPharmacyMapping records.

**Stubs:** Test-connection action toasts "Coming soon." Bulk-import of pharmacy price lists is a placeholder. Real adapter credential storage is in AWS Secrets Manager — the UI just shows the ARN.

### Add a product to the catalog
**Trigger:** New product line being added.

**Path:**
1. Ops nav → Catalog → "+" button.
2. CatalogDetailPage in new-product mode.
3. Fill name, description, `controlled` flag, `active`.
4. **Add at least one Form** (FormType + strengthUnit + quantityUnit). The system blocks save without one.
5. Save → product persisted with no mappings yet.
6. Pharmacy Mappings section unlocks → click "Add mapping" → pick pharmacy + FormType (constrained to FormTypes this Product has) → fill strength (or "Customizable"), quantity (or "Customizable"), SKU, price/tax/shipping/lead-time.
7. Each mapping persists immediately on Save.

**Persists:** Product + ProductPharmacyMapping records.

**Stubs:** Bulk-import is a placeholder.

### Verify a Prescriber's DEA license
**Trigger:** New prescriber activated and uploaded DEA, or existing prescriber's renewal needs review.

**Path:**
1. Ops nav → DEA Verification Queue.
2. Filter by DEA pending / state license pending / expiring soon (60/30/7 day windows).
3. Rows sorted by urgency (already-expired first).
4. Click "Open verification" → UserDetailPage with DEA card focused.
5. Verify DEA → modal: decision (approve/reject) + notes.
6. Submit → writes `prescriber.deaReview = Review(byUser = self, approved, notes, at = now())`.
7. Toast confirms; prescriber is now verified (or the rejection puts them in a pending state pending re-upload).

**Persists:** PrescriberLicensing.deaReview (or one of stateLicenses[i].review).

### Monitor network orders for stuck/failed
**Trigger:** Ops daily routine — clear the alerts.

**Path:**
1. Ops nav → Order Monitor.
2. KPI tiles at top: Active, Stuck > 1h, Rejected (24h), Avg ship time.
3. Tap a tile to apply the matching filter to the list.
4. List shows network-wide PrescriptionOrders with clinic column + alert reason column.
5. Per-row actions:
   - **Re-route**: opens picker of alternate eligible pharmacies (currently placeholder — would cancel original + create new PrescriptionOrder against same Prescription).
   - **Contact pharmacy**: opens pharmacy contact info + placeholder "Send templated outreach."
   - **Cancel with note**: opens reason input, writes `cancellation = Cancellation(at = now(), by = self, reason)` — this one is real.

**Persists:** PrescriptionOrder.cancellation when cancelled.

**Stubs:** Re-route action is placeholder (doesn't actually create the replacement order). Contact-pharmacy outreach is placeholder. The Cancel-with-note path is fully working.

### Mark an invoice as paid
**Trigger:** Settlement reconciliation; manual confirmation outside the Stripe flow.

**Path:**
1. Ops nav → Invoices.
2. Filter to unpaid.
3. Click invoice → InvoiceDetailPage.
4. "Mark paid" button (Ops-only) → confirms → stamps `paidAt = now()`.

**Persists:** ClinicInvoice.paidAt.

**Stubs:** Force-regenerate is placeholder. Receipt download is placeholder.

### Promote a user to Admin/Developer role
**Trigger:** New HeroScript team member onboarding.

**Path:**
1. Ops nav → Users → "+" (or find existing User).
2. UserDetailPage → Edit → change `role` to Admin / Developer / Root.
3. Save.

**Persists:** User.role.

**Constraint:** Only Developer or Root can grant ≥ Admin. UI surfaces the reason inline if you try.

---

## Patient (passive, no app access)

### Receive shipment notification
**Trigger:** A pharmacy ships a package.

**Flow:**
1. Pharmacy returns a Shipped event via API (with carrier + tracking number).
2. HeroScript writes a `Shipment` record with `shippedAt = now`, links it to the PrescriptionOrder(s) in the package.
3. SMS dispatched to patient via Twilio HIPAA tier: "Hi {firstName}, package N of M from your {clinic} order shipped via {carrier}. Track it: {link}. Questions? Call your clinic at {clinic phone}."
4. Optionally email to same effect.

**Persists:** Shipment + Notification records (the latter handled outside canonical models per build direction).

**Stubs in V1:** The whole pharmacy-shipped event isn't wired yet — pharmacy adapter dispatch and webhook ingestion are placeholders. SMS dispatch via Twilio also placeholder. The UI surfaces the Notification subpanel on Order Detail as "Notifications will appear here once the notification mechanism lands."

---

## Cross-role: the order lifecycle end-to-end

Combines roles. This is the canonical demo flow.

1. **MA** opens Patients → adds a patient (verifying address inline) → starts an order from Patient Detail.
2. **MA** picks product, form, strength, freehand sig → picks Prescriber from dropdown → picks pharmacy → Save draft.
3. **Prescriber** logs in (or is already in) → Dashboard shows "Drafts awaiting my submission" → clicks the draft.
4. **Prescriber** reviews on OrderDetailPage → clicks Submit (ID.me) → 1-second stub → status advances to Submitted.
5. **(External, stubbed)** Pharmacy returns Accepted → status timeline advances. Then In Process → Shipped events. SMS to patient on each Shipped.
6. **MA or Prescriber** can view the order anytime from Orders list, Patient Detail's Orders sublist, or the Refill Queue when the refill window approaches.
7. **Patient** receives SMS notifications per shipment.
8. **ClinicAdmin** sees the resulting `PharmacyOrder` line on their next ClinicInvoice (daily settlement).

The five steps that aren't real yet in V1 (stubs):
- ID.me real verification (1-second fake success now).
- Pharmacy adapter dispatch (the order lands in the DB but isn't transmitted out).
- Pharmacy webhook ingestion of Accepted / In Process / Shipped events (status timeline derives from model fields that aren't yet populated by external traffic).
- Twilio SMS dispatch.
- Smarty/Lob address verification (manual stamping now).

These are the five integration boundaries that need real plumbing before V1 launch. Everything else — the data model, every screen's workflow, all role gating, status derivation, and end-to-end navigation — is real.

---

## Demo presentation notes

When demoing to Jon Benson / Gameday, lead with these flows in order:

1. **MA drafts an order for an existing patient → Prescriber submits.** Hits OrderEntryPage's pharmacy-comparison panel and the Submit (ID.me) modal. Shows the "Recent sigs from this prescriber" prefill — a small delight.
2. **Refill Queue → one-click reorder.** Demonstrates that refill is two clicks (Reorder → Submit) and the prescription composer is intentionally read-only in refill mode.
3. **Ops Order Monitor → cancel a stuck order with a note.** Shows the Cancel-with-note flow is real, and Re-route is staged for V1.x.
4. **ClinicAdmin invites a new Prescriber.** Shows the activation email + DEA verification handoff to Ops via the DEA Queue.
5. **Mobile-responsive demo.** Resize the browser narrow and walk through the same flows — the layout reflows, the hamburger drawer opens, every screen still works.

**Explicitly call out as placeholders:**
- "ID.me will be wired before V1 launch — this is the modal it'll open."
- "Pharmacy adapter dispatch is the next integration; the UI is the end-state."
- "Address verification will use Smarty/Lob — same UX, real API."
- "Twilio SMS will fire on Shipped events — same template you see in the Order Detail mockup."
- "DEA Verification today is manual Ops review; V2 adds Verisys auto-lookup."

**Avoid demoing**:
- The Invoice Detail page's "Download receipt" button (placeholder toast).
- The Order Detail "Resend tracking SMS" button (placeholder, will trip questions about the missing notifications backend).
- The Pharmacy detail "Test connection" button (placeholder).
- The Profile "Re-enroll MFA" / "Recovery codes" buttons (placeholders).
- The Catalog "Bulk import" button (placeholder).
- Anything that requires logged-in admin-with-clinic-membership behavior — the dashboard race on first-render shows "No active clinic" briefly. Pre-navigate to a different page and back before showing the dashboard.
