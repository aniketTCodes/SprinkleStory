package com.anikettcodes.ims.controller;

import com.anikettcodes.ims.dto.supplier.CreateSupplierRequest;
import com.anikettcodes.ims.dto.supplier.SupplierDto;
import com.anikettcodes.ims.service.SupplierService;
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
public class SupplierController {

    private final SupplierService service;

    @GetMapping("/suppliers")
    public List<SupplierDto> getSuppliers() {
        return service.getAllSuppliers();
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto createSupplier(@RequestBody CreateSupplierRequest req) {
        return service.saveSupplier(req);
    }

    @PutMapping("/suppliers/{id}")
    public SupplierDto putSupplier(@RequestBody CreateSupplierRequest req, @PathVariable UUID id) {
        return service.putSupplier(req, id);
    }

    @DeleteMapping("/suppliers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupplier(@PathVariable UUID id) {
        service.deleteSupplier(id);
    }
}
