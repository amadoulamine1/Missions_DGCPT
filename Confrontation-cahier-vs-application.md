# Confrontation cahier des charges ⇄ application réalisée

*État au 27/06/2026 — cahier de référence : **v13**. Écarts entre le cahier des charges et l'application livrée, vérifiés dans le code.*

## 1. Globalement conforme

Le cœur métier est couvert : postes (TPR) et **chefs de poste historisés** ; agents (informaticiens / agents de poste) ; missions (création, membres, chef de mission **parmi les membres**, **chef de poste facultatif**, **chevauchement de périodes autorisé**, édition, statut temporel, **clôture**) ; matériel et ses types avec caractéristiques ; **mode hors-ligne** (canevas Excel pré-rempli, contrôles, aperçu, **validation**, intégration) ; **consolidation multi-fichiers et arbitrage des conflits** ; **historisations** (chef de poste ; affectation matériel **avec réaffectation et historique des propriétaires** ; **rattachement agent↔TPR avec mutation** ; **agent traitant par mission**) ; restitutions (résultats de mission, inventaire d'un poste, **inventaire à une date avec état observé figé**, **fiche poste avec historiques** missions/fichiers/affectations/chefs) ; recherche / filtres / tri / pagination ; **tableau de bord filtrable par poste** ; **exports** (parc, relevés, statistiques Excel + PDF) ; **administration des référentiels** (logiciels, catégories de câble, **types de matériel paramétrables**) ; authentification et **4 rôles** (dont le **Manager** de pilotage en lecture seule) ; **rapport annuel** (tendance sur 5 ans + prévisions).

## 2. Écarts fonctionnels restants (vs cahier)

Aucun écart fonctionnel structurant. Les chantiers du cadrage sont réalisés, y compris le **snapshot exact à une date** (cf. §5). Restent des reliquats mineurs (§4) et la mise en production opérationnelle (§3).

## 3. Robustesse / mise en production

- **Sécurité durcie** : protection **CSRF réactivée** (formulaires Thymeleaf porteurs du jeton) ; profil `application-prod` (HTTPS, cookies sécurisés) ; **changement de mot de passe forcé** à la première connexion et après réinitialisation (cf. §5). Reste à faire côté exploitation : **déploiement HTTPS** effectif.
- **Sauvegarde** : scripts `pg_dump` + rotation et procédure de restauration présents ; reste leur **planification opérationnelle** (tâche planifiée / cron) et un **test de restauration**.
- **Tests automatisés** — ~75 tests : pipeline d'import (`ImportIntegrationTest`, Testcontainers), **lecture du canevas + colonne logiciel « AD »** (`CanevasFixtureTest`), **consolidation des conflits** (`ConsolidationServiceTest`), **contrôles d'import + filet anti-doublon, onglet générique** (`ControleImportTest`), **restitutions parc/missions + inventaire daté** (`ConsultationServiceTest`), **règles métier des missions** — désignation du chef, clôture, **chevauchement autorisé** (`MissionServiceTest`),
  **rapport annuel** — agrégation par année, parc au 31/12, écarts N‑1 (`RapportAnnuelServiceTest`) et
  **prévision** par régression linéaire (`PrevisionTest`), **validation déclarative des formulaires** (`FormValidationTest`), **sécurité par rôle** des URL (`SecuriteAccesParRoleTest`), réaffectation/mutation (`AffectationServiceTest`), export statistiques (`StatsExporterTest`). **Intégration continue** (GitHub Actions : build + tests à chaque push). *Restent peu couverts* : exports PDF, génération du canevas en écriture (Apache POI).
  - *Note JDK 25* : Mockito ne peut pas instrumenter les **classes concrètes** (inline mock) ; mocker uniquement des **interfaces** (dépôts) ou recourir à des **doublures manuelles** (sous-classes) pour les services concrets.

## 4. Incohérences mineures / cosmétique

- **Rattachement créé hors formulaire** : un agent créé *via l'import* ou *via la désignation d'un chef* n'obtient sa ligne d'historique de rattachement qu'à sa première édition (les agents existants sont repris par le backfill de la migration V7).
- **Performance** : pagination et tri **en mémoire** — sans enjeu au volume cible (~millier d'équipements).

## 5. Résolus

- **Agent traitant historisé par mission** (§3.5/§3.6) — l'agent traitant est porté par le **relevé** (migration **V11**) ; le canevas ne le pré-remplit plus (ressaisie à chaque mission) ; l'ordinateur conserve le « dernier traitant » pour l'affichage ; historique visible sur la fiche équipement.
- **Historique des statuts** — le statut observé est conservé sur chaque relevé (migration **V12**) ; colonne « Statut » dans l'historique des relevés de la fiche équipement.
- **Historiques sur la fiche poste** — missions, fichiers (canevas) chargés, affectations de matériel et chefs de poste successifs, avec un **menu « Aller à »** en tête de fiche pour les repérer.
- **Statut du matériel obligatoire** — contrôle bloquant à l'import (tous onglets) **et** surlignage rouge sur le canevas si vide.
- **Relevé réseau obligatoire** — état du câblage (Neuf / Bon / Pas bon) et catégorie de câble obligatoires ; valeurs **reportées sur la mission à l'import** (la mission est créée en ligne sans ces champs, l'agent les saisit dans le canevas).
- **Canevas par agent** — le téléchargement produit **un canevas par agent membre** (ZIP), pré-renseigné (agent saisisseur en B11) ; onglets réordonnés (Agents TPR après l'en-tête, « 7-Autres matériels » avant les référentiels) ; onglet générique stylé (en-têtes dorés, volet figé) et garde-fous (validation MAC, mises en forme conditionnelles). **Nommage** : ZIP `Canevas-{codePoste}-{début}_{fin}.zip`, fichiers internes suffixés par l'agent. **Téléchargement en lot** depuis la liste des missions (sélection multiple → un seul ZIP à plat).
- **Affichage des relevés** (fiche mission) — saisisseur en « matricule — prénom nom », **statut** affiché, **zone masquée** ; liste des missions **triée du plus récent**.
- **Refonte du design** — tableau de bord (anneau de disponibilité, KPI), identité éditoriale globale (titres serif, filet doré), Parc en mode large, listes et fiches alignées ; **accessibilité** (contrastes AA, focus, `aria-current`, `scope`).
- **Correctif** `etat_observe` — colonne ramenée de `jsonb` (héritée de V1) à `TEXT` (migration **V10**) ; débloque l'intégration/arbitrage.
- **Types de matériel paramétrables** (§3.8 / §9.12) — référentiel `categorie_materiel` (migration **V9**) : l'admin ajoute un type (libellé + préfixe) ; les types système restent câblés et protégés ; onglet canevas **« 7-Autres matériels »** (liste déroulante, intégration en famille *Autre*, contrôles + anti-doublon). **Observation** du matériel désormais affichée (fiche poste + infobulle parc).
- **Snapshot exact à une date** (§9.5) — la photo `etat_observe` est figée à chaque intégration (migration **V8**, rendue idempotente : `ADD COLUMN IF NOT EXISTS`) ; l'inventaire à une date affiche l'état observé au relevé le plus proche ≤ date.
- **Clôture de mission** — bouton dédié « Clôturer » (ADMIN / CHEF_MISSION, masqué si déjà clôturée).
- **Export Excel du parc** — contient désormais **RAM / processeur / disque**.
- **Cartes agents** — **e-mail et téléphone** affichés sur les fiches.
- **Sécurité production** — **CSRF réactivée** ; profil `application-prod` (HTTPS, cookies sécurisés) ; scripts de sauvegarde/restauration PostgreSQL.
- **Administration des référentiels** (§3.8) — écran admin pour **logiciels**, **catégories de câble** et **types de matériel** : ajout, renommage, suppression (avec garde si l'élément est utilisé).
- **Alerte anti-doublon à l'aperçu** (§4) — une ligne **sans n° d'inventaire** dont la **MAC / le n° de série** est déjà connu en base est signalée (**AVERTISSEMENT**) au chef avant validation.
- **Pages d'erreur 403 / 404 / 500** (+ générique) avec menu et message clair, à la place de la page « whitelabel ».
- **Réaffectation du matériel** (même TPR, historique des propriétaires) et **mutation d'un agent** (agent↔TPR, historisée) — le matériel reste dans son poste (§3.2 / §3.7).
- **Tableau de bord filtrable par poste** ; **disponibilité** recalculée sur le matériel statué.
- **Changement de mot de passe forcé** (§9.14) — drapeau `mot_de_passe_a_changer` (migration **V13**) : le compte initial et tout compte réinitialisé doivent changer leur mot de passe avant toute navigation (intercepteur + page self-service).
- **Logiciel « AD »** (§9.14) — ajouté aux logiciels des ordinateurs (migration **V14**) ; colonne Oui/Non du canevas après *Sysbudget* (Aster, Antivirus, SicCDD, CIC, Sysbudget, **AD**).
- **Chef de poste facultatif** (§9.14) — optionnel à la création (migration **V15** : colonne `chef_poste_fige_matricule` nullable) ; renseigné via le canevas à l'import si laissé vide.
- **Dates de mission via le canevas** (§9.14) — les dates début/fin saisies dans le canevas sont reportées à la mission à l'import.
- **Chevauchement de périodes autorisé** (§9.14) — suppression du contrôle bloquant : un agent peut figurer sur plusieurs missions simultanées (création et édition).
- **Qualité** — performance (suppression du N+1 du parc, parse unique du canevas à l'intégration) ; **validation déclarative** (Bean Validation) des formulaires agent/poste ; **journalisation** des créations « à la volée » à l'import ; **intégration continue** (GitHub Actions).
- **Rôle Manager** (§9.15) — profil de pilotage en **lecture seule** (migration **V16** : contrainte `CHECK` du rôle) ; accès aux restitutions + rapport annuel, mutations et import refusés ; navigation adaptée.
- **Rapport annuel** (§9.15) — revue annuelle missions + parc (réservée ADMIN/Manager) : synthèse avec **écarts N‑1** et **prévision N+1** (régression linéaire), **tendance sur ≤ 5 ans**, parc au 31/12 reconstitué via le dernier relevé daté, incidents, activité des agents ; **exports Excel/PDF**.

---

**Restant, par priorité :** déploiement HTTPS + changement du compte initial · planification des sauvegardes (et test de restauration) · tests des exports PDF et de la génération du canevas · finitions (rattachement hors formulaire, pagination/tri côté base).
