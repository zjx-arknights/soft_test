package com.houkiang.ordersettlement.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@ActiveProfiles("test")
class HttpServerSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void startsApplicationAndServesRealHttpRequests() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> products = client.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/products"))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, products.statusCode());
        assertEquals("P001", mapper.readTree(products.body()).get(0).get("productId").asText());

        HttpRequest preview = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/settlements/preview"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "items": [{"productId": "P001", "quantity": 2}],
                          "address": {"recipientName": "Test", "phone": "123", "detailAddress": "Test Road",
                            "regionType": "NORMAL"},
                          "deliveryType": "STANDARD",
                          "couponCodes": ["FULL100_20", "DISCOUNT90", "FIXED10"]
                        }
                        """))
                .build();
        HttpResponse<String> response = client.send(preview, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonNode result = mapper.readTree(response.body());
        assertEquals(62, result.get("payableAmount").intValue());
        assertEquals(3, result.get("appliedCouponCodes").size());
    }
}
