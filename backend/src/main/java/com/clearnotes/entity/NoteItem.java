package com.clearnotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "notes")
public class NoteItem extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recording_id", nullable = false)
    private Recording recording;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_segment_id", nullable = false)
    private TranscriptSegment sourceSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 20)
    private NoteType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private String owner;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Column(name = "meeting_start_time")
    private LocalTime meetingStartTime;

    @Column(name = "meeting_end_time")
    private LocalTime meetingEndTime;

    protected NoteItem() { }

    public NoteItem(Recording recording, TranscriptSegment sourceSegment, NoteType type,
                    String content, String owner, LocalDate dueDate, LocalDate meetingDate,
                    LocalTime meetingStartTime, LocalTime meetingEndTime) {
        this.recording = recording;
        this.sourceSegment = sourceSegment;
        this.type = type;
        this.content = content;
        this.owner = owner;
        this.dueDate = dueDate;
        this.meetingDate = meetingDate;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
    }

    public Recording getRecording() { return recording; }
    public TranscriptSegment getSourceSegment() { return sourceSegment; }
    public NoteType getType() { return type; }
    public void setType(NoteType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getMeetingDate() { return meetingDate; }
    public LocalTime getMeetingStartTime() { return meetingStartTime; }
    public LocalTime getMeetingEndTime() { return meetingEndTime; }
}
