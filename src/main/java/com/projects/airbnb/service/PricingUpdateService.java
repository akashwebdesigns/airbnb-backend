package com.projects.airbnb.service;


import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.HotelMinPrice;
import com.projects.airbnb.entity.Inventory;
import com.projects.airbnb.repository.HotelMinPriceRepository;
import com.projects.airbnb.repository.HotelRepository;
import com.projects.airbnb.repository.InventoryRepository;
import com.projects.airbnb.strategy.PricingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingUpdateService {

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final PricingService pricingService;

    @Scheduled(cron = "0 0 * * * *")
    public void updatePrices(){
        log.info("Starting Cron job..");
        int page = 0;
        int batchSize = 100;

        while (true){

            Page<Hotel> hotels = hotelRepository.findAll(PageRequest.of(page, batchSize));

            if (hotels.isEmpty()) break;

            hotels.getContent().forEach(this::updateHotelPrices);

            if (!hotels.hasNext()) break;

            page++;
        }


    }

    private void updateHotelPrices(Hotel hotel){
        log.info("Updating hotel prices for hotel ID: {}", hotel.getId());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        List<Inventory> inventories = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);

        updateInventoryPrices(inventories);

        updateHotelMinPrice(hotel,inventories);
    }

    private void updateInventoryPrices(List<Inventory> inventories){
        log.info("Calculating the dynamic price via updateInventoryPrices method");

        inventories.forEach((inventory)->{
            BigDecimal dynamicPrice = pricingService.calculateDynamicPrice(inventory);
            inventory.setPrice(dynamicPrice);
        });

        inventoryRepository.saveAll(inventories);
    }

    private void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList){

        //Here we are calculating the minimum price on a particular date out of all the inventories of the hotel of 365 days
        log.info("Calculating the minimum price of a hotel on a particular date via updateHotelMinPrice");
        Map<LocalDate, BigDecimal> dailyMinPrices = inventoryList.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().orElse(BigDecimal.ZERO)
                ));
        //01-01-2026---1500
        //02-01-2026---1800

        //Now we will map the minimum prices of the hotel to the HotelMinPrice repository
        List<HotelMinPrice> hotelPrices = new ArrayList<>();
        dailyMinPrices.forEach((date, price) -> {
            HotelMinPrice hotelPrice = hotelMinPriceRepository.findByHotelAndDate(hotel, date)
                    .orElse(new HotelMinPrice(hotel, date));
            hotelPrice.setPrice(price);
            hotelPrices.add(hotelPrice);
        });

        // Save all HotelPrice entities in bulk
        hotelMinPriceRepository.saveAll(hotelPrices);

    }
}
