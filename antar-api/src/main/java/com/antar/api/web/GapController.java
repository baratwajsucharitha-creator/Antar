package com.antar.api.web;

import com.antar.api.security.CurrentUser;
import com.antar.api.service.GapService;
import com.antar.api.web.dto.GapComputeRequest;
import com.antar.api.web.dto.GapComputeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Compute the coverage gap for a hospital bill against one of your policies",
            description = "Returns the payout, the gap, and a trace naming the clause behind every deduction.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gap computed"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "No such policy for this user")
    })
    public ResponseEntity<GapComputeResponse> compute(@Valid @RequestBody GapComputeRequest request,
                                                      CurrentUser caller) {
        return ResponseEntity.ok(service.computeGap(request, caller));
    }
}
