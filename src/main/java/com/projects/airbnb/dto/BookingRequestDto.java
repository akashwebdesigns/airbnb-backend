package com.projects.airbnb.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequestDto {

        @NotNull(message = "Hotel id is required")
        private Long hotelId;

        @NotNull(message = "Room id is required")
        private Long roomId;

        @NotNull(message = "Check-in date is required")
        private LocalDate checkInDate;

        @NotNull(message = "Check-out date is required")
        private LocalDate checkOutDate;

        @NotNull(message = "Rooms count is required")
        @Min(value = 1, message = "At least one room must be booked")
        private Integer roomsCount;
}
