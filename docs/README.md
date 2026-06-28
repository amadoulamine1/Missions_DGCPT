# Dossier de projet — Gestion des missions et du parc informatique (DGCPT)

Ce dossier rassemble la documentation du projet, **du cadrage et des spécifications jusqu'à la recette**, pour la mise en place et la réalisation de l'application.

## Contexte

La **Direction Générale de la Comptabilité Publique et du Trésor (DGCPT)** dispose de **postes régionaux (TPR)**, chacun avec un chef de poste, des agents et un parc informatique. Des **missions** d'inventaire et de relevé réseau sont menées sur ces postes. L'application centralise la gestion des postes, des agents, du parc, des missions et de leurs relevés, en ligne ou **hors-ligne** (canevas Excel) en cas de coupure réseau.

## Cycle de vie documentaire

| Phase | Document | Objet |
|---|---|---|
| Cadrage | [01 — Note de cadrage](01-Note-de-cadrage.md) | Contexte, objectifs, périmètre, acteurs, risques, planning |
| Spécifications | [Cahier des charges](../Cahier-des-charges-Gestion-Missions-Parc.md) | Expression du besoin (référence métier, v15) |
| Spécifications | [02 — Spécifications fonctionnelles détaillées](02-Specifications-fonctionnelles-detaillees.md) | Cas d'usage, écrans, règles de gestion, matrice des droits |
| Études techniques | [03 — Dossier d'architecture technique](03-Dossier-architecture-technique.md) | Stack, architecture en couches, sécurité, hors-ligne, déploiement |
| Études techniques | [04 — Modèle de données](04-Modele-de-donnees.md) | Entités, relations, tables, migrations, historisations |
| Réalisation | (code) `src/`, migrations `db/migration/V1..V20` | Application Spring Boot |
| Tests / Recette | [05 — Plan de tests et cahier de recette](05-Plan-de-tests-et-cahier-de-recette.md) | Stratégie de test, cas de recette, PV |
| Exploitation | [06 — Guide d'installation et d'exploitation](06-Guide-installation-exploitation.md) | Pré-requis, build, déploiement, sauvegardes |
| Exploitation | [Politique de sauvegarde et de sécurité](../Politique-sauvegarde-securite.md) | Sécurité et sauvegardes |
| Utilisation | [07 — Manuel utilisateur](07-Manuel-utilisateur.md) | Prise en main par rôle, workflows |
| Déploiement | [08 — Guide de déploiement](08-Guide-de-deploiement.md) | Mise en production : build, config, HTTPS, service, recette, checklist sécurité |
| Déploiement | [11 — Guide de déploiement sécurisé](11-Guide-de-deploiement-securise.md) | Reverse-proxy **Nginx & Apache**, HTTPS forcé, TLS durci, en-têtes, pare-feu, vérifications |
| Développement | [09 — Guide d'explication du code](09-Guide-d-explication-du-code.md) | Organisation des packages, cycle d'une requête, flux métier, conventions |
| Maintenance | [10 — Guide de mise à jour](10-Guide-de-mise-a-jour.md) | Mise à jour, migrations Flyway, dépendances, dépannage |
| Qualité | [Confrontation cahier ⇄ application](../Confrontation-cahier-vs-application.md) | Écarts résiduels et points de vigilance |
| Hors-ligne | [Guide de remplissage du canevas](../Guide-remplissage-canevas.md) | Remplir le fichier Excel d'inventaire |

## État du projet

Application **réalisée et fonctionnelle** couvrant l'ensemble du périmètre métier (postes, agents, parc, missions, hors-ligne et consolidation, historisations, restitutions, tableau de bord, référentiels, comptes). **Durcie pour la production** : 4 rôles, anti-force-brute, en-têtes de sécurité, journal d'audit, supervision (`/actuator/health`), pagination côté base, journalisation fichier, sauvegardes planifiées + test de restauration, guides de déploiement (dont sécurisé Nginx/Apache). Reste, **côté terrain** : installation du certificat/proxy HTTPS, copie des sauvegardes hors-serveur, changement du compte initial.

## Pile technique (résumé)

Spring Boot 3.3 · Java 17 · Maven · Thymeleaf · Spring Data JPA (Hibernate) · Spring Security · PostgreSQL · Flyway · Apache POI.
