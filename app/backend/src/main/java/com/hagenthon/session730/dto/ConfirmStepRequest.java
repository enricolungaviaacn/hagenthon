package com.hagenthon.session730.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmStepRequest(@NotNull String confirmedValue) {}
