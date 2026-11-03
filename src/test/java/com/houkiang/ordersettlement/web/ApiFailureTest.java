package com.houkiang.ordersettlement.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.houkiang.ordersettlement.order.service.SettlementPreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SettlementController.class)
class ApiFailureTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SettlementPreviewService service;

    @Test
    void hidesInternalFailureDetails() throws Exception {
        when(service.preview(anyList(), any(), any(), anyList()))
                .thenThrow(new IllegalStateException("internal-sensitive-detail"));
        mvc.perform(post("/api/settlements/preview").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "items": [{"productId": "P001", "quantity": 1}],
                  "address": {"recipientName": "Test", "phone": "123", "detailAddress": "Test Road",
                    "regionType": "NORMAL"},
                  "deliveryType": "STANDARD"
                }
                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("服务器处理请求失败"))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
