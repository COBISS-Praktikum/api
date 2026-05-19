package com.cobiss.backend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node({"skos__Concept", "Resource"})
@Getter
@Setter
@NoArgsConstructor
public class SkosConcept extends Resource {

    // Map these as explicit flat fields populated by our repository custom query projections
    private String prefLabelSl;
    private String prefLabelEn;

    // This holds the raw data from the DB array just in case
    @Property("skos__prefLabel")
    private List<String> rawPrefLabels;

    @Property("skos__altLabel")
    private List<String> altLabel;

    @Property("skos__definition")
    private String definition;

    @Relationship(type = "skos__related", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> related;

    @Relationship(type = "skos__broader", direction = Relationship.Direction.OUTGOING)
    private List<SkosConcept> broader;

    @Relationship(type = "skos__narrower", direction = Relationship.Direction.INCOMING)
    private List<SkosConcept> narrower;

    @Relationship(type = "skos__inScheme", direction = Relationship.Direction.OUTGOING)
    private SkosConceptScheme inScheme;

    /**
     * Fallback strategy for GraphQL schema compatibility.
     * Prioritizes Slovenian, defaults to English if empty.
     */
    public String getPrefLabel() {
        return this.prefLabelSl != null ? this.prefLabelSl : this.prefLabelEn;
    }
}