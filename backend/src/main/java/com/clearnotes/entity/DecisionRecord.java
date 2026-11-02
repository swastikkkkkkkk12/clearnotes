package com.clearnotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "decision_records")
public class DecisionRecord extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recording_id", nullable = false)
    private Recording recording;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_segment_id", nullable = false)
    private TranscriptSegment sourceSegment;

    @Column(name = "decision_text", nullable = false, columnDefinition = "TEXT")
    private String decision;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_status", nullable = false, length = 20)
    private FollowUpStatus followUpStatus;

    protected DecisionRecord() { }

    public DecisionRecord(Recording recording, TranscriptSegment sourceSegment, String decision) {
        this.recording = recording;
        this.sourceSegment = sourceSegment;
        this.decision = decision;
        this.followUpStatus = FollowUpStatus.OPEN;
    }

    public Recording getRecording() { return recording; }
    public TranscriptSegment getSourceSegment() { return sourceSegment; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public FollowUpStatus getFollowUpStatus() { return followUpStatus; }
    public void setFollowUpStatus(FollowUpStatus followUpStatus) { this.followUpStatus = followUpStatus; }
}
