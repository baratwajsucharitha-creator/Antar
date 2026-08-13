package com.antar.api.catalogue.web;

import com.antar.api.catalogue.service.CatalogueQueryService;
import com.antar.api.catalogue.web.dto.ProductVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Catalogue", description = "Public health-insurer reference data. NOT owner-scoped - " +
        "these endpoints require no Easy Auth principal and answer the same for every caller.")
public class ProductController {

    private static final CacheControl CATALOGUE_CACHE = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

    private final CatalogueQueryService queryService;

    public ProductController(CatalogueQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List a product's versions (UINs)",
            description = "Public reference data - not owner-scoped, no authentication required. " +
                    "A policy is governed by the wording in force when it was bought, so each version " +
                    "carries its own effective window. Without asOf, every version is returned. With " +
                    "asOf, only the version(s) whose window contains that date - possibly none, which " +
                    "is a valid answer, e.g. for a date before this product's first cleared version.")
    public ResponseEntity<List<ProductVersionResponse>> versions(
            @PathVariable Long id,
            @Parameter(description = "Resolve which version was in force on this date " +
                    "(effectiveFrom <= asOf and (effectiveTo is null or effectiveTo >= asOf)).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok().cacheControl(CATALOGUE_CACHE).body(queryService.listVersions(id, asOf));
    }
}
