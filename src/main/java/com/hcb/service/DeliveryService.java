package com.hcb.service;

public interface DeliveryService {

    boolean isPincodeDeliverable(String pincode);

    String getPincodeErrorMessage(String pincode);

    boolean isRestrictionEnabled();

    String getMinPincode();

    String getMaxPincode();

    String getDeliveryRegionName();
}
