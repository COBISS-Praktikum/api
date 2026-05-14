package com.cobiss.backend.models;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node({"skos__Concept", "Resource"})
public class SkosConcept extends Resource {

    @Property("skos__prefLabel")
    private String prefLabel;

    @Property("skos__altLabel")
    private String altLabel;

    @Property("skos__definition")
    private String definition;

    // Hierarchical Relationships
    @Relationship(type = "skos__broader", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> broader;

    @Relationship(type = "skos__narrower", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> narrower;

    @Relationship(type = "skos__inScheme", direction = Relationship.Direction.OUTGOING)
    private SkosConceptScheme inScheme;
}