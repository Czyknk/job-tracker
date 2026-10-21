package com.example.job_tracker.entity;

import com.example.job_tracker.entity.enums.SalaryBasis;
import com.example.job_tracker.entity.enums.SalaryPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class SalaryRange {
    
    @Column(name = "salary_min")
    private BigDecimal min;
    
    @Column(name = "salary_max")
    private BigDecimal max;
    
    @Column(name = "salary_currency", length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "salary_period", length = 10)
    private SalaryPeriod period;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "salary_basis", length = 10)
    private SalaryBasis basis;
}
