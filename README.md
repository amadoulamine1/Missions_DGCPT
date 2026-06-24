# Gestion des missions et du parc informatique — DGCPT

Application de gestion des missions d'inventaire et du parc informatique des postes régionaux de la Direction Générale de la Comptabilité Publique et du Trésor (DGCPT).

## Contexte

Des agents informaticiens de la Direction Informatique effectuent des missions sur les postes régionaux pour relever l'état du câblage réseau et inventorier le matériel (ordinateurs, imprimantes, switchs, access points, scanners de chèque). L'application centralise ces relevés, historise les changements (chefs de poste, affectations) et permet de reconstituer l'inventaire d'un poste ou de tout le parc à une date donnée.

Un **mode hors-ligne** (canevas Excel) permet la saisie en cas de coupure réseau, avec consolidation et validation par le chef de mission à la reconnexion.

## Pile technique

- Application web centralisée, hébergée en interne (intranet)
- **Java** — Spring Boot, Spring Data JPA, Spring Security, Thymeleaf, Apache POI
- Base de données **PostgreSQL**
- Volumétrie : ~20 postes, ~2 000 agents (dont ~700 en poste)

## Livrables

| Fichier | Description |
|---|---|
| `Cahier-des-charges-Gestion-Missions-Parc.md` | Cadrage métier complet (entités, règles, mode hors-ligne, plateforme) |
| `Modele-de-donnees.mermaid` | Diagramme entité-association du modèle de données |
| `schema-postgresql.sql` | Schéma PostgreSQL (tables, contraintes, index, référentiels) |
| `Canevas-Saisie-Mission-Parc.xlsx` | Canevas Excel de saisie hors-ligne |
| `Specification-import-canevas.md` | Spécification de l'import et de la consolidation des canevas |

## Statut

Application **en cours de développement** (Spring Boot/PostgreSQL). Réalisés : gestion des TPR et des agents, création de missions (avec chefs, membres et contrôles), import des canevas (aperçu → validation → intégration), et consultation (parc, postes, missions). Reste notamment l'**authentification avec les rôles**. Voir le §9 du cahier des charges pour le détail des évolutions.

## Développement

Projet Spring Boot (Java 17, Maven).

### Pré-requis
- JDK 17+
- Maven 3.9+
- PostgreSQL 12+ (base `missions_parc`)

### Configuration
Adapter `src/main/resources/application.properties` (URL, utilisateur et mot de passe de la base).

### Lancer
    mvn spring-boot:run

Au démarrage, **Flyway** applique la migration `db/migration/V1__schema_initial.sql` (tables et référentiels), puis l'application est disponible sur http://localhost:8080.

### Structure
- `domain/` — entités JPA et énumérations
- `repository/` — accès aux données (Spring Data JPA)
- `web/` — contrôleurs
- `config/` — configuration (sécurité)
