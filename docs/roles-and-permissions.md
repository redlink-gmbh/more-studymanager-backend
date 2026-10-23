# Benutzerrollen und Berechtigungen

Der MORE Studymanager kennt zwei Rollenebenen:

- **Plattform-Rollen** (`PlatformRole`): gelten global und kommen aus Keycloak.
- **Studien-Rollen** (`StudyRole`): werden pro Studie über die ACL vergeben.

## Plattform-Rollen

Die Zuordnung der Keycloak-Rollen zu den Plattform-Rollen ist in `application.yaml` unter `more.auth.global-roles`
konfiguriert.

| Rolle           | Keycloak-Rolle  | Bedeutung                                                                                                                           |
|-----------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `MORE_VIEWER`   | `more-viewer`   | Darf bestehende Studien sehen, in denen der Benutzer in der ACL eingetragen ist                                                     |
| `MORE_OPERATOR` | `more-operator` | Darf neue Studien anlegen                                                                                                           |
| `MORE_ADMIN`    | `more-admin`    | System- bzw. Plattform-Administrator: verwaltet Benutzer und Notfallfunktionen. Darf **keine Daten sehen und keine Studien ändern** |

> **Hinweis:** Das Backend prüft derzeit keine der Plattform-Rollen. `AuthenticatedUser.canViewStudies()` und
> `canCreateStudies()` werden nirgends aufgerufen. `createStudy`, `importStudy` und `listStudies` stehen jedem
> angemeldeten Benutzer offen. Die Einschränkung passiert nur im Frontend.

## Studien-Rollen

Diese Rollen werden mit der Annotation `@RequiresStudyRole` an den Controller-Methoden geprüft (siehe
`MethodSecurityConfiguration`). Steht in der Annotation keine Rolle, reicht irgendeine Rolle in der Studie.

| Rolle            | Beschreibung                                                                              |
|------------------|-------------------------------------------------------------------------------------------|
| `STUDY_VIEWER`   | Lesezugriff auf die Studie und auf die erhobenen Daten                                    |
| `STUDY_OPERATOR` | Konfiguriert die Studie, sieht aber keine erhobenen Daten                                 |
| `STUDY_ADMIN`    | Hat alle Rechte in der Studie, inklusive Mitarbeiterverwaltung, Statuswechsel und Löschen |

Wer eine Studie anlegt oder importiert, bekommt automatisch alle drei Studien-Rollen (`StudyService.createStudy`).

### Berechtigungsmatrix

| Bereich / Aktion                                                      | Viewer | Operator | Admin |
|-----------------------------------------------------------------------|:------:|:--------:|:-----:|
| Studie ansehen (`getStudy`)                                           |   ✅   |    ✅    |  ✅   |
| Mitarbeiter und deren Rollen auflisten                                |   ✅   |    ✅    |  ✅   |
| Studien- und Beobachtungsgruppen ansehen                              |   ✅   |    ✅    |  ✅   |
| Apps der Studie ansehen (`getStudyApplications`)                      |   ✅   |    ✅    |  ✅   |
| Erhobene Daten: Datenpunkte, Teilnahmedaten, Observation-Views        |   ✅   |    ❌    |  ✅   |
| Studie bearbeiten (`updateStudy`)                                     |   ❌   |    ✅    |  ✅   |
| Studien- und Beobachtungsgruppen anlegen, ändern, löschen             |   ❌   |    ✅    |  ✅   |
| Observations verwalten (inkl. Tokens und Attachments)                 |   ❌   |    ✅    |  ✅   |
| Interventions verwalten (inkl. Trigger und Actions)                   |   ❌   |    ✅    |  ✅   |
| Teilnehmer verwalten: anlegen, ändern, löschen, Zugangsdaten, Resync  |   ❌   |    ✅    |  ✅   |
| Teilnehmer importieren und exportieren                                |   ❌   |    ✅    |  ✅   |
| Meilensteine und Kalender bzw. Timeline                               |   ❌   |    ✅    |  ✅   |
| Studiendaten exportieren (`exportStudyData`, `generateDownloadToken`) |   ❌   |    ✅    |  ✅   |
| Studie als Ganzes exportieren (`exportStudy`)                         |   ❌   |    ❌    |  ✅   |
| Studienstatus ändern (`setStatus`)                                    |   ❌   |    ❌    |  ✅   |
| Studie löschen (`deleteStudy`)                                        |   ❌   |    ❌    |  ✅   |
| Rollen von Mitarbeitern setzen oder entfernen                         |   ❌   |    ❌    |  ✅   |
| Audit-Log ansehen und exportieren                                     |   ❌   |    ❌    |  ✅   |

## Öffentliche Endpunkte

Diese Endpunkte sind ohne Login erreichbar (`WebSecurityConfiguration`):

- `GET /api`, `GET /api/v1/me`
- `GET /api/v1/config/ui`, `GET /api/v1/config/buildInfo`
- `GET /api/v1/studies/*/export/studydata/*`: über ein eigenes Download-Token abgesichert
- `GET /api/v1/studies/*/calendar.ics`
- `/api/v1/components/observation/lime-survey-observation/end.html`
- `GET /api/v1/components/observation/lime-survey-observation/admin`

## Offene Punkte

- Die Plattform-Rollen werden im Backend nicht durchgesetzt (siehe Hinweis oben).
- `STUDY_OPERATOR` darf die Rohdaten der Studie exportieren (`exportStudyData`), obwohl die Rolle die Daten in der
  Oberfläche nicht sehen darf.
