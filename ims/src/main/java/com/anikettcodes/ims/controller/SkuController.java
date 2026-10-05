package com.anikettcodes.ims.controller;

import com.anikettcodes.ims.dto.sku.CreateSkuRequest;
import com.anikettcodes.ims.dto.sku.SkuDto;
import com.anikettcodes.ims.service.SkuService;
import com.anikettcodes.ims.util.enums.SkuStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService service;

    @GetMapping("/sku")
    public List<SkuDto> getSkus(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SkuStatus status,
            @RequestParam(required = false) UUID categoryId
    ){
        return service.getSkus(q, status, categoryId);
    }

    @GetMapping("/sku/{id}")
    public SkuDto getSku(@PathVariable UUID id) {
        return service.getSku(id);
    }

    @PostMapping("/sku")
    @ResponseStatus(HttpStatus.CREATED)
    public SkuDto createSku(@RequestBody CreateSkuRequest req) {
        return service.saveSku(req);
    }

    @PutMapping("/sku/{id}")
    public SkuDto putSku(@RequestBody CreateSkuRequest req, @PathVariable UUID id) {
        return service.putSku(req, id);
    }

    @PostMapping("/sku/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableSku(@PathVariable UUID id) {
        service.disableSku(id);
    }
}
