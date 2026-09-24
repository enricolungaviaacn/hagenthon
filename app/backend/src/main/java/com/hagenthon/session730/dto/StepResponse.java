package com.hagenthon.session730.dto;

public record StepResponse(
        int stepIndex,
        String stepName,
        String documentRequired,
        String documentDescription,
        boolean alreadyUploaded,
        String previewValue,
        boolean isCompleted
) {}
