# Lumina Clinics: a dependable first step into online booking

**Proposal for the founder · 10 September 2026 · Scope: six-week phase one**

## What we propose

Give clients a simple way to choose a treatment, branch and available time, with the option to see their preferred therapist. Receptionists remain responsible for the clinic diary, assisted bookings, exceptions and client care. Online and receptionist bookings must eventually share one diary; keeping independent paper and online capacity would recreate the Christmas double booking.

The first release should protect three things: a room and qualified therapist really are available; a deposit is collected once; and the client clearly knows whether their booking is confirmed. A visual refresh is valuable, but these are our launch criteria.

## The client experience

1. Choose a treatment, one of the six branches, and optionally a therapist.
2. See times that fit both the treatment and room cleaning. Choose a date and time.
3. Give a name, email and telephone number, and see the deposit and cancellation terms before booking.
4. Verified members receive confirmation without a deposit. Other clients temporarily reserve their slot and pay the 300 deposit through a payment page.
5. See confirmation and a private booking reference. Clients can return to their booking and cancel within the permitted window.

If another client takes a slot first, the system explains this and offers another time. A hanging payment page must be safe to retry. If payment does not finish within the provisional 15-minute hold, the slot returns to availability.

The working demonstration includes this path and simulated payment. It does not collect real money. A live payment provider and verified membership records are necessary before real clients use deposit exemptions or payments.

## Decisions to agree at kickoff

**Cleaning and therapist time.** Every room needs 15 minutes after treatment. The senior facialist's ability to work immediately concerns her time, not the room's cleaning. She can see the next client back to back only if another suitable, clean room is available. We provisionally give other therapists a 15-minute gap. Please confirm which therapists and treatments qualify.

**The MMK 300 deposit.** The demonstration uses Myanmar kyat as requested by the user, preserving the brief's numeric deposit of 300. Please confirm whether the deposit is deducted from the final bill, and the handling of no-shows, late arrivals, partial refunds and payment disputes. Treatment prices are illustrative demo amounts, not market quotations or converted prices.

**Cancellation.** We interpret “up to 24 hours before” as including exactly 24 hours. Later online cancellation is declined with a request to contact reception; the booking remains reserved. An eligible paid cancellation releases the slot and creates a refund-pending record. Reception handles the refund until a provider integration is approved.

**Opening hours and services.** The six fictional branches are in Bahan, Kamayut, Sanchaung, Tamwe, Thingangyun and Yankin, Yangon. Appointments use Asia/Yangon (Myanmar time, UTC+06:30), as requested. We assume Monday–Saturday, 09:00–18:00. Rooms, treatment names, prices and therapists are demonstration data. We need actual addresses, branch hours, closures, staff rosters, room/equipment suitability and existing future appointments before opening any real availability.

**Client identity and consent.** An appointment needs contact details. We propose keeping ID numbers and dates of birth out of phase-one booking storage. Confirm their specific consent-form purpose, access controls and retention with the clinic's responsible adviser before designing a separate clinical-consent workflow. Appointment booking alone does not constitute treatment consent.

## Receptionists' role

Receptionists are partners in the change. Ask one from a busy branch to help design the daily process and test the pilot. They will assist clients who prefer phone or WhatsApp, correct data and manage exceptional cancellations and refunds. The demonstration includes a protected branch diary for visibility. Assisted booking, roster editing and a full staff permissions system are additional work before a real clinic rollout.

Before launch, import or enter every future paper booking for the pilot branch and reconcile it with staff. Pick a clear cutover time and one authoritative diary. Maintain a written outage process: record requests, explain that times are provisional, then reconcile availability before confirming them. Do not promise a slot from a disconnected paper diary.

## A six-week delivery plan

| Week | Result and acceptance |
|---|---|
| 1 | Founder confirms the above policies; reception maps its daily process; inventory and future-booking data are collected. Freeze the first-release scope. |
| 2 | Agree the client journey and receptionist diary with staff. Load verified services, staff and rooms. Demonstrate the room-cleaning rule. |
| 3 | Complete reservation and membership integration. Prove simultaneous requests cannot reserve the same room or therapist. |
| 4 | Integrate the chosen payment provider, validate retry and webhook behaviour, and agree refunds and client notifications. |
| 5 | Pilot one branch with staff training, imported appointments, monitoring, backup/restore rehearsal and accessibility checks. Reconcile the diary daily. |
| 6 | Resolve pilot issues, obtain founder and reception sign-off, and expand gradually to the remaining branches. Keep a rollback decision point. |

The budget is fixed, but its amount is not given. This is a scope proposal, not a price commitment. If payment onboarding or data preparation slips, reduce the rollout to a supervised single-branch pilot; do not remove concurrency protection or silently launch simulated payment as a real service. Defer loyalty redesign, marketing automation, clinical records, advanced reports and self-service roster administration.

## Hosting and launch decision

The existing shared host cannot run the Java application. Keep the current marketing site and link to a separate booking service. The review build can run on a free Render web service with a free Neon PostgreSQL database. Free services have sleep and usage limits and are suitable for a demonstration. See the linked [deployment guide](deployment.md) for current provider limits and setup.

Real operation requires an agreed hosting budget, backups, an owner for alerts, proven restoration, secure staff access and the completed integrations above. Launch approval should follow observed tests for simultaneous bookings, cleaning time, duplicate payment attempts, expiry and cancellation boundaries, plus a staff rehearsal. Success means clients can book independently and reception can trust the diary.
