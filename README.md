# SGC Navigator Backend API

## O SGC Navigator Backend-u

**SGC Navigator Backend** je spletni REST API za Splošni geslovnik COBISS (SGC / Splošni geslovnik COBISS), namenjen podpiranju iskalnih in raziskovalnih funkcij frontenda. Backend upravlja podatke o približno **700.000 SKOS izrazih** v Neo4j grafu znanja, omogoča hitro iskanje pojmov in različne vrste zahtevkov za izpis hierarhičnih ter asociativnih razmerij med izrazi.

Backend oskrbuje GraphQL in/ali REST končne točke, ki jih aplikacija SGC Navigator Frontend ponaša uporabnikom.

**Ciljni uporabniki**: Notranje storitve platforme COBISS.SI

**Infrastruktura**: Zaledni sistem je nameščen na GCP, lokalni razvoj prek Docker Compose

## Tehnološki sklad

- **Jezik**: Java 21 (LTS - moderne programske zmožnosti)
- **Ogrodje**: Spring Boot 4.0.6 z Spring Data Neo4j
- **Gradnja**: Gradle 8
- **Baza podatkov**: Neo4j (označene grafe)
- **Upravljanje konfiguracije**: dotenv-java za spremenljivke okolja
- **Boilerplate**: Lombok za zmanjšanje kode
- **Kontejnerizacija**: Docker z večstopenjsko gradnjo + Docker Compose
- **Testiranje**: JUnit 5 (Jupiter) s Spring Boot testnimi starterji

## Predpogoji

- Java 21 JDK (ali le JRE za zagon)
- Gradle 8 (ali uporaba `./gradlew` wrapper)
- Docker & Docker Compose (za lokalni svilupni polni sklad)
- Dostop do Neo4j podatkovne baze (lokalno ali prek GCP)

## Dostopni ukazi

| Ukaz | Opis |
|---|---|
| `./gradlew build` | Preverjanje tipov in gradnja JAR datoteke |
| `./gradlew bootRun` | Zagon aplikacije direktno (zahteva `.env` konfiguracijo) |
| `./gradlew test` | Zagon testne suite (zahteva dostop do Neo4j) |
| `./gradlew clean` | Brisanje `build/` direktorija |
| `docker build -t cobiss-backend:latest .` | Gradnja Docker slike |
| `docker-compose up` | Zagon polnega sklopa (Neo4j + Spring Boot) |
| `docker-compose down` | Zaustavitev in čiščenje storitev |

**Ključni vzorec**: Entity modeliramo s `@Node` anotacijami, repozitorije s Spring Data Neo4j `CrudRepository`, odvedljive komponente s Lombok `@Data`.

## Docker

Večstopenjska Docker gradnja pospešuje zagon in minimalizira velikost slike:

### Lokalni razvoj z Docker Compose

```bash
docker-compose up
```

Neo4j browser je dostopen na `http://localhost:7474`.

## CI/CD

GitHub Actions delovni tok samodejno:

- **Lint** — Preverjanje Java kode s standardnimi orodji
- **Preverjanje tipov** — Validacija Gradle kompilacije
- **Gradnja** — Ustvarjanje produkcijske JAR datoteke (`./gradlew build`)
- **Testi** — Zagon testne suite (`./gradlew test`)
- **Docker** — Gradnja in nalaganje slike (na zahtevi za merge)

Vodovod se zagne na:
- Vsak push na `main` / `develop` veje
- Vse zahteve za povlečenje

## Ključne datoteke

- **`build.gradle`** — Gradle konfiguracijo, odvisnosti (Java 21 toolchain)
- **`settings.gradle`** — Ime projekta: `backend`
- **`src/main/java/com/cobiss/backend/BackendApplication.java`** — Vstopna točka (naloži `.env`, zažene Spring)
- **`src/main/resources/application.properties`** — Spring Boot nastavitve
- **`Dockerfile`** — Večstopenjska gradnja (gradle:8-jdk21 → eclipse-temurin:21-jre)
- **`docker-compose.yml`** — Orkestracija Neo4j + Spring Boot
- **`.env`** — Spremenljivke okolja (IGNORED v git-u)
