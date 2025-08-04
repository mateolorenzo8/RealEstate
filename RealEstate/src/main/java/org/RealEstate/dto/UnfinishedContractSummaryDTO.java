package org.RealEstate.dto;

import java.math.BigDecimal;

public class UnfinishedContractSummaryDTO {
    private long contractId;
    private BigDecimal expected;
    private BigDecimal actual;

    public UnfinishedContractSummaryDTO(long contractId, BigDecimal expected, BigDecimal actual) {
        this.contractId = contractId;
        this.expected = expected;
        this.actual = actual;
    }

    public long getContractId() {
        return contractId;
    }

    public BigDecimal getExpected() {
        return expected;
    }

    public BigDecimal getActual() {
        return actual;
    }
}
