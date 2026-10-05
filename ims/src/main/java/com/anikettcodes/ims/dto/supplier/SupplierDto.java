package com.anikettcodes.ims.dto.supplier;

import java.util.UUID;

public record SupplierDto(
        UUID id,
        String name,
        String contact,
        String corpName,
        String pocName
) {
}
