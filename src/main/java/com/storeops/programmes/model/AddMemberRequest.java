package com.storeops.programmes.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank String staffId, @NotNull ProjectRole role) { }
