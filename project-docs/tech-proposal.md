#  

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

HeroScript: Prescription Ordering/Management Platform \- Project Proposal   
5/20/26  
Lightning Kite  
255 S 300 W, Logan UT 84321  
(435) 753-3260

## Project Proposal Table of Contents (Make sure to refresh the table before submission)  {#project-proposal-table-of-contents-(make-sure-to-refresh-the-table-before-submission)}

[Project Proposal Table of Contents (Make sure to refresh the table before submission)	2](#project-proposal-table-of-contents-\(make-sure-to-refresh-the-table-before-submission\))

[High Level Scope Statement:	3](#high-level-scope-statement:)

[Project Primary Objectives:	3](#project-primary-objectives:)

[Project Deliverables:	3](#project-deliverables:)

[Scope Exclusions (If applicable):	3](#scope-exclusions-\(if-applicable\):)

[Criteria of Success:	3](#heading=h.j4hqyvo1n0yk)

[Budget and Timeline expectations:	4](#heading=h.p6nbvcpocki1)

[Itemized Tasks:	4](#heading=h.rppulmuk5d73)

## 

## Executive Summary:

The current operational model for Gameday Men's Health clinics is severely hindered by complexity and fragmentation. With over 400 locations manually managing prescriptions across approximately 12 distinct pharmacy portals, the network experiences substantial friction, resulting in tracking blindspots, error-prone messaging, and excessive manual overhead. HeroScript V1 addresses this challenge by establishing a singular, authoritative orchestration layer that unifies the B2B marketplace for men's health clinical networks. The platform will act as the unified control plane for prescription management by achieving three core objectives:

* **Catalog Aggregation:** Consolidating the full product catalog from all integrated pharmacies into one unified interface.  
* **Demand Consolidation:** Centralizing network demand to negotiate and enforce preferred pricing models.  
* **Workflow Automation:** Providing centralized order history and automating patient-facing shipment alerts without manual intervention.

Lightning Kite  is a US-based software development agency located in Logan, Utah. We specialize in delivering superior multi-platform applications and websites with unmatched speed and partnership. Unlike agencies that rely on bloated, off-the-shelf templates, we utilize efficient frameworks we are intimately familiar with to deploy high-performance solutions. Our entire team works on-site in Utah, guaranteeing faster progress through seamless collaboration and high-touch communication.

Our strengths are uniquely suited to the challenges of the HeroScript V1 MVP:

* **Expertise in Highly Regulated Systems:** We have proven experience building full Software-as-a-Service (SaaS) compliance solutions, such as the Healthcare Compliance Pros (HCP) platform, which manages healthcare compliance and utilizes sophisticated Role-Based Access Control (RBAC). This experience directly mitigates the high-impact risks associated with HIPAA compliance and Protected Health Information (PHI) storage required for HeroScript.  
* **Technical Velocity and Single-Codebase Efficiency:** Our use of efficient, modern frameworks and an on-site, US-based team guarantees superior development speed and high-touch collaboration. This is critical for meeting the timeline for the V1 pilot launch.  
* **Complex Systems Integration:** The core technical challenge of HeroScript is integrating and normalizing data from up to 12 disparate third-party pharmacy APIs. We specialize in building custom, robust integration layers for complex enterprise data systems, ensuring reliable and secure bi-directional data exchange.

## High Level Scope Statement: {#high-level-scope-statement:}

The purpose of the HeroScript V1 MVP is to establish a unified, authoritative orchestration layer for prescription management in the men's health clinical network by consolidating pharmacy catalogs, centralizing demand, and automating shipment alerts.

## Project Primary Objectives: {#project-primary-objectives:}

1. Unify Clinical Workflow by consolidating approximately 6-12 initial disparate pharmacy portals into a single, reliable platform with a unified product catalog and centralized order history.  
2. Reduce Per-Prescription Costs by aggregating network demand to negotiate and enforce preferred pharmacy pricing models.  
3. Improve Patient Retention and Service by automating refill reminders and providing real-time, text-only shipment tracking notifications without manual staff intervention.

## Project Deliverables: {#project-deliverables:}

1. The HeroScript V1 Clinic-Facing Web Application for order entry, submission, and refill management.  
2. The HeroScript Ops Internal Admin Portal for managing the unified product catalog, pharmacy integrations, and order monitoring.  
3. The External Integration Adapter Layer for connecting and normalizing data from up to six third-party pharmacy APIs.  
4. The core HIPAA-Ready Platform infrastructure, including Role-Based Access Control and an immutable audit log, supporting a transactional SMS notification system for patient shipment alerts.  
5. Compliance Discovery Report: Execute a comprehensive Compliance Discovery to assess and document architectural weak points, the movement of PHI, gap analysis, and a matrix of responsibilities. This process will involve identifying every instance of PHI data storage and outlining the security measures employed for data both in transit and at rest.

## Detailed Discussion:

### CORE MVP ARCHITECTURE & SYSTEM DESIGN

#### **External Integration Adapter Layer**

HeroScript V1 uses a robust, decoupled Integration Adapter Layer to unify the complexities of an initial six third-party pharmacy software suites into a single pipeline.

* **Design:** The layer uses an API/webhook adapter pattern to isolate core business logic.  
* **Reliability:** Standard error-handling is implemented.

#### **Third-Party Services Integration**

The V1 architecture incorporates several critical external services to support core transactional workflows:

| Service | Purpose |
| :---- | :---- |
| **Stripe** | Daily clinic billing collection, supporting ACH and credit card payments. |
| **ID.me** | Prescriber identity-verification hooks, required immediately prior to final order submission/routing. |
| **Smarty/Lob/USPS** | Inline, real-time patient address verification to reduce shipment errors and fulfillment delays. |
| **Twilio (HIPAA Tier)** | Dedicated, compliant channel for secure outbound transactional SMS shipment alerts. |

### 3\. V1 MVP FUNCTIONAL SPECIFICATION OVERVIEW

#### **Role-Based Access Control (RBAC)**

Access and data visibility are strictly governed by the V1 RBAC model:

* **HeroScript Ops Admin:** Full access across all clinics and operational reporting (excluding direct patient PHI/order details unless required for support).  
* **Clinic Admin:** Full administrative access for a single, assigned clinic (including staff management and local order history).  
* **Prescriber:** Ability to create, review, and authorize (sign) prescriptions. Restricted to orders they have initiated or are responsible for.  
* **Medical Assistant (MA):** Permitted to create and manage draft orders. Prohibited from final authorization or submission.

#### **State-Licensing Matrix & Smart Routing**

The platform employs a dynamic compliance mechanism to ensure orders are routed only to legally compliant pharmacies:

* **Compliance Matching:** The order entry engine strictly matches a patient’s shipping state against pre-defined, active pharmacy compliance maps.  
* **Availability Display:** The UI dynamically displays side-by-side pharmacy availability for the selected product, allowing clinic users to select the appropriate, licensed fulfillment partner.

#### **Refill Reminders & One-Click Reorder Flow**

The system automates refill reminder triggers based on consumption logic:

* **Depletion Tracking:** The platform monitors medication supply by calculating depletion dates based on the remaining days of dosage.  
* **Split-Logic Shortcut:**  
  * **MA Action:** Medical Assistants generate a Draft reorder, requiring subsequent Prescriber sign-off.  
  * **Prescriber Action:** Prescribers trigger immediate order submission, automatically initiating the required ID.me checkpoint challenge prior to routing.

#### **Multi-Shipment Tracking & Patient Notifications**

HeroScript manages order fulfillment complexity across multiple vendors:

* **Mapping:** A single order maps to zero or more (0..N) individual shipments.  
* **Communication Policy:** All patient notifications are strictly text-only and transactional. They expose absolutely no clinical diagnostic or medication details.

| Shipment Event | Communication Template (Example) |
| :---- | :---- |
| **Single Shipment** | Your order has shipped and is scheduled for delivery. |
| **Multi-Shipment Event** | Your order has been split into multiple shipments. Shipment 1 of X has shipped. |

### 4\. HIPAA compliance & Posture

Engineering work is categorized into three buckets for clarity on cost and risk management.

#### **Bucket A: Product Development**

Standard, high-velocity engineering with minimal compliance-specific constraints:

* Frontend/backend UI work, core product catalogs, and basic workflow implementation.

#### **Bucket B: HIPAA Compliance Related Engineering**

Slower velocity due to required compliance design patterns:

* AWS KMS encryption for all sensitive data at rest and partitioned databases for data segregation.  
* Development of an immutable, append-only system audit log, structured for 6-year retention.

#### **Bucket C: Business Associate Operational Liability**

Long-term, non-code contractual risks and ongoing responsibilities:

* Contractual obligations assumed under a signed BAA.  
* Implementation and monitoring of processes for downstream sub-vendor management.

#### **Commercial & Risk Frameworks**

Specific engagements required to support ongoing compliance:

* **Compliance Risk Analysis:** Initial engagement of 40 hours to finalize controls and establish the System Security Plan.  
* **Monthly Compliance Operations Retainer:** Ongoing retainer for technical security controls, audit log management, and security event response.  
* **Managed Cloud Insurance:** A premium covering the overhead and policy of maintaining cyber insurance to cover potential HIPAA related incidents.

#### **Shared Responsibility Matrix**

The following matrix formally defines ownership boundaries for critical operational areas:

| Operational Area | Responsible Party |
| :---- | :---- |
| User Access Management & Provisioning | Client |
| Secure Cloud Infrastructure Configuration | Software Provider |
| Endpoint Security Frameworks | Software Provider |
| Identity Access Management (IAM) Policies | Software Provider |
| Internal Staff HIPAA Compliance Training | 3rd Party Vendor |
| Redundant Backup Curation & System Monitoring | Software Provider |
| Accuracy of Medical Records & Prescription Data | Client |

### 5\. NON-FUNCTIONAL REQUIREMENTS & ACCEPTANCE CRITERIA

#### **Compliance & Security Posture**

* **Data Integrity:** Strict zero-PHI leakage policy is mandated across application logging, telemetry, or public AI networks.  
* **Optimization:** Required desktop web optimization only, utilizing the English-US language layout.

#### **MVP Launch Acceptance Criteria**

All three binary criteria must be met to formally declare the V1 MVP successfully shipped:

* **10 active pilot clinics** are fully provisioned and live.  
* **6 functional pharmacy API adapters** are fully integrated and enabled for live routing.  
* **Live ordering is sustained** for a period of **2 consecutive weeks** without a major system-wide incident.

### 6\. Project Timeline

### 7\. Risk Analysis:

#### **What is a risk?**

The purpose of this section is to identify possible risks and worst case scenarios so that preventative action can be taken ahead of time. 

Risks might include external dependencies, unfamiliar or antiquated technology, security concerns, failures of a previous version of the product, etc. They might also include administrative risks such as lack or loss of funding, scope creep, changing deadlines, staff turnover, or changes to regulatory standards. 

Risks can be responded to in one or more of the following ways:

* Avoidance: Work to avoid the occurrence of the risk entirely.  
* Transfer: Utilize third party software well equipped and purpose built to handle certain risks. In so doing you have transferred the risk.  (For example, integrating a known money transfer software as opposed to creating a custom tool.)    
* Mitigation: Work to mitigate the likelihood of occurrence, or the consequences of a risk should the worse case scenario occur. This might also be referred to as “Preemptive damage control.”  
* Acceptance: If there are no options for avoiding, transferring, or mitigating a known risk and the consequences must be accepted as a possible outcome. 

#### **Risk Assessment and Analysis:** 

An initial assessment of the project scope identifies two high level areas of concern: external dependencies and security associated with Protected Health Information (PHI) storage. 

External API Dependency Risk (Third-Party Pharmacy APIs):  
The project is highly dependent on integration with existing third-party pharmacy APIs to place orders. A preliminary assessment indicates these APIs are currently underdeveloped, characterized by unclear or insufficient documentation. While technical hours spent working with the third-party teams to enhance their APIs as needed is not anticipated to be excessive, the dependency introduces a high risk of schedule slippage due to the likely requirement of waiting for external teams to implement necessary updates. 

PHI Data Security Risk:  
The system's requirement for PHI data storage presents a risk where the consequences of a data breach are severe. We have no serious concerns of a technical system failure leading to exposure, but the potential for a breach remains (whether through unrealized technical vulnerabilities or more likely through administrative user error/failure to abide by data privacy practices). Given the severity of the consequences associated with PHI exposure, stringent security protocols, regular audits, and comprehensive administrative training are mandated to mitigate potential compliance and legal risks. A more thorough and complete Risk assessment will be conducted as part of project discovery to catalog all possible risks, and assign appropriate responses. 

#### **Risk Register:** 

* Ri-01: External API Dependency  \- Medium Impact \- High Likelihood.  
  * Risk: Timeline delays while working with Third-party API teams to establish long term working solutions for order placement and catalog syncing.   
  * Response plan: Mitigate \- Develop architecture to be flexible from the beginning to support a wide range of varying API capabilities, begin the API integration at the very start of the project to ensure maximum time allowance for delays, and provide detailed and clear requirements to the API teams so they understand what we need.   
* Ri-02 : PHI Data exposure \- Extremely High Impact \- Low Likelihood.  
  * Risk: PHI Data exposure through technical or administrative failures.   
  * Response plan: Avoidance/Mitigation \- Execute a comprehensive Compliance Risk Analysis to assess and document architectural weak points, the movement of PHI, gap analysis, and a matrix of responsibilities. This process will involve identifying every instance of PHI data storage and outlining the security measures employed for data both in transit and at rest.  
    * We highly recommend a Penetration test as usage volume increases to ensure system security. 

## Scope Exclusions (If applicable): {#scope-exclusions-(if-applicable):}

The following functionalities are explicitly excluded from the V1 MVP scope to ensure focused development and rapid delivery:

1. Insurance claim adjudication.  
2. Clinical charting, EMR functionality, or record storage.  
3. Telehealth services or medical decision-making support.  
4. Patient-facing portals for order management or clinical history.  
5. Direct pharmacy portals for order intake or clinical review.

## Notes on our proposal:

