package com.clearnotes.repository;

import com.clearnotes.entity.Recording;
import com.clearnotes.dto.recording.RecordingListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecordingRepository extends JpaRepository<Recording, Long> {
    // Scope lookups to both the recording and its owner; never fetch by recording ID alone.
    Optional<Recording> findByIdAndUser_Id(Long recordingId, Long userId);

    @Query(value = "select new com.clearnotes.dto.recording.RecordingListItemResponse(" +
            "r.id, r.originalFilename, r.fileSizeBytes, r.createdAt, r.audioDeleted, j.id, j.status) " +
            "from Recording r left join ProcessingJob j on j.recording = r " +
            "where r.user.id = :userId order by r.createdAt desc, r.id desc",
            countQuery = "select count(r) from Recording r where r.user.id = :userId")
    Page<RecordingListItemResponse> findPageForUser(@Param("userId") Long userId, Pageable pageable);
}
