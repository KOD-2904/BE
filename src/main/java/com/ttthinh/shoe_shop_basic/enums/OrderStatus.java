package com.ttthinh.shoe_shop_basic.enums;

public enum OrderStatus {

    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {

            case PENDING ->
                    next == CONFIRMED || next == CANCELED;

            case CONFIRMED ->
                    next == SHIPPED || next == CANCELED;

            case SHIPPED ->
                    next == DELIVERED;

            case DELIVERED ->
                    next == COMPLETED;

            case COMPLETED, CANCELED ->
                    false;
        };
    }

    public boolean userCanCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean adminCanCancel() {
        return this != DELIVERED && this != COMPLETED && this != CANCELED;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELED;
    }
}