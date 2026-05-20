package com.cobiss.backend.services;

import com.cobiss.backend.models.ConceptProjection;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.repositories.ConceptRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return conceptRepository.searchByText(text, limit);
    }

    public List<SkosConceptScheme> getAllSchemes() {
        return conceptRepository.findAllSchemes();
    }

    // Pass relationship lookups to the controller context
    public List<ConceptProjection> getBroader(String uri) {
        return conceptRepository.findBroader(uri);
    }

    public List<ConceptProjection> getNarrower(String uri) {
        return conceptRepository.findNarrower(uri);
    }

    public List<ConceptProjection> getRelated(String uri) {
        return conceptRepository.findRelated(uri);
    }
}