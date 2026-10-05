package com.anikettcodes.ims.service;

import com.anikettcodes.ims.dto.sku.CreateSkuRequest;
import com.anikettcodes.ims.dto.sku.SkuCategoryDto;
import com.anikettcodes.ims.dto.sku.SkuDto;
import com.anikettcodes.ims.dto.sku.SkuUnitDto;
import com.anikettcodes.ims.entity.Category;
import com.anikettcodes.ims.entity.Sku;
import com.anikettcodes.ims.entity.Unit;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.exception.UnexpectedException;
import com.anikettcodes.ims.repository.CategoryRepo;
import com.anikettcodes.ims.repository.InventoryLotRepo;
import com.anikettcodes.ims.repository.SkuRepo;
import com.anikettcodes.ims.repository.UnitRepo;
import com.anikettcodes.ims.util.enums.SkuStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkuService {
    private final SkuRepo skuRepo;
    private final CategoryRepo categoryRepo;
    private final UnitRepo unitRepo;
    private final InventoryLotRepo inventoryLotRepo;

    @Transactional(readOnly = true)
    public List<SkuDto> getSkus(String q, SkuStatus status, UUID categoryId) {
        String query = normalizeSearch(q);
        List<Sku> skus = skuRepo.search(query, status, categoryId);
        Map<UUID, BigDecimal> onHandBySku = onHandBySkuIds(skus.stream().map(Sku::getId).toList());
        return skus.stream()
                .map(sku -> toDto(sku, onHandBySku.getOrDefault(sku.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SkuDto getSku(UUID id) {
        Sku sku = skuRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new DataDoesNotExist("SKU does not exist"));
        return toDto(sku, inventoryLotRepo.sumQtyBySkuId(id));
    }

    @Transactional
    public SkuDto saveSku(CreateSkuRequest req) {
        String displayName = normalizeRequired(req.displayName(), "SKU display name is required");
        BigDecimal mrp = requireNonNegativeMrp(req.mrp());
        String barcode = normalizeBarcode(req.barcode());
        Category category = requireCategory(req.categoryId());
        Unit unit = requireUnit(req.unitId());
        if (barcode != null && skuRepo.existsByBarcodeIgnoreCase(barcode)) {
            throw new DataAlreadyExistException("SKU barcode already exists");
        }
        Sku sku = new Sku();
        sku.setProductCode(nextProductCode());
        sku.setDisplayName(displayName);
        sku.setCategory(category);
        sku.setUnit(unit);
        sku.setMrp(mrp);
        sku.setStatus(SkuStatus.ACTIVE);
        sku.setBarcode(barcode);
        try {
            Sku saved = skuRepo.saveAndFlush(sku);
            return toDto(saved, BigDecimal.ZERO);
        } catch (DataIntegrityViolationException ex) {
            throw new DataAlreadyExistException("SKU already exists");
        }
    }

    @Transactional
    public SkuDto putSku(CreateSkuRequest req, UUID id) {
        Sku sku = skuRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new DataDoesNotExist("SKU does not exist"));
        String displayName = normalizeRequired(req.displayName(), "SKU display name is required");
        BigDecimal mrp = requireNonNegativeMrp(req.mrp());
        String barcode = normalizeBarcode(req.barcode());
        Category category = requireCategory(req.categoryId());
        Unit unit = requireUnit(req.unitId());
        if (barcode != null && skuRepo.existsByBarcodeIgnoreCaseAndIdNot(barcode, id)) {
            throw new DataAlreadyExistException("SKU barcode already exists");
        }
        sku.setDisplayName(displayName);
        sku.setCategory(category);
        sku.setUnit(unit);
        sku.setMrp(mrp);
        sku.setBarcode(barcode);
        try {
            Sku saved = skuRepo.saveAndFlush(sku);
            return toDto(saved, inventoryLotRepo.sumQtyBySkuId(id));
        } catch (DataIntegrityViolationException ex) {
            throw new DataAlreadyExistException("SKU already exists");
        }
    }

    @Transactional
    public void disableSku(UUID id) {
        Sku sku = skuRepo.findById(id)
                .orElseThrow(() -> new DataDoesNotExist("SKU does not exist"));
        if (sku.getStatus() == SkuStatus.INACTIVE) {
            return;
        }
        BigDecimal onHand = inventoryLotRepo.sumQtyBySkuId(id);
        if (onHand.compareTo(BigDecimal.ZERO) > 0) {
            throw new DataInUseException("SKU has inventory and cannot be disabled");
        }
        sku.setStatus(SkuStatus.INACTIVE);
        skuRepo.saveAndFlush(sku);
    }

    private Category requireCategory(UUID categoryId) {
        if (categoryId == null) {
            throw new InvalidDataException("Category is required");
        }
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new DataDoesNotExist("Category does not exist"));
    }

    private Unit requireUnit(UUID unitId) {
        if (unitId == null) {
            throw new InvalidDataException("Unit is required");
        }
        return unitRepo.findById(unitId)
                .orElseThrow(() -> new DataDoesNotExist("Unit does not exist"));
    }

    private String nextProductCode() {
        Long seq = skuRepo.nextProductCode();
        if (seq == null || seq < 1 || seq > 99_999) {
            throw new UnexpectedException("Product code sequence is exhausted");
        }
        return String.format("SS-%05d", seq);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(message);
        }
        return value.trim();
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        return barcode.trim();
    }

    private String normalizeSearch(String q) {
        if (q == null || q.isBlank()) {
            return "";
        }
        return q.trim();
    }

    private BigDecimal requireNonNegativeMrp(BigDecimal mrp) {
        if (mrp == null) {
            throw new InvalidDataException("MRP is required");
        }
        if (mrp.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDataException("MRP must be greater than or equal to 0");
        }
        return mrp;
    }

    private Map<UUID, BigDecimal> onHandBySkuIds(List<UUID> skuIds) {
        Map<UUID, BigDecimal> onHand = new HashMap<>();
        if (skuIds.isEmpty()) {
            return onHand;
        }
        for (Object[] row : inventoryLotRepo.sumQtyBySkuIds(skuIds)) {
            onHand.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return onHand;
    }

    private SkuDto toDto(Sku sku, BigDecimal onHandQty) {
        return new SkuDto(
                sku.getId(),
                sku.getProductCode(),
                sku.getDisplayName(),
                new SkuCategoryDto(sku.getCategory().getId(), sku.getCategory().getName()),
                new SkuUnitDto(sku.getUnit().getId(), sku.getUnit().getCode(), sku.getUnit().getName()),
                sku.getMrp(),
                sku.getStatus(),
                sku.getBarcode(),
                onHandQty
        );
    }
}
