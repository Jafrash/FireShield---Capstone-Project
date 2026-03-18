package org.hartford.fireinsurance.dto;

public class UpdateCustomerRequest {
    private String address;
    private String city;
    private String state;
    private String phoneNumber;

    public UpdateCustomerRequest() {
    }

    public UpdateCustomerRequest(String address, String city, String state, String phoneNumber) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

