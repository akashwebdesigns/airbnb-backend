package com.projects.airbnb.dto;

import com.projects.airbnb.entity.enums.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GuestDto {
    private Long id;

    @NotBlank(message = "Guest name is required")
    private String name;

    @NotNull(message = "Guest gender is required")
    private Gender gender;

    @NotNull(message = "Guest age is required")
    @Min(value = 0, message = "Guest age cannot be negative")
    @Max(value = 120, message = "Guest age seems invalid")
    private Integer age;
}
