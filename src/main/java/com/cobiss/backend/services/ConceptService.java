package com.cobiss.backend.services;

import com.cobiss.backend.models.SkosConcept;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.repositories.ConceptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConceptService {

    private final ConceptRepository conceptRepository;

    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public Optional<SkosConcept> getConceptByUri(String uri) {
        return conceptRepository.findByUri(uri);
    }

    public List<SkosConcept> searchConcepts(String text, int limit) {
        // Sanitize limit to prevent accidental 700k fetches
        int safeLimit = Math.min(limit, 100);
        return conceptRepository.searchByText(text, safeLimit);
    }

    public List<SkosConceptScheme> getAllSchemes() {
        return conceptRepository.findAllSchemes();
    }
}