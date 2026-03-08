package com.projects.airbnb.repository;

import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel,Long> {

    List<Hotel> findByOwner(User user);

}
