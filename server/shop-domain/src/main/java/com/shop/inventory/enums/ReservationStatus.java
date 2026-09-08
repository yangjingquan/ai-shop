package com.shop.inventory.enums;

/** Durable resource reservation states. Transitions are intentionally one-way. */
public enum ReservationStatus {
    RESERVED(0), CONFIRMED(1), RELEASED(2), RESTOCKED(3);

    private final int code;

    ReservationStatus(int code) { this.code = code; }

    public int getCode() { return code; }
}
