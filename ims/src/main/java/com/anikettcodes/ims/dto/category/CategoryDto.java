package com.anikettcodes.ims.dto.category;

import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        String description
) {
}
