package com.clearnotes.dto.recording;

import java.util.List;

public record RecordingPageResponse(List<RecordingListItemResponse> content, int page, int size,
                                    long totalElements, int totalPages) { }
