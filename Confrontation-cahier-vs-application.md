# Confrontation cahier des charges ⇄ application réalisée

*État au 25/06/2026 — cahier de référence : **v9**. Écarts entre le cahier des charges et l'application livrée, vérifiés dans le code.*

## 1. Globalement conforme

Le cœur métier est couvert : postes (TPR) et **chefs de poste historisés** ; agents (informaticiens / agents de poste) ; missions (création, membres, chef de mission **parmi les membres**, contrôle de chevauchement, édition, statut temporel, **clôture**) ; matériel et ses types avec caractéristiques ; **mode hors-ligne** (canevas Excel pré-rempli, contrôles, aperçu, **validation**, intégration) ; **consolidation multi-fichiers et arbitrage des conflits** ; **historisations** (chef de poste ; affectation matériel **avec réaffectation et historique des propriétaires** ; **rattachement agent↔TPR avec mutation**) ; restitutions (résultats de mission, inventaire d'un poste, **inventaire à une date avec état observé figé**) ; recherche / filtres / tri / pagination ; **tableau de bord filtrable par poste** ; **exports** (parc, relevés, statistiques Excel + PDF) ; **administration des référentiels** ; authentification et **3 rôles**.

## 2. Écarts fonctionnels restants (vs cahier)

Aucun écart fonctionnel structurant. Les chantiers du cadrage sont réalisés, y compris le **snapshot exact à une date** (cf. §5). Restent des reliquats mineurs (§4) et la mise en production opérationnelle (§3).

## 3. Robustesse / mise en production

- **Sécurité durcie** : protection **CSRF réactivée** (formulaires Thymeleaf porteurs du jeton) ; profil `application-prod` (HTTPS, cookies sécurisés). Reste à faire côté exploitation : **déploiement HTTPS** effectif et **changement du compte initial `admin / admin`** (idéalement forcé au premier login).
- **Sauvegarde** : scripts `pg_dump` + rotation et procédure de restauration présents ; reste leur **planification opérationnelle** (tâche planifiée / cron) et un **test de restauration**.
- **Tests automatisés** — couverture élargie : pipeline d'import (`ImportIntegrationTest`, Testcontainers), **consolidation des conflits** (`ConsolidationServiceTest`), **contrôles d'import + filet anti-doublon** (`ControleImportTest`), **restitutions parc/missions + inventaire daté** (`ConsultationServiceTest`), réaffectation/mutation (`AffectationServiceTest`), export statistiques (`StatsExporterTest`). *Restent peu couverts* : **sécurité par rôle** (accès des endpoints), **MissionService** (chevauchement des membres, désignation du chef), exports PDF.
  - *Note JDK 25* : Mockito ne peut pas instrumenter les **classes concrètes** (inline mock) ; mocker uniquement des **interfaces** (dépôts) ou recourir à des **doublures manuelles** (sous-classes) pour les services concrets.

## 4. Incohérences mineures / cosmétique

- **Rattachement créé hors formulaire** : un agent créé *via l'import* ou *via la désignation d'un chef* n'obtient sa ligne d'historique de rattachement qu'à sa première édition (les agents existants sont repris par le backfill de la migration V7).
- **Performance** : pagination et tri **en mémoire** — sans enjeu au volume cible (~millier d'équipements).

## 5. Résolus

- **Snapshot exact à une date** (§9.5) — la photo `etat_observe` est figée à chaque intégration (migration **V8**, rendue idempotente : `ADD COLUMN IF NOT EXISTS`) ; l'inventaire à une date affiche l'état observé au relevé le plus proche ≤ date.
- **Clôture de mission** — bouton dédié « Clôturer » (ADMIN / CHEF_MISSION, masqué si déjà clôturée).
- **Export Excel du parc** — contient désormais **RAM / processeur / disque**.
- **Cartes agents** — **e-mail et téléphone** affichés sur les fiches.
- **Sécurité production** — **CSRF réactivée** ; profil `application-prod` (HTTPS, cookies sécurisés) ; scripts de sauvegarde/restauration PostgreSQL.
- **Administration des référentiels** (§3.8) — écran admin pour **logiciels** et **catégories de câble** : ajout, renommage, suppression (avec garde si l'élément est utilisé).
- **Alerte anti-doublon à l'aperçu** (§4) — une ligne **sans n° d'inventaire** dont la **MAC / le n° de série** est déjà connu en base est signalée (**AVERTISSEMENT**) au chef avant validation.
- **Pages d'erreur 403 / 404 / 500** (+ générique) avec menu et message clair, à la place de la page « whitelabel ».
- **Réaffectation du matériel** (même TPR, historique des propriétaires) et **mutation d'un agent** (agent↔TPR, historisée) — le matériel reste dans son poste (§3.2 / §3.7).
- **Tableau de bord filtrable par poste** ; **disponibilité** recalculée sur le matériel statué.

---

**Restant, par priorité :** déploiement HTTPS + changement du compte initial · planification des sauvegardes (et test de restauration) · tests de sécurité par rôle et de `MissionService` · finitions (rattachement hors formulaire, pagination/tri côté base).
