package org.RealEstate.dto;

import java.time.LocalDate;

public class DateRangeDTO {
    private LocalDate fromDate;
    private LocalDate toDate;

    public DateRangeDTO(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}
