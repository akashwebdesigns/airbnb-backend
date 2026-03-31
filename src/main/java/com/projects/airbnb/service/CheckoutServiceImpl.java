package com.projects.airbnb.service;

import com.projects.airbnb.entity.Booking;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.repository.BookingRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j

//For theory of Stripe, go to their docs and see the workflow diagram
public class CheckoutServiceImpl implements CheckoutService{

    private final BookingRepository bookingRepository;

    @Override
    public String createCheckoutSession(Booking booking, String successUrl, String failureUrl) {
        try {
            log.info("Creating session for booking with ID: {}", booking.getId());
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();

            Customer customer = Customer.create(customerParams);

            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)//Single time payment or other kind of payment like subscription based
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)//To give the billing address form in the checkout page
                    .setCustomer(customer.getId())
                    .setSuccessUrl(successUrl)//If payment is successful, then server will redirect to this page
                    .setCancelUrl(failureUrl)////If payment is not successful, then server will redirect to this page
                    .addLineItem(//Line item means the details of the hotel for which you ae going to pay
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(booking.getAmount().multiply(BigDecimal.valueOf(100)).longValue())//Converting to Paisa
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(booking.getHotel().getName()+" : "+booking.getRoom().getType()+" room")
                                                                    .setDescription("Booking ID: "+booking.getId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(sessionParams);//CheckoutSession is created
            booking.setPaymentSessionId(session.getId());
            bookingRepository.save(booking);

            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }
}
