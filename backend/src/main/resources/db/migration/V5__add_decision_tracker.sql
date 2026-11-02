CREATE TABLE decision_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recording_id BIGINT NOT NULL,
    source_segment_id BIGINT NOT NULL,
    decision_text TEXT NOT NULL,
    approved_by VARCHAR(255),
    follow_up_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_decisions_recording_created (recording_id, created_at, id),
    INDEX idx_decisions_source_segment (source_segment_id),
    CONSTRAINT chk_decisions_follow_up_status CHECK (follow_up_status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT fk_decisions_recording FOREIGN KEY (recording_id)
        REFERENCES recordings (id) ON DELETE CASCADE,
    CONSTRAINT fk_decisions_source_segment FOREIGN KEY (source_segment_id)
        REFERENCES transcript_segments (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Existing extracted decisions become initial tracker entries. Approver remains unknown.
INSERT INTO decision_records (recording_id, source_segment_id, decision_text, approved_by,
                              follow_up_status, created_at, updated_at)
SELECT n.recording_id, n.source_segment_id, n.content, NULL, 'OPEN', n.created_at, n.updated_at
FROM notes n
WHERE n.note_type = 'DECISION';
