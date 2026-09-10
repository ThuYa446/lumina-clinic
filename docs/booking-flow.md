# Client booking flow

The diagram describes the implemented first cut. “Pay” is an explicitly labelled simulation; live provider integration remains a launch requirement. The database transaction, rather than the availability preview, decides whether a slot is still available. A [standalone SVG](booking-flow.svg) is also available for viewing without Mermaid support.

```mermaid
flowchart TD
    A([Open Lumina booking]) --> B[Choose treatment and branch]
    B --> C[Choose any therapist or preferred therapist]
    C --> D[Choose date]
    D --> E[Request availability; expire stale holds]
    E --> F{A suitable room and therapist<br/>are free, including turnaround?}
    F -- No --> D
    F -- Yes --> G[Choose an offered time]
    G --> H[Enter contact details; review cancellation and deposit terms]
    H --> I[Submit booking with a stable idempotency key]
    I --> J[In one transaction: expire holds,<br/>recheck resources, reserve room and therapist]
    J --> K{Slot still available?}
    K -- No: conflict --> E
    K -- Yes --> L{Membership result?}
    L -- Code and email verified --> M[Confirm; deposit waived]
    L -- No code supplied --> N[Hold slot for 15 minutes;<br/>show MMK 300 deposit]
    L -- Supplied code or email invalid --> H
    N --> O{Client completes demo payment?}
    O -- No: leave or retry later --> P{Hold still active?}
    P -- Yes --> N
    P -- No --> Q[Expire reservation on next request;<br/>release room and therapist]
    Q --> E
    O -- Yes --> R[Lock booking and check hold;<br/>record at most one deposit]
    R --> S{Hold valid?}
    S -- No --> Q
    S -- Yes --> T[Confirm booking; retries return same payment result]
    M --> U([Show confirmation and private booking access])
    T --> U
    U --> V{Client requests cancellation?}
    V -- No --> W([Attend appointment])
    V -- Yes --> X{At least 24 hours before start?}
    X -- No --> Y[Keep reservation; contact reception]
    X -- Yes --> Z[Cancel and release resources;<br/>paid deposit marked refund pending]
```

Room occupancy is `[start, treatment end + 15 minutes)`. Therapist occupancy is `[start, treatment end + therapist turnaround)`. These independent intervals explain why a senior facialist can work back to back while a room still requires cleaning.

No scheduled worker is required to release capacity: booking and availability requests apply expiry using server time. Expired rows may remain marked held until such a request, but they must not prevent an eligible new booking after cleanup.
