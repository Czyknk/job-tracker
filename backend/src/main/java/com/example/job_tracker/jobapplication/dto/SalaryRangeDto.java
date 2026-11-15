package com.example.job_tracker.jobapplication.dto;

import com.example.job_tracker.jobapplication.SalaryRange;
import com.example.job_tracker.jobapplication.model.SalaryBasis;
import com.example.job_tracker.jobapplication.model.SalaryPeriod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SalaryRangeDto(
        @Schema(example = "8000")
        @PositiveOrZero BigDecimal min,
        @Schema(example = "12000")
        @PositiveOrZero BigDecimal max,
        @Schema(example = "PLN")
        @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code, e.g. PLN") String currency,
        SalaryPeriod period,
        SalaryBasis basis) {

    @JsonIgnore
    @SuppressWarnings("unused") // wywoływana przez Bean Validation (@AssertTrue)
    @AssertTrue(message = "min must not be greater than max")
    public boolean isRangeValid() {
        return min == null || max == null || min.compareTo(max) <= 0;
    }

    public SalaryRange toEntity() {
        return new SalaryRange(min, max, currency, period, basis);
    }

    public static SalaryRangeDto from(SalaryRange salary) {
        if (salary == null) {
            return null;
        }
        return new SalaryRangeDto(
                salary.getMin(), salary.getMax(), salary.getCurrency(), salary.getPeriod(), salary.getBasis());
    }
}
