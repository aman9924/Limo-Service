package com.chicagoelitelimo.dto;

import jakarta.validation.constraints.NotBlank;

public class TrackRequest {

    @NotBlank
    private String bookingReference;

    @NotBlank
    private String phone;

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
