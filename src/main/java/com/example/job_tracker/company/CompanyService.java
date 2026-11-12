package com.example.job_tracker.company;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.dto.CompanyRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public Company create(CompanyRequest request) {
        String name = request.name().trim();
        // Czytelny błąd 409; unikalny indeks w bazie zostaje ostatnią linią obrony (wyścigi).
        if (companyRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Company with name '" + name + "' already exists");
        }
        Company company = new Company(name);
        company.setIndustry(request.industry());
        company.setWebsite(request.website());
        company.setNotes(request.notes());
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public Company get(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    @Transactional(readOnly = true)
    public List<Company> list() {
        return companyRepository.findAll(Sort.by("name"));
    }

    @Transactional
    public Company update(Long id, CompanyRequest request) {
        Company company = get(id);
        String name = request.name().trim();
        if (companyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Company with name '" + name + "' already exists");
        }
        company.setName(name);
        company.setIndustry(request.industry());
        company.setWebsite(request.website());
        company.setNotes(request.notes());
        return company;
    }

    @Transactional
    public void delete(Long id) {
        Company company = get(id);
        long applications = companyRepository.countApplications(id);
        if (applications > 0) {
            throw new ConflictException(
                    "Company has " + applications + " application(s); delete or reassign them first");
        }
        companyRepository.delete(company);
    }
}
