package com.clearnotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "transcript_segments")
public class TranscriptSegment extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recording_id", nullable = false)
    private Recording recording;

    @Column(name = "segment_index", nullable = false)
    private int segmentIndex;

    @Column(name = "start_seconds", nullable = false, precision = 12, scale = 3)
    private BigDecimal startSeconds;

    @Column(name = "end_seconds", nullable = false, precision = 12, scale = 3)
    private BigDecimal endSeconds;

    @Column(name = "transcript_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    protected TranscriptSegment() { }

    public TranscriptSegment(Recording recording, int segmentIndex, BigDecimal startSeconds,
                             BigDecimal endSeconds, String text) {
        this.recording = recording;
        this.segmentIndex = segmentIndex;
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
        this.text = text;
    }

    public Recording getRecording() { return recording; }
    public int getSegmentIndex() { return segmentIndex; }
    public BigDecimal getStartSeconds() { return startSeconds; }
    public BigDecimal getEndSeconds() { return endSeconds; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
