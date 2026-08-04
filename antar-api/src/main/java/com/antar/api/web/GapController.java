package com.antar.api.web;

import com.antar.api.service.GapService;
import com.antar.api.web.dto.GapComputeRequest;
import com.antar.api.web.dto.GapComputeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gap")
@Tag(name = "Gap", description = "Coverage gap computation")
public class GapController {

    private final GapService service;

    public GapController(GapService service) {
        this.service = service;
    }

    @PostMapping("/compute")
    @Operation(summary = "Compute the coverage gap for a hospital bill against a stored policy",
            description = "Returns the payout, the gap, and a trace naming the clause behind every deduction.")
    public ResponseEntity<GapComputeResponse> compute(@Valid @RequestBody GapComputeRequest request) {
        return ResponseEntity.ok(service.computeGap(request));
    }
}
