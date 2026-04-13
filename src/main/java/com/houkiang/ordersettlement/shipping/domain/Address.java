package com.houkiang.ordersettlement.shipping.domain;

public class Address {

    private final String recipientName;
    private final String phone;
    private final String detailAddress;
    private final RegionType regionType;

    public Address(String recipientName, String phone, String detailAddress, RegionType regionType) {
        this.recipientName = recipientName;
        this.phone = phone;
        this.detailAddress = detailAddress;
        this.regionType = regionType;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public RegionType getRegionType() {
        return regionType;
    }
}
