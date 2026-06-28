# 09 — Guide d'explication du code (développeur)

Onboarding technique : comment le code est organisé et où trouver chaque chose. Voir aussi
`03-Dossier-architecture-technique.md` (architecture) et `04-Modele-de-donnees.md` (schéma).

## 1. Pile technique

Spring Boot 3.3 · Java 17 · PostgreSQL · Flyway · Spring Security · Thymeleaf (rendu **côté serveur**,
pas de framework JS) · Apache POI (Excel) · Maven · JUnit 5 / Mockito / Testcontainers.

Point d'entrée : `MissionsParcApplication` (`@SpringBootApplication`).

## 2. Organisation des packages (`sn.dgcpt.missionsparc`)

| Package | Rôle |
|---|---|
| `web` | **Contrôleurs** MVC (un par domaine) + `GlobalModelAdvice` (attributs communs : rôle, notifications) + `EcheanceCompteur` |
| `config` | `SecurityConfig` (sécurité d'URL, CSRF, en-têtes), `WebConfig` (intercepteurs), `DataInitializer` (compte initial) |
| `domain` | **Entités JPA** (Mission, Materiel, Agent, Poste, AuditEvent, OrdreMission…) et enums |
| `repository` | **Dépôts** Spring Data (interfaces `JpaRepository`) + requêtes `@Query` (recherches paginées) |
| `consultation` | **Lectures/restitutions** : `ConsultationService` (parc, postes, missions, inventaire daté), `Pagination`/`PageVue`, exporters, DTO de vue |
| `mission` | Cycle de vie des missions : `MissionService`, `CanevasWriter` (génération Excel), `ChefPosteService`, formulaires |
| `importation` | **Pipeline hors-ligne** : `CanevasReader`, `ControleImport`, `ConsolidationService`, `IntegrationService` |
| `agent` | `AgentService`, `RattachementService` (historisation agent↔TPR) |
| `affectation` | `AffectationService` (réaffectation matériel↔agent, historisée) |
| `compte` | Sécurité comptes : `CompteService` (`UserDetailsService`), `LoginAttemptService` (anti-force-brute) |
| `audit` | `AuditService` (journal des actions sensibles) |

**Convention de couches** : `Controller → Service → Repository → Domain`. Les services de **lecture**
sont `@Transactional(readOnly = true)` ; ceux d'écriture `@Transactional`. Les vues reçoivent des
**DTO** (`*Vue`), jamais les entités directement.

## 3. Cycle d'une requête HTTP

1. `SecurityConfig` filtre l'URL par rôle (ADMIN / CHEF_MISSION / AGENT / MANAGER) ; CSRF actif.
2. `GlobalModelAdvice` (`@ControllerAdvice`) ajoute à **toutes** les vues : `currentUser`, `currentRole`,
   `currentPath` (menu actif) et `nbEcheances` (badge de notifications, via `EcheanceCompteur` mis en cache).
3. Le **contrôleur** appelle un **service**, place des **DTO** dans le `Model`, retourne un nom de
   template Thymeleaf (`templates/*.html`, fragment commun `fragments/layout.html`).

## 4. Flux métier clés

### Mode hors-ligne (canevas Excel) — le cœur du métier
```
Mission ──> CanevasWriter.prestamper()         (génère un .xlsx pré-rempli par agent)
            │  (verrouillage N° inventaire, listes déroulantes, mises en forme conditionnelles)
            ▼
   Agent remplit hors-ligne, puis téléverse
            ▼
CanevasReader.lire()  ──>  ControleImport.controler()   (anomalies bloquantes / avertissements)
            ▼
ConsolidationService   (un fichier = un « lot » ; rapprochement par clé ; arbitrage des conflits)
            ▼
IntegrationService.integrer()   (crée/MAJ le matériel, attribue les N° d'inventaire, fige la photo datée,
                                 historise les affectations, reporte chef de poste/dates à la mission)
```
La **clé** d'un matériel est le **n° d'inventaire** (sinon MAC / n° de série pour l'anti-doublon).
Les agents inconnus sont **créés à la volée** (journalisés) avec leur rattachement (`RattachementService`).

### Recherche & pagination
Les grandes listes (**Parc**, **Missions**) sont filtrées/paginées **côté base** : requête `@Query`
`rechercher(...)` renvoyant un `Page<…>`, enveloppé dans le `PageVue` du projet (gabarit inchangé).
L'**état temporel** des missions (dérivé des dates) est traduit en **prédicats SQL** (filtre) et en
expression **`CASE`** (tri, via `JpaSort.unsafe`). Les petites listes (Postes, Agents) restent paginées
en mémoire (`Pagination.page`), sans N+1.

### Sécurité & audit
`CompteService` implémente `UserDetailsService` ; `LoginAttemptService` verrouille temporairement après
échecs ; les événements d'authentification, clôtures, réaffectations et opérations de compte sont tracés
par `AuditService` dans la table `audit_event` (écran `/journal`, ADMIN).

## 5. Conventions & pièges

- **Nommage en français** (méthodes, variables, libellés) ; commentaires expliquant l'« intention ».
- **Historisation, jamais d'écrasement** : chef de poste, rattachement agent↔TPR, affectation matériel
  (périodes datées) ; chaque relevé est une **photo datée** figée.
- **Migrations Flyway immuables** : ne jamais modifier une migration appliquée — en ajouter une nouvelle
  (cf. `10-Guide-de-mise-a-jour.md`).
- **JDK 25 / Mockito** : impossible de mocker les **classes concrètes** (inline mock). Convention :
  ne mocker que des **interfaces** (dépôts) ; pour un service concret, utiliser une **doublure manuelle**
  (sous-classe) ou un constructeur **no-op** (ex. `new AuditService(null)` — écriture best-effort).
- **CSRF + multipart** : les formulaires `th:action` injectent le jeton `_csrf` (fonctionne pour les
  téléversements, cf. `/import` et les ordres de mission).

## 6. Tests

`mvn test` (Testcontainers nécessite Docker). Voir `05-Plan-de-tests-et-cahier-de-recette.md`.
Niveaux : unitaire (logique métier, mocks de dépôts), intégration (`ImportIntegrationTest` sur une vraie
base), tranche web (`SecuriteAccesParRoleTest`, `@WebMvcTest`). **Intégration continue** : GitHub Actions
(`.github/workflows/ci.yml`) — build + tests à chaque push.

## 7. Où trouver…

| Besoin | Fichier |
|---|---|
| Règles d'accès par rôle | `config/SecurityConfig` |
| Génération du canevas | `mission/CanevasWriter` |
| Contrôles d'import | `importation/ControleImport` |
| Intégration au parc | `importation/IntegrationService` |
| Recherches paginées | `repository/*Repository` (méthodes `rechercher`) + `consultation/ConsultationService` |
| Tableau de bord / alertes | `web/HomeController` + `templates/index.html` |
| Styles | `static/css/app.css` (design tokens `:root`) |
