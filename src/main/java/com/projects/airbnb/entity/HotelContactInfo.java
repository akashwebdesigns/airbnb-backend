package com.projects.airbnb.entity;


import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

//@Embeddable
//Ye batata hai ki ye class:
//
//Independent entity nahi hai
//
//Iska apna table nahi banega
//
//Ye kisi aur entity ke andar embed hogi

@Getter
@Setter
@Embeddable
public class HotelContactInfo {

    private String address;
    private String phoneNumber;
    private String location;
    private String email;

}
