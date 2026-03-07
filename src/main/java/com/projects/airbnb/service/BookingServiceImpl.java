package com.projects.airbnb.service;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequestDto;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.entity.*;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnauthorizedException;
import com.projects.airbnb.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;

    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequestDto bookingRequestDto) {

        log.info("Initialising booking for Hotel: {}, Room: {} for Date: {} - {} ",bookingRequestDto.getHotelId(),
                bookingRequestDto.getRoomId(), bookingRequestDto.getCheckInDate(),bookingRequestDto.getCheckOutDate());

        Hotel hotel = hotelRepository.findById(bookingRequestDto.getHotelId()).
                orElseThrow(()->new ResourceNotFoundException("No hotel found with id: "+bookingRequestDto.getHotelId()));

        Room room =  roomRepository.findById(bookingRequestDto.getRoomId()).
                orElseThrow(()->new ResourceNotFoundException("No room found with id: "+bookingRequestDto.getRoomId()));

        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                room.getId(),
                bookingRequestDto.getCheckInDate(),
                bookingRequestDto.getCheckOutDate(),
                bookingRequestDto.getRoomsCount()
        );

        long daysCount = ChronoUnit.DAYS.between(bookingRequestDto.getCheckInDate(),bookingRequestDto.getCheckOutDate())+1;

        if(inventoryList.size() != daysCount){
            throw new IllegalStateException("Room is no longer available for the given dates");
        }

        //Update the reservedCount to roomsCount in the inventories fetched
        for(Inventory inventory : inventoryList){
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequestDto.getRoomsCount());
        }

        inventoryRepository.saveAll(inventoryList);


        //Create booking
        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequestDto.getCheckInDate())
                .checkOutDate(bookingRequestDto.getCheckOutDate())
                .bookingStatus(BookingStatus.RESERVED)
                .roomsCount(bookingRequestDto.getRoomsCount())
                .amount(BigDecimal.TEN)
                .user(getCurrentUser())
                .build();

        bookingRepository.save(booking);

        return modelMapper.map(booking,BookingDto.class);

    }

    @Override
    @Transactional
    public BookingDto addGuests(Long booking_id, List<GuestDto> guestDtoList) {

        Booking booking = bookingRepository.findById(booking_id).
                orElseThrow(()->new ResourceNotFoundException("No Booking found with id: "+booking_id));

        User currentLoggedInUser = getCurrentUser();
        if(!currentLoggedInUser.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id: "+currentLoggedInUser.getId());
        }

        if(hasBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired");
        }

        if(booking.getBookingStatus() != BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under reserved state, cannot add guests");
        }

        for(GuestDto guestDto : guestDtoList){
            Guest guest = modelMapper.map(guestDto,Guest.class);
            guest.setUser(getCurrentUser());
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);

    }

    public boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
