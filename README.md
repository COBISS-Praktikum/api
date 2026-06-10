# SGC Navigator Backend API

## O SGC Navigator Backend-u

**SGC Navigator Backend** je GraphQL API za Splošni geslovnik COBISS (SGC / Splošni geslovnik COBISS), namenjen podpiranju iskalnih in raziskovalnih funkcij frontenda. Backend upravlja podatke o približno **75.000 SKOS izrazih** v Neo4j grafu znanja, omogoča hitro iskanje pojmov ter izpis hierarhičnih in asociativnih razmerij med izrazi.

Backend oskrbuje GraphQL končno točko, ki jo aplikacija SGC Navigator Frontend uporablja za prikaz konceptnega grafa. Poleg tega zagotavlja REST API za ALTCHA CAPTCHA izzive in preverjanje.

**Ciljni uporabniki**: Notranje storitve platforme COBISS.SI

**Infrastruktura**: Zaledni sistem je nameščen na GCP, lokalni razvoj prek Docker Compose

**Dokumentacija** za Javo je na voljo tukaj: [Swagger-UI](https://34-65-163-129.sslip.io/swagger-ui/index.html)

## Tehnološki sklad

| Kategorija | Tehnologija |
|---|---|
| **Jezik** | Java 21 (LTS — moderne programske zmožnosti) |
| **Ogrodje** | Spring Boot 4.0.6 z Spring Data Neo4j + Spring for GraphQL |
| **Gradnja** | Gradle 8 (Java 21 toolchain) |
| **Baza podatkov** | Neo4j — graf znanja SKOS konceptov |
| **API** | GraphQL (Spring for GraphQL) + REST za ALTCHA (`/api/auth/*`) |
| **CAPTCHA** | ALTCHA — strežniško generiranje izzivov in validacija odgovorov |
| **Konfiguracija** | dotenv-java 3.0.0 za spremenljivke okolja |
| **Boilerplate** | Lombok za zmanjšanje kode (`@Data`, getterji, setterji) |
| **Kontejnerizacija** | Docker z večstopenjsko gradnjo + Docker Compose |
| **Testiranje** | JUnit 5 (Jupiter) s Spring Boot testnimi starterji |

## Predpogoji

- Java 21 JDK
- Gradle 8 (ali `./gradlew` wrapper — priporočeno)
- Docker & Docker Compose (za lokalni polni sklad)
- Dostop do Neo4j podatkovne baze (lokalno ali prek GCP)

## Začetek

```bash
# Lokalni zagon s polnim skladom (Neo4j + Spring Boot)
docker compose -f docker-compose.dev.yml up --build

# Samo Spring Boot (zahteva zunanji Neo4j)
./gradlew bootRun
```

## Dostopni ukazi

| Ukaz | Opis |
|---|---|
| `./gradlew build` | Preverjanje tipov in gradnja JAR datoteke |
| `./gradlew bootRun` | Zagon aplikacije direktno (zahteva `.env` ali env spremenljivke) |
| `./gradlew test` | Zagon testne suite (zahteva dostop do Neo4j) |
| `./gradlew clean build` | Čiščenje in ponovna gradnja |
| `docker build -t cobiss-backend:latest .` | Gradnja Docker slike (večstopenjska) |
| `docker compose -f docker-compose.dev.yml up --build` | Zagon polnega lokalnega sklopa |
| `docker-compose down` | Zaustavitev in čiščenje storitev |

**Ključni vzorec**: Preverjanje tipov se izvede kot del procesa gradnje. Testi zahtevajo živo Neo4j povezavo — najprej zaženi `docker-compose up`. Docker gradnja preskoči teste (`-x test`) za hitrost; zaženi jih ločeno v CI/CD.

## Struktura projekta

```
src/main/java/com/cobiss/backend/
├── BackendApplication.java            # Vstopna točka — naloži .env, zažene Spring
├── controllers/
│   ├── ConceptGraphQLController.java  # Vsi GraphQL query in @SchemaMapping resolverji
│   └── GatewayController.java         # REST: /api/auth/captcha-challenge + /api/auth/verify-gateway
├── models/
│   ├── SkosConcept.java               # @Node entiteta za Neo4j konceptne vozle
│   ├── SkosConceptScheme.java         # @Node entiteta za sheme konceptov
│   ├── ConceptProjection.java         # Projekcijski vmesnik za poizvedbe (uri, prefLabelSl, prefLabelEn ...)
│   ├── ConceptEdge.java               # DTO za povezave grafa (sourceUri, targetUri, relationType)
│   └── Resource.java                  # Generični model vira
├── repositories/
│   └── ConceptRepository.java         # Neo4jRepository — vse @Query Cypher poizvedbe
├── security/
│   └── WebConfig.java                 # CORS konfiguracija za /graphql in /api/**
└── services/
    ├── ConceptService.java             # Poslovna logika — delegira na repozitorij in Neo4jClient
    └── AltchaService.java              # Generiranje CAPTCHA izzivov in validacija odgovorov
src/main/resources/
├── graphql/
│   └── schema.graphqls                # Definicija GraphQL sheme
└── application.properties             # Spring Boot nastavitve (Neo4j URI, GraphiQL, altcha.secret)
src/test/java/com/cobiss/backend/
└── BackendApplicationTests.java       # @SpringBootTest test nalaganja konteksta
```

## Dokumentacija API-ja

### GraphQL — GraphiQL Explorer

GraphiQL interaktivni vmesnik je dostopen na `http://localhost:8080/graphiql` (samo razvoj).

Vsebuje vgrajen panel **Docs** (ikona knjige zgoraj desno), ki samodejno prikazuje vse poizvedbe, tipe in opise polj neposredno iz sheme. Opisi so definirani z `"""` oznakami v datoteki `schema.graphqls`.

### REST — Swagger UI

Swagger UI za REST končne točke (`/api/auth/*`) je dostopen na `http://localhost:8080/swagger-ui`.

Strojno berljiva OpenAPI specifikacija je na voljo na `http://localhost:8080/api-docs`.

Swagger UI je generiran samodejno prek knjižnice **Springdoc OpenAPI** in pokriva:
- `GET /api/auth/captcha-challenge` — generiranje novega ALTCHA izziva
- `POST /api/auth/verify-gateway` — preverjanje rešenega ALTCHA odgovora

## GraphQL API

GraphiQL explorer je dostopen na `http://localhost:8080/graphiql` (samo razvoj).

### Poizvedbe

| Poizvedba | Opis | Argumenti |
|---|---|---|
| `concept(uri)` | Pridobi posamezen koncept po URI | `uri: String!` |
| `searchConcepts(text, limit)` | Iskanje po sl/en prefLabel | `text: String!`, `limit: Int = 20` |
| `conceptNeighborhood(uri)` | Vsa vozlišča v 1-hop soseščini (koren + broader + narrower + related + njihovi related) | `uri: String!` |
| `conceptNeighborhoodEdges(uri)` | Vse povezave iste soseščine | `uri: String!` |
| `schemes` | Seznam vseh ConceptScheme vozlišč | — |

### Tipi

```graphql
type Concept {
    uri: String!
    prefLabel: String      # Slovensko z angleškim nadomestkom (rešeno strežniško)
    prefLabelSl: String    # Čista slovenska oznaka
    prefLabelEn: String    # Čista angleška oznaka
    definition: String
    broader: [Concept]     # Nadrejeni koncepti (lenobno razrešeno)
    narrower: [Concept]    # Podrejeni koncepti (lenobno razrešeno)
    related: [Concept]     # Sorodni koncepti (lenobno razrešeno)
}

type ConceptEdge {
    sourceUri: String!
    targetUri: String!
    relationType: String!  # "broader" | "narrower" | "related"
}
```

> **Opomba**: `broader`, `narrower` in `related` na `Concept` sprožijo dodatne poizvedbe na Neo4j za vsak nadrejeni koncept. Za grafični prikaz prednostno uporabi `conceptNeighborhood` + `conceptNeighborhoodEdges`.

## REST API (ALTCHA CAPTCHA)

| Metoda | Pot | Opis |
|---|---|---|
| `GET` | `/api/auth/captcha-challenge` | Generiraj in vrni nov ALTCHA izziv |
| `POST` | `/api/auth/verify-gateway` | Potrdi rešen ALTCHA odgovor; vrne `200 OK` ali `403 Forbidden` |

## Spremenljivke okolja

Ustvari datoteko `.env` v korenu projekta:

```env
NEO4J_URI=        # Bolt URI Neo4j instance (npr. bolt://localhost:7687)
NEO4J_USERNAME=   # Uporabniško ime Neo4j podatkovne baze
NEO4J_PASSWORD=   # Geslo Neo4j podatkovne baze
FRONTEND_URL=     # Dovoljen CORS izvor frontenda (npr. https://tvoja-app.github.io)
ALTCHA_SECRET=    # Naključen dolg skrivni ključ za podpisovanje
```

> **Opomba**: `BackendApplication` naloži `.env` samodejno pred zagonom Springa. V Docker okolju se spremenljivke injicirajo neposredno - `.env` datoteka ni potrebna. CORS privzeto dovoljuje `*` če `FRONTEND_URL` ni nastavljen; v produkciji vedno nastavi to spremenljivko.

## Docker

Večstopenjska Docker gradnja minimalizira velikost končne slike:
1. **Faza gradnje**: `gradle:8-jdk21` — prevede in pakira JAR
2. **Faza zagona**: `eclipse-temurin:21-jre-jammy` — zaganja optimizirani JAR (brez Gradle)

### Lokalni razvoj z Docker Compose

```bash
docker compose -f docker-compose.dev.yml up --build
```

Neo4j browser je dostopen na `http://localhost:7474`.

Obe storitvi (Neo4j + Spring Boot) bereta isto `.env` datoteko za poverilnice.

## CI/CD

GitHub Actions delovni tok samodejno:

- **Preverjanje tipov** — Validacija Gradle kompilacije
- **Testi** — Zagon testne suite (`./gradlew test`)
- **Gradnja** — Ustvarjanje produkcijske JAR datoteke (`./gradlew build`)
- **Docker** — Gradnja in nalaganje slike (ob zahtevah za združitev)

Priporočen vrstni red v pipeline:

```yaml
- name: Run tests
  run: ./gradlew test
- name: Build
  run: ./gradlew build
```

Vodovod se zagne na:
- Vsak push na `main` / `develop` veje
- Vse zahteve za povlečenje

## Ključne datoteke

- **`build.gradle`** — Gradle 8 konfiguracija, Java 21 toolchain, vse odvisnosti
- **`settings.gradle`** — Ime projekta: `backend`
- **`src/main/java/com/cobiss/backend/BackendApplication.java`** — Vstopna točka (naloži `.env`, zažene Spring)
- **`src/main/resources/application.properties`** — Neo4j URI, GraphiQL, altcha.secret
- **`src/main/resources/graphql/schema.graphqls`** — Celotna GraphQL shema z opisi
- **`Dockerfile`** — Večstopenjska gradnja (gradle:8-jdk21 → eclipse-temurin:21-jre-jammy)
- **`docker-compose.yml`** — Orkestracija Neo4j + Spring Boot za lokalni razvoj
- **`.env`** — Lokalne poverilnice (IGNORED v git-u)
