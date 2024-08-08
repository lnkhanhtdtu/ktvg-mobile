package com.example.tmp1;

public class Customer {
    private int id;
    private String customerName;
    private String phoneNumber;
    private String messageContent;
    private String productName;
    private String vehicleNumber;
    private String registrationDate;
    private String latestRenewalDate;
    private String expirationDate;
    private String lastTime;
    private int expirationDays;

    public Customer(int id, String customerName, String phoneNumber, String messageContent, String productName, String vehicleNumber, String registrationDate, String latestRenewalDate, String expirationDate, String lastTime, int expirationDays) {
        this.id = id;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.messageContent = messageContent;
        this.vehicleNumber = vehicleNumber;
        this.registrationDate = registrationDate;
        this.latestRenewalDate = latestRenewalDate;
        this.expirationDate = expirationDate;
        this.lastTime = lastTime;
        this.expirationDays = expirationDays;
    }

    public int getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getLastTime() {
        return lastTime;
    }

    public String getProductName() {
        return productName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getLatestRenewalDate() {
        return latestRenewalDate;
    }

    public int getExpirationDays() {
        return expirationDays;
    }

    @Override
    public String toString() {
        return "Khách hàng: {" +
                "ID: " + id +
                ", Tên KH: " + customerName + '\'' +
                ", SĐT: " + phoneNumber + '\'' +
                ", ND: " + messageContent + '\'' +
                ", SP: " + productName + '\'' +
                ", Số khung: " + vehicleNumber + '\'' +
                ", Ngày ĐK: " + registrationDate + '\'' +
                ", Ngày GH: " + latestRenewalDate + '\'' +
                ", Ngày HH: " + expirationDate + '\'' +
                ", Gửi gần nhất: " + lastTime + '\'' +
                ", Số ngày còn lại: " + expirationDays +
                '}';
    }
}
