# Journal des modifications (CHANGELOG)

Évolutions de l'application **Missions & Parc DGCPT**, regroupées par **jalon** (version du cahier des
charges + migrations Flyway). Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/).
Catégories : **Ajouté**, **Modifié**, **Corrigé**, **Sécurité**, **Exploitation**, **Doc**.

---

## [Cahier v15] — 2026-06-28 — Durcissement, observabilité, exploitation & finitions
*Migrations : V20 · ~84 tests*

### Sécurité
- **Journal d'audit** des actions sensibles (table `audit_event`, **V20**) : connexions réussies/refusées
  + IP, verrouillages, création/clôture de mission, réaffectation, gestion des comptes ; écran `/journal`
  (ADMIN), filtrable et paginé.
- **Anti-force-brute** : verrouillage temporaire du compte après 5 échecs de connexion.
- **En-têtes de sécurité HTTP** : CSP (rendu serveur), HSTS, Referrer-Policy, anti-MIME/clickjacking.

### Ajouté
- **Tableau de bord — panneau d'alertes** actionnables (missions en retard / à échéance, matériel en panne
  / à changer) et **notification d'échéance** (badge présent sur toutes les pages, ADMIN/Chef).
- **Supervision** : Spring Boot **Actuator** (`/actuator/health` ouvert + sonde PostgreSQL ; reste réservé ADMIN).
- **Export / import de la base** (ADMIN, écran `/donnees`) : export d'une sauvegarde complète (`pg_dump`,
  format custom) et import/restauration **atomique** (`pg_restore --single-transaction`) ; opérations tracées.
- **Tests** : génération du canevas POI (`CanevasWriterTest`), verrou (`LoginAttemptServiceTest`),
  historisation du rattachement (`RattachementServiceTest`).

### Modifié
- **Pagination côté base** des grandes listes : **Parc** et **Missions** (l'état temporel dérivé des
  missions est traduit en prédicats / expression `CASE` SQL).
- **Performance** : suppression des N+1 des listes de postes (comptage matériel groupé) et d'agents
  (postes pré-chargés) ; compteur d'échéances mis en cache.
- **Tableaux denses scrollables** sur petit écran.

