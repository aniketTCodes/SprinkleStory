package com.anikettcodes.ims.dto.sku;

import java.util.UUID;

public record SkuUnitDto(
        UUID id,
        String code,
        String name
) {
}
