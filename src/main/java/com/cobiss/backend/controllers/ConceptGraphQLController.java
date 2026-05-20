package com.cobiss.backend.controllers;

import com.cobiss.backend.models.ConceptProjection;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.services.ConceptService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ConceptGraphQLController {

    private final ConceptService conceptService;

    public ConceptGraphQLController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @QueryMapping
    public ConceptProjection concept(@Argument String uri) {
        return conceptService.getConceptByUri(uri);
    }

    @QueryMapping
    public List<ConceptProjection> searchConcepts(@Argument String text, @Argument Integer limit) {
        int safeLimit = (limit != null) ? limit : 20;
        return conceptService.searchConcepts(text, safeLimit);
    }

    @QueryMapping
    public List<SkosConceptScheme> schemes() {
        return conceptService.getAllSchemes();
    }

    // --- FIELD-LEVEL SCHEMA MAPPINGS ---

    @SchemaMapping(typeName = "Concept", field = "prefLabel")
    public String prefLabel(ConceptProjection concept) {
        if (concept.getPrefLabelSl() != null) {
            return concept.getPrefLabelSl();
        }
        if (concept.getPrefLabelEn() != null) {
            return concept.getPrefLabelEn();
        }

        List<String> rawLabels = concept.getRawPrefLabels();
        if (rawLabels != null && !rawLabels.isEmpty()) {
            for (String label : rawLabels) {
                if (label.endsWith("@sl")) return label;
            }
            for (String label : rawLabels) {
                if (label.endsWith("@en")) return label;
            }
            return rawLabels.get(0);
        }
        return null;
    }

    @SchemaMapping(typeName = "Concept", field = "broader")
    public List<ConceptProjection> broader(ConceptProjection concept) {
        return conceptService.getBroader(concept.getUri());
    }

    @SchemaMapping(typeName = "Concept", field = "narrower")
    public List<ConceptProjection> narrower(ConceptProjection concept) {
        return conceptService.getNarrower(concept.getUri());
    }

    @SchemaMapping(typeName = "Concept", field = "related")
    public List<ConceptProjection> related(ConceptProjection concept) {
        return conceptService.getRelated(concept.getUri());
    }
}