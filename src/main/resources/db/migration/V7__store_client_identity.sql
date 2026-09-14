-- Keep existing bookings valid; their identity details were never collected.
-- The create API requires both fields for all new bookings.
ALTER TABLE bookings
    ADD COLUMN client_id_number varchar(64),
    ADD COLUMN client_date_of_birth date,
    ADD CONSTRAINT bookings_client_identity_complete CHECK (
        (client_id_number IS NULL AND client_date_of_birth IS NULL)
        OR (client_id_number IS NOT NULL AND length(btrim(client_id_number)) > 0
            AND client_date_of_birth IS NOT NULL)
    );

COMMENT ON COLUMN bookings.client_id_number IS 'Client ID number for clinic consent records; preserve formatting and leading zeroes. NULL for legacy bookings.';
COMMENT ON COLUMN bookings.client_date_of_birth IS 'Date of birth, without a time zone, for clinic consent records. NULL for legacy bookings.';
