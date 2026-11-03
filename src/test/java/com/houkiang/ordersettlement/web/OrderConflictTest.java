package com.houkiang.ordersettlement.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.houkiang.ordersettlement.application.OrderApplicationService;
import com.houkiang.ordersettlement.application.OrderSubmissionService;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.exception.OrderUpdateConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderConflictTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderApplicationService service;

    @MockitoBean
    OrderSubmissionService submissions;

    @Test
    void returnsConflictWhenAnotherRequestHasUpdatedTheOrder() throws Exception {
        when(service.transition("ORD-conflict", OrderStatus.PAID))
                .thenThrow(new OrderUpdateConflictException("ORD-conflict"));
        mvc.perform(patch("/api/orders/ORD-conflict/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_UPDATE_CONFLICT"));
    }
}
