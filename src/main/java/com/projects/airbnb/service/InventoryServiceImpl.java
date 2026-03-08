package com.projects.airbnb.service;


import com.projects.airbnb.dto.*;
import com.projects.airbnb.entity.*;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnauthorizedException;
import com.projects.airbnb.repository.HotelMinPriceRepository;
import com.projects.airbnb.repository.InventoryRepository;
import com.projects.airbnb.repository.RoomRepository;
import com.projects.airbnb.strategy.PricingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.projects.airbnb.util.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final PricingService pricingService;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Override
    public void initializeRoomForAYear(Room room) {
        //As soon as a room is created for a hotel and the hotel is active, then we will create inventory for that room for 1 year by our below logic

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for (; !today.isAfter(endDate); today=today.plusDays(1)) {
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);

        }
    }

    @Override
    public void deleteInventories(Room room) {
        LocalDate today = LocalDate.now();
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequestDto hotelSearchRequest) {
        log.info("Searching hotels for {} city, from {} to {}", hotelSearchRequest.getCity(), hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());
        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(),hotelSearchRequest.getSize());
        Long dateCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate())+1;
//        Page<Hotel> hotelPage = inventoryRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(), hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate(), hotelSearchRequest.getRoomsCount(), dateCount, pageable);

        //In page we have map defined, thus we do not have to convert the Page<Hotel> into stream

//        return hotelPage.map((hotel)->modelMapper.map(hotel,HotelDto.class));

        //Now we will use HotelMinPrice to browser the hotels
        Page<HotelPriceDto> hotelPage =
                hotelMinPriceRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                        hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate(), hotelSearchRequest.getRoomsCount(),
                        dateCount, pageable);

        return hotelPage;
    }

    @Override
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {

        log.info("Getting All inventory by room for room with id: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: "+roomId));

        User user = getCurrentUser();
        if(!user.equals(room.getHotel().getOwner())) throw new UnauthorizedException("You are not the owner of room with id: "+roomId);

        return inventoryRepository.findByRoomOrderByDate(room).stream()
                .map((element) -> modelMapper.map(element,
                        InventoryDto.class))
                .collect(Collectors.toList());

    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {
        log.info("Updating All inventory by room for room with id: {} between date range: {} - {}", roomId,
                updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: "+roomId));

        User user = getCurrentUser();

        if(!user.equals(room.getHotel().getOwner())) throw new UnauthorizedException("You are not the owner of room with id: "+roomId);

        //First we lock the desired inventories and then will update it, so that in the meanwhile another transaction does not alter it
        List<Inventory> inventoriesToBeUpdated = inventoryRepository.getInventoryAndLockBeforeUpdate(roomId, updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate());

        inventoriesToBeUpdated.forEach((inventory)->{
            inventory.setClosed(updateInventoryRequestDto.getClosed());
            inventory.setSurgeFactor(updateInventoryRequestDto.getSurgeFactor());
            BigDecimal updatedPrice = pricingService.calculateDynamicPrice(inventory);
            inventory.setPrice(updatedPrice);
        });

        inventoryRepository.saveAll(inventoriesToBeUpdated);
    }

    @Override
    @Transactional
    public void releaseReservation(Booking booking) {
        Room room = booking.getRoom();
        //Lock the inventories
        List<Inventory> inventories = inventoryRepository.getInventoryAndLockBeforeUpdate(room.getId(), booking.getCheckInDate(), booking.getCheckOutDate());
        inventories.forEach((inventory -> {
            inventory.setReservedCount(
                    Math.max(0, inventory.getReservedCount() - booking.getRoomsCount())
            );
        }));

        inventoryRepository.saveAll(inventories);
    }



    @Transactional
    public void updateTotalCountForRoom(Room room, int newTotalCount) {
        log.info("Updating totalCount for room: {} to: {}", room.getId(), newTotalCount);
        inventoryRepository.updateTotalCount(room.getId(), newTotalCount, LocalDate.now());
    }

    @Transactional
    public void updatePricesForRoom(Room room) {
        log.info("Recalculating dynamic prices for room: {}", room.getId());

        List<Inventory> inventories = inventoryRepository
                .findByRoomAndDateGreaterThanEqual(room, LocalDate.now());

        inventories.forEach(inventory ->
                inventory.setPrice(pricingService.calculateDynamicPrice(inventory))
        );

        inventoryRepository.saveAll(inventories);
    }
}
