package com.projects.airbnb.repository;

import com.projects.airbnb.entity.Guest;
import com.projects.airbnb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest,Long> {
    List<Guest> findByUser(User user);
}
