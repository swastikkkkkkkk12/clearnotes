package com.clearnotes.repository;

import com.clearnotes.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {
    void deleteAllByRecording_Id(Long recordingId);
}
