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
| `Confrontation-cahier-vs-application.md` | Écarts entre le cahier des charges et l'application livrée |
| `Politique-sauvegarde-securite.md` | Recommandations de sauvegarde et de sécurité (opérationnel) |

## Statut

Application **fonctionnelle** (Spring Boot/PostgreSQL), cœur métier couvert. Réalisés : gestion des TPR et des agents (historisation des chefs de poste, mutation d'agent) ; missions (chefs, membres, contrôle de chevauchement, édition, clôture) ; import des canevas (aperçu → validation → intégration) avec **consolidation multi-fichiers et arbitrage des conflits** ; **historisation des affectations** (réaffectation, historique des propriétaires) ; restitutions (parc, postes, missions, **inventaire à une date** avec état observé figé) ; recherche / filtres / tri / pagination ; tableau de bord et **exports** (Excel, PDF) ; **authentification et 3 rôles** (administrateur, chef de mission, agent) ; administration des référentiels — logiciels, catégories de câble et **types de matériel paramétrables**. Suite de **tests automatisés** (unitaires + intégration de l'import). Voir le §9 du cahier des charges pour le détail des évolutions.

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

Au démarrage, **Flyway** applique les migrations `db/migration/V1…V11` (schéma initial, caractéristiques, statut/observations, utilisateurs, lots d'import, rattachements, snapshot daté, types de matériel paramétrables, correctif `etat_observe`, agent traitant historisé), puis l'application est disponible sur http://localhost:8080.

> Si une migration déjà appliquée a été retouchée (commentaire, idempotence), Flyway peut signaler un *checksum mismatch* : exécuter une fois `flyway repair` sur l'environnement concerné pour réaligner l'historique.

### Structure
- `domain/` — entités JPA et énumérations
- `repository/` — accès aux données (Spring Data JPA)
- `web/` — contrôleurs
- `config/` — configuration (sécurité)
