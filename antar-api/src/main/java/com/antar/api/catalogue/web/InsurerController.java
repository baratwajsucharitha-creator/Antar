package com.antar.api.catalogue.web;

import com.antar.api.catalogue.persistence.AvailabilityStatus;
import com.antar.api.catalogue.persistence.InsurerType;
import com.antar.api.catalogue.service.CatalogueQueryService;
import com.antar.api.catalogue.web.dto.InsurerResponse;
import com.antar.api.catalogue.web.dto.ProductSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/insurers")
@Tag(name = "Catalogue", description = "Public health-insurer reference data. NOT owner-scoped - " +
        "these endpoints require no Easy Auth principal and answer the same for every caller.")
public class InsurerController {

    private static final CacheControl CATALOGUE_CACHE = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

    private final CatalogueQueryService queryService;

    public InsurerController(CatalogueQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List health insurers",
            description = "Public reference data - not owner-scoped, no authentication required. " +
                    "Standalone health insurers and general insurers that write health business.")
    public ResponseEntity<List<InsurerResponse>> list(
            @Parameter(description = "Filter to STANDALONE_HEALTH or GENERAL. Omit for both.")
            @RequestParam(required = false) InsurerType type,
            @Parameter(description = "Case-insensitive substring match on display name.")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok().cacheControl(CATALOGUE_CACHE).body(queryService.listInsurers(type, q));
    }

    @GetMapping("/{id}/products")
    @Operation(summary = "List an insurer's products",
            description = "Public reference data - not owner-scoped, no authentication required. " +
                    "Returns ALL products by default, including WITHDRAWN and CLOSED_TO_NEW ones: " +
                    "a policyholder's own product may no longer be sold, and that is still the " +
                    "correct answer for them. Pass availability to filter down to just one status.")
    public ResponseEntity<List<ProductSummaryResponse>> products(
            @PathVariable Long id,
            @Parameter(description = "Restrict to one availability status. Omit to get every product, " +
                    "including withdrawn ones - that omission is the deliberate default, not a gap.")
            @RequestParam(required = false) AvailabilityStatus availability,
            @Parameter(description = "Case-insensitive substring match on product name.")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok().cacheControl(CATALOGUE_CACHE).body(queryService.listProducts(id, availability, q));
    }
}
