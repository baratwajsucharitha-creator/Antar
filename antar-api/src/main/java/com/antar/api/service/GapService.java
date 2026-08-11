package com.antar.api.service;

import com.antar.api.persistence.PolicyEntity;
import com.antar.api.persistence.PolicyRepository;
import com.antar.api.security.CurrentUser;
import com.antar.api.web.dto.GapComputeRequest;
import com.antar.api.web.dto.GapComputeResponse;
import com.antar.api.web.dto.PolicyRequest;
import com.antar.api.web.dto.PolicyResponse;
import com.antar.engine.GapEngine;
import com.antar.engine.model.GapResult;
import com.antar.engine.model.HospitalBill;
import com.antar.engine.model.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Every method takes the caller. Ownership is not optional and not defaulted -
 * there is no code path that reads a policy without knowing who is asking.
 */
@Service
public class GapService {

    private static final Logger log = LoggerFactory.getLogger(GapService.class);

    private final PolicyRepository repository;
    private final PolicyMapper mapper;
    private final GapEngine engine;

    public GapService(PolicyRepository repository, PolicyMapper mapper, GapEngine engine) {
        this.repository = repository;
        this.mapper = mapper;
        this.engine = engine;
    }

    @Transactional
    public PolicyResponse createPolicy(PolicyRequest request, CurrentUser caller) {
        PolicyEntity entity = mapper.toEntity(request);
        entity.setOwnerId(caller.id());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public PolicyResponse findPolicy(Long id, CurrentUser caller) {
        return mapper.toResponse(load(id, caller));
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> listPolicies(CurrentUser caller) {
        return repository.findAllByOwnerIdOrderByIdDesc(caller.id()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GapComputeResponse computeGap(GapComputeRequest request, CurrentUser caller) {
        Policy policy = mapper.toDomain(load(request.policyId(), caller));
        HospitalBill bill = mapper.toDomain(request);

        GapResult result = engine.compute(bill, policy);

        // The stable subject id is logged, never the name or email.
        log.info("Computed gap ownerId={} policyId={} procedure={} bill={} payout={} gap={}",
                caller.id(), request.policyId(), request.procedureCode(),
                result.totalBill().amount(), result.payout().amount(), result.gap().amount());

        return mapper.toResponse(result);
    }

    private PolicyEntity load(Long id, CurrentUser caller) {
        return repository.findByIdAndOwnerId(id, caller.id())
                .orElseThrow(() -> new PolicyNotFoundException(id));
    }
}
