package com.projects.airbnb.dto;

import com.projects.airbnb.entity.HotelContactInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HotelDto {

    private Long id;

    @NotBlank(message = "Hotel name is required")
    @Size(max = 50, message = "Hotel name cannot be longer than 50 characters")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City name cannot be longer than 50 characters")
    private String city;

    private String[] photos;
    private String[] amenities;

    @Valid
    @NotNull(message = "Hotel contact information is required")
    private HotelContactInfo hotelContactInfo;

    private Boolean active;

}
