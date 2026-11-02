package com.clearnotes.repository;

import com.clearnotes.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {
    Optional<ProcessingJob> findByIdAndRecording_User_Id(Long jobId, Long userId);
}
