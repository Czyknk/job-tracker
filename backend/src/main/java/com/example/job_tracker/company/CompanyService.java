package com.example.job_tracker.company;

import com.example.job_tracker.common.exceptions.ConflictException;
import com.example.job_tracker.common.exceptions.ResourceNotFoundException;
import com.example.job_tracker.company.dto.CompanyOption;
import com.example.job_tracker.company.dto.CompanyRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    public static final int LOOKUP_DEFAULT_LIMIT = 10;
    public static final int LOOKUP_MAX_LIMIT = 20;

    private final CompanyRepository companyRepository;

    @Transactional
    public Company create(CompanyRequest request) {
        String name = request.name().trim();
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

    /**
     * Autocomplete: companies whose name contains q (case-insensitive), names starting with q first, then by name.
     * A blank q returns the first companies by name. The limit is clamped to 1..20 instead of rejected.
     */
    @Transactional(readOnly = true)
    public List<CompanyOption> lookup(String q, Integer limit) {
        String term = q == null ? "" : escapeLike(q.trim().toLowerCase());
        int size = limit == null ? LOOKUP_DEFAULT_LIMIT : Math.clamp(limit, 1, LOOKUP_MAX_LIMIT);
        return companyRepository.lookup("%" + term + "%", term + "%", Limit.of(size));
    }

    // Makes %, _ and the escape char itself match literally ('!' is the escape char in the query)
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
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
