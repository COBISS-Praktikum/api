package com.cobiss.backend.controllers;

import com.cobiss.backend.models.ConceptProjection;
import com.cobiss.backend.models.SkosConcept;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.services.ConceptService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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
    public ConceptProjection concept(@Argument String uri) {
        return conceptService.getConceptByUri(uri);
    }

    // Matches 'searchConcepts(text: String!, limit: Int): [Concept]'
    @QueryMapping
    public List<ConceptProjection> searchConcepts(@Argument String text, @Argument Integer limit) {
        int safeLimit = (limit != null) ? limit : 20;
        return conceptService.searchConcepts(text, safeLimit);
    }
    // Matches 'schemes: [ConceptScheme]'
    @QueryMapping
    public List<SkosConceptScheme> schemes() {
        return conceptService.getAllSchemes();
    }

    @SchemaMapping(typeName = "Concept", field = "prefLabel")
    public String prefLabel(ConceptProjection concept) {
        // 1. Check if the projection pre-calculated the Slovenian string
        if (concept.getPrefLabelSl() != null) {
            return concept.getPrefLabelSl();
        }

        // 2. Check if the projection pre-calculated the English string
        if (concept.getPrefLabelEn() != null) {
            return concept.getPrefLabelEn();
        }

        // 3. Robust Fallback: Parse the raw database list if it's a nested node (like 'broader')
        List<String> rawLabels = concept.getRawPrefLabels();
        if (rawLabels != null && !rawLabels.isEmpty()) {
            // Find the string ending with @sl
            for (String label : rawLabels) {
                if (label.endsWith("@sl")) return label;
            }
            // If no Slovenian, look for English
            for (String label : rawLabels) {
                if (label.endsWith("@en")) return label;
            }
            // Absolute fallback: just return the first label available
            return rawLabels.get(0);
        }

        return null;
    }
}