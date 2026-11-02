package com.clearnotes.repository;

import com.clearnotes.entity.DecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionRecordRepository extends JpaRepository<DecisionRecord, Long> {
    void deleteAllByRecording_Id(Long recordingId);

    List<DecisionRecord> findAllByRecording_IdAndRecording_User_IdOrderByCreatedAtAscIdAsc(
            Long recordingId, Long userId);

    Optional<DecisionRecord> findByIdAndRecording_IdAndRecording_User_Id(
            Long decisionId, Long recordingId, Long userId);
}
