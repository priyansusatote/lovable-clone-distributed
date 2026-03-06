package com.priyansu.distributed_lovable.account_service.repository;


import com.priyansu.distributed_lovable.account_service.entity.Subscription;
import com.priyansu.distributed_lovable.common_lib.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    //Get the current active Subscription
    Optional<Subscription> findByUserIdAndStatusIn(long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);
}
