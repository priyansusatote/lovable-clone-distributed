package com.priyansu.distributed_lovable.account_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Value("${stripe.api.secret}")
    private String stripeSecretKey;

    //initialize Stripe SDK
    @PostConstruct //Runs a method once, immediately after Spring creates the bean and injects all dependencies.
     public void init(){
         Stripe.apiKey = stripeSecretKey;
     }
}
