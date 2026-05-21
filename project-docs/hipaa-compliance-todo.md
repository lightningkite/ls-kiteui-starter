# HIPAA compliance — TODO for V1 pilot launch

Audit of HeroScript against US HIPAA requirements (45 CFR § 160, § 164 — Privacy Rule, Security Rule, Breach Notification Rule). Identifies what is in place, what is partial, and what must land before V1 ships to pilot clinics with real patient data.

## Secondary audit annotations

**Reviewer:** secondary HIPAA auditor (Claude). **Reviewed:** the full body of this document plus the regulatory text at 45 CFR Part 164 — Subpart C (Security Rule §§ 164.302–318), Subpart D (Breach Notification §§ 164.400–414), Subpart E (Privacy Rule §§ 164.500–534), with eCFR / law.cornell.edu cross-checks. Each TODO carries a classification marker on the line immediately following it. The original text is preserved verbatim — these are annotations only.

**Marker key (exact wording used inline):**

- `[REQUIRED — § 164.xxx]` — explicitly required by a HIPAA rule, citation included.
- `[ADDRESSABLE — § 164.xxx]` — addressable implementation specification (covered entity / business associate must assess, then implement OR document why not and implement an equivalent alternative — not optional).
- `[NOT-REQUIRED — BEST PRACTICE]` — not HIPAA-mandated; aligned with NIST SP 800-66 Rev. 2, OCR guidance, or industry standard for HIPAA-regulated software. Worth doing.
- `[NOT-REQUIRED — NICE TO HAVE]` — not HIPAA-mandated, not standard best-practice for HIPAA posture. First auditor over-included.
- `[CONDITIONAL — depends on X]` — required only under stated condition.
- `[CITATION ERROR — see corrected reference]` — wrong section cited; corrected.

**Executive summary.** Of the 97 TODOs in the original document plus 4 added during this secondary audit (101 total), the breakdown is: **51 Required**, **26 Addressable** (still effectively must-do-or-document-alternative), **16 Best-practice** (not HIPAA-mandated but standard for HIPAA-regulated software), **1 Nice-to-have** (over-included by the first audit), and **7 Conditional** (required only under stated conditions, mostly per-vendor BAA scope). The original audit is broadly accurate on its citations and on the Required/Addressable categorization at the section headers. Over-statement is concentrated in items where the first audit named a standard correctly but baked in a specific numeric threshold (session timeouts, MFA tier-by-role, ≤4-hour termination SLA, 24-72h breach-notification SLAs); HIPAA names the standard but is silent on the threshold, so the threshold itself is best-practice. No HIPAA-required item was missed at the standard level; four discrete Required items were missing as TODOs and have been added (98–101).

**Top three corrections / disagreements with the first audit:**

1. **Specific numeric thresholds presented as required are best-practice, not law.** The first audit asserts session idle timeouts of 15 min (clinic) / 5 min (Ops), absolute lifetimes of 12 h / 8 h, 4-hour termination SLA, 90-day inactivity deactivation, lockout at 10 failed attempts / 15-min lockout, password ≥12 chars + breach-list check, and 24-72 h breach-notification BAA windows. None of these specific numbers appear in HIPAA. § 164.312(a)(2)(iii) (automatic logoff) is addressable and silent on duration; § 164.308(a)(3)(ii)(C) (termination procedures) is addressable and silent on SLA; § 164.410 caps breach notification at 60 days. The auditor's numbers are reasonable industry defaults and several align with NIST 800-66 commentary, but they are not the legal floor. Marked `[NOT-REQUIRED — BEST PRACTICE]` where the underlying standard is itself best-practice; left `[ADDRESSABLE]` on the standard.

