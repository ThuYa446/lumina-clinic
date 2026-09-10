-- Fictional catalogue for the take-home demonstration; no real clinic contact details.
INSERT INTO branches (id, name, address) VALUES
('10000000-0000-0000-0000-000000000001', 'Ari', 'Ari, Bangkok · Demonstration branch'),
('10000000-0000-0000-0000-000000000002', 'Central', 'Pathum Wan, Bangkok · Demonstration branch'),
('10000000-0000-0000-0000-000000000003', 'Riverside', 'Charoen Krung, Bangkok · Demonstration branch'),
('10000000-0000-0000-0000-000000000004', 'Sathorn', 'Sathorn, Bangkok · Demonstration branch'),
('10000000-0000-0000-0000-000000000005', 'Sukhumvit', 'Sukhumvit, Bangkok · Demonstration branch'),
('10000000-0000-0000-0000-000000000006', 'Thonglor', 'Thonglor, Bangkok · Demonstration branch');

INSERT INTO treatments (id, name, description, duration_minutes, price) VALUES
('20000000-0000-0000-0000-000000000001', 'Express facial', 'A gentle cleanse and hydrating facial for a moment of calm.', 30, 900.00),
('20000000-0000-0000-0000-000000000002', 'Signature facial', 'A personalised facial ritual with cleansing, massage and hydration.', 60, 1600.00),
('20000000-0000-0000-0000-000000000003', 'Restorative body ritual', 'An extended relaxation treatment with a soothing body massage.', 90, 2400.00);

INSERT INTO rooms (id, branch_id, name)
SELECT ('30000000-0000-0000-0000-' || lpad((branch_number * 10 + room_number)::text, 12, '0'))::uuid,
       ('10000000-0000-0000-0000-' || lpad(branch_number::text, 12, '0'))::uuid,
       'Suite ' || room_number
FROM generate_series(1, 6) AS branch_number
CROSS JOIN generate_series(1, 2) AS room_number;

INSERT INTO therapists (id, branch_id, name, turnaround_minutes) VALUES
('40000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'Anya', 0),
('40000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'Mali', 15),
('40000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'Narin', 15),
('40000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', 'Dara', 0),
('40000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', 'Kanya', 15),
('40000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000002', 'Pim', 15),
('40000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000003', 'Arun', 0),
('40000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000003', 'Lila', 15),
('40000000-0000-0000-0000-000000000033', '10000000-0000-0000-0000-000000000003', 'Nisa', 15),
('40000000-0000-0000-0000-000000000041', '10000000-0000-0000-0000-000000000004', 'Chai', 0),
('40000000-0000-0000-0000-000000000042', '10000000-0000-0000-0000-000000000004', 'Fah', 15),
('40000000-0000-0000-0000-000000000043', '10000000-0000-0000-0000-000000000004', 'Siri', 15),
('40000000-0000-0000-0000-000000000051', '10000000-0000-0000-0000-000000000005', 'June', 0),
('40000000-0000-0000-0000-000000000052', '10000000-0000-0000-0000-000000000005', 'Mina', 15),
('40000000-0000-0000-0000-000000000053', '10000000-0000-0000-0000-000000000005', 'Tawan', 15),
('40000000-0000-0000-0000-000000000061', '10000000-0000-0000-0000-000000000006', 'Bua', 0),
('40000000-0000-0000-0000-000000000062', '10000000-0000-0000-0000-000000000006', 'Lalin', 15),
('40000000-0000-0000-0000-000000000063', '10000000-0000-0000-0000-000000000006', 'Ploy', 15);

-- Everyone provides both facials. The longer body treatment requires a senior qualification.
INSERT INTO therapist_treatments (therapist_id, treatment_id)
SELECT therapist.id, treatment.id
FROM therapists therapist CROSS JOIN treatments treatment
WHERE treatment.duration_minutes <= 60 OR therapist.turnaround_minutes = 0;
