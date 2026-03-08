package com.projects.airbnb.service;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequestDto;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.dto.HotelReportDto;
import com.projects.airbnb.entity.*;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnauthorizedException;
import com.projects.airbnb.repository.*;
import com.projects.airbnb.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.projects.airbnb.util.AppUtils.getCurrentUser;


@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final CheckoutService checkoutService;
    private final PricingService pricingService;
    private final ModelMapper modelMapper;
    @Value("${frontend.url}")
    private String frontendUrl;

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
//        for(Inventory inventory : inventoryList){
//            inventory.setReservedCount(inventory.getReservedCount() + bookingRequestDto.getRoomsCount());
//        }
//
//        inventoryRepository.saveAll(inventoryList);

        //Locking can be done on SELECT queries only, so we LOCKED the inventories via previous query and we are updating via this @Modifying query
        inventoryRepository.initBooking(room.getId(), bookingRequestDto.getCheckInDate(),
                bookingRequestDto.getCheckOutDate(), bookingRequestDto.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculatePriceForListOfInventoriesForOneRoom(inventoryList);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequestDto.getRoomsCount()));


        //Create booking
        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequestDto.getCheckInDate())
                .checkOutDate(bookingRequestDto.getCheckOutDate())
                .bookingStatus(BookingStatus.RESERVED)
                .roomsCount(bookingRequestDto.getRoomsCount())
                .amount(totalPrice)
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

    @Override
    @Transactional
    public String initiatePayment(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(()->
                new ResourceNotFoundException("Booking not found with ID: "+bookingId));

        User currentLoggedInUser = getCurrentUser();

        if(!currentLoggedInUser.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id: "+currentLoggedInUser.getId());
        }

        if(hasBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired");
        }

        String checkoutUrl = checkoutService.createCheckoutSession(booking,
                frontendUrl +"/payments"+bookingId+"/status",
                frontendUrl +"/payments"+bookingId+"/status");

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);
        return checkoutUrl;

    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())){
            //Deserialize the nested object inside the event
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) return;
            String sessionId = session.getId();
            Booking booking =
                    bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
                            new ResourceNotFoundException("Booking not found for session ID: "+sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());


            log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
        }
        else{
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );

        User user = getCurrentUser();

        if (!user.equals(booking.getUser())) {
            throw new UnauthorizedException("Booking does not belong to this user with id: "+user.getId());
        }

        if(booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        //Handling the refund
        try{
            Session session = Session.retrieve(booking.getPaymentSessionId());//Here we will get the session by retrieving it from the SessionId that we stored in Booking at the time of payment
            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();
            Refund.create(refundParams);

            //PaymentIntent is like the A-Z of the payment information
            //PaymentIntent has an ID like : pi_3NxyzABC123
            //When decompiled, it is something like:
            //{
            //  "id": "pi_3NxyzABC123",
            //  "amount": 2000,
            //  "currency": "usd",
            //  "status": "succeeded"
            //}
        }
        catch (StripeException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public BookingStatus getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnauthorizedException("Booking does not belong to this user with id: "+user.getId());
        }

        return booking.getBookingStatus();
    }

    @Override
    public List<BookingDto> getAllBookingsByHotelId(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
                "found with ID: "+hotelId));

        User user = getCurrentUser();

        log.info("Getting all booking for the hotel with ID: {}", hotelId);

        if(!user.equals(hotel.getOwner())) throw new UnauthorizedException("You are not the owner of hotel with id: "+hotelId);

        List<Booking> bookings = bookingRepository.findByHotel(hotel);

        return bookings.stream()
                .map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
                "found with ID: "+hotelId));

        User user = getCurrentUser();

        log.info("Generating report for hotel with ID: {}", hotelId);

        if(!user.equals(hotel.getOwner())) throw new UnauthorizedException("You are not the owner of hotel with id: "+hotelId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);

        long totalConfirmedBookings = bookings
                .stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = totalConfirmedBookings == 0 ? BigDecimal.ZERO :
                totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP);

        return new HotelReportDto(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);
    }

    @Override
    public List<BookingDto> getMyBookings() {
        User user = getCurrentUser();

        return bookingRepository.findByUser(user)
                .stream().
                map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());
    }

    public boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

}
