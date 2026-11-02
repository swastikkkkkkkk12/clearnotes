package com.clearnotes.repository;

import com.clearnotes.entity.NoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteItemRepository extends JpaRepository<NoteItem, Long> {
    void deleteAllByRecording_Id(Long recordingId);
    List<NoteItem> findAllByRecording_IdAndRecording_User_IdOrderByIdAsc(Long recordingId, Long userId);
    Optional<NoteItem> findByIdAndRecording_IdAndRecording_User_Id(Long noteId, Long recordingId, Long userId);

    @Query("select n from NoteItem n where n.recording.id = :recordingId " +
            "and n.recording.user.id = :userId " +
            "and (:includeDismissed = true or n.dismissedAt is null) order by n.id asc")
    List<NoteItem> findVisibleForUser(@Param("recordingId") Long recordingId,
                                      @Param("userId") Long userId,
                                      @Param("includeDismissed") boolean includeDismissed);
}
