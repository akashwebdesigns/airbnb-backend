package com.projects.airbnb.dto;

import com.projects.airbnb.entity.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;


@Data
public class ProfileUpdateRequestDto {

    @Size(max = 100, message = "Name cannot be longer than 100 characters")
    private String name;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;
}
