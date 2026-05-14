package com.cobiss.backend.models;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Resource")
public class Resource {
    @Id
    @GeneratedValue
    private Long id;
    private String uri;
}