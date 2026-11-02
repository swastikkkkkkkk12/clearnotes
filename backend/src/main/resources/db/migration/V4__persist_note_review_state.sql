ALTER TABLE notes
    ADD COLUMN reviewed_at DATETIME(6) NULL,
    ADD COLUMN dismissed_at DATETIME(6) NULL,
    ADD INDEX idx_notes_recording_dismissed (recording_id, dismissed_at, id);
