package com.littletrip.api.dto;

import jakarta.validation.constraints.Size;

public record ApiKeyUpdateRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        Boolean active
    ) {}
