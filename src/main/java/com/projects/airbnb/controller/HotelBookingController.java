package com.projects.airbnb.controller;


import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequestDto;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class HotelBookingController {

    private final BookingService bookingService;

    @PostMapping("/init")
    @Operation(summary = "Initiate the booking", tags = {"Booking Flow"})
    public ResponseEntity<BookingDto> initialiseBooking(@Valid @RequestBody BookingRequestDto bookingRequestDto){
        return ResponseEntity.ok(bookingService.initialiseBooking(bookingRequestDto));
    }

    @PostMapping("/{bookingId}/addGuests")
    @Operation(summary = "Add guests to the booking", tags = {"Booking Flow"})
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId, @Valid @RequestBody List<@Valid GuestDto> guestDtoList){
        return ResponseEntity.ok(bookingService.addGuests(bookingId,guestDtoList));
    }

    @PostMapping("/{bookingId}/payments")
    @Operation(summary = "Initiate payment flow for the booking", tags = {"Booking Flow"})
    public ResponseEntity<Map<String,String>> initiatePayment(@PathVariable Long bookingId){
        String checkoutUrl = bookingService.initiatePayment(bookingId);
        return ResponseEntity.ok(Map.of("checkoutUrl",checkoutUrl));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel the booking", tags = {"Booking Flow"})
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookingId}/status")
    @Operation(summary = "Check the status of the booking", tags = {"Booking Flow"})
    public ResponseEntity<Map<String, BookingStatus>> getBookingStatus(@PathVariable Long bookingId) {
        return ResponseEntity.ok(Map.of("status",bookingService.getBookingStatus(bookingId)));
    }

}
