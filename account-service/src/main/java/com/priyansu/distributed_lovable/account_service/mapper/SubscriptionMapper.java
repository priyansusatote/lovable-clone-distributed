package com.priyansu.distributed_lovable.account_service.mapper;


import com.priyansu.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.priyansu.distributed_lovable.account_service.entity.Plan;
import com.priyansu.distributed_lovable.account_service.entity.Subscription;
import com.priyansu.distributed_lovable.common_lib.dto.PlanDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);
}
