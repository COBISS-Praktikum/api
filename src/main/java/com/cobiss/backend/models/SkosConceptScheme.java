package com.cobiss.backend.models;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node({"skos__ConceptScheme", "Resource" })
public class SkosConceptScheme extends Resource {

    @Property("ns0__title")
    private String title;

    @Property("ns0__description")
    private String description;

    @Relationship(type = "skos__hasTopConcept", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> topConcepts;
}