package com.anikettcodes.ims.dto.sku;

import com.anikettcodes.ims.util.enums.SkuStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SkuDto(
        UUID id,
        String productCode,
        String displayName,
        SkuCategoryDto category,
        SkuUnitDto unit,
        BigDecimal mrp,
        SkuStatus status,
        String barcode,
        BigDecimal onHandQty
) {
}
