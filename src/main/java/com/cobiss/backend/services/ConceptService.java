package com.cobiss.backend.services;

import com.cobiss.backend.models.ConceptProjection;
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

    public ConceptProjection getConceptByUri(String uri) {
        return conceptRepository.findByUri(uri).orElse(null);
    }

    public List<ConceptProjection> searchConcepts(String text, int limit) {
        int safeLimit = Math.min(limit, 100);
        return conceptRepository.searchByText(text, limit);
    }

    public List<SkosConceptScheme> getAllSchemes() {
        return conceptRepository.findAllSchemes();
    }
}