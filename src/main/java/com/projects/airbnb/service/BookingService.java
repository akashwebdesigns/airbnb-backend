package com.projects.airbnb.service;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequestDto;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.stripe.model.Event;

import java.util.List;

public interface BookingService {

    BookingDto initialiseBooking(BookingRequestDto bookingRequestDto);

    BookingDto addGuests(Long booking_id, List<GuestDto> guestDtoList);

    String initiatePayment(Long bookingId);

    void capturePayment(Event event);

    void cancelBooking(Long bookingId);

    BookingStatus getBookingStatus(Long bookingId);
}
