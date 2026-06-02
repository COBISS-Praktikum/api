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

    // NOTE: skos__definition is stored in Neo4j as a LIST<STRING> (each element
    // carrying its @sl language tag), exactly like skos__prefLabel. Returning the
    // raw list into a String projection field yields null, so every query below
    // extracts a single element the same way the prefLabel handling does:
    //   coalesce([d IN x.skos__definition WHERE d ENDS WITH '@sl'][0], x.skos__definition[0])
    // The @sl tag is left on the value and stripped on the frontend (see
    // useConceptDefinition), consistent with how prefLabelSl/prefLabelEn are handled.

    @Query("MATCH (n:skos__Concept {uri: $uri}) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN n.uri AS uri, " +
            "       coalesce([d IN n.skos__definition WHERE d ENDS WITH '@sl'][0], n.skos__definition[0]) AS definition, " +
            "       coalesce([s IN n.skos__scopeNote WHERE s ENDS WITH '@sl'][0], n.skos__scopeNote[0]) AS scopeNote, " +
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
            "WITH n, raw_sl, raw_en, " +
            "     CASE " +
            "       WHEN toLower(raw_sl) = toLower($text) THEN 0 " +
            "       WHEN toLower(raw_sl) STARTS WITH toLower($text) THEN 1 " +
            "       WHEN toLower(raw_en) = toLower($text) THEN 2 " +
            "       WHEN toLower(raw_en) STARTS WITH toLower($text) THEN 3 " +
            "       ELSE 4 " +
            "     END AS score " +
            "RETURN n.uri AS uri, " +
            "       coalesce([d IN n.skos__definition WHERE d ENDS WITH '@sl'][0], n.skos__definition[0]) AS definition, " +
            "       n.skos__altLabel AS altLabel, " +
            "       n.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn " +
            "ORDER BY score ASC " +
            "LIMIT $limit")
    List<ConceptProjection> searchByText(String text, int limit);

    // --- Relationship Selectors ---

    @Query("MATCH (n:skos__Concept) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl " +
            "WHERE raw_sl IS NOT NULL AND toLower(raw_sl) CONTAINS toLower($text) " +
            "WITH n, raw_sl, " +
            "     CASE " +
            "       WHEN toLower(raw_sl) = toLower($text) THEN 0 " +
            "       WHEN toLower(raw_sl) STARTS WITH toLower($text) THEN 1 " +
            "       ELSE 2 " +
            "     END AS score " +
            "RETURN n.uri AS uri, " +
            "       coalesce([d IN n.skos__definition WHERE d ENDS WITH '@sl'][0], n.skos__definition[0]) AS definition, " +
            "       n.skos__altLabel AS altLabel, " +
            "       n.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, " +
            "       [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS prefLabelEn " +
            "ORDER BY score ASC " +
            "LIMIT $limit")
    List<ConceptProjection> searchByTextSl(String text, int limit);

    @Query("MATCH (n:skos__Concept) " +
            "WITH n, " +
            "     [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "WHERE raw_en IS NOT NULL AND toLower(raw_en) CONTAINS toLower($text) " +
            "WITH n, raw_en, " +
            "     CASE " +
            "       WHEN toLower(raw_en) = toLower($text) THEN 0 " +
            "       WHEN toLower(raw_en) STARTS WITH toLower($text) THEN 1 " +
            "       ELSE 2 " +
            "     END AS score " +
            "RETURN n.uri AS uri, " +
            "       coalesce([d IN n.skos__definition WHERE d ENDS WITH '@sl'][0], n.skos__definition[0]) AS definition, " +
            "       n.skos__altLabel AS altLabel, " +
            "       n.skos__prefLabel AS rawPrefLabels, " +
            "       [lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS prefLabelSl, " +
            "       raw_en AS prefLabelEn " +
            "ORDER BY score ASC " +
            "LIMIT $limit")
    List<ConceptProjection> searchByTextEn(String text, int limit);

    // Parent concepts are INCOMING narrower arrows
    @Query("MATCH (n:skos__Concept {uri: $uri})<-[:skos__narrower]-(m) " +
            "WITH m, " +
            "     [lbl IN m.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN m.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN m.uri AS uri, " +
            "       coalesce([d IN m.skos__definition WHERE d ENDS WITH '@sl'][0], m.skos__definition[0]) AS definition, " +
            "       m.skos__altLabel AS altLabel, " +
            "       m.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findBroader(String uri);

    // Child concepts are OUTGOING narrower arrows
    @Query("MATCH (n:skos__Concept {uri: $uri})-[:skos__narrower]->(o) " +
            "WITH o, " +
            "     [lbl IN o.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN o.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN o.uri AS uri, " +
            "       coalesce([d IN o.skos__definition WHERE d ENDS WITH '@sl'][0], o.skos__definition[0]) AS definition, " +
            "       o.skos__altLabel AS altLabel, " +
            "       o.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findNarrower(String uri);

    @Query("MATCH (n:skos__Concept {uri: $uri})-[:skos__related]->(x) " +
            "WITH x, " +
            "     [lbl IN x.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN x.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN x.uri AS uri, " +
            "       coalesce([d IN x.skos__definition WHERE d ENDS WITH '@sl'][0], x.skos__definition[0]) AS definition, " +
            "       x.skos__altLabel AS altLabel, " +
            "       x.skos__prefLabel AS rawPrefLabels, raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findRelated(String uri);

    @Query("MATCH (n:skos__Concept {uri: $uri}) " +
            "WITH n " +
            "OPTIONAL MATCH (n)<-[:skos__narrower]-(broader) " +
            "OPTIONAL MATCH (n)-[:skos__narrower]->(narrower) " +
            "OPTIONAL MATCH (n)-[:skos__related]->(related) " +
            "WITH n, collect(DISTINCT broader) AS broaderList, " +
            "     collect(DISTINCT narrower) AS narrowerList, " +
            "     collect(DISTINCT related) AS relatedList " +
            "OPTIONAL MATCH (broaderNode)-[:skos__related]->(broaderRelated) " +
            "WHERE broaderNode IN broaderList " +
            "WITH n, broaderList, narrowerList, relatedList, collect(DISTINCT broaderRelated) AS broaderRelatedList " +
            "OPTIONAL MATCH (narrowerNode)-[:skos__related]->(narrowerRelated) " +
            "WHERE narrowerNode IN narrowerList " +
            "WITH n, broaderList, narrowerList, relatedList, broaderRelatedList, collect(DISTINCT narrowerRelated) AS narrowerRelatedList " +
            "WITH [n] + broaderList + narrowerList + relatedList + broaderRelatedList + narrowerRelatedList AS allNodes " +
            "UNWIND allNodes AS node " +
            "WITH DISTINCT node, " +
            "     [lbl IN node.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS raw_sl, " +
            "     [lbl IN node.skos__prefLabel WHERE lbl ENDS WITH '@en'][0] AS raw_en " +
            "RETURN node.uri AS uri, " +
            "       coalesce([d IN node.skos__definition WHERE d ENDS WITH '@sl'][0], node.skos__definition[0]) AS definition, " +
            "       node.skos__altLabel AS altLabel, node.skos__prefLabel AS rawPrefLabels, " +
            "       raw_sl AS prefLabelSl, raw_en AS prefLabelEn")
    List<ConceptProjection> findNeighborhood(String uri);


    @Query("MATCH (s:skos__ConceptScheme) RETURN s")
    List<SkosConceptScheme> findAllSchemes();
}