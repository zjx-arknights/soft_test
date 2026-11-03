package com.houkiang.ordersettlement.web;

import com.houkiang.ordersettlement.order.domain.OrderSummary;
import com.houkiang.ordersettlement.order.service.SettlementPreviewService;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler.ItemSelection;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewRequest;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementPreviewService previewService;

    public SettlementController(SettlementPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/preview")
    public SettlementPreviewResponse preview(@Valid @RequestBody SettlementPreviewRequest request) {
        List<ItemSelection> items = request.items().stream()
                .map(item -> new ItemSelection(item.productId(), item.quantity())).toList();
        SettlementPreviewRequest.AddressRequest address = request.address();
        OrderSummary summary = previewService.preview(items,
                new Address(address.recipientName(), address.phone(), address.detailAddress(), address.regionType()),
                request.deliveryType(), request.couponCodes());
        return SettlementPreviewResponse.from(summary, request.couponCodes());
    }
}
