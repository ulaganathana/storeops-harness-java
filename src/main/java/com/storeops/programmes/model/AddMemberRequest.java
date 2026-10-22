package com.storeops.programmes.model;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(@NotBlank String staffId) {
}
