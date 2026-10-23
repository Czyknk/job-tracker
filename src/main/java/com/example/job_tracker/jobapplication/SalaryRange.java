package com.example.job_tracker.jobapplication;

import com.example.job_tracker.jobapplication.enums.SalaryBasis;
import com.example.job_tracker.jobapplication.enums.SalaryPeriod;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Value object: widełki wynagrodzenia niezmienne.
 * Nazwy kolumn nadpisuje JobApplication (@AttributeOverrides).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class SalaryRange {

    private BigDecimal min;

    private BigDecimal max;

    private String currency;

    @Enumerated(EnumType.STRING)
    private SalaryPeriod period;

    @Enumerated(EnumType.STRING)
    private SalaryBasis basis;


    public SalaryRange(BigDecimal min, BigDecimal max, String currency,
                       SalaryPeriod period, SalaryBasis basis) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimal wage cannot be greater than maximum wage");
        }
        this.min = min;
        this.max = max;
        this.currency = currency;
        this.period = period;
        this.basis = basis;
    }

}
