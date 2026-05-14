package com.cobiss.backend.controllers;

import com.cobiss.backend.models.SkosConcept;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.services.ConceptService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class ConceptGraphQLController {

    private final ConceptService conceptService;

    public ConceptGraphQLController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    // Matches 'concept(uri: String!)': Concept in schema
    @QueryMapping
    public Optional<SkosConcept> concept(@Argument String uri) {
        return conceptService.getConceptByUri(uri);
    }

    // Matches 'searchConcepts(text: String!, limit: Int): [Concept]'
    @QueryMapping
    public List<SkosConcept> searchConcepts(@Argument String text, @Argument int limit) {
        return conceptService.searchConcepts(text, limit);
    }

    // Matches 'schemes: [ConceptScheme]'
    @QueryMapping
    public List<SkosConceptScheme> schemes() {
        return conceptService.getAllSchemes();
    }
}