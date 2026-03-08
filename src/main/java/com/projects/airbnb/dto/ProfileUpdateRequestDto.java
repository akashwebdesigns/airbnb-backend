package com.projects.airbnb.dto;

import com.projects.airbnb.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDate;


@Data
public class ProfileUpdateRequestDto {
    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
}
