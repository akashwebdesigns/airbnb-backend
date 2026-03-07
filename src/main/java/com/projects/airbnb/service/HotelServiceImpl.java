package com.projects.airbnb.service;


import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.dto.HotelInfoDto;
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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new hotel with name: {}", hotelDto.getName());
        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);//initially at the time of creation the hotel is not active

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel.setOwner(user);

        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Created a new hotel with ID: {}",savedHotel.getId());
        return modelMapper.map(savedHotel, HotelDto.class);

    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting hotel with ID: {}", id);

        Hotel hotel = hotelRepository
                      .findById(id)
                      .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+id);
        }

        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Getting hotel with ID: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+id);
        }

        modelMapper.map(hotelDto,hotel);
        hotel.setId(id);


        // 1. By default, when ModelMapper maps a collection from a DTO to an Entity that already has data, it often merges them or skips the update if it thinks the collection hasn't "changed" in a way it understands. Because you are using modelMapper.map(hotelDto, hotel), ModelMapper is trying to be "helpful" by not deleting data it thinks you might have just forgotten to include.

        // 2. Explicitly overwrite the collections/arrays
        // This forces Hibernate to see that the old list is replaced by the new one

        hotel.setAmenities(hotelDto.getAmenities());
        hotel.setPhotos(hotelDto.getPhotos());

        // 3. Handle Nested Object explicitly if needed
        if (hotelDto.getHotelContactInfo() != null) {
            modelMapper.map(hotelDto.getHotelContactInfo(), hotel.getHotelContactInfo());
        }

        hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        log.info("Getting hotel with ID: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+id);
        }

        //TODO: Delete inventories for this hotel
        for (Room room:hotel.getRooms()){
            inventoryService.deleteInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(id);
    }

    @Override
    public void activateHotel(Long id) {
        log.info("Activating the hotel with ID: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnauthorizedException("This user does not own this hotel with id: "+id);
        }

        hotel.setActive(true);

        //As soon as the hotel gets active, we create inventory for all the rooms(assuming no previous inventories are present and we are creating inventory for the first time)

        for(Room room : hotel.getRooms()){
            inventoryService.initializeRoomForAYear(room);
        }
    }

    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        log.info("Getting the hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

        List<RoomDto> rooms = hotel.getRooms().stream().map((room)->modelMapper.map(room, RoomDto.class)).toList();

        return new HotelInfoDto(modelMapper.map(hotel,HotelDto.class),rooms);
    }
}

