package com.clearnotes.repository;

import com.clearnotes.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecordingRepository extends JpaRepository<Recording, Long> {
    // Scope lookups to both the recording and its owner; never fetch by recording ID alone.
    Optional<Recording> findByIdAndUser_Id(Long recordingId, Long userId);
}
