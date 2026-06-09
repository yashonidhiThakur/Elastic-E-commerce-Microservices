package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutRequest {
    @JsonProperty("cart_items")
    private List<CartItem> cartItems;

    public CheckoutRequest() {
    }

    public CheckoutRequest(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }
}
