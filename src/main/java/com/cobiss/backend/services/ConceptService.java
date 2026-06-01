package com.cobiss.backend.services;

import com.cobiss.backend.models.ConceptEdge;
import com.cobiss.backend.models.ConceptProjection;
import com.cobiss.backend.models.SkosConceptScheme;
import com.cobiss.backend.repositories.ConceptRepository;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConceptService {

    private final ConceptRepository conceptRepository;
    private final Neo4jClient neo4jClient;

    public ConceptService(ConceptRepository conceptRepository, Neo4jClient neo4jClient) {
        this.conceptRepository = conceptRepository;
        this.neo4jClient = neo4jClient;
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

    public List<ConceptProjection> getNeighborhood(String uri) {
        return conceptRepository.findNeighborhood(uri);
    }


    public List<ConceptEdge> getNeighborhoodEdges(String uri) {
        String query =
                "MATCH (n:skos__Concept {uri: $uri}) WITH n " +
                        "OPTIONAL MATCH (n)<-[:skos__narrower]-(broader) " +
                        "WITH n, collect(DISTINCT broader) AS broaderList " +
                        "UNWIND broaderList AS b " +
                        "RETURN b.uri AS sourceUri, n.uri AS targetUri, 'broader' AS relationType " +
                        "UNION " +
                        "MATCH (n:skos__Concept {uri: $uri}) WITH n " +
                        "OPTIONAL MATCH (n)-[:skos__narrower]->(narrower) " +
                        "WITH n, collect(DISTINCT narrower) AS narrowerList " +
                        "UNWIND narrowerList AS nr " +
                        "RETURN n.uri AS sourceUri, nr.uri AS targetUri, 'narrower' AS relationType " +
                        "UNION " +
                        "MATCH (n:skos__Concept {uri: $uri}) WITH n " +
                        "OPTIONAL MATCH (n)-[:skos__related]->(related) " +
                        "WITH n, collect(DISTINCT related) AS relatedList " +
                        "UNWIND relatedList AS r " +
                        "RETURN n.uri AS sourceUri, r.uri AS targetUri, 'related' AS relationType " +
                        "UNION " +
                        "MATCH (n:skos__Concept {uri: $uri}) WITH n " +
                        "OPTIONAL MATCH (n)<-[:skos__narrower]-(broader)-[:skos__related]->(broaderRelated) " +
                        "WITH broader, collect(DISTINCT broaderRelated) AS broaderRelatedList " +
                        "WHERE broader IS NOT NULL " +
                        "UNWIND broaderRelatedList AS br " +
                        "RETURN broader.uri AS sourceUri, br.uri AS targetUri, 'related' AS relationType " +
                        "UNION " +
                        "MATCH (n:skos__Concept {uri: $uri}) WITH n " +
                        "OPTIONAL MATCH (n)-[:skos__narrower]->(narrower)-[:skos__related]->(narrowerRelated) " +
                        "WITH narrower, collect(DISTINCT narrowerRelated) AS narrowerRelatedList " +
                        "WHERE narrower IS NOT NULL " +
                        "UNWIND narrowerRelatedList AS nr " +
                        "RETURN narrower.uri AS sourceUri, nr.uri AS targetUri, 'related' AS relationType";

        return new ArrayList<>(neo4jClient.query(query)
                .bind(uri).to("uri")
                .fetchAs(ConceptEdge.class)
                .mappedBy((typeSystem, record) -> new ConceptEdge(
                        record.get("sourceUri").asString(null),
                        record.get("targetUri").asString(null),
                        record.get("relationType").asString(null)
                ))
                .all());
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