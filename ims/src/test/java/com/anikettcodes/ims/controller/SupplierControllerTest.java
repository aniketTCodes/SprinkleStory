package com.anikettcodes.ims.controller;

import com.anikettcodes.ims.dto.supplier.SupplierDto;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.GlobalExceptionHandler;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
@Import(GlobalExceptionHandler.class)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService service;

    @Test
    void createReturnsCreated() throws Exception {
        when(service.saveSupplier(any())).thenReturn(
                new SupplierDto(UUID.randomUUID(), "Frost & Co", "9876543210", "Frost Corp", "Ada"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Frost & Co","contact":"9876543210","corpName":"Frost Corp","pocName":"Ada"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Frost & Co"));
    }

    @Test
    void duplicateNameReturnsConflict() throws Exception {
        when(service.saveSupplier(any())).thenThrow(new DataAlreadyExistException("Supplier already exist!"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Frost & Co","contact":"9876543210","corpName":"","pocName":""}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Supplier already exist!"));
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        when(service.saveSupplier(any())).thenThrow(new InvalidDataException("Supplier name is required"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  ","contact":"","corpName":"","pocName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Supplier name is required"));
    }

    @Test
    void invalidPhoneReturnsBadRequest() throws Exception {
        when(service.saveSupplier(any())).thenThrow(new InvalidDataException("Supplier phone number is invalid"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Frost & Co","contact":"555-0100","corpName":"","pocName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Supplier phone number is invalid"));
    }

    @Test
    void updateMissingSupplierReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.putSupplier(any(), any())).thenThrow(new DataDoesNotExist("Supplier does not exist"));

        mockMvc.perform(put("/api/v1/suppliers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Frost & Co","contact":"9876543210","corpName":"Frost Corp","pocName":"Ada"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInUseReturnsConflict() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DataInUseException("Supplier is in use and cannot be deleted")).when(service).deleteSupplier(id);

        mockMvc.perform(delete("/api/v1/suppliers/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Supplier is in use and cannot be deleted"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
