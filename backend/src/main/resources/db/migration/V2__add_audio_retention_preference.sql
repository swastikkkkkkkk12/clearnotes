ALTER TABLE recordings
    ADD COLUMN delete_audio_after_processing BOOLEAN NOT NULL DEFAULT FALSE
    AFTER audio_deleted;
