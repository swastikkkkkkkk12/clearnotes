package com.clearnotes.dto.recording;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateNoteRequest(
        @NotBlank @Size(max = 10000) String content,
        @Size(max = 255) String owner,
        LocalDate dueDate,
        LocalDate meetingDate,
        LocalTime meetingStartTime,
        LocalTime meetingEndTime) {

    @AssertTrue(message = "meetingEndTime must be after meetingStartTime")
    public boolean isMeetingTimeRangeValid() {
        return meetingStartTime == null || meetingEndTime == null || meetingEndTime.isAfter(meetingStartTime);
    }
}
