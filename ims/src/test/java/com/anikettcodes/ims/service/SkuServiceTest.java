package com.anikettcodes.ims.service;

import com.anikettcodes.ims.dto.sku.CreateSkuRequest;
import com.anikettcodes.ims.dto.sku.SkuDto;
import com.anikettcodes.ims.entity.Category;
import com.anikettcodes.ims.entity.Sku;
import com.anikettcodes.ims.entity.Unit;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.repository.CategoryRepo;
import com.anikettcodes.ims.repository.InventoryLotRepo;
import com.anikettcodes.ims.repository.SkuRepo;
import com.anikettcodes.ims.repository.UnitRepo;
import com.anikettcodes.ims.util.enums.SkuStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock
    private SkuRepo skuRepo;
    @Mock
    private CategoryRepo categoryRepo;
    @Mock
    private UnitRepo unitRepo;
    @Mock
    private InventoryLotRepo inventoryLotRepo;

    @InjectMocks
    private SkuService service;

    @Test
    void createTrimsFieldsDefaultsActiveAndStoresNullBarcodeWhenBlank() {
        UUID categoryId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Category category = category(categoryId, "Cakes");
        Unit unit = unit(unitId, "PCS", "Piece");
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(category));
        when(unitRepo.findById(unitId)).thenReturn(Optional.of(unit));
        when(skuRepo.nextProductCode()).thenReturn(1L);
        when(skuRepo.saveAndFlush(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SkuDto result = service.saveSku(new CreateSkuRequest(
                "  Vanilla Cake  ",
                categoryId,
                unitId,
                new BigDecimal("899.00"),
                "   "
        ));

        ArgumentCaptor<Sku> captor = ArgumentCaptor.forClass(Sku.class);
        verify(skuRepo).saveAndFlush(captor.capture());
        Sku saved = captor.getValue();
        assertEquals("SS-00001", saved.getProductCode());
        assertEquals("Vanilla Cake", saved.getDisplayName());
        assertEquals(SkuStatus.ACTIVE, saved.getStatus());
        assertNull(saved.getBarcode());
        assertEquals("SS-00001", result.productCode());
        assertEquals(BigDecimal.ZERO, result.onHandQty());
    }

    @Test
    void createRejectsBlankDisplayName() {
        assertThrows(InvalidDataException.class, () -> service.saveSku(new CreateSkuRequest(
                "  ",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10"),
                null
        )));
        verify(skuRepo, never()).nextProductCode();
        verify(skuRepo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsNegativeMrp() {
        assertThrows(InvalidDataException.class, () -> service.saveSku(new CreateSkuRequest(
                "Vanilla Cake",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("-1"),
                null
        )));
        verify(skuRepo, never()).nextProductCode();
        verify(skuRepo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateBarcode() {
        UUID categoryId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(category(categoryId, "Cakes")));
        when(unitRepo.findById(unitId)).thenReturn(Optional.of(unit(unitId, "PCS", "Piece")));
        when(skuRepo.existsByBarcodeIgnoreCase("8900000000011")).thenReturn(true);

        assertThrows(DataAlreadyExistException.class, () -> service.saveSku(new CreateSkuRequest(
                "Vanilla Cake",
                categoryId,
                unitId,
                new BigDecimal("10"),
                "8900000000011"
        )));
        verify(skuRepo, never()).nextProductCode();
        verify(skuRepo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMissingCategory() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(DataDoesNotExist.class, () -> service.saveSku(new CreateSkuRequest(
                "Vanilla Cake",
                categoryId,
                UUID.randomUUID(),
                new BigDecimal("10"),
                null
        )));
        verify(skuRepo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMissingUnit() {
        UUID categoryId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(category(categoryId, "Cakes")));
        when(unitRepo.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(DataDoesNotExist.class, () -> service.saveSku(new CreateSkuRequest(
                "Vanilla Cake",
                categoryId,
                unitId,
                new BigDecimal("10"),
                null
        )));
        verify(skuRepo, never()).saveAndFlush(any());
    }

    @Test
    void updateKeepsStatusAndChangesDisplayName() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Sku existing = sku(id, "SS-00001", "Old name", categoryId, unitId, SkuStatus.ACTIVE);
        when(skuRepo.findByIdWithRelations(id)).thenReturn(Optional.of(existing));
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(existing.getCategory()));
        when(unitRepo.findById(unitId)).thenReturn(Optional.of(existing.getUnit()));
        when(skuRepo.saveAndFlush(existing)).thenReturn(existing);
        when(inventoryLotRepo.sumQtyBySkuId(id)).thenReturn(new BigDecimal("8"));

        SkuDto result = service.putSku(new CreateSkuRequest(
                "  Vanilla Celebration Cake  ",
                categoryId,
                unitId,
                new BigDecimal("899.00"),
                "8900000000011"
        ), id);

        assertEquals("SS-00001", existing.getProductCode());
        assertEquals("SS-00001", result.productCode());
        assertEquals("Vanilla Celebration Cake", result.displayName());
        assertEquals(SkuStatus.ACTIVE, existing.getStatus());
        assertEquals(new BigDecimal("8"), result.onHandQty());
        verify(skuRepo, never()).nextProductCode();
    }

    @Test
    void disableBlockedWhenOnHandGreaterThanZero() {
        UUID id = UUID.randomUUID();
        Sku existing = sku(id, "SS-00001", "Vanilla Cake", UUID.randomUUID(), UUID.randomUUID(), SkuStatus.ACTIVE);
        when(skuRepo.findById(id)).thenReturn(Optional.of(existing));
        when(inventoryLotRepo.sumQtyBySkuId(id)).thenReturn(new BigDecimal("8.000"));

        assertThrows(DataInUseException.class, () -> service.disableSku(id));
        verify(skuRepo, never()).saveAndFlush(any());
        assertEquals(SkuStatus.ACTIVE, existing.getStatus());
    }

    @Test
    void disableSucceedsWhenOnHandIsZero() {
        UUID id = UUID.randomUUID();
        Sku existing = sku(id, "SS-00009", "Seasonal Rose Cake", UUID.randomUUID(), UUID.randomUUID(), SkuStatus.ACTIVE);
        when(skuRepo.findById(id)).thenReturn(Optional.of(existing));
        when(inventoryLotRepo.sumQtyBySkuId(id)).thenReturn(BigDecimal.ZERO);
        when(skuRepo.saveAndFlush(existing)).thenReturn(existing);

        service.disableSku(id);

        assertEquals(SkuStatus.INACTIVE, existing.getStatus());
        verify(skuRepo).saveAndFlush(existing);
    }

    @Test
    void disableMissingSku() {
        UUID id = UUID.randomUUID();
        when(skuRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(DataDoesNotExist.class, () -> service.disableSku(id));
    }

    @Test
    void searchMatchesAndAttachesOnHand() {
        UUID id = UUID.randomUUID();
        Sku existing = sku(id, "SS-00001", "Vanilla Cake", UUID.randomUUID(), UUID.randomUUID(), SkuStatus.ACTIVE);
        when(skuRepo.search("vanilla", null, null)).thenReturn(List.of(existing));
        when(inventoryLotRepo.sumQtyBySkuIds(List.of(id))).thenReturn(List.<Object[]>of(new Object[]{id, new BigDecimal("8")}));

        List<SkuDto> result = service.getSkus("  vanilla  ", null, null);

        assertEquals(1, result.size());
        assertEquals("SS-00001", result.getFirst().productCode());
        assertEquals(new BigDecimal("8"), result.getFirst().onHandQty());
    }

    @Test
    void listWithoutSearchPassesEmptyStringNotNull() {
        when(skuRepo.search("", null, null)).thenReturn(List.of());

        service.getSkus(null, null, null);
        service.getSkus("  ", null, null);

        verify(skuRepo, times(2)).search("", null, null);
    }

    private static Sku sku(UUID id, String productCode, String displayName, UUID categoryId, UUID unitId, SkuStatus status) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setProductCode(productCode);
        sku.setDisplayName(displayName);
        sku.setCategory(category(categoryId, "Cakes"));
        sku.setUnit(unit(unitId, "PCS", "Piece"));
        sku.setMrp(new BigDecimal("899.00"));
        sku.setStatus(status);
        sku.setBarcode("8900000000011");
        return sku;
    }

    private static Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("");
        return category;
    }

    private static Unit unit(UUID id, String code, String name) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setCode(code);
        unit.setName(name);
        return unit;
    }
}
