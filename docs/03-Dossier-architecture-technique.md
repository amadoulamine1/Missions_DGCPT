# 03 — Dossier d'architecture technique (DAT)

## 1. Vue d'ensemble

Application **web centralisée** (intranet), consultée via navigateur, avec les **canevas Excel comme mode hors-ligne** intégré. Hébergement sur **serveur interne**. Base **PostgreSQL**. Réalisation et maintenance par l'équipe interne.

```
Navigateur (agents, chefs, admin)
        │ HTTP(S)
        ▼
Application Spring Boot (Tomcat embarqué)
  Web (Thymeleaf + contrôleurs)  →  Services (métier)  →  Repositories (Spring Data JPA)
                                                              │
                                                        PostgreSQL  ◀── Flyway (migrations)
  Apache POI : génération/lecture des canevas Excel (hors-ligne)
```

## 2. Pile technique

| Brique | Choix | Rôle |
|---|---|---|
| Langage / runtime | **Java 17** | Socle |
| Framework | **Spring Boot 3.3.4** | Application, IoC, auto-config |
| Présentation | **Thymeleaf** | Pages serveur (HTML) |
| Persistance | **Spring Data JPA / Hibernate** | ORM, repositories |
| Base de données | **PostgreSQL** | Stockage, historisation, requêtes à une date |
| Migrations | **Flyway** (V1→V20) | Schéma versionné |
| Sécurité | **Spring Security** | Authentification, 4 rôles, CSRF, en-têtes, anti-force-brute |
| Supervision | **Spring Boot Actuator** | Sonde de santé (`/actuator/health`) |
| Bureautique | **Apache POI** | Canevas Excel (.xlsx) |
| Build | **Maven** | Compilation, tests, packaging |
| Tests | **JUnit 5, Mockito, AssertJ, Testcontainers** | Unitaires et intégration |

Accesseurs Java explicites (**sans Lombok**) pour un build robuste sur tout JDK.

## 3. Architecture en couches

- **`web/`** — contrôleurs MVC (Home, Consultation, Agent, Poste, Mission, Consolidation, Import, RapportAnnuel, Referentiel, **Journal**, Auth), rendu Thymeleaf.
- **`mission`, `agent`, `affectation`, `importation`, `consultation`, `compte`, `audit`** — logique métier (création, validation, historisation, consolidation, restitutions, comptes, audit).
- **`repository/`** — interfaces Spring Data JPA (requêtes dérivées + `@Query`, recherches paginées `rechercher(...)`).
- **`domain/`** — entités JPA (modèle).
- **`consultation/dto/`, `importation/dto/`** — objets de transfert/vues pour l'affichage et l'import.

Convention : services de **lecture** `@Transactional(readOnly = true)`, d'écriture `@Transactional` ; les
vues reçoivent des **DTO** (`*Vue`), jamais les entités.

### Composants transverses

- **`GlobalModelAdvice`** (`@ControllerAdvice`) : expose à toutes les vues `currentUser`, `currentRole`,
  `currentPath` (menu actif) et `nbEcheances` (badge de notifications, via `EcheanceCompteur` mis en cache).
- **`SecurityConfig`** : chaîne de filtres (form login, règles par URL/rôle, BCrypt, déconnexion, **CSRF**,
  **en-têtes de sécurité** CSP/HSTS/Referrer-Policy, sécurité Actuator).
- **`CompteService`** (`UserDetailsService`) + **`LoginAttemptService`** (anti-force-brute) ;
  **`AuditService`** (journal des actions sensibles) ; **`RattachementService`** (historisation agent↔TPR).
- **`fragments/layout.html`** : menu latéral commun ; **`static/css/app.css`** : thème (design tokens `:root`).
- **`DataInitializer`** : compte administrateur initial au premier démarrage.

## 4. Sécurité

- **Authentification** par formulaire ; mots de passe **chiffrés (BCrypt)** ; déconnexion.
- **Quatre rôles** (ADMIN, CHEF_MISSION, AGENT, **MANAGER** — pilotage en lecture seule) ; règles d'accès
  par URL et par méthode HTTP.
