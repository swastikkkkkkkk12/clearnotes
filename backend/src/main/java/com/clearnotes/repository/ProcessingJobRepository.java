package com.clearnotes.repository;

import com.clearnotes.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> { }
