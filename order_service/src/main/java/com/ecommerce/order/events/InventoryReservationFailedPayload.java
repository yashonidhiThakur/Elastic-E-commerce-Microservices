package com.ecommerce.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationFailedPayload {
    private String reason;
    private List<String> failedItems;
}
