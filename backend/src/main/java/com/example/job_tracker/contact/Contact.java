package com.example.job_tracker.contact;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Osoba z firmy (lub agencji), z którą mamy kontakt w sprawie rekrutacji.
 */
@Entity
@Table(name = "contact")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 255)
    private String position;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(columnDefinition = "text")
    private String notes;

    public Contact(Company company, String firstName) {
        this.company = company;
        this.firstName = firstName;
    }
}
