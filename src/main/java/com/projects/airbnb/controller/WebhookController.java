package com.projects.airbnb.controller;

import com.projects.airbnb.service.BookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


//What are Webhooks?
//When you create a payment with Stripe (e.g., PaymentIntent), the payment flow may involve bank processing, payment failure,etc.
//Your backend cannot trust the frontend response alone.
//
//Instead, Stripe sends server-to-server events to your backend via webhooks.

//Client → Backend → Stripe
//                ↓
//          PaymentIntent created
//                ↓
//Client confirms payment
//                ↓
//Stripe processes payment
//                ↓
//Stripe → Webhook → Your Backend
//                ↓
//Update booking/payment status

//A webhook endpoint is a destination on your server that receives requests from Stripe, notifying you about events
// that happen on your account such as a customer disputing a charge or a successful recurring payment.
// Add a new endpoint to your server and make sure it’s publicly accessible so we can send unauthenticated POST
// requests.

//Stripe sends the event data in the request body.
// Each event is structured as an Event object with a type, id, and related Stripe resource nested under data.
//
//Server Handle the event

//As soon as you have the event object, check the type to know what kind of event happened. You can use one webhook
// to handle several different event types at once, or set up individual endpoints for specific events.





@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    private final BookingService bookingService;

    @PostMapping("/payment")
    @Operation(summary = "Capture the payments", tags = {"Webhook"})
    public ResponseEntity<Void> capturePayments(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader){

            try {
                log.info("Entered in the Webhook controller");
                Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
                bookingService.capturePayment(event);

                return ResponseEntity.noContent().build();
            } catch (SignatureVerificationException e) {
                throw new RuntimeException(e);
            }
    }

}
