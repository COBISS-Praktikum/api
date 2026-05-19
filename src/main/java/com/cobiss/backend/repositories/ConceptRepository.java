package com.cobiss.backend.repositories;

import com.cobiss.backend.models.SkosConcept;
import com.cobiss.backend.models.SkosConceptScheme;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptRepository extends Neo4jRepository<SkosConcept, Long> {

    @Query("MATCH (n:skos__Concept {uri: $uri}) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "OPTIONAL MATCH (n)-[r_out:skos__broader|skos__related]->(m) " +
            "OPTIONAL MATCH (n)<-[r_in:skos__narrower]-(o) " +
            "RETURN n, " +
            "       substring(raw_sl, 0, size(raw_sl) - 3) AS prefLabelSl, " +
            "       substring(raw_en, 0, size(raw_en) - 3) AS prefLabelEn, " +
            "       collect(r_out), collect(m), collect(r_in), collect(o)")
    Optional<SkosConcept> findByUri(String uri);

    @Query("MATCH (n:skos__Concept) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "WHERE raw_sl CONTAINS $text OR raw_en CONTAINS $text " +
            "RETURN n, " +
            "       substring(raw_sl, 0, size(raw_sl) - 3) AS prefLabelSl, " +
            "       substring(raw_en, 0, size(raw_en) - 3) AS prefLabelEn " +
            "LIMIT $limit")
    List<SkosConcept> searchByText(String text, int limit);

    @Query("MATCH (s:skos__ConceptScheme) RETURN s")
    List<SkosConceptScheme> findAllSchemes();
}