package com.projects.airbnb.service;

import com.projects.airbnb.dto.RoomDto;
import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.Room;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnauthorizedException;
import com.projects.airbnb.repository.HotelRepository;
import com.projects.airbnb.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.projects.airbnb.util.AppUtils.getCurrentUser;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService{

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating a new room in hotel with ID:{}",hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id: "+hotelId));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+hotelId);
        }

        Room room = modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        room=roomRepository.save(room);


        //Room is created, now we will create inventory of this room of whole year only if the hotel is active
        if(hotel.getActive()){
            inventoryService.initializeRoomForAYear(room);
        }

        return modelMapper.map(room,RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all the rooms in hotel with ID:{}",hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id: "+hotelId));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+hotelId);
        }

        return hotel.getRooms()
                .stream()
                .map((room)->modelMapper.map(room, RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting rooms with ID:{}",roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(()->new ResourceNotFoundException("Room not found with id: "+roomId));

        return modelMapper.map(room,RoomDto.class);
    }

    @Override
    @Transactional
    public void deleteRoomById(Long roomId) {
        log.info("Deleting room with ID:{}",roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(()->new ResourceNotFoundException("Room not found with id: "+roomId));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(room.getHotel().getOwner())) {
            throw new UnauthorizedException("This user does not own this room with id: "+roomId);
        }

        inventoryService.deleteInventories(room);
        roomRepository.delete(room);
    }

    @Override
    @Transactional
    public RoomDto updateRoomById(Long hotelId, Long roomId, RoomDto roomDto) {
        log.info("Updating the room with ID: {}", roomId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: "+hotelId));

        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+hotelId);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: "+roomId));

        BigDecimal oldBasePrice = room.getBasePrice();
        int oldTotalCount = room.getTotalCount();

        //Updated the room
        modelMapper.map(roomDto, room);
        room.setId(roomId);
        room = roomRepository.save(room);

        boolean basePriceChanged = !oldBasePrice.equals(roomDto.getBasePrice());
        boolean totalCountChanged = oldTotalCount != roomDto.getTotalCount();

        if (basePriceChanged) {
            inventoryService.updatePricesForRoom(room);
        }

        if (totalCountChanged) {
            inventoryService.updateTotalCountForRoom(room, roomDto.getTotalCount());
        }
        return modelMapper.map(room, RoomDto.class);
    }
}
