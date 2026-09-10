-- Requested Myanmar kyat prices for the existing demonstration treatments.
-- Preserve previously applied migrations and all booking/payment records.
UPDATE treatments AS treatment
SET price = pricing.price
FROM (VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, 20000.00),
    ('20000000-0000-0000-0000-000000000002'::uuid, 45000.00),
    ('20000000-0000-0000-0000-000000000003'::uuid, 70000.00)
) AS pricing(id, price)
WHERE treatment.id = pricing.id;
