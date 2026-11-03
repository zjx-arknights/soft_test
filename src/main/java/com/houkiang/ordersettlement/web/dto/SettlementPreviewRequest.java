package com.houkiang.ordersettlement.web.dto;

import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record SettlementPreviewRequest(
        @NotEmpty @Size(max = 100) List<@NotNull @Valid ItemRequest> items,
        @NotNull @Valid AddressRequest address,
        @NotNull DeliveryType deliveryType,
        @Size(max = 20) List<@NotBlank @Size(max = 64) String> couponCodes) {

    public SettlementPreviewRequest {
        items = items == null ? null : Collections.unmodifiableList(new ArrayList<>(items));
        couponCodes = Collections.unmodifiableList(new ArrayList<>(couponCodes == null ? List.of() : couponCodes));
    }

    public record ItemRequest(
            @NotBlank @Size(max = 64) String productId,
            @NotNull @Min(1) @Max(10000) Integer quantity) {
    }

    public record AddressRequest(
            @NotBlank @Size(max = 100) String recipientName,
            @NotBlank @Size(max = 32) String phone,
            @NotBlank @Size(max = 500) String detailAddress,
            @NotNull RegionType regionType) {
    }
}
