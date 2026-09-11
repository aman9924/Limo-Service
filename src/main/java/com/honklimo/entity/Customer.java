package com.honklimo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    private String email;

    @Column(name = "sms_consent")
    private Boolean smsConsent = false;

    @Column(name = "sms_consent_timestamp")
    private java.time.Instant smsConsentTimestamp;

    public Customer() {
    }

    public Customer(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getSmsConsent() {
        return smsConsent;
    }

    public void setSmsConsent(Boolean smsConsent) {
        this.smsConsent = smsConsent;
    }

    public java.time.Instant getSmsConsentTimestamp() {
        return smsConsentTimestamp;
    }

    public void setSmsConsentTimestamp(java.time.Instant smsConsentTimestamp) {
        this.smsConsentTimestamp = smsConsentTimestamp;
    }
}
