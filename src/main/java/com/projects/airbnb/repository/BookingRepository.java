package com.projects.airbnb.repository;

import com.projects.airbnb.entity.Booking;
import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);
    List<Booking> findByHotel(Hotel hotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime,LocalDateTime endDateTime);

    List<Booking> findByUser(User user);

    List<Booking> findByBookingStatusInAndCreatedAtBefore(
            List<BookingStatus> statuses,
            LocalDateTime time
    );
}

