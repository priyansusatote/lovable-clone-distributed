package com.priyansu.distributed_lovable.account_service.dto.subscription;

public record PlanLimitsResponse(
        String planName,
        Integer maxTokenPerDay,
        Integer maxProjects,
        Boolean unlimitedAi
) {
}
