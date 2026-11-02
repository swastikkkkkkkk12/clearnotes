package com.clearnotes.repository;

import com.clearnotes.entity.NoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteItemRepository extends JpaRepository<NoteItem, Long> {
    void deleteAllByRecording_Id(Long recordingId);
    List<NoteItem> findAllByRecording_IdAndRecording_User_IdOrderByIdAsc(Long recordingId, Long userId);
    Optional<NoteItem> findByIdAndRecording_IdAndRecording_User_Id(Long noteId, Long recordingId, Long userId);
}
