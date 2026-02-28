package com.projects.airbnb.dto;

import com.projects.airbnb.entity.User;
import com.projects.airbnb.entity.enums.Gender;
import lombok.Data;

@Data
public class GuestDto {
    private Long id;
    private User user;
    private String name;
    private Gender gender;
    private Integer age;
}
