package com.antar.api.web;

import com.antar.api.security.CurrentUser;
import com.antar.api.service.GapService;
import com.antar.api.web.dto.PolicyRequest;
import com.antar.api.web.dto.PolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies", description = "Policy terms")
public class PolicyController {

    private final GapService service;

    public PolicyController(GapService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Store a policy's terms against your account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Policy stored"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request,
                                                 CurrentUser caller) {
        PolicyResponse created = service.createPolicy(request, caller);
        return ResponseEntity.created(URI.create("/api/v1/policies/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List your own policies")
    public ResponseEntity<List<PolicyResponse>> list(CurrentUser caller) {
        return ResponseEntity.ok(service.listPolicies(caller));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one of your policies, including its computed eligible room rent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "No such policy for this user")
    })
    public ResponseEntity<PolicyResponse> get(@PathVariable Long id, CurrentUser caller) {
        return ResponseEntity.ok(service.findPolicy(id, caller));
    }
}
