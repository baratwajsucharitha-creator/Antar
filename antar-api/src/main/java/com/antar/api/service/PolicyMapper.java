package com.antar.api.service;

import com.antar.api.persistence.PolicyEntity;
import com.antar.api.persistence.SubLimitEmbeddable;
import com.antar.api.web.dto.*;
import com.antar.engine.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The anti-corruption layer. Persistence and HTTP shapes never leak into the engine,
 * and the engine's value objects never leak out onto the wire.
 */
@Component
public class PolicyMapper {

    public PolicyEntity toEntity(PolicyRequest request) {
        PolicyEntity entity = new PolicyEntity();
        entity.setInsurerName(request.insurerName());
        entity.setProductName(request.productName());
        entity.setSumInsured(request.sumInsured());
        entity.setRemainingSumInsured(request.remainingSumInsured());
        entity.setRoomRentPercentOfSumInsuredPerDay(request.roomRentPercentOfSumInsuredPerDay());
        entity.setProportionPharmacy(request.proportionPharmacy());
        entity.setCoPayPercent(request.coPayPercent());
        entity.setCoPayOrder(request.coPayOrder());
        entity.setSubLimits(request.subLimits() == null ? List.of()
                : request.subLimits().stream()
                        .map(s -> new SubLimitEmbeddable(s.procedureCode(), s.cap()))
                        .toList());
        return entity;
    }

    public Policy toDomain(PolicyEntity entity) {
        List<SubLimit> subLimits = entity.getSubLimits().stream()
                .map(s -> new SubLimit(s.getProcedureCode(), new Money(s.getCapAmount())))
                .toList();

        CoPayRule coPayRule = entity.getCoPayPercent().signum() == 0
                ? CoPayRule.NONE
                : new CoPayRule(entity.getCoPayPercent(), entity.getCoPayOrder());

        PolicyTerms terms = new PolicyTerms(
                new RoomRentRule(entity.getRoomRentPercentOfSumInsuredPerDay(), entity.isProportionPharmacy()),
                subLimits,
                coPayRule);

        return new Policy(
                entity.getInsurerName(),
                entity.getProductName(),
                new Money(entity.getSumInsured()),
                new Money(entity.getRemainingSumInsured()),
                terms);
    }

    public HospitalBill toDomain(GapComputeRequest request) {
        List<BillLine> lines = request.lines().stream()
                .map(l -> new BillLine(l.description(), l.category(), new Money(l.amount())))
                .toList();

        return new HospitalBill(
                request.procedureCode(),
                request.daysAdmitted(),
                new Money(request.actualRoomRentPerDay()),
                lines);
    }

    public GapComputeResponse toResponse(GapResult result) {
        List<DeductionTraceResponse> deductions = result.trace().stream()
                .map(t -> new DeductionTraceResponse(
                        t.clauseReference(), t.explanation(), t.amountRemoved().amount()))
                .toList();

        return new GapComputeResponse(
                result.totalBill().amount(),
                result.payout().amount(),
                result.gap().amount(),
                result.gapAsPercentOfBill(),
                deductions);
    }

    public PolicyResponse toResponse(PolicyEntity entity) {
        Policy domain = toDomain(entity);
        return new PolicyResponse(
                entity.getId(),
                entity.getInsurerName(),
                entity.getProductName(),
                entity.getSumInsured(),
                entity.getRemainingSumInsured(),
                domain.eligibleRoomRentPerDay().amount(),
                entity.getCoPayPercent(),
                entity.getCoPayOrder(),
                entity.getSubLimits().stream()
                        .map(s -> new SubLimitDto(s.getProcedureCode(), s.getCapAmount()))
                        .toList());
    }
}
