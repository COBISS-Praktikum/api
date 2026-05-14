package com.cobiss.backend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

/**
 * Represents a SKOS Concept.
 * Primary label is 'skos__Concept', but also inherits the 'Resource' label.
 */
@Node({"skos__Concept", "Resource"})
@Getter
@Setter
@NoArgsConstructor
public class SkosConcept extends Resource {

    @Property("skos__prefLabel")
    private String prefLabel;

    @Property("skos__altLabel")
    private String altLabel;

    @Property("skos__definition")
    private String definition;

    /**
     * Based on Cypher results: 'skos__related' links point OUT to other concepts.
     */
    @Relationship(type = "skos__related", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> related;

    /**
     * Based on Cypher results: 'skos__broader' links point OUT to parent concepts.
     */
    @Relationship(type = "skos__broader", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> broader;

    /**
     * Based on Cypher results: 'skos__narrower' links are INCOMING from child concepts.
     * Setting this to INCOMING allows Spring to find children that point to this node.
     */
    @Relationship(type = "skos__narrower", direction = Relationship.Direction.INCOMING)
    private List<SkosConcept> narrower;

    /**
     * Links the concept to the ConceptScheme it belongs to.
     */
    @Relationship(type = "skos__inScheme", direction = Relationship.Direction.OUTGOING)
    private SkosConceptScheme inScheme;

}