package com.clearnotes.dto.recording;

import jakarta.validation.constraints.NotNull;

public record SetNoteStateRequest(@NotNull Boolean reviewed, @NotNull Boolean dismissed) { }
