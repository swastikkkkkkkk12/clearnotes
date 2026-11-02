package com.clearnotes.event;

import com.clearnotes.service.RecordingProcessingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecordingQueuedListener {
    private final RecordingProcessingService processing;

    public RecordingQueuedListener(RecordingProcessingService processing) { this.processing = processing; }

    @Async("recordingProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(RecordingQueuedEvent event) {
        processing.process(event.jobId());
    }
}
