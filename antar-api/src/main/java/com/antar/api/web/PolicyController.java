package com.antar.api.web;

import com.antar.api.service.GapService;
import com.antar.api.web.dto.PolicyRequest;
import com.antar.api.web.dto.PolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies", description = "Policy terms")
public class PolicyController {

    private final GapService service;

    public PolicyController(GapService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Store a policy's terms")
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request) {
        PolicyResponse created = service.createPolicy(request);
        return ResponseEntity.created(URI.create("/api/v1/policies/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a stored policy, including its computed eligible room rent")
    public ResponseEntity<PolicyResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findPolicy(id));
    }
}
