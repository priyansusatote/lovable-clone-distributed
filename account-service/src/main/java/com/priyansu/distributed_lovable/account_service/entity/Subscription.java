package com.priyansu.distributed_lovable.account_service.entity;


import com.priyansu.distributed_lovable.common_lib.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Subscription {

    @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)  //(many Subscription have by one User)        user can have manySubscription
    @JoinColumn(nullable = false, name = "user_id")
    User user; //each subscription can have 1 user

    @ManyToOne(fetch = FetchType.LAZY)     //many subscription is part on one plan
    @JoinColumn(nullable = false, name = "plan_id")
    Plan plan;

    @Enumerated(EnumType.STRING)
    SubscriptionStatus status;


    String stripeSubscriptionId;  //also called as "gatewaySubscriptionId"

    Instant currentPeriodStart;
    Instant currentPeriodEnd;
    Boolean cancelAtPeriodEnd;

    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;


}
