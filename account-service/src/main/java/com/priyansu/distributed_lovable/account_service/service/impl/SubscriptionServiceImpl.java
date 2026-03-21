package com.priyansu.distributed_lovable.account_service.service.impl;


import com.priyansu.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.priyansu.distributed_lovable.account_service.entity.Plan;
import com.priyansu.distributed_lovable.account_service.entity.Subscription;
import com.priyansu.distributed_lovable.account_service.entity.User;
import com.priyansu.distributed_lovable.account_service.mapper.SubscriptionMapper;
import com.priyansu.distributed_lovable.account_service.repository.PlanRepository;
import com.priyansu.distributed_lovable.account_service.repository.SubscriptionRepository;
import com.priyansu.distributed_lovable.account_service.repository.UserRepository;
import com.priyansu.distributed_lovable.account_service.service.SubscriptionService;
import com.priyansu.distributed_lovable.common_lib.dto.PlanDto;
import com.priyansu.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.priyansu.distributed_lovable.common_lib.exception.ResourceNotFoundException;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    // private final ProjectMemberRepository projectMemberRepository;
    private final Integer FREE_TIER_PROJECTS_ALLOWED = 100;  //for development try and tasting(in real= 1)

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        long userId = authUtil.getCurrentUserId();


        var currentSubscription = subscriptionRepository.findByUserIdAndStatusIn(userId, Set.of(
                SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.TRAILING, SubscriptionStatus.INCOMPLETE
        )).orElse(
                new Subscription());

        return subscriptionMapper.toSubscriptionResponse(currentSubscription);
    }

    //stored subscription object inside DB, (this happens after a CheckOut done) ,after Invoice paid event then we will mark this Subscription event status to Complete
    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {

        boolean exists = subscriptionRepository.existsByStripeSubscriptionId(subscriptionId);
        if (exists) {
            log.debug("Subscription already exists: {}", subscriptionId);
            return;
        }

        User user = getUser(userId);
        Plan plan = getPlan(planId);

        try {
            com.stripe.model.Subscription stripeSub =
                    com.stripe.model.Subscription.retrieve(subscriptionId);

            Long periodStart = null;
            Long periodEnd = null;

            // ✅ SAFE ACCESS (IMPORTANT)
            if (stripeSub.getItems() != null &&
                    stripeSub.getItems().getData() != null &&
                    !stripeSub.getItems().getData().isEmpty()) {

                var item = stripeSub.getItems().getData().get(0);

                periodStart = item.getCurrentPeriodStart();
                periodEnd = item.getCurrentPeriodEnd();
            }

            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .stripeSubscriptionId(subscriptionId)

                    // 🔥 FIX
                    .status(SubscriptionStatus.ACTIVE)

                    .currentPeriodStart(
                            periodStart != null ? Instant.ofEpochSecond(periodStart) : null
                    )
                    .currentPeriodEnd(
                            periodEnd != null ? Instant.ofEpochSecond(periodEnd) : null
                    )

                    .cancelAtPeriodEnd(stripeSub.getCancelAtPeriodEnd())

                    .build();

            subscriptionRepository.save(subscription);

            log.info("Subscription activated successfully: {}", subscriptionId);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error while activating subscription {}", subscriptionId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);

        boolean hasSubscriptionUpdated = false;   //flag

        if (status != null && status != subscription.getStatus()) { //means it is changed/updated
            subscription.setStatus(status);

            hasSubscriptionUpdated = true;
        }

        if (periodStart != null && !periodStart.equals(subscription.getCurrentPeriodStart())) {
            subscription.setCurrentPeriodStart(periodStart);

            hasSubscriptionUpdated = true;
        }

        if (periodEnd != null && !periodEnd.equals(subscription.getCurrentPeriodEnd())) {
            subscription.setCurrentPeriodEnd(periodEnd);

            hasSubscriptionUpdated = true;
        }

        if (cancelAtPeriodEnd != null && cancelAtPeriodEnd != subscription.getCancelAtPeriodEnd()) {
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);

            hasSubscriptionUpdated = true;
        }

        if (planId != null && !planId.equals(subscription.getPlan().getId())) {
            Plan plan = getPlan(planId);
            subscription.setPlan(plan);

            hasSubscriptionUpdated = true;
        }

        if (hasSubscriptionUpdated) {
            log.debug("Subscription has been updated: {}", gatewaySubscriptionId);
            subscriptionRepository.save(subscription);
        }


    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);

        subscription.setStatus(SubscriptionStatus.CANCELLED);

        subscriptionRepository.save(subscription);

    }

    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);   //used Utility method (from below last)
        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE || subscription.getStatus() == SubscriptionStatus.INCOMPLETE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        subscriptionRepository.save(subscription);
    }

    @Override
    public void markSubscriptionPastDue(String gatewaySubscriptionId) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            log.debug("Subscription is Already Past Due, {}", gatewaySubscriptionId);
            return;
        }

        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        //Notify user Via email...
    }

    @Override
    public PlanDto getCurrentSubscribedPlanByUser() {
        Long userId = authUtil.getCurrentUserId();
        SubscriptionResponse subscriptionResponse = getCurrentSubscription();

        return subscriptionResponse.plan();
    }


//Responsibility of Workspace-Service
//    @Override
//    public boolean canCreateNewProject() {
//        SubscriptionResponse currentSubscription = getCurrentSubscription();
//
//        long userId = authUtil.getCurrentUserId();
//
//        int countOfOwnedProjects = projectMemberRepository.countProjectOwnedByUser(userId);
//
//
//        if(currentSubscription.plan() == null){
//            return countOfOwnedProjects < FREE_TIER_PROJECTS_ALLOWED;
//        }
//
//        return countOfOwnedProjects < currentSubscription.plan().maxProjects();
//    }


    // Utility Methods
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private Plan getPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId.toString()));
    }

    private Subscription getSubscription(String gatewaySubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", gatewaySubscriptionId));
    }
}
