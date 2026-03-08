package com.projects.airbnb.jobs;

import com.projects.airbnb.entity.Booking;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.repository.BookingRepository;
import com.projects.airbnb.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireStaleBookings() {
        log.info("Checking for stale bookings to expire");

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);

        List<Booking> expiredBookings = bookingRepository.findByBookingStatusInAndCreatedAtBefore(
                List.of(BookingStatus.RESERVED, BookingStatus.GUESTS_ADDED, BookingStatus.PAYMENTS_PENDING),
                expiryTime
        );

        expiredBookings.forEach(booking -> {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            inventoryService.releaseReservation(booking);
        });

        bookingRepository.saveAll(expiredBookings);
        log.info("Expired {} stale bookings", expiredBookings.size());
    }
}
