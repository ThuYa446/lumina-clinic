-- Restore the previous catalogue prices while preserving migration history.
-- Existing booking and payment records are unchanged.
UPDATE treatments AS treatment
SET price = pricing.price
FROM (VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, 900.00),
    ('20000000-0000-0000-0000-000000000002'::uuid, 1600.00),
    ('20000000-0000-0000-0000-000000000003'::uuid, 2400.00)
) AS pricing(id, price)
WHERE treatment.id = pricing.id;
