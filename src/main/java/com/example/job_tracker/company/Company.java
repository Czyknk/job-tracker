package com.example.job_tracker.company;

import com.example.job_tracker.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String industry;

    @Column(length = 500)
    private String website;

    @Column(columnDefinition = "text")
    private String notes;


    public Company(String name) {
        this.name = name;
    }

}
