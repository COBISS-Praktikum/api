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

    // This query now returns the node PLUS all its connections
    @Query("MATCH (n:skos__Concept {uri: $uri}) " +
            "OPTIONAL MATCH (n)-[r_out:skos__broader|skos__related]->(m) " +
            "OPTIONAL MATCH (n)<-[r_in:skos__narrower]-(o) " +
            "RETURN n, collect(r_out), collect(m), collect(r_in), collect(o)")
    Optional<SkosConcept> findByUri(String uri);

    @Query("MATCH (n:skos__Concept) WHERE n.skos__prefLabel CONTAINS $text " +
            "RETURN n LIMIT $limit")
    List<SkosConcept> searchByText(String text, int limit);

    @Query("MATCH (s:skos__ConceptScheme) RETURN s")
    List<SkosConceptScheme> findAllSchemes();
}