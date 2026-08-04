package com.antar.api.service;

import com.antar.api.persistence.PolicyEntity;
import com.antar.api.persistence.PolicyRepository;
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
    public PolicyResponse createPolicy(PolicyRequest request) {
        PolicyEntity saved = repository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PolicyResponse findPolicy(Long id) {
        return mapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public GapComputeResponse computeGap(GapComputeRequest request) {
        Policy policy = mapper.toDomain(load(request.policyId()));
        HospitalBill bill = mapper.toDomain(request);

        GapResult result = engine.compute(bill, policy);

        log.info("Computed gap for policyId={} procedure={} bill={} payout={} gap={}",
                request.policyId(), request.procedureCode(),
                result.totalBill().amount(), result.payout().amount(), result.gap().amount());

        return mapper.toResponse(result);
    }

    private PolicyEntity load(Long id) {
        return repository.findById(id).orElseThrow(() -> new PolicyNotFoundException(id));
    }
}