2. **Hard-delete-as-HIPAA-violation argument (items 27–28, blocker #20) is partially wrong.** HIPAA's 6-year retention applies to *documentation of policies, procedures, and required actions/activities/assessments* under § 164.316(b)(1), and to certain audit-trail records under § 164.312(b)/§ 164.528. HIPAA does NOT impose a blanket "no hard delete of patient records" rule; the rule that does is *state medical-record retention law* (TN requires 10 years post-last-contact under Tenn. Comp. R. & Regs. 1050-02-.18 — see appendix). The first auditor's conclusion is operationally correct (don't hard-delete) but the source is state law plus § 164.530(j)(2) policy-documentation retention, not the Security Rule. Marker reflects this.

3. **The "two-person rule for role promotion" (item 10) and "break-glass dual-control + 24h ClinicAdmin notification" (item 4) are not HIPAA requirements.** Separation-of-duties and dual-control are NIST 800-53 controls that an organization may adopt to meet § 164.308(a)(4) (Information Access Management) and § 164.308(a)(1)(ii)(A) (Risk Analysis). HIPAA itself does not prescribe a two-person promotion rule or a 24-hour break-glass notification SLA. Strong recommendations, but classified `[NOT-REQUIRED — BEST PRACTICE]`.

**Items added to the document because the first audit missed them** (inserted in the relevant section, numbered 98–101):

- § 164.514(h) verification of identity of person requesting PHI before disclosure (new TODO 98 under Privacy Rule).
- § 164.316(b)(1)–(2) policy/procedure documentation retention (6 years from creation OR last-effective date, whichever is later) (new TODO 99 under Administrative Safeguards).
- § 164.530(j) — retention of all Privacy-Rule-related documentation for six years (Covered Entity rule; flows to BA via BAA) (new TODO 100 under Privacy Rule).
- § 164.504(e)(1)(ii) — BA must report any use/disclosure not provided for by the BAA, not only breaches (new TODO 101 under Breach Notification).

See the **Secondary audit appendix** at the bottom for the covered-entity/business-associate framing, the per-vendor BAA-reach analysis, and the state-law preemption notes.

---

HeroScript will be a Business Associate (BA) to every pilot clinic (each clinic is the Covered Entity). HeroScript will in turn pass PHI to downstream BAs (each pharmacy, Twilio, SendGrid, ID.me, Smarty/Lob, the payment processor, AWS). The compliance burden therefore covers (a) HeroScript's own BA obligations to each clinic and (b) HeroScript's flow-down obligations to its sub-BAs.

## Methodology

Reviewed:
- PRD § 03 (In scope > Compliance) and § 11 (Non-Functional > Compliance) — `project-docs/prd.txt`.
- Full data model — `shared/src/commonMain/kotlin/com/heroscript/models.kt`.
- Auth and proof flow — `server/src/main/kotlin/com/heroscript/UserAuth.kt`.
- Every endpoint permission file — `server/src/main/kotlin/com/heroscript/data/*Endpoints.kt`.
- Server wiring + settings — `server/src/main/kotlin/com/heroscript/Server.kt`, `settings.json`.
- UI surfaces where PHI appears — `project-docs/ui.md`, `project-docs/user-flows.md`.
- Known gaps + design decisions — `TODO.md`, `FEEDBACK.md`, `project-docs/transcript.md`, `project-docs/questions.md`.
- HIPAA discipline section of `.claude/CLAUDE.md`.

Not directly reviewable from the repo (called out in TODOs anyway):
- AWS account configuration (BAA status, KMS settings, S3 encryption, CloudTrail, network topology, IAM, VPC isolation).
- Existing organizational policies, BAAs already executed, workforce training records.
- Operational runbooks, incident-response playbooks, backup/restore procedures.

Scope of this list: code-level controls inside HeroScript, infrastructure controls at the AWS/vendor boundary, and the operational/policy controls a Business Associate is required to maintain. UI improvements that affect HIPAA posture (e.g. masking PHI on dashboards) are flagged; pixel-level UI work is the parallel UX audit's job.

## PHI inventory

Every field below either *is* PHI under 45 CFR § 160.103 (individually identifiable health information held by a covered entity or business associate) or, when combined with other fields in the same record, becomes PHI. This grounds the rest of the document — each TODO references back to these surfaces.

**Patient record (`Patient`) — PHI core:**
- `firstName`, `lastName`, `dateOfBirth`, `gender`, `phoneNumber`, `email` — direct identifiers (HIPAA Safe Harbor list).
- `shippingAddress` (`VerifiedAddress.address` — line1/line2/city/state/zip) — geographic identifier finer than state.
- `allergies`, `diseases`, `otherMedications` (each `List<ClinicalEntry>` with `description`, `code`, `reaction`, dates) — clinical PHI.
- `smsConsent`, `emailConsent` (timestamps) — administrative metadata, but reveal patient-clinic relationship.
- `clinic` (foreign key) — establishes patient-provider relationship, PHI by association.

**Prescription / order chain — PHI by relationship + content:**
- `Prescription.patient`, `Prescription.product`, `Prescription.form`, `Prescription.strength`, `Prescription.instructions` (freehand sig), `Prescription.prescribedBy` — clinical PHI; the sig field is the highest-PHI freehand area in the system.
- `PrescriptionOrder` — every denormalized snapshot field (`patient`, `product`, `form`, `strength`, `instructions`, `prescribedBy`, `destination`, `quantity`, `willLastDays`, `consentAffirmedAt`, `cancellation.reason`) is PHI.
- `PharmacyOrder.destination`, `PharmacyOrder.destinationIsClinic` — PHI when destination is patient address.
- `Shipment.carrier`, `Shipment.trackingNumber`, `Shipment.shippingUrl` — quasi-PHI: tracking number alone is not PHI, but linked to a `PrescriptionOrder` it identifies who got what medication where.

**Prescriber / user — partly PHI, partly operational:**
- `User.firstName`, `lastName`, `email`, `phoneNumber` — workforce identity, not patient PHI but governed by Security Rule workforce safeguards.
- `PrescriberLicensing.deaNumber`, `deaLicenseImage`, `stateLicenses[].licenseNumber`, `idMeSubjectId` — sensitive professional identifiers; DEA license images often contain home addresses. Treat as confidential.
- `User.role`, `mfaEnrolledAt`, `lastLoginAt` — administrative.

**Clinic / pharmacy / catalog — administrative metadata, not PHI:**
- `Clinic.*`, `Pharmacy.*` (except `credentialsSecretRef` value, which is a sensitive secret), `Product.*`, `ProductPharmacyMapping.*`, `ClinicInvoice.*` — but `ClinicInvoice` line items derive from `PharmacyOrder`, which is PHI; the invoice as displayed must not surface patient identity.
- `AppRelease`, `FcmToken` — administrative; `FcmToken.user` links a device to a user but FCM token itself is not PHI.

**Out-of-model surfaces that will carry PHI (per TODO.md § 1.6 / § 1.7):**
- Audit log records — actor, target entity ID, action, timestamp, source IP, payload hash. Target entity IDs are PHI by association. Payload hashes are not PHI.
- Notification records — recipient phone/email is PHI; the message body (per template) must contain no PHI other than first name and tracking link.
- Pharmacy API request/response payloads — the highest-volume PHI transmission in the system.

## Status legend

- ✅ In place
- 🟡 Partial / planned but not implemented
- ❌ Missing

---

## Security Rule — Technical Safeguards (§ 164.312)

### § 164.312(a) Access Control

**Status: 🟡 Partial.** Authentication, role-based access, and clinic-scoped read filtering are in place at the endpoint layer via `ModelPermissions` + the three auth caches (`UserAuth.kt`). Several gaps remain: no session idle/absolute timeout, no automatic logoff, no documented emergency access procedure, no field-level encryption beyond what the database/disk layer provides, no user inactivity deactivation.

Required vs addressable:
- Unique user identification (§ 164.312(a)(2)(i)): **required**.
- Emergency access procedure (§ 164.312(a)(2)(ii)): **required**.
- Automatic logoff (§ 164.312(a)(2)(iii)): **addressable** — must be implemented or alternative justified.
- Encryption and decryption (§ 164.312(a)(2)(iv)): **addressable** — at-rest encryption is the standard alternative.

TODOs:

1. **[code] Implement session idle timeout.** `UserAuth.SessionEndpoints.sessionStaleAfter` and `sessionExpiration` currently both return `null`. Set idle timeout to 15 minutes for clinic users, 5 minutes for Ops users (`UserRole >= Admin`). Absolute session lifetime: 12 hours clinic, 8 hours Ops. Persist last-activity timestamp on session and reject requests past idle limit.
   `[ADDRESSABLE — § 164.312(a)(2)(iii)]` Standard is "automatic logoff" — implementation is addressable. The specific numeric thresholds (15/5 min, 12/8 h) are best-practice, not in the regulation.
2. **[code] Implement client-side auto-logoff.** On idle expiry the SDK must clear local session state and redirect to login. Coordinate with the UX audit on the warning modal at T-60s.
   `[ADDRESSABLE — § 164.312(a)(2)(iii)]` Client-side enforcement of the same standard.
3. **[code] Implement automatic user deactivation after 90 days of inactivity** (`User.lastLoginAt`). Daily scheduled task sets `deactivatedAt`. Required for least-privilege.
   `[NOT-REQUIRED — BEST PRACTICE]` HIPAA does not mandate inactivity-based deactivation; it does require workforce access management under § 164.308(a)(3) (which is addressable). 90 days is an industry default, not a regulatory floor.
4. **[code] Add emergency access procedure: "break-glass" Ops account flow.** A documented procedure for `Root`-role HeroScript engineer to access a clinic's data during a P0 incident (e.g. database corruption, patient safety report). Every break-glass access must (a) require dual-control approval (two Ops people), (b) write a flagged audit record, (c) notify the affected Clinic Admin via email within 24 h.
   `[REQUIRED — § 164.312(a)(2)(ii)]` Emergency access procedure is a Required implementation specification — must exist. The dual-control + 24h-notification specifics are best-practice; HIPAA does not prescribe either.
5. **[infrastructure] Confirm at-rest encryption is on for every data store.** MongoDB Atlas encryption-at-rest (or equivalent for self-hosted), S3 `files` bucket with SSE-KMS using a customer-managed key, DynamoDB cache encryption (the project uses `DynamoDbCache`), application logs in CloudWatch with KMS encryption. Document the KMS key ARNs in an infrastructure inventory.
   `[ADDRESSABLE — § 164.312(a)(2)(iv)]` Encryption/decryption of ePHI is addressable; at-rest encryption is the conventional satisfying control and is also the safe-harbor for the Breach Notification Rule (encrypted PHI per HHS guidance is "not unsecured").
6. **[code] Lock down `ShipmentEndpoints` read permission.** Currently `Condition.Always` for all authenticated users (`ShipmentEndpoints.kt:25`). A Shipment linked to a `PrescriptionOrder` reveals which user/clinic/patient is receiving what. Restrict reads to system admin OR clinic members whose orders link to the shipment (subquery via `PrescriptionOrder.shipment`). At minimum, require an authenticated user with a `ClinicMembership`.
   `[REQUIRED — § 164.502(b) + § 164.312(a)(1)]` Minimum-necessary plus access-control standard. Current state violates both — any authenticated user reading any patient's shipment is an impermissible disclosure.
7. **[code] Audit `PharmacyOrderEndpoints` read scope.** Clinic members can currently read every `PharmacyOrder` for their clinic, including `accepted` price/total fields. Pricing is role-gated in the UI per PRD § 03 (MAs may not see pricing); enforce server-side either by serializer field-mask or by gating read of pricing fields on `ClinicRole == ClinicAdmin || ClinicRole == Prescriber`.
   `[REQUIRED — § 164.502(b) minimum necessary]` Role-scoped access falls under minimum-necessary.
8. **[code] Add MA-clinic permission helper** (TODO.md § 1.11) — `medicalAssistantClinicIds()` on `ClinicMembershipsCache`. Needed to gate pricing-bearing reads as in item 7.
   `[NOT-REQUIRED — BEST PRACTICE]` Implementation detail supporting item 7's minimum-necessary enforcement; the requirement is item 7, the helper is the means.
9. **[code] Add per-prescriber-only "my orders" read mode.** A Prescriber should not by default see another Prescriber's orders unless the clinic policy requires it. Either (a) default Order list to current-user-as-prescriber filter or (b) document that intra-clinic visibility is acceptable for V1. Resolve before pilot.
   `[NOT-REQUIRED — BEST PRACTICE]` Intra-clinic prescriber-to-prescriber visibility is a clinic-policy decision; HIPAA's minimum-necessary standard (§ 164.502(b)) gives the CE wide latitude on workforce-internal access.
10. **[code/policy] Enforce least-privilege on `User.role`.** Currently a Developer or Root can self-promote (no separation-of-duties). Add a "two-person rule" for promotion to Admin+: write to a `PendingRoleChange` table that requires a second Developer/Root to approve before the role actually changes. (`UserEndpoints.kt:55` allows `it.role.requires(admin) { it.inside(allowedRoles) }`.)
    `[NOT-REQUIRED — BEST PRACTICE]` Separation-of-duties is a NIST 800-53 control; HIPAA itself does not prescribe a two-person promotion rule. The underlying standard § 164.308(a)(4) (Information Access Management) is itself addressable.
11. **[code] Audit `Server.cache = "ram"` and `database = "ram"` defaults** in `settings.json`. These must never be used in production. Add a startup assertion that refuses to boot if `general.debug == false` AND any of (`cache`, `database`, `email`) is set to an in-memory/console implementation.
    `[NOT-REQUIRED — BEST PRACTICE]` Defensive configuration check. The underlying obligation (production ePHI must be on durable, properly-controlled stores) follows from §§ 164.308(a)(7) (contingency plan) and 164.312(a)(2)(iv) (encryption); the boot-time assertion is one means.
12. **[infrastructure] AWS IAM least-privilege.** Every Lambda/ECS task role should grant only the S3 prefixes, DynamoDB tables, KMS keys, Secrets Manager paths it needs. No `*:*`. Workforce IAM users must use SSO + MFA, not long-lived access keys.
    `[ADDRESSABLE — § 164.308(a)(4) + § 164.312(a)(1)]` Information-access-management standard at the infrastructure layer; least-privilege is the conventional satisfying control.

### § 164.312(b) Audit Controls

**Status: ❌ Missing.** Per PRD § F14, an immutable append-only audit log capturing every PHI access and every state-changing write is required with 6-year retention. `TODO.md` § 1.7 confirms the audit-log mechanism is not yet chosen and is "handled outside the canonical models." `ui.md` references an audit viewer that has no backing store. This is a **required** implementation specification with no addressable alternative.

TODOs:

13. **[code] Choose and implement the audit log mechanism.** Options: (a) Lightning Server's built-in audit hooks if available, (b) a dedicated `AuditEvent` model with `@AppendOnly` semantics enforced in the database layer, (c) ship-to-CloudWatch-Logs immutable log group with `RetentionInDays = 2192` (6 years) and KMS encryption. Recommendation: (b) for V1 (queryable from the UI per `ui.md`) plus (c) as an immutable backup.
    `[REQUIRED — § 164.312(b)]` Audit controls is a Required standard with no addressable sub-spec. Must implement "hardware, software, and/or procedural mechanisms that record and examine activity in information systems that contain or use ePHI."
14. **[code] Define the audit event schema.** Per PRD § F14 fields: `actor: User.ID`, `actorRole: UserRole`, `actorClinicRole: ClinicRole?`, `clinicContext: Clinic.ID?`, `action: AuditAction` (enum: ReadPatient / ReadPrescription / ReadOrder / WriteOrder / SubmitOrder / CancelOrder / UpdatePatient / VerifyDea / RoleChange / Login / Logout / IdleExpiry / Impersonate / Export / BreakGlass / ...), `targetType: String`, `targetId: String`, `timestamp: Instant`, `sourceIp: String`, `userAgent: String`, `requestId: Uuid`, `payloadHash: String`, `success: Boolean`, `failureReason: String?`. Pre-decide the enum — undefined actions are a leakage path.
    `[REQUIRED — § 164.312(b)]` Schema is the means; the standard requires the records. Granularity sufficient to support § 164.528 (Accounting of disclosures) is the legal floor.
15. **[code] Instrument every PHI read.** Lightning Server's request pipeline must record an audit event for every successful GET against `Patient`, `Prescription`, `PrescriptionOrder`, `PharmacyOrder`, `Shipment`, and the `PrescriberLicensing` embedded in `User`. Include list queries (record the filter, not the result set).
    `[REQUIRED — § 164.312(b) + § 164.528]` Both audit-controls and accounting-of-disclosures depend on read instrumentation.
16. **[code] Instrument every state-changing write.** All POST/PUT/PATCH/DELETE against the same models, plus `User` role changes, MFA enrollment changes, DEA verification decisions, ClinicMembership invites/role-changes/deactivations, ClinicInvoice creation/mark-paid, Pharmacy create/update.
    `[REQUIRED — § 164.312(b)]`
17. **[code] Instrument authentication events.** Successful login, failed login (do NOT include the password attempt), MFA challenge, MFA success/failure, password change, session creation, session refresh, logout, idle expiry, break-glass access.
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(C)]` Log-in monitoring is an addressable spec under Security Awareness and Training. Implementation typically via audit-log instrumentation; counted as addressable here.
18. **[code] Instrument administrative actions.** Pharmacy adapter dispatch (with payload hash, never the payload), webhook ingestion, refill-reminder send, SMS dispatch (with destination phone hash, never the body), invoice email send.
    `[REQUIRED — § 164.312(b)]` Disclosures to BAs are activity in ePHI systems.
19. **[code] Audit log integrity controls.** Each event must include the previous event's hash (chained hashes) or be written to an append-only WORM-style store. At minimum, no write/delete API on the audit table for any user; only the server's internal recorder writes. Document the cryptographic chain or the IAM policy enforcing append-only.
    `[ADDRESSABLE — § 164.312(c)(2)]` Mechanism to authenticate ePHI (here, the audit records themselves). Chained-hash / WORM is one satisfying alternative; HIPAA does not mandate the specific approach.
20. **[code] Audit log search/export for Ops.** Per `ui.md` and PRD § 04, an Ops UI that filters by actor/role/clinic/action/target/date-range, plus a raw CSV export. CSV export itself must be audit-logged (export of audit data is a high-value action) and rate-limited.
    `[REQUIRED — § 164.528]` Accounting-of-disclosures requires the ability to produce a query result per individual over a date range; some queryable form is required, even if the UI is not.
21. **[infrastructure] 6-year retention.** Whatever store backs the audit log must have a 6-year retention policy. If MongoDB, document the operational deletion rule and prove with a tested retention job. If CloudWatch Logs, set `RetentionInDays = 2192`. Backup the audit log to S3 with Object Lock in compliance mode for 6 years to guarantee tamper-evidence.
    `[REQUIRED — § 164.316(b)(2) + § 164.530(j)]` Six-year retention for documentation of policies, procedures, communications, and required actions/activities. Note also: TN state law requires 10 years for medical records — see appendix.
22. **[code] Audit log alerts.** Per PRD § 04 "audit-log access events flagged for review (anomaly indicators)." Define anomalies: (a) >100 patient reads/hour by one actor, (b) any cross-clinic read by a non-Ops user (impossible given the permission model — but if it ever fires, P0), (c) any failed-permission rate > N/min for any actor, (d) any break-glass access. Send to Ops on-call channel.
    `[REQUIRED — § 164.308(a)(1)(ii)(D)]` Information system activity review is a Required implementation specification. HIPAA requires review; "alerts vs daily-batch-review" is implementation choice.

### § 164.312(c) Integrity

**Status: 🟡 Partial.** Lightning Server's `updateRestrictions` + `cannotBeModified()` (used pervasively in endpoint files) prevent improper modification of immutable post-create fields (createdAt, createdBy, prescription denormalized snapshots, etc.). Integrity authentication (the addressable spec for proving PHI hasn't been altered) is partly addressed by the audit log payload hash but not yet end-to-end.

Required vs addressable:
- Mechanism to authenticate ePHI (§ 164.312(c)(2)): **addressable**.

TODOs:

23. **[code] Server-side validation of state transitions on `PrescriptionOrder`.** UI hides editing once `clinicianReview` is set (per `PrescriptionOrderEndpoints.kt:53-54` comment) but Lightning's `updateRestrictions` can't express "depends on existing row state." Add explicit pre-update hooks that reject writes to `prescription`, `pharmacy`, `destination`, `instructions`, `strength`, `quantity` when `clinicianReview != null`. Without this, a clinic user could modify a submitted order before the pharmacy fulfills.
    `[REQUIRED — § 164.312(c)(1)]` Integrity standard: "Protect ePHI from improper alteration or destruction." Allowing post-submission tampering with a clinician-reviewed prescription violates the standard directly.
24. **[code] Cryptographic integrity for pharmacy webhook payloads.** Per PRD § F5 + TODO.md § 1.1, webhook auth via per-pharmacy shared secret. Sign every inbound webhook with HMAC-SHA256, verify before processing. Reject unsigned/mis-signed payloads. Audit-log the verification result.
    `[ADDRESSABLE — § 164.312(e)(2)(i)]` Integrity controls in transmission. HMAC is one satisfying mechanism; mutual TLS another. Also implicates § 164.312(d) (Person/entity authentication of the pharmacy as a system principal) — Required.
25. **[code] Payload hash on every pharmacy adapter dispatch.** Store SHA-256 of the outbound payload on `PrescriptionOrder.Fulfillment` (or in audit log keyed by `Fulfillment.by`). Per PRD § F14 — also the basis for non-repudiation if a pharmacy disputes what it received.
    `[ADDRESSABLE — § 164.312(c)(2)]` Mechanism to authenticate ePHI / integrity of disclosed data.
26. **[code] Database backup integrity.** Each backup must include a manifest checksum. Restore drill must verify checksum before treating the restore as valid. Tied to contingency plan (§ 164.308(a)(7)).
    `[REQUIRED — § 164.308(a)(7)(ii)(A)]` Data backup plan is Required. Integrity checks support § 164.308(a)(7)(ii)(D) (testing and revision — addressable).
27. **[code] No bulk delete of PHI via the API.** `PatientEndpoints.delete = systemAdmin or inMyAdminClinic` — a malicious or compromised ClinicAdmin could mass-delete every patient. Either rate-limit delete (max N/day), require soft-delete (add `deactivatedAt` and prevent hard delete), or remove DELETE entirely and replace with `deactivatedAt` writes. **Recommend soft-delete; HIPAA's 6-year retention plus medical-record retention statutes both prohibit hard delete during the retention window.**
    `[CONDITIONAL — depends on state law + § 164.316(b)/§ 164.530(j)]` HIPAA does not blanket-prohibit hard delete of patient records; the 6-year retention in § 164.316(b)/§ 164.530(j) applies to *Privacy/Security Rule documentation* and to audit records under § 164.528. The "no hard delete of patient data" rule comes from state medical-record retention statutes (TN: 10 years post last contact, Tenn. Comp. R. & Regs. 1050-02-.18). Operationally correct, but the legal basis the first auditor cited (HIPAA 6-year retention) is overstated.
28. **[code] No hard delete for `Prescription`, `PrescriptionOrder`, `Shipment`, `PharmacyOrder`, `ClinicInvoice`, `User`** for the same reason. Replace DELETE with `deactivatedAt`/`cancelledAt` semantics. Audit any retained DELETE endpoints.
    `[CONDITIONAL — depends on state law]` Same analysis as item 27. State-law-driven, not HIPAA-driven for patient records. For audit-log records and Privacy/Security policy documentation, HIPAA 6-year retention is the floor.

### § 164.312(d) Person or Entity Authentication

**Status: 🟡 Partial.** Multiple proof types are wired (`UserAuth.kt`): email PIN, password, TOTP, backup codes. `requiredProofStrengthFor` returns 20 (strong) when >1 non-backup proof is established, else 10 (weak). MFA enrollment is not enforced at first login. Account lockout/anti-bruteforce is partly in place via `constrainAttemptRate` but only on the email-pin send path. Password complexity policy is undocumented.

Required: Authentication of person/entity (§ 164.312(d)). MFA is the de-facto standard for HIPAA at the workforce-access layer.

TODOs:

29. **[code] Force MFA enrollment at first login.** Per PRD § 03 — "Email + password authentication with MFA for all clinic users." Currently `mfaEnrolledAt` is nullable and never asserted. Add a server-side guard: if `requiredProofStrengthFor(user) < 20` after first sign-in, redirect to the MFA-enrollment flow before any PHI endpoint is reachable. The "I'll set up MFA later" branch in `user-flows.md` § "First-time activation" must be removed for production.
    `[REQUIRED — § 164.312(d)]` Standard is "person or entity authentication." MFA is not literally in the regulation, but OCR guidance and the 2024 NPRM treat MFA as the de-facto means for compliant authentication of PHI access. Also: PRD § 03 contractually commits HeroScript to MFA — the contractual commitment can itself become a HIPAA issue if not delivered (failure to maintain promised safeguards is a § 164.530(c)(1) gap).
30. **[code] Stronger MFA for Ops** (PRD § 11). `Admin+` users must have at least TOTP-based MFA (not email-PIN-only). Add a check in `requiredProofStrengthFor` or a separate session validator.
    `[NOT-REQUIRED — BEST PRACTICE]` Distinction between email-PIN MFA and TOTP MFA for Ops is industry practice, not HIPAA text. Underlying § 164.312(d) is Required.
31. **[code] Password policy enforcement.** Minimum 12 chars, breach-list check (e.g. `haveibeenpwned` API or local k-anonymity list), no reuse of last 5. Lightning Server's password proof may have this; verify and document. Without breach-list check, password-stuffing attacks routinely succeed against medical systems.
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(D)]` Password management is an addressable spec under Security Awareness and Training. Specific thresholds (12 chars, last-5, breach-list) align with NIST SP 800-63B, not the HIPAA text.
32. **[code] Account lockout / brute-force protection across all proof types.** `constrainAttemptRate` is applied to email-PIN sending; verify equivalent protection on password verification, TOTP verification, and backup-code redemption. Target: 10 failed attempts → 15-minute lockout, with exponential backoff. Audit-log every lockout.
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(C)]` Log-in monitoring is addressable; lockout is the conventional protective control.
33. **[code] Re-authentication for sensitive actions.** Submitting a controlled-substance order requires ID.me step-up (per PRD § F4) which is a separate flow, but other sensitive actions (changing email, changing password, adding/replacing MFA, role promotion in Ops, viewing/exporting audit log, exporting patient records) should require re-entering a proof. Define the list and implement.
    `[NOT-REQUIRED — BEST PRACTICE]` Step-up authentication is industry standard but not specified in HIPAA. EPCS (electronic prescribing of controlled substances) re-authentication is a DEA requirement under 21 CFR § 1311, not HIPAA — out of scope for this audit but a separate hard requirement.
34. **[code] Remove or strongly gate the `AppStoreTester` user backdoor.** `UserEndpoints.kt:74-98` initializes a known-credentials user with a hardcoded password (`r[cFRPyBkRqY`) and `requiredProofStrengthFor` returns 10 (single-factor) for that user (`UserAuth.kt:230`). This is a HIPAA-relevant credential. For V1 with real PHI: gate the entire `initAppStoreTester` startup task on a `general.appStoreTesterEnabled` setting that defaults to `false` in production, and audit-log every login by that ID.
    `[REQUIRED — § 164.308(a)(5)(ii)(D) + § 164.312(a)(2)(i)]` Unique-user-identification (Required) and password-management (Addressable). A shared credential reachable by anyone reading the source is a direct violation of unique-user-identification when the account is on a production system with ePHI.
35. **[code] Remove `appstoretester@lightningkite.com` from any production deployment configuration.** Hardcoded password literals in source code violate § 164.308(a)(5)(ii)(D) password management practices regardless of environment.
    `[REQUIRED — § 164.308(a)(5)(ii)(D)]` Cited correctly by first auditor; addressable spec, but treating hardcoded production credentials as out of bounds is the safe-harbor implementation.
36. **[code] Replace the hardcoded root-user bootstrap** (`UserEndpoints.kt:115` — `root("josephivie@gmail.com")`). In production this should come from settings, not be a code constant. Audit-log every root user creation. Restrict to first-boot only (the `startupOnce` mechanism already does this — confirm).
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(D)]` Password / credential management — similar argument as item 35. Stronger if the hardcoded value is itself a credential; weaker if it is only a username and the password is configured elsewhere.

### § 164.312(e) Transmission Security

**Status: 🟡 Partial.** TLS termination is the deployer's responsibility; the Lightning Server itself does HTTP. Pharmacy webhooks signed with shared secrets are listed in `TODO.md` but not implemented.

Required vs addressable:
- Integrity controls (§ 164.312(e)(2)(i)): **addressable**.
- Encryption (§ 164.312(e)(2)(ii)): **addressable** — standard alternative is TLS 1.2+.

TODOs:

37. **[infrastructure] Enforce TLS 1.2+ on every public endpoint.** ALB / API Gateway / CloudFront SSL policy = `ELBSecurityPolicy-TLS13-1-2-2021-06` or equivalent. Disable TLS 1.0/1.1. HSTS header with `max-age >= 31536000; includeSubDomains; preload`. Test with `ssllabs.com` — target A+ rating.
    `[ADDRESSABLE — § 164.312(e)(2)(ii)]` Encryption-in-transit is addressable; TLS 1.2+ is the conventional satisfying control and also satisfies the Breach-Notification safe-harbor (encrypted-in-transit PHI per HHS guidance is not "unsecured").
38. **[infrastructure] TLS for every outbound integration.** Each pharmacy adapter, Twilio, SendGrid, Smarty/Lob, ID.me, payment processor must use TLS. Pin certificates for the pharmacy adapters if their docs support it (defense against MITM at AWS networking boundaries).
    `[ADDRESSABLE — § 164.312(e)(2)(ii)]` Cert-pinning is best-practice, not HIPAA text.
39. **[code] HMAC-signed webhooks both directions.** Inbound: every pharmacy webhook verified per item 24. Outbound: webhooks we send (settlement, etc.) signed and documented.
    `[ADDRESSABLE — § 164.312(e)(2)(i) + § 164.312(d)]` Integrity controls + person/entity authentication.
40. **[infrastructure] Database connection TLS.** MongoDB Atlas: SRV + TLS required, `tlsAllowInvalidCertificates = false`. Same for any RDS, ElastiCache, DynamoDB endpoints (DynamoDB defaults to TLS — verify the SDK isn't downgrading).
    `[ADDRESSABLE — § 164.312(e)(2)(ii)]` Encryption-in-transit applied to the database hop.
41. **[code] CORS lockdown** (`Server.kt:51`). `CorsSettings()` defaults are not loaded from `settings.json` `cors` block with any specific allowlist (current `settings.json` shows empty arrays). Production must whitelist exact domains: clinic web app domain only. `allowCredentials = true` if cookies are used; `forbidOnMatchFail = true` is correctly defaulted.
    `[NOT-REQUIRED — BEST PRACTICE]` Defense-in-depth against CSRF/cross-origin script abuse. The underlying access-control standard (§ 164.312(a)(1)) is Required, but CORS is one of many means.
42. **[code] HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy headers** on every response. Lightning Server may have built-in support; verify and configure. Important for the web client because PHI rendered in the DOM should not be exposable via frame embedding.
    `[NOT-REQUIRED — BEST PRACTICE]` Standard browser-security hardening. Not in HIPAA text.
43. **[code] No PHI in URLs.** Audit every endpoint path — none should accept PHI as a path component (patient ID is OK; patient name is not). Confirm no endpoint takes `email` or `phoneNumber` as a query string. Same for query parameters logged in nginx/ALB access logs.
    `[REQUIRED — § 164.312(b) + § 164.502(b)]` PHI in URLs lands in access logs (which themselves are not designed as PHI-aware audit stores) — this is an impermissible disclosure to the logging subsystem and a minimum-necessary violation.

---

## Security Rule — Administrative Safeguards (§ 164.308)

### § 164.308(a)(1) Security Management Process

**Status: ❌ Missing.** No risk analysis on file. No sanction policy. No documented information-system-activity review cadence.

Required:
- Risk analysis (§ 164.308(a)(1)(ii)(A)): **required**.
- Risk management (§ 164.308(a)(1)(ii)(B)): **required**.
- Sanction policy (§ 164.308(a)(1)(ii)(C)): **required**.
- Information system activity review (§ 164.308(a)(1)(ii)(D)): **required**.

TODOs:

44. **[policy] Conduct and document a HIPAA Security Rule risk analysis.** Use NIST SP 800-66 Rev. 2 methodology. Inventory PHI assets (the PHI inventory above is a starting point), identify threats and vulnerabilities for each, assess likelihood and impact, document mitigations. Re-run annually or after any major architectural change. **Must exist on paper before V1.**
    `[REQUIRED — § 164.308(a)(1)(ii)(A)]` Risk analysis is the foundational Required spec. Failure to perform is the single most-cited finding in OCR enforcement actions.
45. **[policy] Document risk-management strategy.** For each risk in the analysis, document the chosen control and its rationale (especially for "addressable" specs where an alternative is chosen). This is the document an OCR auditor asks for first.
    `[REQUIRED — § 164.308(a)(1)(ii)(B)]`
46. **[policy] Workforce sanction policy.** Written policy describing consequences for workforce members who violate HIPAA policies (verbal warning → written warning → termination → reporting to relevant boards). Must apply to HeroScript employees, contractors, and Lightning Kite engineers with PHI access.
    `[REQUIRED — § 164.308(a)(1)(ii)(C)]`
47. **[policy] Information system activity review procedure.** Monthly review of: audit log anomalies (per item 22), access-pattern reports, failed-login spikes, off-hours admin actions, list of all users with `Admin+` role, list of all break-glass accesses. Document who reviews, when, and what is escalated. Assign to the Security Officer (next section).
    `[REQUIRED — § 164.308(a)(1)(ii)(D)]` Monthly cadence is best-practice; HIPAA names the standard not the frequency.

### § 164.308(a)(2) Assigned Security Responsibility

**Status: ❌ Missing.**

Required.

TODOs:

48. **[policy] Designate a HIPAA Security Officer.** Named individual on staff at HeroScript responsible for the security rule compliance program. Update org chart and BAAs to reflect. Same person may also be the Privacy Officer.
    `[REQUIRED — § 164.308(a)(2)]` Standard is "Assigned security responsibility" — must identify the security official.
49. **[policy] Designate a HIPAA Privacy Officer.** Required by Privacy Rule § 164.530(a). Often the same person as the Security Officer at small organizations.
    `[REQUIRED — § 164.530(a)(1)]` Note: § 164.530 applies to Covered Entities directly; § 164.504(e)(2)(ii)(I) flows the obligation to BAs via BAA. HeroScript-as-BA must designate the Security Officer (§ 164.308(a)(2)); a separate Privacy Officer is a CE-rule requirement but standard practice for BAs at clinical scale.

### § 164.308(a)(3) Workforce Security

**Status: 🟡 Partial.** Role-based access is technically enforced. Authorization-on-hire, clearance, and termination procedures are not documented.

Required vs addressable:
- Authorization and/or supervision (§ 164.308(a)(3)(ii)(A)): **addressable**.
- Workforce clearance procedure (§ 164.308(a)(3)(ii)(B)): **addressable**.
- Termination procedures (§ 164.308(a)(3)(ii)(C)): **addressable**.

TODOs:

50. **[policy] Workforce authorization procedure.** Documented process for granting PHI access to new HeroScript employees / Lightning Kite engineers: hiring manager submits access request → Security Officer approves → IT provisions → access reviewed quarterly. Tie to the role-promotion two-person rule (item 10).
    `[ADDRESSABLE — § 164.308(a)(3)(ii)(A)]` Authorization and/or supervision.
51. **[policy] Background checks for workforce with PHI access.** Standard practice for HIPAA — criminal background check at minimum for anyone whose role grants production PHI access (Ops, on-call engineers).
    `[ADDRESSABLE — § 164.308(a)(3)(ii)(B)]` Workforce clearance procedure. HIPAA does NOT specify "criminal background check" as the means; the clearance procedure is for the CE/BA to design.
52. **[policy] Termination procedure / access removal SLA.** Within 4 hours of termination notice: revoke SSO, deactivate `User` record (set `deactivatedAt`), revoke any active sessions (force session invalidation, not just expiry), rotate any shared secrets the person had access to, retain audit log of their prior actions for 6 years. Tabletop-test the procedure quarterly.
    `[ADDRESSABLE — § 164.308(a)(3)(ii)(C)]` Termination procedures. The 4-hour SLA is best-practice, not in HIPAA text.

### § 164.308(a)(4) Information Access Management

**Status: 🟡 Partial.** Access establishment and modification happen technically (ClinicMembership invites, role changes) but no documented isolation between the BA's functions and any "non-health" use of PHI.

Required vs addressable:
- Isolating healthcare clearinghouse functions (§ 164.308(a)(4)(ii)(A)): N/A — HeroScript is not a clearinghouse.
- Access authorization (§ 164.308(a)(4)(ii)(B)): **addressable**.
- Access establishment and modification (§ 164.308(a)(4)(ii)(C)): **addressable**.

TODOs:

53. **[policy] Document the role-to-permission matrix.** Spreadsheet or doc that maps every `UserRole × ClinicRole` combination to the data they can read/write. Used during audits and during onboarding. The matrix lives implicitly in the endpoint files today — externalize it.
    `[ADDRESSABLE — § 164.308(a)(4)(ii)(B)]` Access authorization documentation.
54. **[policy] Access review cadence.** Quarterly Ops-side review: list every user with `UserRole >= Admin`, every ClinicAdmin per clinic, every active prescriber, confirm each is still authorized. Document the review and any access revocations.
    `[ADDRESSABLE — § 164.308(a)(4)(ii)(C)]` Access establishment and modification. Quarterly cadence is best-practice; HIPAA text is silent on cadence.
55. **[code] Force re-acceptance of new BAA versions.** When a clinic's BAA is updated, the ClinicAdmin must re-accept before any further PHI access. Add a `Clinic.baaVersion` and a `User.baaAcceptedVersion` (or per-clinic equivalent on `ClinicMembership`); block PHI endpoints when out of date.
    `[NOT-REQUIRED — NICE TO HAVE]` BAA execution is a paper / contract process. In-app re-acceptance is a thoughtful operational control but is not a HIPAA requirement and creates a friction surface (e.g. a real PHI need during BAA renegotiation is awkward to block). Recommend out-of-band BAA tracking unless the clinic asks for in-app gating.

### § 164.308(a)(5) Security Awareness and Training

**Status: ❌ Missing.**

Required vs addressable:
- Security reminders (§ 164.308(a)(5)(ii)(A)): **addressable**.
- Protection from malicious software (§ 164.308(a)(5)(ii)(B)): **addressable**.
- Log-in monitoring (§ 164.308(a)(5)(ii)(C)): **addressable**.
- Password management (§ 164.308(a)(5)(ii)(D)): **addressable**.

TODOs:

56. **[policy] HIPAA training program for HeroScript workforce.** All employees and contractors with PHI access complete HIPAA training within 30 days of hire and annually thereafter. Document completion. Cover: HIPAA overview, HeroScript-specific PHI surfaces, phishing recognition, secure development practices, incident reporting.
    `[REQUIRED — § 164.308(a)(5)(i)]` Standard ("Security awareness and training") is Required; the four sub-specs (reminders, malware, log-in monitoring, password management) are addressable. Most BAs implement an annual training program as the canonical satisfying control.
57. **[policy] HIPAA training material for pilot clinic users.** Brief in-app / PDF orientation covering: don't share login credentials, MFA importance, log out when stepping away, don't paste patient data into external tools (Slack/email/AI). Optional but materially reduces social-engineering risk.
    `[NOT-REQUIRED — BEST PRACTICE]` Training clinic users (CE workforce) is the clinic's obligation, not HeroScript's. Providing material is a value-add, not a HIPAA requirement on the BA.
58. **[code] Login monitoring alerts.** Surface failed-login bursts (item 32) and successful logins from new IPs/geographies to the user via email ("New sign-in from <city> on <device>. If this wasn't you, [link to lock account]"). Reduces unauthorized-access dwell time.
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(C)]` Log-in monitoring.
59. **[infrastructure] Endpoint protection on workforce laptops.** Standard EDR (e.g. CrowdStrike, SentinelOne) on every device with production access. MDM-enforced disk encryption, screen lock, no USB mass storage. Document.
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(B)]` Protection from malicious software (addressable); standard satisfying control is EDR on workforce devices. Disk encryption / screen lock are also implicated by § 164.310(d) (Device and Media Controls — addressable).

### § 164.308(a)(6) Security Incident Procedures

**Status: ❌ Missing.**

Required: Response and reporting (§ 164.308(a)(6)(ii)).

TODOs:

60. **[policy] Incident response plan.** Written procedure covering: detection (alerting paths), triage (severity scoring), containment, eradication, recovery, post-incident review. Roles: on-call engineer, Security Officer, Privacy Officer, legal counsel, executive sponsor. Communication templates for affected clinics. Tested via tabletop exercise once before V1 launch.
    `[REQUIRED — § 164.308(a)(6)(ii)]` Response and reporting is the Required spec under Security Incident Procedures.
61. **[policy] Incident notification SLAs to clinics.** Per BAA, HeroScript notifies the clinic (the Covered Entity) of any suspected breach within the contractually agreed window (HIPAA default: "without unreasonable delay" and no later than 60 days; most BAAs tighten to 24-72 hours). Pre-draft notification templates.
    `[REQUIRED — § 164.410]` Business-associate breach notification to the CE without unreasonable delay, no later than 60 calendar days from discovery. 24-72h is the typical BAA-tightened SLA; HIPAA's outer limit is 60 days.
62. **[code] Anomaly alerts (per item 22) feed the incident response process.** Document who receives each alert and the expected response time.
    `[REQUIRED — § 164.308(a)(1)(ii)(D) + § 164.308(a)(6)(ii)]` Information system activity review feeding incident response — both Required.

### § 164.308(a)(7) Contingency Plan

**Status: 🟡 Partial.** Backup is the deployer's responsibility; no documented restore drill or BCP/DR plan exists in repo.

Required vs addressable:
- Data backup plan (§ 164.308(a)(7)(ii)(A)): **required**.
- Disaster recovery plan (§ 164.308(a)(7)(ii)(B)): **required**.
- Emergency mode operation plan (§ 164.308(a)(7)(ii)(C)): **required**.
- Testing and revision procedures (§ 164.308(a)(7)(ii)(D)): **addressable**.
- Applications and data criticality analysis (§ 164.308(a)(7)(ii)(E)): **addressable**.

TODOs:

63. **[infrastructure] Daily encrypted backups of the database and `files` bucket.** Document RPO (recovery point objective) — target 24h for V1. Backups stored in a separate AWS account or region. Retain backups for 6 years (matches PHI retention).
    `[REQUIRED — § 164.308(a)(7)(ii)(A)]` Data backup plan. Specific RPO and retention numbers are best-practice; the standard requires backups exist.
64. **[infrastructure] Documented restore runbook with quarterly drill.** Tabletop or actual restore from backup to a staging environment. Verify data integrity after restore (item 26). Document drill results.
    `[ADDRESSABLE — § 164.308(a)(7)(ii)(D)]` Testing and revision procedures. Quarterly is best-practice; HIPAA-text is silent on cadence.
65. **[policy] Disaster recovery plan.** Written: RTO (target 4 hours for V1), RPO, recovery sequence, dependencies (DNS, secrets, runtime). Tested once before V1.
    `[REQUIRED — § 164.308(a)(7)(ii)(B)]` Disaster recovery plan. Specific RTO is best-practice.
66. **[policy] Emergency mode operation plan.** What HeroScript does if the platform is unavailable and clinics need to place orders manually. Recommended: pre-staged template emails to pharmacies the clinic can use, plus a documented manual order-tracking spreadsheet template. Communicated to pilot clinics.
    `[REQUIRED — § 164.308(a)(7)(ii)(C)]` Emergency mode operation plan.
67. **[policy] Applications and data criticality analysis.** Brief doc ranking each system by criticality: Database (Critical — 4h RTO), Pharmacy adapters (Critical — outage = no orders), SMS dispatch (High — degrade gracefully), Audit log (Critical — never lose), Notification UI (Medium), AppRelease (Low). Drives the DR runbook priorities.
    `[ADDRESSABLE — § 164.308(a)(7)(ii)(E)]`

### § 164.308(a)(8) Evaluation

**Status: ❌ Missing.**

Required.

TODOs:

68. **[policy] Annual HIPAA security evaluation.** Independent review (third-party assessor or internal Security Officer with documented methodology) confirming the implemented safeguards are operating. The PRD mentions annual pen test and quarterly internal review post-pilot (§ 11); this satisfies the technical portion. Add a non-technical portion that reviews policies, training records, and access reviews.
    `[REQUIRED — § 164.308(a)(8)]` Standard is Required; cadence is not text-specified but "periodic" is named — annual is the conventional reading.
69. **[policy] Penetration test before V1 launch.** PRD § 11 schedules annual; recommend the first one is *before* pilot launch, not after. Scope: web app, pharmacy webhook endpoints, authentication flows, file upload. Engage a qualified firm with healthcare experience.
    `[NOT-REQUIRED — BEST PRACTICE]` Penetration testing is one means of satisfying § 164.308(a)(8) Evaluation but is not itself named. Strongly industry-standard for HIPAA-regulated SaaS.
70. **[code] Continuous dependency vulnerability scanning.** GitHub Dependabot or equivalent for Kotlin/Gradle. Fail CI on Critical CVEs. Manual SCA review monthly.
    `[ADDRESSABLE — § 164.308(a)(5)(ii)(B)]` Protection from malicious software. Dependency scanning is a defensible satisfying control.

### § 164.308(b) Business Associate Contracts

**Status: ❌ Missing — pre-launch blocker for every PHI integration.**

Required.

TODOs (each one is a launch-blocking dependency for the corresponding integration):

71. **[policy] BAA with AWS** — must be on file before any PHI is stored. Free, in AWS Artifact. Sign before first deployment.
    `[REQUIRED — § 164.308(b)(1) + § 164.502(e)]` Contract with any BA (here, AWS as sub-BA to HeroScript) before disclosure of PHI.
72. **[policy] BAA with each pilot clinic** — HeroScript is the BA, clinic is the Covered Entity. Pre-built template. Signed before clinic's PHI lands in production. Pilot acceptance criteria (10 clinics live per PRD § 12) is impossible without 10 BAAs.
    `[REQUIRED — § 164.504(e)]` BAA between CE (clinic) and BA (HeroScript). The CE's obligation, but HeroScript cannot accept PHI without one.
73. **[policy] BAA with each pharmacy integration partner** (target ≥6 at launch). LifeFile, Empower, and proprietary pharmacy operators are all sub-BAs receiving PHI from HeroScript.
    `[CONDITIONAL — depends on whether the pharmacy is itself a CE]` If pharmacy is dispensing under its own DEA/state license as a healthcare provider, it is itself a CE and HeroScript-to-pharmacy is a CE-to-CE disclosure for treatment under § 164.506(c)(2), which does NOT require a BAA. If pharmacy is acting as a BA of the prescribing clinic (e.g. handling PHI on the clinic's behalf), then `[REQUIRED — § 164.308(b)(2) sub-BA via HITECH 13408]`. Verify per-pharmacy. The conservative position is a BAA either way.
74. **[policy] BAA with Twilio** (HIPAA tier, not standard). Must be on the HIPAA-eligible Twilio plan; standard Twilio does not include a BAA.
    `[REQUIRED — § 164.308(b)(2)]` Sub-BA agreement (HITECH § 13408 flow-down).
75. **[policy] BAA with SendGrid** if email notifications ship in V1. Same — must be HIPAA tier.
    `[REQUIRED — § 164.308(b)(2)]` Same as Twilio.
76. **[policy] BAA with ID.me** (or whichever identity verifier is chosen — see TODO.md § 1.9). Verify the chosen provider offers a BAA. Stripe Identity does; ID.me does for some configurations; Persona does.
    `[CONDITIONAL — depends on whether prescriber identity data sent is PHI]` Prescriber identity verification with no patient PHI in the payload is a workforce-credentialing transaction, not a PHI disclosure — no BAA required for that scope. If the verifier receives any patient-linked data, `[REQUIRED — § 164.308(b)(2)]`.
77. **[policy] BAA with Smarty / Lob / USPS** (address verifier). Smarty does not offer a BAA on standard plans — confirm before integrating, or strip the address to non-PHI granularity before sending (line1/city/state/zip with no patient name).
    `[CONDITIONAL — depends on whether name + address is sent]` Address alone with no name and not linked to a patient identifier in the request is not PHI (per § 164.514 Safe Harbor de-identification standards, address-only is not individually identifiable). If the request includes patient name or links the verification to a patient record on Smarty's side, `[REQUIRED — § 164.308(b)(2)]`. The "strip to non-PHI" path the first auditor proposes is the correct compliance-by-design move.
78. **[policy] BAA with payment processor** (Stripe / Priority Payments — see TODO.md § 1.10). Card PAN and ACH routing are not PHI per se, but the link `(clinic, charge, order set)` is. Most processors offer a BAA on enterprise tier.
    `[CONDITIONAL — depends on processor's exposure to PHI]` If charge descriptors and metadata are scrubbed of patient identifiers and per-prescription detail, payment data alone is not PHI. If processor receives any per-patient or per-prescription detail, `[REQUIRED — § 164.308(b)(2)]`. Most processors will negotiate a BAA for healthcare clients.
79. **[policy] BAA with MongoDB Atlas** (if used) — Atlas offers HIPAA-eligible clusters under BAA.
    `[REQUIRED — § 164.308(b)(2)]` Sub-BA holding PHI.
80. **[policy] Sub-BA inventory and tracking spreadsheet.** Single source of truth for every sub-BA, the BAA's effective date, the next renewal, the contact at the vendor. Owned by the Security Officer.
    `[NOT-REQUIRED — BEST PRACTICE]` A specific spreadsheet is not in HIPAA; § 164.316(b) requires documentation of BAAs, which the executed BAAs themselves satisfy. The tracking spreadsheet is the operational means.

---

## Security Rule — Physical Safeguards (§ 164.310)

### § 164.310(a) Facility Access Controls

**Status: ✅ In place (delegated to AWS).** Since HeroScript runs entirely in AWS HIPAA-eligible services, AWS's physical controls satisfy this. No on-prem servers. Reaffirm in the risk analysis (item 44).

### § 164.310(b)(c) Workstation Use & Security

**Status: 🟡 Partial.** No documented policy on workstations used to access production PHI.

TODOs:

81. **[policy] Workstation policy.** Workforce devices accessing production PHI must: be company-managed (MDM), full-disk encrypted, screen-lock at ≤5 min idle, run EDR (item 59), and be reported lost/stolen within 4 hours. Document.
    `[REQUIRED — § 164.310(b) + § 164.310(c)]` Workstation use and workstation security standards are both Required (no addressable sub-spec). Specific thresholds (5 min, 4 hours) are best-practice.

### § 164.310(d) Device and Media Controls

**Status: 🟡 Partial.** Cloud-only deployment limits exposure; only the workforce-laptop case applies.

Required vs addressable:
- Disposal (§ 164.310(d)(2)(i)): **required**.
- Media re-use (§ 164.310(d)(2)(ii)): **required**.
- Accountability (§ 164.310(d)(2)(iii)): **addressable**.
- Data backup and storage (§ 164.310(d)(2)(iv)): **addressable**.

TODOs:

82. **[policy] Device disposal procedure.** When a workforce device is retired or returned, run the vendor's secure-erase (FileVault FDE makes this fast — destroy the encryption key). Document each disposal. AWS handles EBS volume disposal under their BAA.
    `[REQUIRED — § 164.310(d)(2)(i) + § 164.310(d)(2)(ii)]` Disposal and media re-use are both Required implementation specifications.
83. **[policy] Inventory of devices with PHI access.** Asset registry: each laptop, each workforce member, each role. Reconciled quarterly.
    `[ADDRESSABLE — § 164.310(d)(2)(iii)]` Accountability (track movement of hardware/media).

---

## Privacy Rule (§ 164.500-534)

### § 164.520 Notice of Privacy Practices

**Status: ❌ Not applicable to HeroScript directly** as Business Associate (Notice is the Covered Entity's obligation), **but** HeroScript should require pilot clinics to confirm their NPP covers HeroScript's processing.

TODOs:

84. **[policy] Clinic onboarding checklist includes NPP confirmation.** Before a clinic goes live, verify its Notice of Privacy Practices includes language permitting electronic transmission to pharmacies via a Business Associate (most do). Optional: provide model NPP language to clinics. Documented in the onboarding workflow.
    `[NOT-REQUIRED — BEST PRACTICE]` § 164.520 NPP is the CE's obligation. A BA has no NPP duty. Pharmacy disclosures for treatment are permitted by default under § 164.506(c) without specific NPP language. This is risk-management courtesy, not a HIPAA-on-HeroScript obligation.

### § 164.522-528 Patient Rights

**Status: ❌ Missing — out-of-band handling required for V1.** Patient access portal is deferred to V2 per PRD § 12. For V1, patient rights requests must flow through the clinic (the CE) with HeroScript fulfilling on the clinic's behalf as the BA.

TODOs:

85. **[policy] Out-of-band patient rights request process.** Document the workflow when a clinic forwards a patient request to HeroScript:
    - Right to access PHI (§ 164.524): clinic emails HeroScript Ops → Ops exports the patient's `Patient`, `Prescription`, `PrescriptionOrder`, `Shipment`, and notification records as a structured file → secure delivery to the clinic → audit-logged. SLA: 30 days.
    - Right to amendment (§ 164.526): clinic instructs HeroScript on the amendment → Ops applies via edit + audit log → if a denormalized snapshot exists on a submitted `PrescriptionOrder`, also document that the historical snapshot remains immutable but a correction note is recorded. SLA: 60 days.
    - Right to accounting of disclosures (§ 164.528): query the audit log for the patient's record over the requested period → produce the accounting. Six-year audit retention (item 21) directly enables this.
    - Right to restrictions (§ 164.522(a)): clinic-only decision; HeroScript records the restriction on the patient record and honors it operationally. Add a `Patient.restrictions: List<String>` field if any restriction will alter system behavior (e.g. "do not share with pharmacy X").
    - Right to confidential communications (§ 164.522(b)): channel preferences are already on `Patient.smsConsent` / `emailConsent`; document that clinics can flip these per patient request.
    - Right to deletion: HIPAA has no general right to deletion; deletions are limited by the 6-year retention rule and state medical-record retention statutes (often 7-10 years post-last-treatment). Document HeroScript's deletion policy: soft-delete only during retention window; hard-delete possible only after retention window expires.

    `[REQUIRED — § 164.524, § 164.526, § 164.528, § 164.522 + § 164.504(e)(2)(ii)(E)/(F)/(G)]` These are CE obligations under Privacy Rule Subpart E. The BAA must obligate HeroScript to make PHI available to fulfill CE rights requests (§ 164.504(e)(2)(ii)). The first auditor's SLAs (30 / 60 days) match the regulation. The 30-day access SLA is § 164.524(b)(2); 60-day amendment SLA is § 164.526(b)(2); accounting of disclosures § 164.528(c)(1) — also 60 days. Right-to-deletion: confirmed NOT a HIPAA right (a common misconception conflated with GDPR/CCPA).

86. **[code] Patient data export tool for Ops** (supports item 85). Single Ops command/endpoint: given a `Patient.ID`, produce a JSON+PDF bundle of all PHI on that patient, audit-logged, encrypted in transit to the clinic. Hard requirement to fulfill access requests within SLA.
    `[REQUIRED — § 164.524(c)(2) + § 164.504(e)(2)(ii)(E)]` Provide access in a designated form. The CE may delegate the export to its BA; HeroScript's BAA will obligate it to support this.

98. **[policy / code] Verification of identity for PHI requests.** *[ADDED by secondary audit — first audit missed.]* Before HeroScript Ops fulfills any patient-rights export (item 86) or any out-of-band disclosure to a CE-named individual, verify the requestor's identity and authority. Documented procedure: clinic forwards request with attestation that requestor is the patient (or personal representative); HeroScript matches the request against the patient record in the named clinic; any direct-patient-to-HeroScript request is rejected back to the clinic. Audit-log the verification result.
    `[REQUIRED — § 164.514(h)]` Verification of identity prior to disclosure of PHI is a Required Privacy Rule control that applies to BAs by flow-down via § 164.504(e)(2)(ii)(B) (BA may use/disclose PHI only as permitted by the BAA).

### § 164.502(b) Minimum Necessary Standard

**Status: 🟡 Partial.** Role-based access is the mechanism; the implementation is sound at the model level but has known gaps (items 6, 7, 9).

TODOs:

87. **[code] Audit endpoint reads for minimum necessary.** Re-review each endpoint's read scope:
    - `Patient` (`PatientEndpoints.kt`) — clinic-scoped is correct, but every clinic member can read every patient. Consider: should an MA see another MA's-only patients? Default yes for V1 (clinic-scoped); revisit post-pilot.
    - `Prescription` / `PrescriptionOrder` — clinic-scoped; revisit per-prescriber filter (item 9).
    - `User` (`UserEndpoints.kt`) — `read = admin or self or coClinic` — clinic colleagues can read each other. PHI surface: `firstName/lastName/email/phone` + the embedded `PrescriberLicensing`. **`PrescriberLicensing` should NOT be readable by every co-clinic user** — it contains DEA number and license image (item 88).
    `[REQUIRED — § 164.502(b) + § 164.514(d)]` Minimum-necessary standard.
88. **[code] Restrict `User.prescriber` (`PrescriberLicensing`) read.** DEA numbers and license images are confidential professional credentials. Add a field-level read restriction: `prescriber` is readable by `self`, `ClinicAdmin` of any of the prescriber's clinics, or `UserRole >= Admin`. Other clinic members get the user record minus the prescriber block.
    `[NOT-REQUIRED — BEST PRACTICE]` `PrescriberLicensing` is workforce/professional credentialing data, NOT patient PHI. HIPAA's minimum-necessary applies to PHI. Restricting DEA-number/license-image access is sound information-security practice (and may be required under DEA regulations 21 CFR § 1311 for EPCS) but is not HIPAA-mandated.
89. **[code] Pricing field masking.** `ProductPharmacyMapping.price/tax/shippingFee/total` are PHI-adjacent commercial data per PRD § 03 (MAs may not see pricing). Mask server-side, not just UI (per item 7).
    `[NOT-REQUIRED — BEST PRACTICE]` Pricing is product/commercial data, not PHI by itself. Per-order pricing tied to a specific patient becomes PHI; mapping-level pricing (a generic price list) is not. The PRD-level role restriction is a business rule, not a HIPAA rule. Server-side enforcement remains best-practice.

### § 164.502 Permitted Uses and Disclosures

**Status: ✅ In place by design.** HeroScript's processing falls under "treatment, payment, and healthcare operations" (TPO) for the clinic. No marketing, no sale of PHI, no fundraising. Reaffirm in BAA.

TODOs:

90. **[policy] Explicit prohibition on non-TPO use in BAA.** Standard clause in HeroScript-as-BA contract: PHI used only for the services described in the SOW. No analytics-as-a-service, no aggregated reporting to third parties unless de-identified per § 164.514 Safe Harbor.
    `[REQUIRED — § 164.504(e)(2)(i)]` BAA must specify permitted uses and disclosures.
91. **[policy] No PHI to AI/LLM systems** (already in `.claude/CLAUDE.md` HIPAA discipline section, restate as policy). Audit log captures `Export` action; periodic review confirms no LLM-targeted exports.
    `[REQUIRED — § 164.502(a) + § 164.504(e)(2)(i)]` Any disclosure to a system not covered by a BAA / not within TPO is impermissible. The control is the underlying disclosure rule; the policy statement is one means of enforcement.
92. **[code] De-identification helper if any reporting is added.** For PRD § 04 metrics, ensure metric reports do not embed PHI. Numbers and counts are fine; lists of patient/order IDs paired with diagnoses are not.
    `[CONDITIONAL — depends on whether reports leave the BAA-covered scope]` De-identification per § 164.514(a)–(c) is only required if the output is shared with a party not covered by an appropriate BAA / TPO permission. Within-BAA reporting is fine without de-identification.

---

## Breach Notification Rule (§ 164.400-414)

### § 164.404-410 Breach Detection, Risk Assessment, Notification

**Status: ❌ Missing.** As a BA, HeroScript must notify each affected Covered Entity (clinic) within the BAA-specified window. Without an audit log (item 13) and incident response plan (item 60), HeroScript cannot detect a breach, let alone notify.

TODOs:

93. **[policy] Breach detection criteria.** Document what triggers a breach investigation:
    - Unauthorized access to PHI detected via audit log anomalies (item 22).
    - Lost or stolen workforce device (item 81).
    - Confirmed phishing/credential compromise of any user with PHI access.
    - Vendor breach notification from any sub-BA.
    - PHI sent to an unauthorized recipient (misdirected SMS/email/notification).
    - Pharmacy adapter sending PHI to a pharmacy not authorized for the patient.
    `[REQUIRED — § 164.308(a)(6) + § 164.410]` Security incident procedures + business associate breach notification — both require detection capability as the prerequisite.
94. **[policy] Four-factor breach risk assessment** (§ 164.402(2)). For each suspected breach, document:
    - Nature and extent of PHI involved.
    - Unauthorized person who accessed or used the PHI.
    - Whether PHI was actually acquired or viewed.
    - Mitigation extent.
    Use this to determine whether notification is required (default: required unless low-probability-of-compromise demonstrated and documented).
    `[REQUIRED — § 164.402(2)]` Cited correctly; the four-factor analysis is the regulatory test for "breach" vs "incident without compromise."
95. **[policy] Breach notification templates and SLAs.** Pre-drafted: clinic notification (≤24h triage notice; full disclosure within BAA window typically ≤72h; final report ≤30 days). HHS notification for ≥500-individual breaches: HeroScript escalates to each affected CE; each CE is then responsible for HHS notification.
    `[REQUIRED — § 164.410 + § 164.404(b) + § 164.408]` BA must notify CE without unreasonable delay, no later than 60 days. CE in turn notifies individuals within 60 days; HHS within 60 days for ≥500 individuals (§ 164.408(b)) or in annual aggregate for <500 (§ 164.408(c)). The 24h/72h/30-day numbers the first auditor lists are BAA-tightened SLAs, not HIPAA-text. HIPAA's outer limit is 60 days.
96. **[code] Breach scope query tooling.** When a breach is suspected, Ops must be able to quickly enumerate: which patients' PHI was accessible during the incident window, which clinics they belong to, which users were involved. The audit log (item 13-22) is the source. Have a pre-tested query.
    `[REQUIRED — § 164.410(c)]` BA's notification must identify the individuals affected; query tooling is the means.
97. **[policy] Annual breach response tabletop.** Exercise simulating each detection trigger (item 93). Document gaps and remediate.
    `[NOT-REQUIRED — BEST PRACTICE]` Tabletop exercises are NIST 800-66 / 800-34 best-practice; not in HIPAA text. The underlying § 164.308(a)(7)(ii)(D) testing-and-revision is addressable.

99. **[policy] Six-year documentation retention for all Security Rule policies, procedures, communications, and required actions.** *[ADDED by secondary audit — first audit covered audit-log retention only.]* Maintain in written or electronic form: the risk analysis (item 44), risk-management strategy (item 45), sanction policy (item 46), activity-review records (item 47), Security/Privacy Officer designations (items 48-49), authorization/clearance/termination records (items 50-52), access-management records (items 53-55), training completion records (item 56), incident response records (items 60-62), contingency plan and test results (items 63-67), evaluation records (item 68), all executed BAAs (items 71-79), workstation/device policies and disposal records (items 81-83). Each retained for SIX YEARS from date of creation OR the date last in effect, whichever is later.
    `[REQUIRED — § 164.316(b)(1) + § 164.316(b)(2)(i)]` Documentation retention applies broadly across Security Rule artifacts; this is what an OCR investigator will subpoena.

100. **[policy] Six-year retention of Privacy Rule documentation.** *[ADDED by secondary audit.]* Companion to item 99 for Privacy Rule artifacts: BAAs (mirrors item 99), executed disclosure logs (per § 164.528), patient-rights request handling records (items 85-86, 98), records of any restrictions agreed to under § 164.522(a).
    `[REQUIRED — § 164.530(j)(1) + § 164.530(j)(2)]` Six-year retention; CE rule that flows to BA via BAA § 164.504(e)(2)(ii)(I).

101. **[policy] BA must report ANY use or disclosure not provided for by the BAA, not only breaches.** *[ADDED by secondary audit — first audit's breach criteria (item 93) cover breaches but miss the broader BAA obligation.]* Document and communicate to the CE every impermissible use or disclosure HeroScript becomes aware of, including disclosures that do not rise to the "breach" threshold under § 164.402 (e.g. minor accidental disclosures, near-misses, sub-BA non-compliance). The reporting cadence is BAA-defined (commonly within 5–10 business days of discovery for non-breach events).
    `[REQUIRED — § 164.504(e)(2)(ii)(C)]` Distinct from § 164.410 (breach notification): the BAA must obligate BAs to report any impermissible use/disclosure, broader than the "breach" definition.

---

## V1 launch blockers (consolidated must-do list)

The following items MUST land before V1 pilot launch (real patients, real PHI). Each one is referenced to the section above. Format: `[#] [category | effort] title`.

The PRD's launch criteria (10 clinics + ≥6 pharmacies + 2 weeks sustained ordering) implies real PHI from day 1; HIPAA requires every blocker below before the first piece of real PHI lands. Effort markers: S = ≤1 week, M = 1-4 weeks, L = >1 month.

1. **[policy | S] Sign AWS BAA** (item 71). Without this, no PHI in AWS. Free and immediate.
2. **[policy | M] Designate Security Officer and Privacy Officer** (items 48-49). Pre-requisite to every policy item.
3. **[policy | M] HIPAA Security Rule risk analysis** (item 44). Required and forms the basis for every other control decision.
4. **[policy | L] BAAs with all pilot clinics (10)** (item 72). One per clinic. The pilot cannot start without these.
5. **[policy | L] BAAs with all V1 pharmacies (≥6)** (item 73). Each pharmacy integration is blocked until its BAA is signed.
6. **[policy | S] BAA with Twilio HIPAA tier** (item 74). Required before SMS dispatch goes live.
7. **[policy | S] BAA with the identity verifier** (item 76). Required before ID.me-or-equivalent integration carries any prescriber PII.
8. **[policy | S] BAA with the address verifier** (item 77). Or strip the patient name from address-verification payloads.
9. **[policy | S] BAA with payment processor** (item 78). Required before invoicing carries any clinic-linked PHI.
10. **[policy | S] BAA with MongoDB Atlas if used** (item 79).
11. **[code | M] Audit log mechanism implemented** (items 13-22, 96). Required spec; required for breach notification; required for accounting of disclosures. Single largest engineering task on this list.
12. **[code | S] Lock down `ShipmentEndpoints` reads** (item 6). Current `Condition.Always` allows any logged-in user to enumerate every shipment.
13. **[code | S] Force MFA enrollment at first login** (item 29). PRD § 03 commits to MFA for all clinic users; current code makes it optional.
14. **[code | S] Session idle timeout + auto-logoff** (items 1-2). PRD § 11 commits to idle timeout.
15. **[code | S] Remove or gate the `AppStoreTester` hardcoded-credentials path** (items 34-35) before any production deployment.
16. **[code | S] Lock the server boot if production settings use in-memory database/cache/email** (item 11).
17. **[code | S] Enforce `clinicianReview != null` immutability on `PrescriptionOrder`** (item 23). Without this, submitted orders can be tampered with before the pharmacy fills.
18. **[code | S] HMAC verification on inbound pharmacy webhooks** (item 24). Without this, any actor who learns a webhook URL can write to `PrescriptionOrder.fulfilled` / `shipment` / `PharmacyOrder.accepted`.
19. **[code | S] Server-side enforcement of order submission preconditions** (TODO.md § 1.3, also HIPAA-relevant): no submission with expired/unverified DEA, no controlled-substance submission without DEA, no submission to a pharmacy not licensed in destination state. UI checks alone are insufficient.
20. **[code | S] Replace hard delete with soft delete on `Patient`, `Prescription`, `PrescriptionOrder`, `Shipment`, `PharmacyOrder`, `ClinicInvoice`, `User`** (items 27-28). Required to satisfy 6-year retention.
21. **[code | S] No PHI in URLs, logs, or error messages** (item 43 + .claude/CLAUDE.md HIPAA discipline). Add a CI/runtime guard if cheap; review every existing log line before launch.
22. **[code | S] Restrict `User.prescriber` reads** (item 88). DEA numbers and license images currently readable by every co-clinic user.
23. **[code | M] Patient SMS template must contain no PHI beyond first name + tracking link** (PRD § F7 / Screen 7). Implement the template literal once and forbid programmatic addition of any other patient field. Test before pilot.
24. **[code | M] Pharmacy adapter outbound payload signing + audit logging** (items 25, 39). Required to support breach scope queries.
25. **[infrastructure | M] HIPAA-eligible AWS production environment** with VPC isolation, KMS-encrypted S3 / MongoDB Atlas / DynamoDB / Secrets Manager (items 5, 37, 40). Production isolated from any dev/sandbox account that has touched non-HIPAA data.
26. **[infrastructure | S] TLS 1.2+ enforced, HSTS, security headers** (items 37, 42).
27. **[infrastructure | M] Secrets in AWS Secrets Manager, none in code or `settings.json`** (already partly designed via `Pharmacy.credentialsSecretRef`; finish the rotation procedure and verify `settings.json` is not committed with real values).
28. **[infrastructure | M] Daily encrypted backups with documented restore drill** (items 63-64).
29. **[policy | S] Incident response plan + breach detection criteria + notification templates** (items 60-62, 93-95). Cannot legally operate as a BA without these.
30. **[policy | S] Workforce HIPAA training completed for everyone with production access** (item 56).
31. **[policy | S] Termination procedure documented and tested** (item 52).
32. **[policy | S] Workstation security policy documented and enforced (MDM/FDE/EDR)** (items 59, 81).
33. **[policy | M] Sanction policy, access authorization procedure, access review cadence** (items 46, 50, 54).
34. **[policy | M] Out-of-band patient rights request process documented** (items 85-86). Patient access requests will arrive from pilot clinics; cannot deflect.
35. **[policy | M] Pre-launch penetration test by qualified third party** (item 69). Findings remediated before pilot.

**~35 V1 blockers.**

## V1.x or later (still required, can come post-pilot)

Required by HIPAA but defensible to land post-pilot under tight scope (e.g. limited number of clinics, accelerated incident-detection SLA, executive sign-off documented). Most are operational maturity items.

36. **[policy] Annual HIPAA evaluation** (item 68). Required annually — first one within 12 months of launch.
37. **[policy] Annual penetration test** (item 69). After the pre-launch one.
38. **[policy] Background checks for workforce with PHI access** (item 51). Required for new hires from V1.x onward.
39. **[code] Role-promotion two-person rule** (item 10). V1 can rely on a small documented Ops team where this is procedurally enforced; codify in V1.x.
40. **[code] Re-authentication for sensitive actions** (item 33).
41. **[code] Audit log anomaly alerting** (item 22). Manual review is acceptable for V1; automated alerts ship V1.x.
42. **[code] Break-glass procedure formalized in code** (item 4). Manual procedure with audit-log discipline is acceptable at pilot scale.
43. **[code] BAA versioning + re-acceptance flow** (item 55). Manual tracking for V1.
44. **[code] Login-monitoring user emails** ("New sign-in from..." — item 58).
45. **[infrastructure] CloudWatch immutable audit-log mirror with Object Lock** (item 21). Database-backed audit log ships V1; the immutable mirror is defense-in-depth.
46. **[infrastructure] Quarterly access review** (item 54). V1 can do annually with monthly anomaly review.
47. **[policy] Annual breach response tabletop** (item 97).
48. **[policy] Quarterly information system activity review** (item 47). V1 can ship with monthly cadence; quarterly is the long-term floor.
49. **[code] Disaster recovery drill** (items 64-65). Tabletop pre-launch; full restore drill in V1.x.
50. **[code] Account inactivity deactivation** (item 3). 90-day inactivity → deactivated. Manual review acceptable for V1.
51. **[code] Continuous dependency vulnerability scanning** (item 70). Pre-launch baseline scan; Dependabot ongoing.
52. **[policy] Sub-BA inventory tracking spreadsheet** (item 80). Lightweight V1; formal vendor management program V1.x.

## Notes on architectural decisions worth revisiting

Places in the current design where a HIPAA-driven change would materially improve posture. These are worth surfacing for the project lead before V1 implementation continues.

**A. Audit log is "handled outside the canonical models" — but a queryable view is required.** `ui.md` references the audit log viewer and `TODO.md § 1.7` defers the mechanism. The Privacy Rule's "Accounting of disclosures" (§ 164.528) and the Breach Notification Rule's scope assessment (§ 164.402) both require a queryable, per-patient view. Recommend: model a first-class `AuditEvent` table even if the canonical-models exclusion was originally about avoiding bloat — the operational requirements force a queryable model anyway. The 6-year retention can be enforced as a partition / TTL exclusion.

**B. `PrescriptionOrder` denormalizes patient and prescription fields for query convenience, but doubles the PHI surface area.** `patient`, `product`, `form`, `strength`, `instructions`, `prescribedBy` are all `@Denormalized` on the order. Each is required for displaying the order without joins, but each is also a duplicated PHI surface that must be (a) protected by the same permissions as the source, (b) included in patient data exports (item 86), and (c) frozen once `clinicianReview` is set (item 23). The denormalization is correct for performance and audit integrity; just be aware it expands the PHI inventory and complicates the right-to-amendment flow (item 85): an amended `Prescription.instructions` does NOT retroactively update the snapshot on past `PrescriptionOrder`s, which is the legally correct behavior (medical records are immutable) but must be documented.

**C. `Patient.allergies/diseases/otherMedications` clinical fields — confirm they are needed for V1.** PRD § 02 explicitly states "HeroScript will not function as an EMR or store clinical chart data beyond what is needed to route an order." The model stores allergies/diseases/other-medications "to mirror LifeFile's clinical[] entries" (per the model comment) so LifeFile orders can pass these through. The data is genuinely needed for some pharmacy adapters but it sharply increases the breach impact: a leak of `Patient.diseases` is more severe than a leak of `Patient.name + medication`. Consider:
  - Storing the clinical fields only when a `Pharmacy.adapterType` actually requires them (out-of-model rule).
  - Encrypting these specific fields with envelope encryption at the application layer (separate KMS key from the rest of the row), so a database snapshot leak does not expose them.
  - At minimum, restating in the BAA that these fields are stored.

**D. Notification subpanel on Order Detail surfaces what was sent.** Per `ui.md`: "we display what was actually sent (§ F7). (Notification model is being handled outside the canonical models)." Ensure the stored notification body is rendered only to clinic members of the order's clinic (same permission scope as the order itself), and that the patient's phone number on the notification record is masked in the UI to the last 4 digits unless the viewer is a ClinicAdmin. Without explicit masking, every MA in the clinic can read every patient's phone number from the notification history.

**E. `User.prescriber.deaLicenseImage` is a `ServerFile` with `maxSize = 10_000_000`.** DEA license images frequently include the prescriber's home address. Today the file storage is the same S3 bucket as everything else (`files` setting). Recommend: store DEA license images in a separate S3 prefix with a separate IAM policy that further restricts access to (a) the prescriber themselves, (b) `UserRole >= Admin` for verification, (c) no one else. Audit-log every download.

**F. `appstoretester@lightningkite.com` + hardcoded password in `UserEndpoints.kt`.** This is a known anti-pattern in mobile-app-store testing workflows but it lives in the same codebase that will store real PHI. Per HIPAA workforce security and password management, the existence of this account in any environment connected to real PHI is unacceptable. Must be gated by environment flag and never run in production.

**G. The current `settings.json` has `"general.debug": false` but `database = "ram"`, `email = "console"`, `notifications = "console"`.** These are correct for local dev but a production boot must refuse to start with any of these (item 11). Add a defensive assertion. Same goes for the `Seed.kt` runner — guarded by `general.debug = true` per CLAUDE.md, but worth confirming the guard cannot be bypassed in production.

**H. `Pharmacy.credentialsSecretRef` is the right pattern.** The model already routes the actual secret value through AWS Secrets Manager, not the database. Extend this pattern: any secret HeroScript holds (Twilio token, SendGrid token, processor key, ID.me client secret, FCM service account key) should follow the same `credentialsSecretRef` pointer pattern. Currently the FCM private key is mentioned as living in `settings.json` (per `.claude/CLAUDE.md`) — move it to Secrets Manager.

**I. ID.me cadence as a config flag (per `ui.md` § "Open strategy ideas").** Storing the cadence policy as a config is the right call (PRD § F4 leaves it open). For HIPAA: persist `ClinicianReview.idEvent` regardless of cadence so the audit trail is uniform — already in the model. Confirm the `idEvent` value is not a JWT or similar that itself carries PHI; it should be an opaque verifier-side identifier.

**J. `consentAffirmedAt: Instant?` is a single timestamp.** PRD § 03 / § 11 require patient consent for SMS/email. The model has `Patient.smsConsent` and `Patient.emailConsent` (also instants — when consent was given) and a per-order `PrescriptionOrder.consentAffirmedAt` (when clinic affirmed at order). Confirm the recorded consent timestamps survive a `Patient` update (the underlying consent fact is event-sourced, not the latest-state). If a patient revokes consent, the model needs a way to record that — currently null-vs-non-null doesn't distinguish "never consented" from "revoked." Add `smsConsentRevokedAt: Instant?` / `emailConsentRevokedAt: Instant?` before V1 pilot or before any SMS opt-out flow lands.

---

## Secondary audit appendix

### Covered Entity vs Business Associate — HeroScript's role

**HeroScript is a Business Associate (BA), not a Covered Entity (CE).** HIPAA defines BA at 45 CFR § 160.103 as a person or entity that "creates, receives, maintains, or transmits" PHI on behalf of a CE for a function regulated by HIPAA. HeroScript fits squarely: each pilot clinic (the prescribing healthcare provider) is the CE; HeroScript performs the prescribing-workflow / pharmacy-routing function on the clinic's behalf and necessarily handles PHI to do so. The first audit's framing in its preamble is correct.

Two practical consequences worth restating:

1. **Most Privacy Rule patient-rights obligations sit with the CE.** § 164.520 (NPP), § 164.522 (restrictions / confidential communications), § 164.524 (access), § 164.526 (amendment), and § 164.528 (accounting of disclosures) are CE-direct rules. They reach HeroScript via the BAA's § 164.504(e)(2)(ii)(E)–(G) flow-down clauses, which obligate the BA to *make PHI available to the CE* so the CE can fulfill the patient's request. HeroScript does not field patient requests directly — clinic does, HeroScript supports. Item 85 captures this; item 86 (the export tool) is the operational support obligation.

2. **A BA is directly liable for the Security Rule and parts of the Privacy Rule under the HITECH Act.** § 164.302 and the Omnibus Rule make every Security Rule standard directly enforceable against BAs by OCR. Treat every § 164.308 / § 164.310 / § 164.312 item in this document as HeroScript's first-person obligation, not the clinic's.

### Per-vendor BAA-coverage analysis

Per § 164.314 (organizational requirements) and § 164.308(b)(2) (sub-BA), HeroScript must execute a written BAA with every sub-BA that creates/receives/maintains/transmits PHI on its behalf. The analysis below mirrors items 71–79 but flags nuance:

- **AWS** — BAA required (item 71). AWS BAA at signature covers a specific list of "HIPAA-eligible services." Verify every AWS service HeroScript uses is on that list (notably: Lambda, S3, KMS, Secrets Manager, MongoDB Atlas via AWS Marketplace if used, CloudWatch Logs, DynamoDB, API Gateway, ALB, SES, SNS — most are HIPAA-eligible but a handful like AWS Q for Business, certain Bedrock model providers, AWS Glue DataBrew are not). PHI must never touch a non-HIPAA-eligible AWS service.

- **Pilot clinics** — CE-to-BA BAA, the clinic's responsibility to execute but HeroScript provides the template (item 72). Required before any clinic PHI lands in production.

- **Pharmacies** — Nuanced (item 73). A pharmacy dispensing under its own DEA/state license is itself a CE (healthcare provider). HeroScript-to-pharmacy is then a CE-to-CE disclosure for *treatment* of a shared patient under § 164.506(c)(2), which does NOT require a BAA. However, if a pharmacy operator is performing functions on the prescribing clinic's behalf (e.g. consolidated invoicing, patient communication on clinic letterhead), it becomes a BA of the clinic; HeroScript-to-pharmacy is then a BA-to-BA chain and § 164.308(b)(2) requires a sub-BA agreement. **Conservative position: BAA with every pharmacy regardless.** Confirm each pharmacy's posture and document in the sub-BA inventory.

- **Twilio** — BAA required (item 74). Twilio offers HIPAA-eligible products on specific plans; standard Twilio is not BAA-covered. Same for SendGrid (a Twilio property).

- **SendGrid** — BAA required (item 75) if email carries PHI. Note: per HHS guidance, even sending an unencrypted email TO a patient at their request can be permissible (§ 164.522(b) confidential communications + patient's explicit choice); but sending PHI to/through SendGrid means SendGrid handles PHI → BAA required.

- **ID.me / identity verifier** — Conditional (item 76). If the verification payload contains only prescriber workforce data (DEA, name, license number), this is workforce credentialing and not PHI disclosure. No BAA required for that scope. If the verifier ever sees patient data, BAA required. Recommend designing the integration so prescriber-verification and patient-data flows are disjoint.

- **Smarty / Lob / USPS** — Conditional (item 77). Address-verification with the patient's name and address is PHI. Address-only verification (line1/city/state/zip with no name and no patient identifier in the API request body) is generally not individually identifiable under § 164.514(b) Safe Harbor analysis. The first audit's "strip patient name" suggestion is the correct design. If you do send name + address, BAA is required, and Smarty does not offer one on standard plans — Lob does for enterprise tiers, USPS APIs depend on which endpoint.

- **Payment processor (Stripe / Priority Payments)** — Conditional (item 78). Card PAN + ACH routing alone are not PHI. The processor becomes PHI-exposed if invoice metadata, charge descriptors, or per-line-item references reveal the patient or the prescription. Standard practice: scrub line items to opaque order IDs and aggregate amounts before sending to processor; then no BAA is strictly required. If you send rich metadata, BAA required (Stripe has a BAA-eligible enterprise tier).

- **MongoDB Atlas** — BAA required (item 79) if used and PHI is stored. Atlas's HIPAA-eligible cluster tier comes with a BAA. Note: HeroScript is currently configured for either MongoDB or JSON files; the JSON-file production path is a non-starter for HIPAA (no encryption, no access controls beyond file permissions, no backup story), so this BAA is a forcing-function for choosing MongoDB Atlas at production scale.

- **FCM (Firebase Cloud Messaging)** — Not in the first audit's list but is mentioned in `settings.json`. FCM push notifications carry whatever the notification body contains. Per item 23 (consolidated blocker on SMS template) the body should contain no PHI beyond first name + tracking link, which means FCM is structurally outside PHI scope — push notifications carry only an opaque event signal and a deep link. **Confirm in the codebase that no PHI is ever in the FCM payload.** If true, no BAA required with Google for FCM. If false, Google offers BAA-eligible Firebase products on specific tiers.

### State-law preemption notes

HIPAA establishes a federal floor, not a ceiling. § 160.203 preserves state laws that are *more stringent* than HIPAA. For the V1 pilot (seeded clinic is in Tennessee per `Seed.kt`):

- **Tennessee medical-record retention is 10 years post last professional contact** (Tenn. Comp. R. & Regs. 1050-02-.18), longer than HIPAA's 6-year retention floor for documentation. This is the binding retention for patient records (Prescription, PrescriptionOrder, Shipment, ClinicalEntry). Items 27-28 and blocker #20 (no hard delete) are operationally correct — the state law is the binding source for patient records; HIPAA is the binding source for Security/Privacy Rule documentation and audit logs.

- **Tennessee Identity Theft Deterrence Act (Tenn. Code § 47-18-2107) imposes its own breach notification standard** for "personal information" (which includes name + SSN, name + DL, name + financial-account info). HeroScript stores none of those by default, so primary exposure is the HIPAA breach notification rule, not the TN data-breach rule. Confirm no SSN / DL collection slips in via pharmacy adapter requirements.

- **Tennessee Patients' Privacy Protection Act (Tenn. Code § 68-11-1502)** restricts disclosure of medical records by hospitals/healthcare providers. Mostly redundant with HIPAA at the clinic boundary; no additional obligation on HeroScript-as-BA beyond what HIPAA imposes.

- **Other pilot states** — for any pilot clinic in CA, NY, MA, IL, or TX, add a per-state preemption check before go-live. CA's CMIA (Confidentiality of Medical Information Act) is materially more stringent than HIPAA on intentional disclosures and carries private right of action. NY SHIELD Act and the recently amended NY public health privacy laws also add obligations. Out of scope for this audit but flag in onboarding.

### Items intentionally NOT added to the document

The following came up during the review but were deliberately excluded because they are outside HIPAA scope:

- **EPCS (Electronic Prescribing of Controlled Substances)** — DEA regulation 21 CFR § 1311, not HIPAA. The PRD references controlled substances; HeroScript will need to comply with EPCS for any Schedule II–V prescription. Two-factor authentication for the prescriber at signing, audit trail of the signing event, and DEA-registrant identity proofing are all EPCS requirements that LOOK like HIPAA requirements but are separately mandated.

- **State pharmacy practice acts** — vary by state, govern what a pharmacy can dispense and to whom. Out of HIPAA scope.

- **PCI-DSS** for payment data — applies to the processor primarily; HeroScript's exposure depends on whether it ever handles raw card data (recommend: never; tokenize at processor).

- **GDPR / CCPA / state privacy laws** — out of scope per task brief; flagged here only because the first audit mentioned "right to deletion" which is a GDPR/CCPA concept that does not exist in HIPAA.
