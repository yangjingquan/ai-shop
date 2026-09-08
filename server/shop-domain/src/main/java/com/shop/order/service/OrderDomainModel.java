package com.shop.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.enums.FulfillmentMethod;
import com.shop.order.enums.OrderStatus;
import com.shop.order.enums.OrderType;
import com.shop.order.dto.OrderDictionaryVO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;

/** Shared persistence and presentation vocabulary for every order producer. */
public final class OrderDomainModel {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OrderDomainModel() {
    }

    public static void initialize(Order order, OrderType type, FulfillmentMethod fulfillmentMethod) {
        order.setOrderType(type.getCode());
        order.setFulfillmentMethod(fulfillmentMethod.getCode());
    }

    public static void refreshOrderSnapshot(Order order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderType", OrderType.fromCode(order.getOrderType()).name());
        snapshot.put("orderTypeCode", order.getOrderType());
        snapshot.put("state", OrderStatus.statusText(order.getStatus() == null ? -1 : order.getStatus()));
        snapshot.put("stateCode", order.getStatus());
        snapshot.put("fulfillmentMethod", FulfillmentMethod.fromCode(order.getFulfillmentMethod()).name());
        snapshot.put("fulfillmentMethodCode", order.getFulfillmentMethod());
        snapshot.put("address", order.getAddressSnapshot());
        snapshot.put("pricing", order.getPricingSnapshotJson());
        snapshot.put("coupon", order.getCouponSnapshotJson());
        snapshot.put("promotion", order.getPromotionSnapshotJson());
        snapshot.put("bundle", order.getBundleSnapshotJson());
        snapshot.put("groupBuyGroupId", order.getGroupBuyGroupId());
        snapshot.put("seckillSessionId", order.getSeckillSessionId());
        snapshot.put("seckillSkuId", order.getSeckillSkuId());
        snapshot.put("pointsRedeemId", order.getPointsRedeemId());
        snapshot.put("lotteryRewardId", order.getLotteryRewardId());
        snapshot.put("presaleOrderId", order.getPresaleOrderId());
        snapshot.put("presaleStage", order.getPresaleStage());
        order.setOrderSnapshotJson(toJson(snapshot));
    }

    public static void refreshItemSnapshot(OrderItem item, Order order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderType", OrderType.fromCode(order.getOrderType()).name());
        snapshot.put("fulfillmentMethod", FulfillmentMethod.fromCode(order.getFulfillmentMethod()).name());
        snapshot.put("productId", item.getProductId());
        snapshot.put("skuId", item.getSkuId());
        snapshot.put("productName", item.getProductName());
        snapshot.put("mainImage", item.getMainImage());
        snapshot.put("specText", item.getSpecText());
        snapshot.put("unitPrice", item.getUnitPrice());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("subtotal", item.getSubtotal());
        snapshot.put("pricing", item.getPricingSnapshotJson());
        item.setItemSnapshotJson(toJson(snapshot));
    }

    public static String orderTypeText(Integer code) {
        return OrderType.fromCode(code).getText();
    }

    public static String fulfillmentMethodText(Integer code) {
        return FulfillmentMethod.fromCode(code).getText();
    }

    public static OrderDictionaryVO dictionary() {
        OrderDictionaryVO result = new OrderDictionaryVO();
        result.setOrderTypes(Arrays.stream(OrderType.values())
                .map(value -> new OrderDictionaryVO.Item(value.getCode(), value.getText())).toList());
        result.setStates(Arrays.stream(OrderStatus.values())
                .map(value -> new OrderDictionaryVO.Item(value.getCode(), value.getText())).toList());
        result.setFulfillmentMethods(Arrays.stream(FulfillmentMethod.values())
                .map(value -> new OrderDictionaryVO.Item(value.getCode(), value.getText())).toList());
        return result;
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("订单领域快照序列化失败", e);
        }
    }
}
