package com.anikettcodes.ims.dto.supplier;

public record CreateSupplierRequest(
        String name,
        String contact,
        String corpName,
        String pocName
) {
}
