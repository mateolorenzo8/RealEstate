package org.RealEstate.dto;

import org.RealEstate.enums.PropertyType;

import java.math.BigDecimal;

public class FinishedContractSummaryDTO {
    private PropertyType propertyType;
    private long quantity;
    private BigDecimal total;

    public FinishedContractSummaryDTO(PropertyType propertyType, long quantity, BigDecimal total) {
        this.propertyType = propertyType;
        this.quantity = quantity;
        this.total = total;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
