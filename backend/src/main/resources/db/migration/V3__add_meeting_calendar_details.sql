ALTER TABLE notes DROP CHECK chk_notes_type;

ALTER TABLE notes
    ADD COLUMN meeting_date DATE NULL,
    ADD COLUMN meeting_start_time TIME NULL,
    ADD COLUMN meeting_end_time TIME NULL,
    ADD CONSTRAINT chk_notes_type CHECK (note_type IN ('DECISION', 'TASK', 'MEETING')),
    ADD CONSTRAINT chk_meeting_time_order CHECK (
        meeting_end_time IS NULL OR (meeting_start_time IS NOT NULL AND meeting_end_time > meeting_start_time)
    );
