package com.anikettcodes.ims.service;

import com.anikettcodes.ims.dto.category.CategoryDto;
import com.anikettcodes.ims.dto.category.CreateCategoryRequest;
import com.anikettcodes.ims.entity.Category;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.repository.CategoryRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepo repo;

    @InjectMocks
    private CategoryService service;

    @Test
    void updateKeepsNameAndChangesDescription() {
        UUID id = UUID.randomUUID();
        Category existing = category(id, "Cakes", "old");
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.existsByNameIgnoreCaseAndIdNot("Cakes", id)).thenReturn(false);
        when(repo.saveAndFlush(existing)).thenReturn(existing);

        CategoryDto result = service.putCategory(new CreateCategoryRequest("  Cakes  ", "  layer cakes  "), id);

        assertEquals("Cakes", result.name());
        assertEquals("layer cakes", result.description());
        assertEquals("layer cakes", existing.getDescription());
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(repo.existsByNameIgnoreCase("cakes")).thenReturn(true);

        assertThrows(DataAlreadyExistException.class,
                () -> service.saveCategory(new CreateCategoryRequest("  cakes  ", "desc")));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsNameUsedByAnotherCategory() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(category(id, "Cakes", "old")));
        when(repo.existsByNameIgnoreCaseAndIdNot("Cookies", id)).thenReturn(true);

        assertThrows(DataAlreadyExistException.class,
                () -> service.putCategory(new CreateCategoryRequest("Cookies", "old"), id));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(InvalidDataException.class,
                () -> service.saveCategory(new CreateCategoryRequest("   ", "desc")));
        verify(repo, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void createStoresTrimmedNameAndEmptyDescriptionWhenMissing() {
        when(repo.existsByNameIgnoreCase("Cakes")).thenReturn(false);
        when(repo.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDto result = service.saveCategory(new CreateCategoryRequest("  Cakes  ", null));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repo).saveAndFlush(captor.capture());
        assertEquals("Cakes", captor.getValue().getName());
        assertEquals("", captor.getValue().getDescription());
        assertEquals("Cakes", result.name());
        assertEquals("", result.description());
    }

    @Test
    void deleteMissingCategory() {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(false);

        assertThrows(DataDoesNotExist.class, () -> service.deleteCategory(id));
        verify(repo, never()).deleteById(id);
    }

    @Test
    void deleteCategoryInUse() {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("sku.category_id")).when(repo).flush();

        assertThrows(DataInUseException.class, () -> service.deleteCategory(id));
    }

    private static Category category(UUID id, String name, String description) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        return category;
    }
}
