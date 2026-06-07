# AGENTS.md - Backend API Development Guide

## Architecture Overview
This is a **Spring Boot 4.0.6 backend API** connected to a **Neo4j graph database**, serving a GraphQL API to the SGC Navigator frontend. The backend indexes ~75,000 SKOS concepts from the Slovenian COBISS thesaurus and exposes graph traversal queries (broader, narrower, related terms) alongside full-text search and ALTCHA CAPTCHA verification.

**Key Technology Stack:**
- **Java 21** (LTS — configured via Gradle toolchain)
- **Spring Boot 4.0.6** with Spring Data Neo4j + Spring for GraphQL
- **Gradle 8** build system
- **Lombok** (compile-time annotation processor)
- **dotenv-java 3.0.0** for environment variable management
- **Neo4j** graph database (local dev via docker-compose, GCP in production)
- **ALTCHA** for server-side CAPTCHA challenge generation and verification
- **Docker** multi-stage builds for containerization

## Build & Development Workflow

### Build Commands
```bash
./gradlew build          # Compile + package JAR
./gradlew bootRun        # Run locally (requires .env or env vars)
./gradlew test           # Run tests (JUnit 5, requires Neo4j access)
./gradlew clean build    # Clean rebuild
docker build -t cobiss-backend:latest .   # Multi-stage Docker image build
docker compose -f docker-compose.dev.yml up --build  # Full local stack
docker-compose down      # Tear down services
```

**Key Workflow**: Tests require a live Neo4j connection. Run `docker-compose up` first if testing locally. Docker image builds skip tests (`-x test`) for speed — run tests separately in CI.

### Environment Configuration
The application uses **dotenv-java** for secure credential management. Create a `.env` file in the project root:

```env
NEO4J_URI=         # Bolt URI of the Neo4j instance (e.g. bolt://localhost:7687)
NEO4J_USERNAME=    # Neo4j database username
NEO4J_PASSWORD=    # Neo4j database password
FRONTEND_URL=      # Allowed CORS origin for the frontend (e.g. https://your-app.github.io)
```

Additionally, `application.properties` contains:
```properties
altcha.secret=     # Secret key used by AltchaService for CAPTCHA challenge signing (change in production)
```

`BackendApplication.main()` loads `.env` automatically via `Dotenv.configure().ignoreIfMissing().load()` before Spring starts, so env vars work in both local (`.env`) and Docker (injected env) environments.

**Important**: Never commit `.env` files. Use env vars in CI/CD pipelines.

### Project Structure
```
src/main/java/com/cobiss/backend/
├── BackendApplication.java         # Entry point — loads .env, bootstraps Spring
├── controllers/
│   ├── ConceptGraphQLController.java  # All GraphQL query and field-level resolvers
│   └── GatewayController.java         # REST: /api/auth/captcha-challenge (GET) + /api/auth/verify-gateway (POST)
├── models/
│   ├── SkosConcept.java              # @Node entity for Neo4j concept nodes
│   ├── SkosConceptScheme.java        # @Node entity for concept scheme nodes
│   ├── ConceptProjection.java        # Interface projection for query results (uri, prefLabelSl, prefLabelEn, etc.)
│   ├── ConceptEdge.java              # DTO for graph edges (sourceUri, targetUri, relationType)
│   └── Resource.java                 # (generic resource model)
├── repositories/
│   └── ConceptRepository.java        # Neo4jRepository — all @Query Cypher queries
├── security/
│   └── WebConfig.java                # CORS configuration for /graphql and /api/**
└── services/
    ├── ConceptService.java            # Business logic — delegates to repository + Neo4jClient for complex queries
    └── AltchaService.java             # CAPTCHA challenge generation and response validation
src/main/resources/
├── graphql/
│   └── schema.graphqls               # GraphQL schema definition
└── application.properties            # Spring Boot config (Neo4j URI, GraphiQL, altcha.secret)
src/test/java/com/cobiss/backend/
└── BackendApplicationTests.java      # @SpringBootTest context load test
```

### Docker Development Setup
- **docker-compose.yml** orchestrates Neo4j + Spring Boot
- Neo4j exposed on `localhost:7687` (bolt) and `localhost:7474` (browser UI)
- Spring Boot app on `localhost:8080`
- Both services read the same `.env` file for credentials

## GraphQL API

GraphiQL explorer available at `http://localhost:8080/graphiql` (dev only).

### Schema

```graphql
type Query {
    concept(uri: String!): Concept
    schemes: [ConceptScheme]
    searchConcepts(text: String!, limit: Int = 20): [Concept]
    conceptNeighborhood(uri: String!): [Concept!]!
    conceptNeighborhoodEdges(uri: String!): [ConceptEdge!]!
}

type Concept {
    uri: String!
    prefLabel: String       # Slovenian label with English fallback (resolved server-side)
    prefLabelSl: String     # Raw Slovenian label
    prefLabelEn: String     # Raw English label
    definition: String
    broader: [Concept]      # Resolved via @SchemaMapping (separate query per concept)
    narrower: [Concept]     # Resolved via @SchemaMapping
    related: [Concept]      # Resolved via @SchemaMapping
}

type ConceptEdge {
    sourceUri: String!
    targetUri: String!
    relationType: String!   # "broader" | "narrower" | "related"
}

type ConceptScheme {
    uri: String!
    title: String
    description: String
    topConcepts: [Concept]
}
```

### Query Reference

| Query | Description | Key args |
|---|---|---|
| `concept(uri)` | Fetch a single concept by URI | `uri: String!` |
| `searchConcepts(text, limit)` | Full-text search across sl/en prefLabels | `text: String!`, `limit: Int = 20` |
| `conceptNeighborhood(uri)` | All nodes in 1-hop neighborhood (root + broader + narrower + related + their related) | `uri: String!` |
| `conceptNeighborhoodEdges(uri)` | All edges in the same neighborhood | `uri: String!` |
| `schemes` | List all ConceptScheme nodes | — |

