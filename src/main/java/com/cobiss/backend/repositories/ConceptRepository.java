package com.cobiss.backend.repositories;

import com.cobiss.backend.models.ConceptProjection;
import com.cobiss.backend.models.SkosConceptScheme;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptRepository extends Neo4jRepository<com.cobiss.backend.models.SkosConcept, Long> {

    @Query("MATCH (n:skos__Concept {uri: $uri}) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN n.uri AS uri, " +
            "       n.skos__definition AS definition, " +
            "       n.skos__altLabel AS altLabel, " +
            "       n.skos__prefLabel AS rawPrefLabels, " +
            "       raw_sl AS prefLabelSl, " +
            "       raw_en AS prefLabelEn")
    Optional<ConceptProjection> findByUri(String uri);

    @Query("MATCH (n:skos__Concept) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "WHERE (raw_sl IS NOT NULL AND toLower(raw_sl) CONTAINS toLower($text)) " +
            "   OR (raw_en IS NOT NULL AND toLower(raw_en) CONTAINS toLower($text)) " +
            "RETURN n.uri AS uri, n.skos__definition AS definition, n.skos__altLabel AS altLabel, " +
            "       n.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn " +
            "LIMIT $limit")
    List<ConceptProjection> searchByText(String text, int limit);
    // --- Relationship Selectors ---

    // Parent concepts are INCOMING narrower arrows
    @Query("MATCH (n:skos__Concept {uri: $uri})<-[:skos__narrower]-(m) " +
            "WITH m, " +
            "     [lbl IN m.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN m.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN m.uri AS uri, m.skos__definition AS definition, m.skos__altLabel AS altLabel, " +
            "       m.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findBroader(String uri);

    // Child concepts are OUTGOING narrower arrows
    @Query("MATCH (n:skos__Concept {uri: $uri})-[:skos__narrower]->(o) " +
            "WITH o, " +
            "     [lbl IN o.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN o.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN o.uri AS uri, o.skos__definition AS definition, o.skos__altLabel AS altLabel, " +
            "       o.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findNarrower(String uri);
    @Query("MATCH (n:skos__Concept {uri: $uri})-[:skos__related]->(x) " +
            "WITH x, " +
            "     [lbl IN x.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN x.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN x.uri AS uri, x.skos__definition AS definition, x.skos__altLabel AS altLabel, " +
            "       x.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findRelated(String uri);

    @Query("MATCH (s:skos__ConceptScheme) RETURN s")
    List<SkosConceptScheme> findAllSchemes();
}