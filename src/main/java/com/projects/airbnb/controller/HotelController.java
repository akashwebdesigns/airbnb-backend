package com.projects.airbnb.controller;


import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/hotels")
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelDto> createNewHotel(@RequestBody HotelDto hotelDto){
        log.info("Attempting to create a new hotel with name: {}",hotelDto.getName());
        HotelDto hotel = hotelService.createNewHotel(hotelDto);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);
    }


    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable(name = "hotelId") Long id){
        log.info("Attempting to fetch hotel with ID: {}",id);
        HotelDto hotel = hotelService.getHotelById(id);
        return new ResponseEntity<>(hotel,HttpStatus.OK);
    }


    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDto> updateHotelById(@PathVariable(name = "hotelId") Long id, @RequestBody HotelDto hotelDto){
        log.info("Attempting to update hotel with ID: {}", id);
        HotelDto hotel = hotelService.updateHotelById(id,hotelDto);
        return new ResponseEntity<>(hotel,HttpStatus.OK);
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable(name = "hotelId") Long id){
        log.info("Attempting to delete hotel with ID: {}", id);
        hotelService.deleteHotelById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{hotelId}")
    public ResponseEntity<Void> activateHotel(@PathVariable(name = "hotelId") Long id){
        log.info("Attempting to activate hotel with ID: {}", id);
        hotelService.activateHotel(id);
        return ResponseEntity.noContent().build();
    }
}
