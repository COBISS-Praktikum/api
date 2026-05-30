package com.cobiss.backend.controllers;

import com.cobiss.backend.models.ConceptProjection;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.services.ConceptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConceptGraphQLControllerTest {

    @Mock
    private ConceptService conceptService;

    @InjectMocks
    private ConceptGraphQLController controller;

    // -------------------------------------------------------------------------
    // concept(uri)
    // -------------------------------------------------------------------------

    @Test
    void concept_returnsConceptForGivenUri() {
        ConceptProjection mock = mockConcept("http://example.com/1", "Knjiga", null);
        when(conceptService.getConceptByUri("http://example.com/1")).thenReturn(mock);

        ConceptProjection result = controller.concept("http://example.com/1");

        assertThat(result).isEqualTo(mock);
        verify(conceptService).getConceptByUri("http://example.com/1");
    }

    @Test
    void concept_returnsNullWhenNotFound() {
        when(conceptService.getConceptByUri("http://example.com/missing")).thenReturn(null);

        assertThat(controller.concept("http://example.com/missing")).isNull();
    }

    // -------------------------------------------------------------------------
    // searchConcepts(text, limit)
    // -------------------------------------------------------------------------

    @Test
    void searchConcepts_usesDefaultLimitOf20WhenLimitIsNull() {
        when(conceptService.searchConcepts("test", 20)).thenReturn(List.of());

        controller.searchConcepts("test", null);

        verify(conceptService).searchConcepts("test", 20);
    }

    @Test
    void searchConcepts_respectsExplicitLimit() {
        when(conceptService.searchConcepts("test", 5)).thenReturn(List.of());

        controller.searchConcepts("test", 5);

        verify(conceptService).searchConcepts("test", 5);
    }

    @Test
    void searchConcepts_returnsResults() {
        List<ConceptProjection> results = List.of(
                mockConcept("http://example.com/1", "Knjiga", null),
                mockConcept("http://example.com/2", "Knjižnica", null)
        );
        when(conceptService.searchConcepts("Knjig", 20)).thenReturn(results);

        assertThat(controller.searchConcepts("Knjig", null)).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // schemes()
    // -------------------------------------------------------------------------

    @Test
    void schemes_returnsAllSchemes() {
        List<SkosConceptScheme> schemes = List.of(mock(SkosConceptScheme.class));
        when(conceptService.getAllSchemes()).thenReturn(schemes);

        assertThat(controller.schemes()).isEqualTo(schemes);
    }

    // -------------------------------------------------------------------------
    // prefLabel field mapping
    // -------------------------------------------------------------------------

    @Test
    void prefLabel_prefersSlovenianLabel() {
        ConceptProjection concept = mock(ConceptProjection.class);
        when(concept.getPrefLabelSl()).thenReturn("Knjiga");

        assertThat(controller.prefLabel(concept)).isEqualTo("Knjiga");
    }

    @Test
    void prefLabel_fallsBackToEnglishWhenNoSlovenian() {
        ConceptProjection concept = mock(ConceptProjection.class);
        when(concept.getPrefLabelSl()).thenReturn(null);
        when(concept.getPrefLabelEn()).thenReturn("Book");

        assertThat(controller.prefLabel(concept)).isEqualTo("Book");
    }

    @Test
    void prefLabel_fallsBackToRawSlLabel() {
        ConceptProjection concept = mock(ConceptProjection.class);
        when(concept.getPrefLabelSl()).thenReturn(null);
        when(concept.getPrefLabelEn()).thenReturn(null);
        when(concept.getRawPrefLabels()).thenReturn(List.of("Libro@es", "Knjiga@sl", "Book@en"));

        assertThat(controller.prefLabel(concept)).isEqualTo("Knjiga@sl");
    }

    @Test
    void prefLabel_fallsBackToFirstRawLabelWhenNoSlOrEn() {
        ConceptProjection concept = mock(ConceptProjection.class);
        when(concept.getPrefLabelSl()).thenReturn(null);
        when(concept.getPrefLabelEn()).thenReturn(null);
        when(concept.getRawPrefLabels()).thenReturn(List.of("Libro@es", "Kniga@ru"));

        assertThat(controller.prefLabel(concept)).isEqualTo("Libro@es");
    }

    @Test
    void prefLabel_returnsNullWhenAllSourcesEmpty() {
        ConceptProjection concept = mock(ConceptProjection.class);
        when(concept.getPrefLabelSl()).thenReturn(null);
        when(concept.getPrefLabelEn()).thenReturn(null);
        when(concept.getRawPrefLabels()).thenReturn(List.of());

        assertThat(controller.prefLabel(concept)).isNull();
    }

    // -------------------------------------------------------------------------
    // broader / narrower / related
    // -------------------------------------------------------------------------

    @Test
    void broader_delegatesToService() {
        ConceptProjection concept = mockConcept("http://example.com/1", null, null);
        List<ConceptProjection> broader = List.of(mockConcept("http://example.com/parent", null, null));
        when(conceptService.getBroader("http://example.com/1")).thenReturn(broader);

        assertThat(controller.broader(concept)).isEqualTo(broader);
    }

    @Test
    void narrower_delegatesToService() {
        ConceptProjection concept = mockConcept("http://example.com/1", null, null);
        List<ConceptProjection> narrower = List.of(mockConcept("http://example.com/child", null, null));
        when(conceptService.getNarrower("http://example.com/1")).thenReturn(narrower);

        assertThat(controller.narrower(concept)).isEqualTo(narrower);
    }

    @Test
    void related_delegatesToService() {
        ConceptProjection concept = mockConcept("http://example.com/1", null, null);
        List<ConceptProjection> related = List.of(mockConcept("http://example.com/2", null, null));
        when(conceptService.getRelated("http://example.com/1")).thenReturn(related);

        assertThat(controller.related(concept)).isEqualTo(related);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ConceptProjection mockConcept(String uri, String prefLabelSl, String prefLabelEn) {
        ConceptProjection concept = mock(ConceptProjection.class);
        when(concept.getUri()).thenReturn(uri);
        lenient().when(concept.getPrefLabelSl()).thenReturn(prefLabelSl);
        lenient().when(concept.getPrefLabelEn()).thenReturn(prefLabelEn);
        lenient().when(concept.getRawPrefLabels()).thenReturn(null);
        return concept;
    }
}