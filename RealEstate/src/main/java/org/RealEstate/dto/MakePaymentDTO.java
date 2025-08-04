package org.RealEstate.dto;

import java.math.BigDecimal;

public class MakePaymentDTO {
    private long contractId;
    private BigDecimal amount;

    public MakePaymentDTO(long contractId, BigDecimal amount) {
        this.contractId = contractId;
        this.amount = amount;
    }

    public long getContractId() {
        return contractId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
