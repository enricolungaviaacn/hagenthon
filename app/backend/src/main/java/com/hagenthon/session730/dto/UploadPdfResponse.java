package com.hagenthon.session730.dto;

import java.util.UUID;

public record UploadPdfResponse(UUID sessionId, String currentStep, int currentStepIndex) {}
