package com.honklimo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Values are supplied via environment variables — see application.yml. Never hardcode real credentials here.
@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

    private String accountSid;
    private String authToken;
    private String whatsappFrom;
    private String ownerWhatsappTo;
    private String contentSidOwner;
    private String contentSidCustomer;
    private String contentSidCustomerConfirmed;
    private String contentSidCustomerCancelled;

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getWhatsappFrom() {
        return whatsappFrom;
    }

    public void setWhatsappFrom(String whatsappFrom) {
        this.whatsappFrom = whatsappFrom;
    }

    public String getOwnerWhatsappTo() {
        return ownerWhatsappTo;
    }

    public void setOwnerWhatsappTo(String ownerWhatsappTo) {
        this.ownerWhatsappTo = ownerWhatsappTo;
    }

    public String getContentSidOwner() {
        return contentSidOwner;
    }

    public void setContentSidOwner(String contentSidOwner) {
        this.contentSidOwner = contentSidOwner;
    }

    public String getContentSidCustomer() {
        return contentSidCustomer;
    }

    public void setContentSidCustomer(String contentSidCustomer) {
        this.contentSidCustomer = contentSidCustomer;
    }

    public String getContentSidCustomerConfirmed() {
        return contentSidCustomerConfirmed;
    }

    public void setContentSidCustomerConfirmed(String contentSidCustomerConfirmed) {
        this.contentSidCustomerConfirmed = contentSidCustomerConfirmed;
    }

    public String getContentSidCustomerCancelled() {
        return contentSidCustomerCancelled;
    }

    public void setContentSidCustomerCancelled(String contentSidCustomerCancelled) {
        this.contentSidCustomerCancelled = contentSidCustomerCancelled;
    }
}
