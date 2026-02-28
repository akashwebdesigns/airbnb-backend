package com.projects.airbnb.repository;

import com.projects.airbnb.dto.HotelPriceDto;
import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice,Long> {


    //In JPQL (Java Persistence Query Language), when you want the query result to be mapped directly into a custom object (DTO), you must:
    //
    //Use the new keyword
    //
    //Provide the fully qualified class name (FQCN)
    //
    //Match a constructor inside that DTO


    // When we will search hotels in a city between a start and end date, this will scan the HotelMinRepository
    // and will group the hotels with their MinPrice inventories for that date range, and will return the avg of the
    // min prices between those dates
    //The sole purpose of the HotelMinPrice repository is to store the minimum price of a particular hotel for a particular date
    @Query("""
     SELECT new com.projects.airbnb.dto.HotelPriceDto(i.hotel,AVG(i.price)) FROM HotelMinPrice i
        WHERE i.hotel.city = :city
            AND i.date BETWEEN :startDate AND :endDate
            AND i.hotel.active = true
        GROUP BY i.hotel
    """)
    Page<HotelPriceDto> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
    );

    Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);
}
