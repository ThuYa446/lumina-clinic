-- Fictional Yangon locations requested for the demonstration.
-- Keep branch IDs and existing booking/payment records intact on upgrades.
UPDATE branches AS branch
SET name = location.name,
    address = location.name || ', Yangon, Myanmar · Demonstration branch'
FROM (VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'Bahan'),
    ('10000000-0000-0000-0000-000000000002'::uuid, 'Kamayut'),
    ('10000000-0000-0000-0000-000000000003'::uuid, 'Sanchaung'),
    ('10000000-0000-0000-0000-000000000004'::uuid, 'Tamwe'),
    ('10000000-0000-0000-0000-000000000005'::uuid, 'Thingangyun'),
    ('10000000-0000-0000-0000-000000000006'::uuid, 'Yankin')
) AS location(id, name)
WHERE branch.id = location.id;
