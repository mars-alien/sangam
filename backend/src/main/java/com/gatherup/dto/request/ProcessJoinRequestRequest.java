package com.gatherup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProcessJoinRequestRequest(
        @NotBlank
        @Pattern(regexp = "^(APPROVE|REJECT)$", message = "action must be APPROVE or REJECT")
        String action
) {}
