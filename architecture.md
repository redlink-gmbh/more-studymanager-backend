# Architecture

Java/Spring Boot multi-module Maven project (`more-studymanager-backend`), managing MORE Platform studies.

## Modules

Build order (`pom.xml` `<modules>`), each a Maven module with its own `pom.xml`:

* **studymanager-core** — shared domain contracts/utilities used by all other modules.
* **studymanager-auth-token** — login-token authentication (`io.redlink.more.auth`).
* **studymanager-observation** — observation component definitions (measurements).
* **studymanager-intervention** — intervention component definitions (triggers/actions).
* **studymanager-goaltemplates** — goal template configuration.
* **studymanager-services** — service layer: business logic, models, persistence access, consumed by the API layer.
* **studymanager** — the Spring Boot application: REST controllers (`controller/studymanager/*APIV1Controller`), OpenAPI-generated DTOs/API interfaces (`target/generated-sources/.../api/v1/model`), and app wiring/config. This is the deployable artifact.

## Layering

```
Controller (studymanager)
  -> Service (studymanager-services)
    -> Model / Repository (studymanager-services)
```

Controllers implement OpenAPI-generated interfaces and delegate to services in `studymanager-services`; services own the domain model and DB access. Cross-cutting component plugins (observation/intervention/goaltemplates) implement the `ComponentFactory` / `ComponentInterface` contracts described in `docs/component-concept.md` and depend on `studymanager-core`.

## Key References

* [`docs/component-concept.md`](docs/component-concept.md) — Component (Measurement/Trigger/Action) plugin model.
* [`docs/adr`](docs/adr) — Architecture Decision Records.
* [`README.md`](README.md) — local dev setup (Docker Compose, Keycloak, login-token config).

## Conventions

* Local dev services via `docker-compose.yaml`.
* Releases tagged `v<Major>.<Minor>.<Patch>`; CI builds/tests/publishes a Docker image on tag push.