- **CSRF activé** ; jeton injecté automatiquement dans les formulaires Thymeleaf (`th:action`).
- **Changement de mot de passe forcé** à la première connexion et après réinitialisation.
- **Anti-force-brute** : verrouillage temporaire du compte après plusieurs échecs.
- **En-têtes de sécurité** : CSP (rendu serveur, sans origine externe), HSTS, Referrer-Policy, anti-MIME/clickjacking.
- **Journal d'audit** : actions sensibles tracées en base (table `audit_event`), écran `/journal` (ADMIN).
- **Comptes liés** à un agent informaticien (identifiant = matricule).
- **Production** : profil dédié — **HTTPS** par reverse-proxy (`forward-headers-strategy`) **ou** keystore
  PKCS12, cookies de session sécurisés (`HttpOnly`, `SameSite`, `Secure`), supervision `/actuator/health`
  réservée à l'ADMIN (sauf `/health`). Compte `admin/admin` initial à changer ; accès réseau restreint à
  l'intranet. Configuration durcie détaillée dans `11-Guide-de-deploiement-securise.md`.

## 5. Mode hors-ligne

Pipeline d'import (package `importation`) :

1. **CanevasWriter** (POI) : génère un canevas pré-rempli (en-tête, membres, agents du TPR, inventaire connu, listes déroulantes, garde-fous).
2. **CanevasReader** (POI) : lit le fichier rempli en DTO de lignes.
3. **ControleImport** : contrôles autonomes (obligatoires, MAC) + **alerte anti-doublon** (interrogation base par MAC / n° de série).
4. **ConsolidationService** : lots, rapprochement par clé, **détection de conflits**, arbitrage.
5. **IntegrationService** : intégration transactionnelle (matériel créé/mis à jour, affectations historisées, **relevé daté avec photo `etat_observe`**).

## 6. Historisation et requêtes à une date

Tables à période (`date_debut` / `date_fin`) : `chef_poste`, `rattachement_agent`, `affectation_materiel`. Les **relevés** (`releve_materiel`) figent une **photo datée** (dont `etat_observe`). L'inventaire à une date s'appuie sur les affectations *actives à la date* et la photo la plus récente ≤ date.

## 7. Déploiement

- **Build** : `mvn clean package` → JAR exécutable.
- **Exécution** : `java -jar` (Tomcat embarqué) ; profil `prod` pour la production ; **reverse-proxy**
  (Nginx/Apache) recommandé pour la terminaison TLS (l'app n'écoute alors que sur `127.0.0.1`).
- **Base** : PostgreSQL ; schéma appliqué automatiquement par **Flyway** au démarrage.
- **Configuration** : variables d'environnement (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `LOG_FILE`…) ou `application-local.properties` (non versionné).
- **Journalisation** : en prod, logs techniques **sur fichier avec rotation** (en plus de la console).
- **Sauvegardes** : scripts PostgreSQL (`scripts/`) + **planification** systemd/cron (Linux) ou tâche
  planifiée PowerShell (Windows) + **test de restauration** (base jetable).

Guides : [06 installation/exploitation](06-Guide-installation-exploitation.md),
[08 déploiement](08-Guide-de-deploiement.md), [11 déploiement sécurisé](11-Guide-de-deploiement-securise.md),
[10 mise à jour](10-Guide-de-mise-a-jour.md).

## 9. Performance & observabilité

- **Pagination côté base** des grandes listes (**Parc**, **Missions**) : requêtes `rechercher(...)` +
  `Pageable` ; l'état temporel dérivé des missions est traduit en prédicats/`CASE` SQL. Petites listes
  (Postes, Agents) paginées en mémoire, **sans N+1** (comptages/jointures groupés).
- **Supervision** : `/actuator/health` (état applicatif + PostgreSQL) pour la sonde réseau.
- **Traçabilité** : journal d'audit en base + logs techniques fichier.

## 8. Choix techniques — justification

- **Web serveur (Thymeleaf)** plutôt que SPA : simplicité de déploiement intranet, pas de build front séparé, robuste hors connectivité externe.
- **Flyway** : schéma reproductible et versionné, indispensable pour les déploiements et les évolutions.
- **PostgreSQL** : gratuit, transactionnel (DDL inclus), adapté à l'historisation.
- **POI** : standard de fait pour les fichiers Excel, mode hors-ligne natif.
