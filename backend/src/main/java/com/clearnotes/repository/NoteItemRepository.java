package com.clearnotes.repository;

import com.clearnotes.entity.NoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteItemRepository extends JpaRepository<NoteItem, Long> {
    void deleteAllByRecording_Id(Long recordingId);
    List<NoteItem> findAllByRecording_IdAndRecording_User_IdOrderByIdAsc(Long recordingId, Long userId);
}
