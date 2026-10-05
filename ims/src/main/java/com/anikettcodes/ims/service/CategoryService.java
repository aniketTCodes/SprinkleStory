package com.anikettcodes.ims.service;

import com.anikettcodes.ims.dto.category.CategoryDto;
import com.anikettcodes.ims.dto.category.CreateCategoryRequest;
import com.anikettcodes.ims.entity.Category;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.repository.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepo repo;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategory() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CategoryDto saveCategory(CreateCategoryRequest req) {
        String name = normalizeName(req.name());
        String description = normalizeDescription(req.description());
        if (repo.existsByNameIgnoreCase(name)) {
            throw new DataAlreadyExistException("Category already exist!");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        try {
            return toDto(repo.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new DataAlreadyExistException("Category already exist!");
        }
    }

    @Transactional
    public CategoryDto putCategory(CreateCategoryRequest req, UUID id) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new DataDoesNotExist("Category does not exist"));
        String name = normalizeName(req.name());
        String description = normalizeDescription(req.description());
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DataAlreadyExistException("Category name already present");
        }
        category.setName(name);
        category.setDescription(description);
        try {
            return toDto(repo.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new DataAlreadyExistException("Category name already present");
        }
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!repo.existsById(id)) {
            throw new DataDoesNotExist("Category does not exist");
        }
        try {
            repo.deleteById(id);
            repo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new DataInUseException("Category is in use and cannot be deleted");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDataException("Category name is required");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.trim();
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }
}
