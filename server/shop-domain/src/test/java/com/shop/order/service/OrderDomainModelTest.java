package com.shop.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.FulfillmentMethod;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDomainModelTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void initializesAllCanonicalOrderTypesWithoutChangingPersistedCodes() {
        for (OrderType type : OrderType.values()) {
            Order order = new Order();
            OrderDomainModel.initialize(order, type, FulfillmentMethod.EXPRESS);
            assertEquals(type.getCode(), order.getOrderType());
            assertEquals(FulfillmentMethod.EXPRESS.getCode(), order.getFulfillmentMethod());
            assertEquals(type.getText(), OrderDomainModel.orderTypeText(order.getOrderType()));
        }
    }

    @Test
    void freezesOrderAndItemMeaningAtCreationTime() throws Exception {
        Order order = new Order();
        order.setOrderType(OrderType.BUNDLE.getCode());
        order.setFulfillmentMethod(FulfillmentMethod.EXPRESS.getCode());
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        order.setAddressSnapshot("{\"receiver\":\"张三\"}");
        order.setPricingSnapshotJson("{\"ruleVersion\":62}");
        order.setBundleActivityId(99L);
        OrderDomainModel.refreshOrderSnapshot(order);

        OrderItem item = new OrderItem();
        item.setProductId(10L); item.setSkuId(11L); item.setProductName("套餐主商品");
        item.setSpecText("蓝色"); item.setUnitPrice(new BigDecimal("88.00"));
        item.setQuantity(2); item.setSubtotal(new BigDecimal("176.00"));
        item.setPricingSnapshotJson("{\"price\":88}");
        OrderDomainModel.refreshItemSnapshot(item, order);

        JsonNode orderSnapshot = objectMapper.readTree(order.getOrderSnapshotJson());
        JsonNode itemSnapshot = objectMapper.readTree(item.getItemSnapshotJson());
        assertEquals("BUNDLE", orderSnapshot.path("orderType").asText());
        assertEquals("EXPRESS", orderSnapshot.path("fulfillmentMethod").asText());
        assertEquals("套餐主商品", itemSnapshot.path("productName").asText());
        assertEquals(2, itemSnapshot.path("quantity").asInt());

        item.setProductName("后来改名");
        assertEquals("套餐主商品", objectMapper.readTree(item.getItemSnapshotJson()).path("productName").asText());
    }

    @Test
    void exposesCompleteSingleSourceDictionary() {
        var dictionary = OrderDomainModel.dictionary();
        assertEquals(OrderType.values().length, dictionary.getOrderTypes().size());
        assertEquals(OrderStatus.values().length, dictionary.getStates().size());
        assertEquals(FulfillmentMethod.values().length, dictionary.getFulfillmentMethods().size());
        assertTrue(dictionary.getStates().stream().anyMatch(item -> item.getCode() == OrderStatus.PRESALE_WAIT_BALANCE.getCode()));
        assertFalse(dictionary.getOrderTypes().stream().anyMatch(item -> item.getText().isBlank()));
    }
}
