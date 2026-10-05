package com.anikettcodes.ims.dto.sku;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSkuRequest(
        String displayName,
        UUID categoryId,
        UUID unitId,
        BigDecimal mrp,
        String barcode
) {
}
