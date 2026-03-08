package com.projects.airbnb.service;

import com.projects.airbnb.dto.*;
import com.projects.airbnb.entity.Booking;
import com.projects.airbnb.entity.Room;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequestDto hotelSearchRequest);

    List<InventoryDto> getAllInventoryByRoom(Long roomId);

    void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto);

    void releaseReservation(Booking booking);

    void updateTotalCountForRoom(Room room, int newTotalCount);

    void updatePricesForRoom(Room room);
}
