package com.anikettcodes.ims.controller;

import com.anikettcodes.ims.dto.category.CategoryDto;
import com.anikettcodes.ims.dto.category.CreateCategoryRequest;
import com.anikettcodes.ims.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping("/categories")
    public List<CategoryDto> getCategories() {
        return service.getAllCategory();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody CreateCategoryRequest req) {
        return service.saveCategory(req);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto putCategory(@RequestBody CreateCategoryRequest req, @PathVariable UUID id) {
        return service.putCategory(req, id);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        service.deleteCategory(id);
    }
}
