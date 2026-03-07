package com.projects.airbnb.service;

import com.projects.airbnb.entity.Booking;

public interface CheckoutService {

    String createCheckoutSession(Booking booking, String successUrl, String failureUrl);
}
