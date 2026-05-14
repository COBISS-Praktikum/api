package com.cobiss.backend.repositories;

import com.cobiss.backend.models.SkosConcept;
import com.cobiss.backend.models.SkosConceptScheme;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface ConceptRepository extends Neo4jRepository<SkosConcept, Long> {

    @Query("MATCH (n:skos__Concept {uri: $uri}) RETURN n")
    Optional<SkosConcept> findByUri(String uri);

    @Query("MATCH (n:skos__Concept) WHERE n.skos__prefLabel CONTAINS $text " +
            "RETURN n LIMIT $limit")
    List<SkosConcept> searchByText(String text, int limit);

    @Query("MATCH (s:skos__ConceptScheme) RETURN s")
    List<SkosConceptScheme> findAllSchemes();
}

