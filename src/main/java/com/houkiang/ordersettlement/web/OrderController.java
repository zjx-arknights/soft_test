package com.houkiang.ordersettlement.web;

import com.houkiang.ordersettlement.application.OrderApplicationService;
import com.houkiang.ordersettlement.application.OrderSubmissionResult;
import com.houkiang.ordersettlement.application.OrderSubmissionService;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.web.dto.OrderResponse;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService service;
    private final OrderSubmissionService submissions;

    public OrderController(OrderApplicationService service, OrderSubmissionService submissions) {
        this.service = service;
        this.submissions = submissions;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody SettlementPreviewRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        OrderSubmissionResult result = submissions.submit(request, key);
        OrderResponse order = result.order();
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(URI.create("/api/orders/" + order.orderId())).body(order);
    }

    @GetMapping("/{orderId}")
    public OrderResponse find(@PathVariable("orderId") String orderId) {
        return OrderResponse.from(service.find(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse transition(@PathVariable("orderId") String orderId, @Valid @RequestBody StatusRequest request) {
        return OrderResponse.from(service.transition(orderId, request.status()));
    }

    public record StatusRequest(@NotNull OrderStatus status) {
    }
}
