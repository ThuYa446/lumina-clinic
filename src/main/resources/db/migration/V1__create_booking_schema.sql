-- Keep this extension in public so independently migrated application/test schemas can share it.
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE branches (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    address varchar(255) NOT NULL
);

CREATE TABLE treatments (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    description varchar(255) NOT NULL,
    duration_minutes integer NOT NULL CHECK (duration_minutes > 0),
    price numeric(10,2) NOT NULL CHECK (price >= 0)
);

CREATE TABLE rooms (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branches(id),
    name varchar(255) NOT NULL,
    UNIQUE (id, branch_id),
    UNIQUE (branch_id, name)
);

CREATE TABLE therapists (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branches(id),
    name varchar(255) NOT NULL,
    turnaround_minutes integer NOT NULL CHECK (turnaround_minutes >= 0),
    UNIQUE (id, branch_id)
);

CREATE TABLE therapist_treatments (
    therapist_id uuid NOT NULL REFERENCES therapists(id),
    treatment_id uuid NOT NULL REFERENCES treatments(id),
    PRIMARY KEY (therapist_id, treatment_id)
);

CREATE TABLE bookings (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    branch_id uuid NOT NULL REFERENCES branches(id),
    treatment_id uuid NOT NULL REFERENCES treatments(id),
    therapist_id uuid NOT NULL REFERENCES therapists(id),
    room_id uuid NOT NULL REFERENCES rooms(id),
    client_name varchar(120) NOT NULL,
    client_email varchar(254) NOT NULL,
    client_phone varchar(30) NOT NULL,
    member boolean NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    room_occupied_until timestamptz NOT NULL,
    therapist_occupied_until timestamptz NOT NULL,
    hold_expires_at timestamptz,
    created_at timestamptz NOT NULL,
    status varchar(30) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    payment_status varchar(30) NOT NULL CHECK (payment_status IN ('NOT_REQUIRED', 'UNPAID', 'PAID', 'REFUND_PENDING', 'REFUNDED')),
    deposit_amount numeric(10,2) NOT NULL CHECK (deposit_amount >= 0),
    currency varchar(3) NOT NULL,
    idempotency_key uuid NOT NULL UNIQUE,
    request_fingerprint varchar(64) NOT NULL,
    refund_reference varchar(120),
    FOREIGN KEY (room_id, branch_id) REFERENCES rooms(id, branch_id),
    FOREIGN KEY (therapist_id, branch_id) REFERENCES therapists(id, branch_id),
    FOREIGN KEY (therapist_id, treatment_id) REFERENCES therapist_treatments(therapist_id, treatment_id),
    CHECK (starts_at < ends_at),
    CHECK (room_occupied_until >= ends_at + interval '15 minutes'),
    CHECK (therapist_occupied_until >= ends_at),
    CHECK (status <> 'PENDING_PAYMENT' OR hold_expires_at IS NOT NULL),
    CHECK (payment_status NOT IN ('REFUND_PENDING', 'REFUNDED') OR status = 'CANCELLED'),
    CHECK (payment_status <> 'REFUNDED' OR refund_reference IS NOT NULL),
    CONSTRAINT bookings_no_room_overlap EXCLUDE USING gist
        (room_id WITH =, tstzrange(starts_at, room_occupied_until, '[)') WITH &&)
        WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED')),
    CONSTRAINT bookings_no_therapist_overlap EXCLUDE USING gist
        (therapist_id WITH =, tstzrange(starts_at, therapist_occupied_until, '[)') WITH &&)
        WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED'))
);

CREATE INDEX bookings_branch_start_idx ON bookings(branch_id, starts_at);
CREATE INDEX bookings_expiring_holds_idx ON bookings(branch_id, hold_expires_at) WHERE status = 'PENDING_PAYMENT';
CREATE INDEX bookings_refund_queue_idx ON bookings(starts_at) WHERE payment_status = 'REFUND_PENDING';

CREATE TABLE payment_transactions (
    id uuid PRIMARY KEY,
    booking_id uuid NOT NULL UNIQUE REFERENCES bookings(id),
    idempotency_key uuid NOT NULL UNIQUE,
    reference varchar(120) NOT NULL UNIQUE,
    amount numeric(10,2) NOT NULL CHECK (amount > 0),
    currency varchar(3) NOT NULL,
    mode varchar(16) NOT NULL,
    created_at timestamptz NOT NULL
);

COMMENT ON TABLE payment_transactions IS 'Demonstration payment receipts only. The DEMO adapter never moves money.';
