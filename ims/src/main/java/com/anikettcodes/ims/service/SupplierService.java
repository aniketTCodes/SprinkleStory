package com.anikettcodes.ims.service;

import com.anikettcodes.ims.dto.supplier.CreateSupplierRequest;
import com.anikettcodes.ims.dto.supplier.SupplierDto;
import com.anikettcodes.ims.entity.Supplier;
import com.anikettcodes.ims.exception.DataAlreadyExistException;
import com.anikettcodes.ims.exception.DataDoesNotExist;
import com.anikettcodes.ims.exception.DataInUseException;
import com.anikettcodes.ims.exception.InvalidDataException;
import com.anikettcodes.ims.repository.SupplierRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private static final Pattern NON_PHONE_CHARS = Pattern.compile("[\\s()-]");
    private static final Pattern LOCAL_MOBILE = Pattern.compile("[6-9]\\d{9}");

    private final SupplierRepo repo;

    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SupplierDto saveSupplier(CreateSupplierRequest req) {
        String name = normalizeName(req.name());
        String contact = normalizeContact(req.contact());
        String corpName = normalizeOptional(req.corpName());
        String pocName = normalizeOptional(req.pocName());
        if (repo.existsByNameIgnoreCase(name)) {
            throw new DataAlreadyExistException("Supplier already exist!");
        }
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setContact(contact);
        supplier.setCorpName(corpName);
        supplier.setPocName(pocName);
        try {
            return toDto(repo.saveAndFlush(supplier));
        } catch (DataIntegrityViolationException ex) {
            throw new DataAlreadyExistException("Supplier already exist!");
        }
    }

    @Transactional
    public SupplierDto putSupplier(CreateSupplierRequest req, UUID id) {
        Supplier supplier = repo.findById(id)
                .orElseThrow(() -> new DataDoesNotExist("Supplier does not exist"));
        String name = normalizeName(req.name());
        String contact = normalizeContact(req.contact());
        String corpName = normalizeOptional(req.corpName());
        String pocName = normalizeOptional(req.pocName());
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DataAlreadyExistException("Supplier name already present");
        }
        supplier.setName(name);
        supplier.setContact(contact);
        supplier.setCorpName(corpName);
        supplier.setPocName(pocName);
        try {
            return toDto(repo.saveAndFlush(supplier));
        } catch (DataIntegrityViolationException ex) {
            throw new DataAlreadyExistException("Supplier name already present");
        }
    }

    @Transactional
    public void deleteSupplier(UUID id) {
        if (!repo.existsById(id)) {
            throw new DataDoesNotExist("Supplier does not exist");
        }
        try {
            repo.deleteById(id);
            repo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new DataInUseException("Supplier is in use and cannot be deleted");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDataException("Supplier name is required");
        }
        return name.trim();
    }

    private String normalizeContact(String contact) {
        if (contact == null || contact.isBlank()) {
            throw new InvalidDataException("Supplier phone number is required");
        }
        String compact = NON_PHONE_CHARS.matcher(contact.trim()).replaceAll("");
        if (compact.startsWith("+91")) {
            compact = compact.substring(3);
        } else if (compact.startsWith("91") && compact.length() == 12) {
            compact = compact.substring(2);
        }
        if (!LOCAL_MOBILE.matcher(compact).matches()) {
            throw new InvalidDataException("Supplier phone number is invalid");
        }
        return compact;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private SupplierDto toDto(Supplier supplier) {
        return new SupplierDto(
                supplier.getId(),
                supplier.getName(),
                supplier.getContact(),
                supplier.getCorpName(),
                supplier.getPocName()
        );
    }
}
