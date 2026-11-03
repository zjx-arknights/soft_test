package com.houkiang.ordersettlement.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.houkiang.ordersettlement.application.OrderSubmissionResult;
import com.houkiang.ordersettlement.application.OrderSubmissionService;
import com.houkiang.ordersettlement.order.exception.InsufficientStockException;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:checkout-flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutFlowTest {

    private static final String REQUEST = OrderPersistenceTest.REQUEST;
    private static final String KEY = "checkout-request-0001";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    OrderSubmissionService submissions;

    @BeforeEach
    void prepare() {
        resetDatabase();
    }

    @AfterEach
    void cleanUp() {
        resetDatabase();
    }

    @Test
    void creatingOrderReservesStockAndPreviewRemainsReadOnly() throws Exception {
        String id = create(REQUEST, null).get("orderId").asText();
        assertEquals(18, stock("P001"));
        assertEquals(1, orderCount());
        mvc.perform(post("/api/settlements/preview").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payableAmount").value(62.00));
        assertEquals(18, stock("P001"));
        mvc.perform(get("/api/orders/" + id)).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void cancellingPendingOrderRestoresStockExactlyOnce() throws Exception {
        String id = create(REQUEST, null).get("orderId").asText();
        cancel(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(20, stock("P001"));
        cancel(id).andExpect(status().isConflict());
        assertEquals(20, stock("P001"));
    }

    @Test
    void cancellingPaidOrderAlsoRestoresStock() throws Exception {
        String id = create(REQUEST, null).get("orderId").asText();
        transition(id, "PAID").andExpect(status().isOk());
        cancel(id).andExpect(status().isOk());
        assertEquals(20, stock("P001"));
    }

    @Test
    void cancellingAnOlderOrderWithoutReservationDoesNotInflateStock() throws Exception {
        String id = create(REQUEST, null).get("orderId").asText();
        jdbc.update("UPDATE settlement_orders SET stock_reserved = FALSE WHERE order_id = ?", id);
        jdbc.update("UPDATE products SET stock = 20 WHERE product_id = 'P001'");
        cancel(id).andExpect(status().isOk());
        assertEquals(20, stock("P001"));
    }

    @Test
    void shippingAnOrderKeepsStockReserved() throws Exception {
        String id = create(REQUEST, null).get("orderId").asText();
        transition(id, "PAID").andExpect(status().isOk());
        transition(id, "SHIPPED").andExpect(status().isOk());
        cancel(id).andExpect(status().isConflict());
        assertEquals(18, stock("P001"));
    }

    @Test
    void repeatedKeyReplaysExistingOrderWithoutAnotherReservation() throws Exception {
        String id = create(REQUEST, KEY).get("orderId").asText();
        String equivalentJson = json.writeValueAsString(json.readTree(REQUEST));
        JsonNode replay = postOrder(equivalentJson, KEY, 200);
        assertEquals(id, replay.get("orderId").asText());
        assertEquals(1, orderCount());
        assertEquals(18, stock("P001"));
        assertEquals(1, claimCount());
    }

    @Test
    void reusedKeyWithDifferentRequestReturnsConflict() throws Exception {
        create(REQUEST, KEY);
        ObjectNode changed = (ObjectNode) json.readTree(REQUEST);
        ((ObjectNode) changed.withArray("items").get(0)).put("quantity", 3);
        mvc.perform(post("/api/orders").header("Idempotency-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(changed)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        assertEquals(18, stock("P001"));
        assertEquals(1, orderCount());
    }

    @Test
    void replayAfterCancellationReturnsTheCancelledOrder() throws Exception {
        String id = create(REQUEST, KEY).get("orderId").asText();
        cancel(id).andExpect(status().isOk());
        assertEquals("CANCELLED", postOrder(REQUEST, KEY, 200).get("status").asText());
        assertEquals(20, stock("P001"));
        assertEquals(1, orderCount());
    }

    @Test
    void replayUsesHistoricalSnapshotAfterCatalogChanges() throws Exception {
        String id = create(REQUEST, KEY).get("orderId").asText();
        jdbc.update("UPDATE products SET unit_price = 99.99 WHERE product_id = 'P001'");
        JsonNode replay = postOrder(REQUEST, KEY, 200);
        assertEquals(id, replay.get("orderId").asText());
        assertEquals(0, new BigDecimal("62.00").compareTo(replay.get("payableAmount").decimalValue()));
        assertEquals(0, new BigDecimal("50.00").compareTo(replay.get("items").get(0).get("unitPrice").decimalValue()));
        assertEquals(18, stock("P001"));
    }

    @Test
    void distinctKeysCreateDistinctOrders() throws Exception {
        String first = create(REQUEST, KEY).get("orderId").asText();
        String second = create(REQUEST, "checkout-request-0002").get("orderId").asText();
        assertNotEquals(first, second);
        assertEquals(16, stock("P001"));
        assertEquals(2, orderCount());
    }

    @Test
    void requestsWithoutKeysRemainSupported() throws Exception {
        assertNotEquals(create(REQUEST, null).get("orderId").asText(),
                create(REQUEST, null).get("orderId").asText());
        assertEquals(16, stock("P001"));
    }

    @Test
    void invalidKeyIsRejectedBeforeReservingStock() throws Exception {
        mvc.perform(post("/api/orders").header("Idempotency-Key", "short")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
        assertEquals(20, stock("P001"));
        assertEquals(0, claimCount());
    }

    @Test
    void failedAttemptReleasesTheKeyForANewAttempt() throws Exception {
        ObjectNode tooMany = (ObjectNode) json.readTree(REQUEST);
        ((ObjectNode) tooMany.withArray("items").get(0)).put("quantity", 21);
        mvc.perform(post("/api/orders").header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(tooMany)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertEquals(0, claimCount());
        create(REQUEST, KEY);
        assertEquals(18, stock("P001"));
        assertEquals(1, claimCount());
    }

    @Test
    void multipleProductsAreReservedAndRestoredTogether() throws Exception {
        ObjectNode request = (ObjectNode) json.readTree(REQUEST);
        request.withArray("items").addObject().put("productId", "P002").put("quantity", 3);
        String id = create(json.writeValueAsString(request), KEY).get("orderId").asText();
        assertEquals(18, stock("P001"));
        assertEquals(7, stock("P002"));
        cancel(id).andExpect(status().isOk());
        assertEquals(20, stock("P001"));
        assertEquals(10, stock("P002"));
    }

    @Test
    void concurrentDifferentOrdersCannotOversell() throws Exception {
        SettlementPreviewRequest request = quantityRequest(15);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<Object> first = pool.submit(runSubmission(start, request, "checkout-request-0001"));
            Future<Object> second = pool.submit(runSubmission(start, request, "checkout-request-0002"));
            start.countDown();
            List<Object> results = List.of(first.get(), second.get());
            assertEquals(1, results.stream().filter(OrderSubmissionResult.class::isInstance).count());
            assertEquals(1, results.stream().filter(InsufficientStockException.class::isInstance).count());
            assertEquals(5, stock("P001"));
            assertEquals(1, orderCount());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentRetriesWithSameKeyCreateOneOrder() throws Exception {
        SettlementPreviewRequest request = quantityRequest(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<Object> first = pool.submit(runSubmission(start, request, KEY));
            Future<Object> second = pool.submit(runSubmission(start, request, KEY));
            start.countDown();
            OrderSubmissionResult one = assertInstanceOf(OrderSubmissionResult.class, first.get());
            OrderSubmissionResult two = assertInstanceOf(OrderSubmissionResult.class, second.get());
            assertEquals(one.order().orderId(), two.order().orderId());
            assertTrue(one.created() != two.created());
            assertEquals(18, stock("P001"));
            assertEquals(1, orderCount());
            assertEquals(1, claimCount());
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Object> runSubmission(CountDownLatch start, SettlementPreviewRequest request, String key) {
        return () -> {
            start.await();
            try {
                return submissions.submit(request, key);
            } catch (InsufficientStockException exception) {
                return exception;
            }
        };
    }

    private SettlementPreviewRequest quantityRequest(int quantity) throws Exception {
        ObjectNode request = (ObjectNode) json.readTree(REQUEST);
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", quantity);
        request.putArray("couponCodes");
        request.put("deliveryType", "SELF_PICKUP");
        return json.treeToValue(request, SettlementPreviewRequest.class);
    }

    private JsonNode create(String body, String key) throws Exception {
        return postOrder(body, key, 201);
    }

    private JsonNode postOrder(String body, String key, int expectedStatus) throws Exception {
        var request = post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        var response = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse();
        assertTrue(response.getHeader("Location").startsWith("/api/orders/"));
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.ResultActions cancel(String id) throws Exception {
        return transition(id, "CANCELLED");
    }

    private org.springframework.test.web.servlet.ResultActions transition(String id, String status) throws Exception {
        return mvc.perform(patch("/api/orders/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private int stock(String productId) {
        return jdbc.queryForObject("SELECT stock FROM products WHERE product_id = ?", Integer.class, productId);
    }

    private int orderCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM settlement_orders", Integer.class);
    }

    private int claimCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_requests", Integer.class);
    }

    private void resetDatabase() {
        jdbc.update("DELETE FROM idempotency_requests");
        jdbc.update("DELETE FROM order_coupons");
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM settlement_orders");
        jdbc.update("UPDATE products SET stock = 20, unit_price = 50.00 WHERE product_id = 'P001'");
        jdbc.update("UPDATE products SET stock = 10, unit_price = 38.00 WHERE product_id = 'P002'");
    }
}