### Field-Level Resolvers (`@SchemaMapping`)
`broader`, `narrower`, and `related` fields on `Concept` are resolved lazily via `@SchemaMapping` — they trigger additional Neo4j queries per parent concept. Use `conceptNeighborhood` + `conceptNeighborhoodEdges` for batch graph fetching instead.

`prefLabel` is also a `@SchemaMapping` that applies Slovenian-first fallback logic server-side.

## REST API (ALTCHA CAPTCHA)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/auth/captcha-challenge` | Generate and return a new ALTCHA challenge payload |
| `POST` | `/api/auth/verify-gateway` | Validate a solved ALTCHA response; returns `200 OK` or `403 Forbidden` |

CORS for both `/graphql` and `/api/**` is configured in `WebConfig.java`. The `FRONTEND_URL` env var controls the allowed origin (defaults to `*` if not set — lock down in production).

## Neo4j Data Model

Concepts are stored as `skos__Concept` nodes with properties using SKOS vocabulary prefixed with `skos__`:
- `uri` — unique concept identifier
- `skos__prefLabel` — list of strings with language tags (e.g. `"računalnik@sl"`, `"computer@en"`)
- `skos__altLabel` — list of alternative labels
- `skos__definition` — concept definition string

Relationships (also prefixed):
- `skos__narrower` — directed: broader concept → narrower concept
- `skos__related` — directed: concept → related concept

**Key Cypher pattern** used throughout the repository:
```cypher
[lbl IN n.skos__prefLabel WHERE lbl ENDS WITH '@sl'][0] AS prefLabelSl
```
This filters the label list by language tag at query time.

## Development Patterns

### Adding a New GraphQL Query
1. Add the field to `schema.graphqls` under `type Query`
2. Add `@QueryMapping` method to `ConceptGraphQLController.java`
3. Add business logic to `ConceptService.java`
4. Add `@Query` Cypher method to `ConceptRepository.java` (or use `Neo4jClient` for complex multi-step queries)

### Adding a New REST Endpoint
1. Add method to `GatewayController.java` (or create a new `@RestController`)
2. Ensure the path pattern is covered by `WebConfig.java` CORS mapping
3. Add service logic as needed

### Lombok Usage
- `@Data` on model classes — generates getters, setters, equals, hashCode, toString
- Configured as both `compileOnly` + `annotationProcessor` (and matching test variants)
- No need to write boilerplate accessor methods

### Neo4j Complex Queries
For multi-step graph traversals that Spring Data's `@Query` can't express cleanly, inject `Neo4jClient` directly into the service (see `ConceptService.getNeighborhoodEdges()` for the pattern):
```java
neo4jClient.query(cypherString)
    .bind(value).to("paramName")
    .fetchAs(MyDto.class)
    .mappedBy((typeSystem, record) -> new MyDto(...))
    .all();
```

## Testing Approach
- **JUnit 5 (Jupiter)** via `useJUnitPlatform()`
- `BackendApplicationTests.java` uses `@SpringBootTest` for full context load test
- Tests run via `./gradlew test`
- **Note**: Tests require a live Neo4j instance (set `NEO4J_URI` in `.env` or env)

## Docker & Containerization

### Multi-Stage Docker Build
1. **Build stage**: `gradle:8-jdk21` — compiles and packages the JAR
2. **Runtime stage**: `eclipse-temurin:21-jre-jammy` — runs the optimized JAR (no Gradle in final image)

Tests are skipped in Docker builds (`-x test`) — run them in dedicated CI jobs.

### Running the Docker Image
```bash
docker run -p 8080:8080 \
  -e NEO4J_URI=<bolt uri> \
  -e NEO4J_USERNAME=<username> \
  -e NEO4J_PASSWORD=<password> \
  -e FRONTEND_URL=<frontend origin> \
  cobiss-backend:latest
```

## Key Files Reference
- `build.gradle` — Gradle 8 config, Java 21 toolchain, all dependencies
- `settings.gradle` — project name: `backend`
- `src/main/java/com/cobiss/backend/BackendApplication.java` — dotenv loader + Spring entry point
- `src/main/resources/application.properties` — Neo4j URI, GraphiQL, altcha.secret
- `src/main/resources/graphql/schema.graphqls` — complete GraphQL schema
- `Dockerfile` — multi-stage build (gradle:8-jdk21 → eclipse-temurin:21-jre-jammy)
- `docker-compose.yml` — Neo4j + Spring Boot local dev stack
- `.env` — local credentials (git-ignored)

## Important Notes for Agents
1. **GraphQL-first API** — all concept data is served via GraphQL (`/graphql`). REST (`/api/auth/*`) is only for CAPTCHA.
2. **Field resolvers are lazy** — `broader`, `narrower`, `related` on `Concept` trigger N+1 queries. Prefer `conceptNeighborhood` + `conceptNeighborhoodEdges` for graph page data.
3. **SKOS label convention** — labels are stored as `"term@lang"` strings in a list property. Always use the list-filter pattern in Cypher to extract by language.
4. **Environment-driven config** — use `.env` locally, injected env vars in Docker/CI. Never hardcode credentials.
5. **CORS is wide open by default** — `FRONTEND_URL` defaults to `*` in `WebConfig.java` if the env var is unset. Always set it in production.
6. **Java 21 LTS** — records, sealed classes, pattern matching, and virtual threads are available.
7. **GraphiQL** is enabled in `application.properties` and accessible at `/graphiql` — useful for manual query testing in dev.