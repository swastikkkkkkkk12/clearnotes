package com.clearnotes.repository;

import com.clearnotes.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {
    void deleteAllByRecording_Id(Long recordingId);
    List<TranscriptSegment> findAllByRecording_IdOrderBySegmentIndexAsc(Long recordingId);
}
