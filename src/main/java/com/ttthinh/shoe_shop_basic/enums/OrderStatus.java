package com.ttthinh.shoe_shop_basic.enums;

public enum OrderStatus {

    PENDING,
    CONFIRMED,
    PACKING,
    READY_TO_SHIP,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    FAILED,
    RETURNED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {

            case PENDING ->
                    next == CONFIRMED || next == CANCELLED;

            case CONFIRMED ->
                    next == PACKING || next == CANCELLED;

            case PACKING ->
                    next == READY_TO_SHIP;

            case READY_TO_SHIP ->
                    next == SHIPPING;

            case SHIPPING ->
                    next == DELIVERED || next == FAILED;

            case FAILED ->
                    next == SHIPPING || next == RETURNED;

            case DELIVERED, CANCELLED, RETURNED ->
                    false;
        };
    }

    public boolean userCanCancel() {
        return this == PENDING;
    }

    public boolean adminCanCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED;
    }
}
