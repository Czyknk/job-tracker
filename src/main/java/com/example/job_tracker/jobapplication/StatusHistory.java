package com.example.job_tracker.jobapplication;

import com.example.job_tracker.common.BaseEntity;
import com.example.job_tracker.contact.Contact;
import com.example.job_tracker.jobapplication.enums.RecruitmentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Wpis w historii rekrutacji. Log zmian jest niezmienny.
 */
@Entity
@Table(name = "status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory extends BaseEntity {

    @ManyToMany
    @JoinTable(name = "status_history_contact",
            joinColumns = @JoinColumn(name = "status_history_id"),
            inverseJoinColumns = @JoinColumn(name = "contact_id"))
    private Set<Contact> contacts = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecruitmentStatus status;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(columnDefinition = "text")
    private String note;

    public StatusHistory(JobApplication application, RecruitmentStatus status, Instant changedAt, String note) {
        this.application = application;
        this.status = status;
        this.changedAt = changedAt;
        this.note = note;
    }
    
    public Set<Contact> getContacts() {
        return Collections.unmodifiableSet(contacts);
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
    }

    public boolean removeContactById(Long contactId) {
        return contacts.removeIf(c -> Objects.equals(c.getId(), contactId));
    }
    
}