### Corrigé
- **Rattachement agent↔TPR créé à la volée** (import, désignation d'un chef, création de mission) :
  la ligne d'historique est désormais ouverte immédiatement, et plus seulement à la première édition.
- Suppression de code mort (`listerMissions` en mémoire) après passage au SQL.

### Exploitation
- **Journalisation sur fichier avec rotation** en production (`logging.file.name`, surchargeable par `LOG_FILE` ;
  10 Mo/jour, 30 jours, 500 Mo).
- **Sauvegardes planifiées** : unités systemd + cron (Linux), **script PowerShell + tâche planifiée** (Windows),
  **script de test de restauration** (base jetable).
- Profil de production **prêt pour HTTPS** (reverse-proxy `forward-headers-strategy` ou keystore PKCS12).

### Doc
- Nouveaux guides : **08** déploiement, **09** explication du code, **10** mise à jour,
  **11** déploiement sécurisé (reverse-proxy Nginx/Apache, HTTPS forcé, TLS durci, en-têtes, pare-feu).
- Passe de cohérence générale (cahier §9.17, dossier d'architecture, confrontation, README, index).

---

## [Cahier v13–v14] — 2026-06-27 — Rapport annuel, rôle Manager, ordres de mission
*Migrations : V16, V17, V18 (V19 non utilisée)*

### Ajouté
- **Rapport annuel** (ADMIN / Manager) : revue des missions et du parc — écarts N‑1, **prévisions** N+1
  (régression linéaire), tendance sur ≤ 5 ans, parc au 31/12, exports Excel/PDF.
- **Rôle MANAGER** (**V16**) : pilotage en **lecture seule** (restitutions + rapport annuel, sans modification).
- **Liste des missions** : **agents en charge** affichés (chef en tête) ; **filtres** par poste, région,
  agent de mission et état.
- **Ordres de mission (PDF)** joignables par mission : un (**V17**) puis **plusieurs** (**V18**),
  téléchargeables et supprimables.
- **Téléchargement des canevas en lot** depuis la liste des missions ; nommage ZIP/fichiers par code poste,
  période et agent.

### Modifié / Sécurité
- **Verrouillage de la colonne N° d'inventaire** dans le canevas (lecture seule).
- **Chef de poste obligatoire dans le canevas** : chargement bloqué s'il est vide (mise en forme conditionnelle).

### Doc
- Guide de remplissage du canevas ; mises à jour cahier, specs, modèle, recette, manuel.

---

## [Cahier v12] — 2026-06-25 — Design, qualité, souplesse des missions
*Migrations : V9 → V15*

### Ajouté
- **Refonte du design** : identité éditoriale, tableau de bord (anneau de disponibilité, KPI, graphiques CSS),
  Parc en mode large, cartes d'agents ; **accessibilité** (contrastes AA, focus, ARIA).
- **Types de matériel paramétrables** (**V9**) : référentiel `categorie_materiel`, onglet « 7-Autres matériels ».
- **Agent traitant historisé par mission** (**V11**) ; **historique des statuts** du matériel (**V12**) ;
  historiques sur la fiche poste.
- **Logiciel « AD »** dans le canevas (**V14**, colonne Oui/Non).
- **Souplesse missions** : dates modifiables via le canevas, **chevauchement de périodes autorisé**,
  **chef de poste facultatif à la création** (**V15**).

### Sécurité / Qualité
- **Changement de mot de passe forcé** à la première connexion et après réinitialisation (**V13**).
- **Bean Validation** des formulaires ; journalisation des créations à la volée à l'import ;
  **intégration continue** (GitHub Actions).
- **Performance (P1)** : suppression du N+1 du parc, parse unique du canevas à l'intégration.

### Corrigé
- `releve_materiel.etat_observe` ramené de `jsonb` à `TEXT` (**V10**) ; idempotence de la migration V8.

---

## [Cahier v4–v9] — 2026-06-24 — Authentification, restitutions, consolidation
*Migrations : V2 → V8*

### Ajouté
- **Authentification** Spring Security (form login, BCrypt, rôles) et **gestion des comptes** (liés à un
  agent informaticien) (**V4**, **V5**).
- **Réaffectation du matériel** (même TPR, historique des propriétaires) et **mutation d'agent**
  (agent↔TPR, **V7**), historisées.
- **Tableau de bord** (KPI, graphiques, disponibilité, filtrable par poste) + **exports** Excel/PDF.
- **Inventaire à une date** ; **photo datée** `etat_observe` figée à l'intégration (**V8**).
- **Consolidation multi-fichiers et arbitrage des conflits** (**V6**) ; **alerte anti-doublon**.
- **Administration des référentiels** (logiciels, catégories de câble) ; **pages d'erreur** habillées ;
  **étiquettes** imprimables ; recherche / filtres / tri / pagination.
- Caractéristiques ordinateur RAM/processeur/disque (**V2**) ; statut & observations du matériel (**V3**).

### Doc
- Politique de sauvegarde et de sécurité ; dossier de projet (`docs/01`…`07`).

---

## [Cahier v1–v3] — 2026-06-23 — Socle applicatif
*Migration : V1*

### Ajouté
- **Squelette Spring Boot** : build Maven, schéma PostgreSQL initial (**V1**) via Flyway, entités JPA,
  repositories, sécurité de base, page d'accueil.
- **Mode hors-ligne** : génération et lecture des **canevas Excel** (Apache POI), contrôles, aperçu,
  validation explicite, **intégration en base** (rapprochement, sous-types, affectations, relevés datés).
- **Écrans de consultation** : postes, parc, missions, relevés.
- **Création de mission en ligne** : génération du N° et canevas pré-estampillé.
- **Tests d'intégration** du pipeline d'import (Testcontainers).

### Corrigé
- Retrait de Lombok (accesseurs explicites) pour un build robuste sur tout JDK.

---

> Conventions : migrations Flyway **immuables** (cf. `docs/10-Guide-de-mise-a-jour.md`) ; la numérotation
> peut comporter un trou (V19 inutilisée). Détail des évolutions : §9 du cahier des charges et
> `Confrontation-cahier-vs-application.md`.
