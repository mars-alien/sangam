package com.gatherup.dto.request;

import jakarta.validation.constraints.Size;

public record SendJoinRequestRequest(
        @Size(max = 300) String message
) {}
