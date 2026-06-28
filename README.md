# Gestion des missions et du parc informatique — DGCPT

Application de gestion des missions d'inventaire et du parc informatique des postes régionaux de la Direction Générale de la Comptabilité Publique et du Trésor (DGCPT).

> Documentation : dossier [`docs/`](docs/README.md) · historique des évolutions : [`CHANGELOG.md`](CHANGELOG.md).

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
| `Confrontation-cahier-vs-application.md` | Écarts entre le cahier des charges et l'application livrée |
| `Politique-sauvegarde-securite.md` | Recommandations de sauvegarde et de sécurité (opérationnel) |

## Statut

Application **fonctionnelle** (Spring Boot/PostgreSQL), cœur métier couvert. Réalisés : gestion des TPR et des agents (historisation des chefs de poste, mutation d'agent) ; missions (chefs, membres, **chef de poste facultatif à la création mais obligatoire au canevas**, **chevauchement de périodes autorisé**, édition, clôture, liste **filtrable (poste, région, agent, état)**, **agents en charge listés** et **ordres de mission PDF joignables**) ; import des canevas (aperçu → validation → intégration) avec **consolidation multi-fichiers et arbitrage des conflits**, report à la mission des dates et du chef de poste saisis dans le canevas ; **historisation des affectations** (réaffectation, historique des propriétaires) ; restitutions (parc, postes, missions, **inventaire à une date** avec état observé figé) ; recherche / filtres / tri / pagination ; tableau de bord et **exports** (Excel, PDF) ; **authentification et 4 rôles** (administrateur, chef de mission, agent, **manager** de pilotage en lecture seule) avec **changement de mot de passe forcé** ; **rapport annuel** (revue des missions et du parc, tendance sur 5 ans, écarts N‑1 et **prévisions**, exports Excel/PDF) ; administration des référentiels — logiciels (dont **AD**), catégories de câble et **types de matériel paramétrables**. Validation déclarative des formulaires, **journal d'audit** des actions sensibles (connexions, missions, réaffectations, comptes), supervision **Actuator**, **anti-force-brute** et en-têtes de sécurité, suite de **tests automatisés** (unitaires + intégration de l'import) et **intégration continue**. Voir le §9 du cahier des charges pour le détail des évolutions.

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

Au démarrage, **Flyway** applique les migrations `db/migration/V1…V20` (schéma initial, caractéristiques, statut/observations, utilisateurs, lots d'import, rattachements, snapshot daté, types de matériel paramétrables, correctif `etat_observe`, agent traitant historisé, historique des statuts, **changement de mot de passe forcé** (V13), **logiciel « AD »** (V14), **chef de poste de mission rendu optionnel** (V15), **rôle MANAGER** (V16), **ordre de mission PDF** (V17), **plusieurs ordres de mission par mission** (V18), **journal d'audit** (V20)), puis l'application est disponible sur http://localhost:8080.

> Si une migration déjà appliquée a été retouchée (commentaire, idempotence), Flyway peut signaler un *checksum mismatch* : exécuter une fois `flyway repair` sur l'environnement concerné pour réaligner l'historique.

### Structure
- `domain/` — entités JPA et énumérations
- `repository/` — accès aux données (Spring Data JPA)
- `web/` — contrôleurs
- `config/` — configuration (sécurité)
