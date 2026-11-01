CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE recordings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    file_size_bytes BIGINT NOT NULL,
    audio_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recordings_stored_filename UNIQUE (stored_filename),
    INDEX idx_recordings_user_id (user_id),
    CONSTRAINT fk_recordings_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recording_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at DATETIME(6),
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_jobs_recording_id (recording_id),
    CONSTRAINT fk_jobs_recording FOREIGN KEY (recording_id)
        REFERENCES recordings (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE transcript_segments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recording_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    start_seconds DECIMAL(12, 3) NOT NULL,
    end_seconds DECIMAL(12, 3) NOT NULL,
    transcript_text TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_segments_recording_index UNIQUE (recording_id, segment_index),
    CONSTRAINT chk_segments_nonnegative CHECK (start_seconds >= 0),
    CONSTRAINT chk_segments_time_order CHECK (end_seconds >= start_seconds),
    CONSTRAINT fk_segments_recording FOREIGN KEY (recording_id)
        REFERENCES recordings (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recording_id BIGINT NOT NULL,
    source_segment_id BIGINT NOT NULL,
    note_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    owner VARCHAR(255),
    due_date DATE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notes_recording_id (recording_id),
    INDEX idx_notes_source_segment_id (source_segment_id),
    CONSTRAINT chk_notes_type CHECK (note_type IN ('DECISION', 'TASK')),
    CONSTRAINT fk_notes_recording FOREIGN KEY (recording_id)
        REFERENCES recordings (id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_source_segment FOREIGN KEY (source_segment_id)
        REFERENCES transcript_segments (id) ON DELETE CASCADE
) ENGINE=InnoDB;
