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

Phase de conception terminée. Prochaine étape : développement (squelette Spring Boot, entités JPA).
