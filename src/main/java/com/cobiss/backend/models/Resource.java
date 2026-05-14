package com.cobiss.backend.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Resource")
@Getter
@NoArgsConstructor
public class Resource {
    @Id
    @GeneratedValue
    private Long id;
    private String uri;
}