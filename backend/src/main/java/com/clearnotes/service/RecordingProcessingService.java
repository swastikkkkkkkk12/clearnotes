package com.clearnotes.service;

import com.clearnotes.service.ProcessingJobService.ProcessingInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecordingProcessingService {
    private static final Logger log = LoggerFactory.getLogger(RecordingProcessingService.class);
    private final ProcessingJobService jobs;
    private final AudioStorageService storage;
    private final TranscriptionClient transcription;
    private final NoteExtractionService noteExtraction;

    public RecordingProcessingService(ProcessingJobService jobs, AudioStorageService storage,
                                      TranscriptionClient transcription, NoteExtractionService noteExtraction) {
        this.jobs = jobs;
        this.storage = storage;
        this.transcription = transcription;
        this.noteExtraction = noteExtraction;
    }

    public void process(Long jobId) {
        ProcessingInput input = jobs.beginTranscription(jobId);
        if (input == null) return; // Ignore duplicate delivery after the job has already started.
        try {
            var response = transcription.transcribe(storage.pathFor(input.storedFilename()));
            jobs.saveTranscriptAndEnterExtraction(jobId, response.segments());
            noteExtraction.extract(input.recordingId());
            jobs.complete(jobId);
        } catch (Exception failure) {
            log.error("Audio processing failed for job {}", jobId, failure);
            try { jobs.fail(jobId); }
            catch (Exception updateFailure) { log.error("Could not mark job {} as failed", jobId, updateFailure); }
        } finally {
            if (input.deleteAudioAfterProcessing() && storage.delete(input.storedFilename())) {
                try { jobs.markAudioDeleted(input.recordingId()); }
                catch (Exception updateFailure) { log.error("Could not record audio deletion for job {}", jobId, updateFailure); }
            }
        }
    }
}
