package com.projects.airbnb.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


//@Bean is used when you want Spring to create and manage an object that other parts of the application can inject.


//Here you are not creating any object.
//You are simply setting a static variable in the Stripe SDK:
@Configuration
public class StripeConfg {

    public StripeConfg(@Value("${stripe.secret.key}") String stripeSecretKey){
        Stripe.apiKey = stripeSecretKey;
    }

}
