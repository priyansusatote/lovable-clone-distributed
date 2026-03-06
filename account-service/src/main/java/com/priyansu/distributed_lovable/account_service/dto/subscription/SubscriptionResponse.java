package com.priyansu.distributed_lovable.account_service.dto.subscription;

import com.priyansu.distributed_lovable.common_lib.dto.PlanDto;

import java.time.Instant;

public record SubscriptionResponse(

        PlanDto plan,

        String status,

        Instant currentPeriodEnd,

        Long tokenUsedThisCycle


) {

}
