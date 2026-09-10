# Assumptions, decisions and delivery status

The exercise PDF is a requirements source. Its submission instructions do not authorize sending messages, opening accounts or handling real payments. The user's requested implementation adds Java 17, the current Angular release, PostgreSQL, an Angular source folder inside the Spring project, an integrated Maven build, and a free deployment.

## Working assumptions

| Topic | First-cut decision | Needed before clinic launch |
|---|---|---|
| Clinic data | Six fictional Yangon branches and a small seeded catalogue, localized to Myanmar as requested | Obtain and validate real addresses, rooms, staff qualifications and treatment names |
| Time | Asia/Yangon (UTC+06:30) as requested; assumed Monday–Saturday, 09:00–18:00, including turnaround | Confirm business hours, rosters, public holidays and closures |
| Slot grid | Starts on the hour or half hour; strictly in the future; through today plus 90 days inclusive | Confirm lead time and booking horizon |
| Treatment | 30, 60 or 90 minutes | Confirm pricing and room/equipment compatibility |
| Room cleaning | Always 15 minutes; no overlap | Confirm any treatment-specific longer cleaning |
| Therapist gap | Standard 15 minutes; senior facialist 0 | Confirm who qualifies and whether exceptions apply to all treatments |
| Deposit | MMK 300 (Myanmar kyat), using the requested currency and the brief's numeric amount | Confirm tax/accounting treatment and deduction from final bill |
| Membership | Demo code plus exact configured email verifies one demo member | Integrate an authoritative membership system and proof of identity |
| Payment hold | 15 minutes, enforced on subsequent reads/writes | Confirm duration and provider handling of late successful payments |
| Cancellation | Allowed at least 24 hours before; otherwise contact reception | Confirm no-shows, late arrivals, partial refunds and exceptional overrides |
| Refund | Eligible paid cancellation becomes refund pending | Assign staff responsibility and add provider reconciliation |
| Client data | Name, email and phone; no ID number or date of birth | Agree privacy notice, lawful purpose, retention and clinical-consent boundary |
| Diary | Database is authoritative for online bookings; staff can inspect it | Assisted bookings, future-paper-diary import and cutover process |
| Notifications | On-screen confirmation and private retrieval | Email/WhatsApp provider and delivery/retry operation |

## What the first cut proves

The code demonstrates independently calculated availability, resource reservation under concurrent requests, separate room/therapist turnaround, one deposit per booking under retry, member exemption, request-driven hold expiry, cancellation boundaries, an embedded Angular application and versioned PostgreSQL migrations. The README records reproducible tests and a deliberately disabled rule with its real failing output.

## What is intentionally incomplete

- Payments are simulated. There is no gateway charge, signed webhook, settlement or automatic refund.
- A demo code and email are not a real membership identity solution. The demonstration must use fictional contact details.
- Staff access is a single configured account, not identity-provider SSO, MFA, individual accounts, access review or a complete audit trail.
- The diary does not yet provide the full operations workflow: booking import, authenticated assisted bookings, roster editing, equipment downtime and staff overrides.
- Retention/deletion automation, clinical consent, reminders, waiting lists and rescheduling need defined policies and further implementation.
- Source is pushed to GitHub and CI passed. The [Render Free demo](https://lumina-clinic.onrender.com) is live in Ohio with Neon; public health and Myanmar/MMK catalogue checks passed. See the verification record for observed results and remaining operational limits.
- The free review environment is not a production service commitment. Recovery procedures, monitoring alerts, load testing, backup policy and payment/security reviews are launch gates.

## Delivery milestones

See the [founder proposal](proposal.md) for the six-week plan. Work proceeds in this order: confirm policy and inventory; validate booking/cleaning rules; integrate membership/payment and staff workflows; pilot one branch; test recovery and train staff; expand after sign-off. The fixed budget requires scope prioritization, and the actual budget and team capacity remain unknown.

## AI disclosure

An OpenAI Codex assistant helped interpret the exercise, design the architecture and rules, generate Java/Angular/SQL, write test cases and documentation, investigate build failures and research official deployment documentation. Parallel assistant work was used for backend, frontend and documentation. The README distinguishes commands actually run and outcomes observed from hosting steps that still require account access. The implementer remains responsible for understanding, reviewing and explaining the submission in the live session.
