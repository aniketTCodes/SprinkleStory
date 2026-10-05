package com.anikettcodes.ims.controller;

import com.anikettcodes.ims.dto.sku.SkuCategoryDto;
import com.anikettcodes.ims.dto.sku.SkuDto;
import com.anikettcodes.ims.dto.sku.SkuUnitDto;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.GlobalExceptionHandler;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.service.SkuService;
import com.anikettcodes.ims.util.enums.SkuStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SkuController.class)
@Import(GlobalExceptionHandler.class)
class SkuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkuService service;

    @Test
    void createReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.saveSku(any())).thenReturn(skuDto(id));

        mockMvc.perform(post("/api/v1/sku")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Vanilla Cake",
                                  "categoryId": "%s",
                                  "unitId": "%s",
                                  "mrp": 899.00,
                                  "barcode": "8900000000011"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").value("SS-00001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        when(service.saveSku(any())).thenThrow(new InvalidDataException("SKU display name is required"));

        mockMvc.perform(post("/api/v1/sku")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "  ",
                                  "categoryId": "%s",
                                  "unitId": "%s",
                                  "mrp": 899.00
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("SKU display name is required"));
    }

    @Test
    void disableReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/sku/{id}/disable", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void disableInStockReturnsConflict() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DataInUseException("SKU has inventory and cannot be disabled")).when(service).disableSku(id);

        mockMvc.perform(post("/api/v1/sku/{id}/disable", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("SKU has inventory and cannot be disabled"));
    }

    @Test
    void disableMissingSkuReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DataDoesNotExist("SKU does not exist")).when(service).disableSku(id);

        mockMvc.perform(post("/api/v1/sku/{id}/disable", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("SKU does not exist"));
    }

    private static SkuDto skuDto(UUID id) {
        return new SkuDto(
                id,
                "SS-00001",
                "Vanilla Cake",
                new SkuCategoryDto(UUID.randomUUID(), "Cakes"),
                new SkuUnitDto(UUID.randomUUID(), "PCS", "Piece"),
                new BigDecimal("899.00"),
                SkuStatus.ACTIVE,
                "8900000000011",
                BigDecimal.ZERO
        );
    }
}
