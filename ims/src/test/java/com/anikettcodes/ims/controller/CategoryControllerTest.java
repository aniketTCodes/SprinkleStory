package com.anikettcodes.ims.controller;

import com.anikettcodes.ims.dto.category.CategoryDto;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.GlobalExceptionHandler;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.service.CategoryService;
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

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService service;

    @Test
    void createReturnsCreated() throws Exception {
        when(service.saveCategory(any())).thenReturn(new CategoryDto(UUID.randomUUID(), "Cakes", ""));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cakes\",\"description\":\"\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cakes"));
    }

    @Test
    void duplicateNameReturnsConflict() throws Exception {
        when(service.saveCategory(any())).thenThrow(new DataAlreadyExistException("Category already exist!"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cakes\",\"description\":\"\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category already exist!"));
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        when(service.saveCategory(any())).thenThrow(new InvalidDataException("Category name is required"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category name is required"));
    }

    @Test
    void updateMissingCategoryReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.putCategory(any(), any())).thenThrow(new DataDoesNotExist("Category does not exist"));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cakes\",\"description\":\"layer\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInUseReturnsConflict() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DataInUseException("Category is in use and cannot be deleted")).when(service).deleteCategory(id);

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category is in use and cannot be deleted"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
