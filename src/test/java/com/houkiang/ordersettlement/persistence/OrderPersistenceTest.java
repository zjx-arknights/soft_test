package com.houkiang.ordersettlement.persistence;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.houkiang.ordersettlement.catalog.CouponCatalog;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.repository.OrderRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderPersistenceTest {

    static final String REQUEST = """
            {
              "items": [{"productId": "P001", "quantity": 2}],
              "address": {"recipientName": "张三", "phone": "13800138000",
                "detailAddress": "北京市测试路1号", "regionType": "NORMAL"},
              "deliveryType": "STANDARD",
              "couponCodes": ["FULL100_20", "DISCOUNT90", "FIXED10"]
            }
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    OrderRepository orders;
    @Autowired
    ProductCatalog products;
    @Autowired
    CouponCatalog coupons;

    @Test
    void createsAndReadsAnOrderWithAllSnapshots() throws Exception {
        String id = createOrder(REQUEST);
        mvc.perform(get("/api/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(id))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].productName").value("Java 入门"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(100.00))
                .andExpect(jsonPath("$.address.recipientName").value("张三"))
                .andExpect(jsonPath("$.coupons[*].couponCode", contains("FULL100_20", "DISCOUNT90", "FIXED10")))
                .andExpect(jsonPath("$.payableAmount").value(62.00));
        assertEquals(1, count("order_items", id));
        assertEquals(3, count("order_coupons", id));
    }

    @Test
    void readsUpdatedCatalogValuesFromTheDatabase() throws Exception {
        jdbc.update("UPDATE products SET unit_price = ? WHERE product_id = ?", new BigDecimal("60.00"), "P001");
        jdbc.update("UPDATE coupons SET reduction_amount = ? WHERE coupon_code = ?", new BigDecimal("30.00"), "FULL100_20");
        mvc.perform(post("/api/settlements/preview").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemAmount").value(120.00))
                .andExpect(jsonPath("$.payableAmount").value(71.00));
    }

    @Test
    void readsUpdatedStockFromTheDatabase() throws Exception {
        jdbc.update("UPDATE products SET stock = 1 WHERE product_id = ?", "P001");
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void historicalOrderSurvivesCatalogChangesAndDeletion() throws Exception {
        String id = createOrder(REQUEST);
        jdbc.update("UPDATE products SET product_name = ?, unit_price = 999 WHERE product_id = ?", "New name", "P001");
        jdbc.update("DELETE FROM coupons");
        jdbc.update("DELETE FROM products WHERE product_id = ?", "P001");
        mvc.perform(get("/api/orders/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Java 入门"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
                .andExpect(jsonPath("$.coupons[0].reductionAmount").value(20.00))
                .andExpect(jsonPath("$.payableAmount").value(62.00));
    }

    @Test
    void preservesDecimalPrecisionAndEmptyCoupons() throws Exception {
        jdbc.update("UPDATE products SET unit_price = 0.10 WHERE product_id = ?", "P001");
        ObjectNode request = (ObjectNode) json.readTree(REQUEST);
        request.putArray("couponCodes");
        request.put("deliveryType", "SELF_PICKUP");
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 3);
        String id = createOrder(json.writeValueAsString(request));
        assertEquals(new BigDecimal("0.30"), orders.findById(id).orElseThrow().getPayableAmount());
        mvc.perform(get("/api/orders/" + id))
                .andExpect(jsonPath("$.coupons", hasSize(0)))
                .andExpect(jsonPath("$.items[0].subtotal").value(0.30));
    }

    @Test
    void mergesRepeatedItemsBeforePersisting() throws Exception {
        ObjectNode request = (ObjectNode) json.readTree(REQUEST);
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 1);
        request.withArray("items").addObject().put("productId", "P001").put("quantity", 1);
        String id = createOrder(json.writeValueAsString(request));
        assertEquals(1, count("order_items", id));
        assertEquals(2, orders.findById(id).orElseThrow().getShoppingCart().getItems().get(0).getQuantity());
    }

    @Test
    void persistsMultipleItemsInRequestOrder() throws Exception {
        ObjectNode request = (ObjectNode) json.readTree(REQUEST);
        request.withArray("items").addObject().put("productId", "P002").put("quantity", 1);
        String id = createOrder(json.writeValueAsString(request));
        mvc.perform(get("/api/orders/" + id))
                .andExpect(jsonPath("$.items[*].productId", contains("P001", "P002")))
                .andExpect(jsonPath("$.itemAmount").value(138.00))
                .andExpect(jsonPath("$.payableAmount").value(96.20));
    }

    @Test
    void previewDoesNotWriteOrders() throws Exception {
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM settlement_orders", Integer.class);
        mvc.perform(post("/api/settlements/preview").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM settlement_orders", Integer.class));
        assertEquals(20, products.findProduct("P001").orElseThrow().getStock());
    }

    @Test
    void validationFailureDoesNotWriteAnOrder() throws Exception {
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM settlement_orders", Integer.class);
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"quantity\": 2", "\"quantity\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM settlement_orders", Integer.class));
    }

    @Test
    void reportsMissingOrdersForReadAndUpdate() throws Exception {
        mvc.perform(get("/api/orders/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mvc.perform(patch("/api/orders/missing/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void persistsLegalStatusTransitionsAndRejectsTerminalChanges() throws Exception {
        String id = createOrder(REQUEST);
        for (String next : new String[]{"PAID", "SHIPPED", "COMPLETED"}) {
            mvc.perform(patch("/api/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + next + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
            assertEquals(OrderStatus.valueOf(next), orders.findById(id).orElseThrow().getStatus());
        }
        mvc.perform(patch("/api/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"));
        assertEquals(OrderStatus.COMPLETED, orders.findById(id).orElseThrow().getStatus());
    }

    @Test
    void rejectsMissingStatus() throws Exception {
        String id = createOrder(REQUEST);
        mvc.perform(patch("/api/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertEquals(OrderStatus.PENDING_PAYMENT, orders.findById(id).orElseThrow().getStatus());
    }

    @Test
    void staleStatusUpdateCannotOverwriteTheWinner() throws Exception {
        String id = createOrder(REQUEST);
        assertTrue(orders.updateStatus(id, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID));
        assertFalse(orders.updateStatus(id, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        assertEquals(OrderStatus.PAID, orders.findById(id).orElseThrow().getStatus());
    }

    @Test
    void identifiersRemainCaseSensitiveAndAreBoundAsParameters() {
        assertTrue(products.findProduct("p001").isEmpty());
        assertTrue(products.findProduct("P001 ").isEmpty());
        assertTrue(coupons.findCoupon("fixed10").isEmpty());
        assertTrue(coupons.findCoupon("FIXED10 ").isEmpty());
        assertTrue(products.findProduct("P001' OR '1'='1").isEmpty());
        assertTrue(orders.findById("' OR '1'='1").isEmpty());
    }

    String createOrder(String body) throws Exception {
        String response = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode result = json.readTree(response);
        return result.get("orderId").asText();
    }

    int count(String table, String id) {
        // Table names are fixed test constants, never request data.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE order_id = ?", Integer.class, id);
    }
}
