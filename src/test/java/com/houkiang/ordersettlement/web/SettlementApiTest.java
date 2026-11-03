package com.houkiang.ordersettlement.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettlementApiTest {

    private static final String PREVIEW = "/api/settlements/preview";
    private static final String VALID_REQUEST = """
            {
              "items": [{"productId": "P001", "quantity": 2}],
              "address": {
                "recipientName": "张三", "phone": "13800138000",
                "detailAddress": "北京市测试路1号", "regionType": "NORMAL"
              },
              "deliveryType": "STANDARD",
              "couponCodes": []
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void listsServerProductsInStableOrder() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].productId", contains("P001", "P002", "P003")))
                .andExpect(jsonPath("$[0].unitPrice").value(50.00))
                .andExpect(jsonPath("$[0].stock").value(20))
                .andExpect(jsonPath("$[2].stock").value(0));
    }

    @Test
    void listsServerCouponRules() throws Exception {
        mvc.perform(get("/api/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].couponCode", contains("DISCOUNT90", "FIXED10", "FULL100_20")))
                .andExpect(jsonPath("$[0].discountRate").value(0.9))
                .andExpect(jsonPath("$[2].thresholdAmount").value(100.00));
    }

    @Test
    void previewsThreeCouponsWithOriginalAmountFreeShipping() throws Exception {
        ObjectNode request = request();
        request.putArray("couponCodes").add("FULL100_20").add("DISCOUNT90").add("FIXED10");
        preview(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.itemAmount").value(100.00))
                .andExpect(jsonPath("$.discountAmount").value(38.00))
                .andExpect(jsonPath("$.shippingFee").value(0.00))
                .andExpect(jsonPath("$.payableAmount").value(62.00))
                .andExpect(jsonPath("$.appliedCouponCodes", contains("FULL100_20", "DISCOUNT90", "FIXED10")))
                .andExpect(jsonPath("$.orderId").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void treatsNullOrOmittedCouponsAsNoDiscount(boolean explicitNull) throws Exception {
        ObjectNode request = request();
        if (explicitNull) {
            request.putNull("couponCodes");
        } else {
            request.remove("couponCodes");
        }
        preview(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.payableAmount").value(100.00))
                .andExpect(jsonPath("$.discountAmount").value(0.00))
                .andExpect(jsonPath("$.appliedCouponCodes", hasSize(0)));
    }

    @ParameterizedTest
    @CsvSource({"STANDARD,NORMAL,1,8", "STANDARD,REMOTE,4,15",
            "EXPRESS,NORMAL,3,15", "EXPRESS,NORMAL,4,0", "SELF_PICKUP,REMOTE,1,0"})
    void appliesDeliveryAndRegionRules(String delivery, String region, int quantity, double fee) throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", quantity);
        ((ObjectNode) request.get("address")).put("regionType", region);
        request.put("deliveryType", delivery);
        preview(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingFee").value(fee))
                .andExpect(jsonPath("$.payableAmount").value(50.00 * quantity + fee));
    }

    @Test
    void grantsFreeShippingAtEightyEight() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 1);
        request.withArray("items").addObject().put("productId", "P002").put("quantity", 1);
        preview(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.itemAmount").value(88.00))
                .andExpect(jsonPath("$.shippingFee").value(0.00));
    }

    @Test
    void mergesRepeatedProductsBeforePricing() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 1);
        request.withArray("items").addObject().put("productId", "P001").put("quantity", 1);
        request.withArray("couponCodes").add("FULL100_20");
        preview(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.itemAmount").value(100.00))
                .andExpect(jsonPath("$.payableAmount").value(80.00));
    }

    @Test
    void checksStockAfterMergingRepeatedProducts() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 12);
        request.withArray("items").addObject().put("productId", "P001").put("quantity", 9);
        preview(request).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void repeatedPreviewsDoNotDeductStock() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 20);
        preview(request).andExpect(status().isOk());
        preview(request).andExpect(status().isOk());
        mvc.perform(get("/api/products")).andExpect(jsonPath("$[0].stock").value(20));
    }

    @Test
    void reportsUnknownProduct() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("productId", "MISSING");
        preview(request).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void reportsUnknownCoupon() throws Exception {
        ObjectNode request = request();
        request.withArray("couponCodes").add("MISSING");
        preview(request).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
    }

    @Test
    void rejectsOutOfStockProduct() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("productId", "P003");
        preview(request).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void rejectsCouponBelowThreshold() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.withArray("items").get(0)).put("quantity", 1);
        request.withArray("couponCodes").add("FULL100_20");
        preview(request).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_APPLICABLE"));
    }

    @Test
    void appliesCouponsInRequestOrder() throws Exception {
        ObjectNode request = request();
        request.withArray("couponCodes").add("DISCOUNT90").add("FULL100_20");
        preview(request).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_APPLICABLE"));
    }

    @Test
    void rejectsRepeatedCouponCode() throws Exception {
        ObjectNode request = request();
        request.withArray("couponCodes").add("FIXED10").add("FIXED10");
        preview(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COUPON"));
    }

    @Test
    void rejectsExpressDeliveryToRemoteRegion() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.get("address")).put("regionType", "REMOTE");
        request.put("deliveryType", "EXPRESS");
        preview(request).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_DELIVERY"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidJsonAndClientDefinedPrices(String body) throws Exception {
        mvc.perform(post(PREVIEW).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    static Stream<String> invalidRequests() {
        return Stream.of("{", "", "null",
                VALID_REQUEST.replace("STANDARD", "UNKNOWN"),
                VALID_REQUEST.replace("\"STANDARD\"", "0"),
                VALID_REQUEST.replace("NORMAL", "UNKNOWN"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": 1.5"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": 2147483648"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": 2, \"unitPrice\": 0.01"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": 2, \"stock\": 10000"));
    }

    @ParameterizedTest
    @MethodSource("validationFailures")
    void returnsFieldViolationsForInvalidParameters(String body) throws Exception {
        mvc.perform(post(PREVIEW).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", not(hasSize(0))));
    }

    static Stream<Arguments> validationFailures() {
        return Stream.of(
                VALID_REQUEST.replace("[{\"productId\": \"P001\", \"quantity\": 2}]", "[]"),
                VALID_REQUEST.replace("[{\"productId\": \"P001\", \"quantity\": 2}]", "null"),
                VALID_REQUEST.replace("[{\"productId\": \"P001\", \"quantity\": 2}]", "[null]"),
                VALID_REQUEST.replace("\"P001\"", "\" \""),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": 0"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": -1"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": 10001"),
                VALID_REQUEST.replace("\"quantity\": 2", "\"quantity\": null"),
                VALID_REQUEST.replace("\"张三\"", "\" \""),
                VALID_REQUEST.replace("\"13800138000\"", "\"\""),
                VALID_REQUEST.replace("\"北京市测试路1号\"", "\"\""),
                VALID_REQUEST.replace("\"NORMAL\"", "null"),
                VALID_REQUEST.replace("\"STANDARD\"", "null"),
                VALID_REQUEST.replace("\"couponCodes\": []", "\"couponCodes\": [\"\"]"),
                VALID_REQUEST.replace("\"couponCodes\": []", "\"couponCodes\": [null]"))
                .map(Arguments::of);
    }

    @Test
    void reportsNestedFieldPathWithoutEchoingPersonalData() throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.get("address")).put("recipientName", " ");
        preview(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("address.recipientName"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    }

    @Test
    void rejectsMissingAddress() throws Exception {
        ObjectNode request = request();
        request.remove("address");
        preview(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("address"));
    }

    @Test
    void boundsItemListBeforeCalculating() throws Exception {
        ObjectNode request = request();
        ArrayNode items = request.putArray("items");
        for (int i = 0; i < 101; i++) {
            items.addObject().put("productId", "P001").put("quantity", 10000);
        }
        preview(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void handlesUnsupportedHttpMethod() throws Exception {
        mvc.perform(get(PREVIEW)).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void handlesUnsupportedContentType() throws Exception {
        mvc.perform(post(PREVIEW).contentType(MediaType.TEXT_PLAIN).content(VALID_REQUEST))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void handlesUnsupportedResponseType() throws Exception {
        mvc.perform(get("/api/products").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void handlesMissingRoute() throws Exception {
        mvc.perform(get("/api/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private ObjectNode request() throws Exception {
        return (ObjectNode) mapper.readTree(VALID_REQUEST);
    }

    private ResultActions preview(ObjectNode request) throws Exception {
        return mvc.perform(post(PREVIEW).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(request)));
    }
}
