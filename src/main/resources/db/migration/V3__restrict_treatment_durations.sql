ALTER TABLE treatments ADD CONSTRAINT treatments_supported_duration
    CHECK (duration_minutes IN (30, 60, 90));
