package com.projects.airbnb.service;

import com.projects.airbnb.dto.ProfileUpdateRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.entity.User;

public interface UserService {

    public User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

    UserDto getMyProfile();

    void promoteToHotelManager();
}
