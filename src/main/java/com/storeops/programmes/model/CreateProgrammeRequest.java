package com.storeops.programmes.model;

import jakarta.validation.constraints.NotBlank;

public record CreateProgrammeRequest(@NotBlank String name) { }
