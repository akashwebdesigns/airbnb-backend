package com.projects.airbnb.controller;


import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequestDto;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.service.BookingService;
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
    public ResponseEntity<BookingDto> initialiseBooking(@RequestBody BookingRequestDto bookingRequestDto){
        return ResponseEntity.ok(bookingService.initialiseBooking(bookingRequestDto));
    }

    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId, @RequestBody List<GuestDto> guestDtoList){
        return ResponseEntity.ok(bookingService.addGuests(bookingId,guestDtoList));
    }

    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<Map<String,String>> initiatePayment(@PathVariable Long bookingId){
        String checkoutUrl = bookingService.initiatePayment(bookingId);
        return ResponseEntity.ok(Map.of("checkoutUrl",checkoutUrl));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<Map<String, BookingStatus>> getBookingStatus(@PathVariable Long bookingId) {
        return ResponseEntity.ok(Map.of("status",bookingService.getBookingStatus(bookingId)));
    }

}
