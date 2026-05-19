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
            "OPTIONAL MATCH (n)-[r_out:skos__broader]->(m) " +
            "OPTIONAL MATCH (n)-[r_rel:skos__related]->(x) " +
            "OPTIONAL MATCH (n)<-[r_in:skos__narrower]-(o) " +
            "RETURN n, " + // Crucial: Returning 'n' gives SDN the structural base for the projection
            "       raw_sl AS prefLabelSl, " +
            "       raw_en AS prefLabelEn, " +
            "       collect(r_out), collect(m), " +
            "       collect(r_rel), collect(x), " +
            "       collect(r_in), collect(o)")
    Optional<ConceptProjection> findByUri(String uri);

    @Query("MATCH (n:skos__Concept) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "WHERE (raw_sl CONTAINS $text) OR (raw_en CONTAINS $text) " +
            "RETURN n.uri AS uri, " +
            "       n.skos__definition AS definition, " +
            "       n.skos__altLabel AS altLabel, " +
            "       raw_sl AS prefLabelSl, " +
            "       raw_en AS prefLabelEn " +
            "LIMIT $limit")
    List<ConceptProjection> searchByText(String text, int limit);

    @Query("MATCH (s:skos__ConceptScheme) RETURN s")
    List<SkosConceptScheme> findAllSchemes();
}