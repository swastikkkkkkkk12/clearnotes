package com.clearnotes.dto.recording;

import com.clearnotes.entity.NoteType;

import java.time.LocalDate;
import java.time.LocalTime;

public record NoteItemResponse(Long id, NoteType type, String content, String owner,
                               LocalDate dueDate, Long sourceSegmentId, LocalDate meetingDate,
                               LocalTime meetingStartTime, LocalTime meetingEndTime,
                               String calendarUrl, String calendarMessage) { }
