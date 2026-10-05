package com.anikettcodes.ims.service;

import com.anikettcodes.ims.dto.supplier.CreateSupplierRequest;
import com.anikettcodes.ims.dto.supplier.SupplierDto;
import com.anikettcodes.ims.entity.Supplier;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.repository.SupplierRepo;
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
class SupplierServiceTest {

    @Mock
    private SupplierRepo repo;

    @InjectMocks
    private SupplierService service;

    @Test
    void updateKeepsNameAndChangesContact() {
        UUID id = UUID.randomUUID();
        Supplier existing = supplier(id, "Frost & Co", "9123456780", "Frost Corp", "Ada");
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.existsByNameIgnoreCaseAndIdNot("Frost & Co", id)).thenReturn(false);
        when(repo.saveAndFlush(existing)).thenReturn(existing);

        SupplierDto result = service.putSupplier(
                new CreateSupplierRequest("  Frost & Co  ", "  +91 98765-43210  ", "  Frost Corp  ", "  Ada  "),
                id
        );

        assertEquals("Frost & Co", result.name());
        assertEquals("9876543210", result.contact());
        assertEquals("9876543210", existing.getContact());
        assertEquals("Frost Corp", existing.getCorpName());
        assertEquals("Ada", existing.getPocName());
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(repo.existsByNameIgnoreCase("frost & co")).thenReturn(true);

        assertThrows(DataAlreadyExistException.class,
                () -> service.saveSupplier(new CreateSupplierRequest("  frost & co  ", "9876543210", "", "")));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsNameUsedByAnotherSupplier() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(supplier(id, "Frost & Co", "", "", "")));
        when(repo.existsByNameIgnoreCaseAndIdNot("Dairy Fresh", id)).thenReturn(true);

        assertThrows(DataAlreadyExistException.class,
                () -> service.putSupplier(new CreateSupplierRequest("Dairy Fresh", "9876543210", "", ""), id));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(InvalidDataException.class,
                () -> service.saveSupplier(new CreateSupplierRequest("   ", "", "", "")));
        verify(repo, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void createStoresTrimmedNameAndEmptyOptionalsWhenMissing() {
        when(repo.existsByNameIgnoreCase("Frost & Co")).thenReturn(false);
        when(repo.saveAndFlush(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierDto result = service.saveSupplier(new CreateSupplierRequest("  Frost & Co  ", "9876543210", null, null));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(repo).saveAndFlush(captor.capture());
        assertEquals("Frost & Co", captor.getValue().getName());
        assertEquals("9876543210", captor.getValue().getContact());
        assertEquals("", captor.getValue().getCorpName());
        assertEquals("", captor.getValue().getPocName());
        assertEquals("Frost & Co", result.name());
        assertEquals("9876543210", result.contact());
    }

    @Test
    void createRejectsBlankPhone() {
        assertThrows(InvalidDataException.class,
                () -> service.saveSupplier(new CreateSupplierRequest("Frost & Co", "   ", "", "")));
        verify(repo, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void createRejectsInvalidPhone() {
        assertThrows(InvalidDataException.class,
                () -> service.saveSupplier(new CreateSupplierRequest("Frost & Co", "555-0100", "", "")));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsInvalidPhone() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(supplier(id, "Frost & Co", "9876543210", "", "")));

        assertThrows(InvalidDataException.class,
                () -> service.putSupplier(new CreateSupplierRequest("Frost & Co", "12345", "", ""), id));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void deleteMissingSupplier() {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(false);

        assertThrows(DataDoesNotExist.class, () -> service.deleteSupplier(id));
        verify(repo, never()).deleteById(id);
    }

    @Test
    void deleteSupplierInUse() {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("purchase_order.supplier_id")).when(repo).flush();

        assertThrows(DataInUseException.class, () -> service.deleteSupplier(id));
    }

    private static Supplier supplier(UUID id, String name, String contact, String corpName, String pocName) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        supplier.setContact(contact);
        supplier.setCorpName(corpName);
        supplier.setPocName(pocName);
        return supplier;
    }
}
