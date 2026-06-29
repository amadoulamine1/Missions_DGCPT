# 02 — Spécifications fonctionnelles détaillées (SFD)

Réf. métier : *Cahier des charges v9*. Ce document décrit les fonctions telles que réalisées.

## 1. Acteurs et matrice des droits

| Fonction | Admin | Manager | Chef de mission | Agent |
|---|:--:|:--:|:--:|:--:|
| Tableau de bord, consultation (postes, parc, missions, inventaire à une date) | ✓ | ✓ | ✓ | ✓ |
| Consulter la liste des agents | ✓ | ✓ | | |
| **Rapport annuel** (tendance, prévisions, exports) | ✓ | ✓ | | |
| Créer / modifier un poste (TPR) | ✓ | | | |
| Désigner le chef d'un poste | ✓ | | | |
| Créer / modifier un agent ; muter un agent | ✓ | | | |
| Créer / éditer / clôturer une mission ; membres | ✓ | | ✓ | |
| Joindre / remplacer / supprimer l'ordre de mission (PDF) | ✓ | | ✓ | |
| Télécharger un canevas, téléverser un fichier rempli | ✓ | | ✓ | ✓ |
| Valider / arbitrer la consolidation d'import | ✓ | | ✓ | |
| Réaffecter un matériel à un agent | ✓ | | | |
| Gérer les référentiels (logiciels, catégories de câble) | ✓ | | | |
| Gérer les comptes utilisateurs | ✓ | | | |
| Consulter le **journal d'audit** | ✓ | | | |
| **Exporter / importer** la base de données | ✓ | | | |

Le **Manager** est un profil de **pilotage en lecture seule** : toutes les restitutions et le rapport
annuel, sans aucune modification ni import. L'accès est filtré par rôle (Spring Security) et la
**navigation s'adapte** (les liens et actions réservés sont masqués).

## 2. Domaine « Postes (TPR) »

- **Lister** les postes : code, nom, région, nombre de matériels ; **tri** par colonnes et **pagination**.
- **Fiche poste** : chef de poste courant, agents rattachés, matériel du poste (avec affectataire et caractéristiques).
- **Créer/Modifier** (admin) : code (unique), nom, région.
- **Chef de poste (historisé)** : désignation/changement depuis la fiche du TPR, avec **date d'effet** ; l'application clôt la période précédente et ouvre la nouvelle.

## 3. Domaine « Agents »

- Présentation en **fiches (cartes)**, séparées en **informaticiens** et **agents de poste** ; **recherche** (matricule, nom, fonction) et, pour les agents de poste, **filtre par TPR** (boutons).
- **Créer/Modifier** (admin) : matricule (clé), nom, prénom, fonction, téléphone, e-mail, type (informaticien / agent de poste), TPR de rattachement pour un agent de poste.
- **Mutation d'un agent (historisée)** : changement de TPR de rattachement avec **date d'effet** ; l'historique des rattachements est conservé. *La mutation ne déplace pas le matériel.*

## 4. Domaine « Parc » (matériel)

- **Identité commune** : n° d'inventaire (clé, **généré** par l'application), type, poste de rattachement (historisé), nom, modèle, statut, observation.
- **Types et attributs** : Ordinateur (MAC ethernet/wifi, nom machine, RAM, processeur, disque, agent attributaire, agent traitant, logiciels), Imprimante (MAC, wifi, IP), Switch / Access point (MAC, IP), Scanner de chèque (n° de série, marque).
- **Statut** : En service / En panne / À changer.
- **Liste du parc** : recherche (n°, nom, modèle), filtres rapides (**type** et **statut** en pastilles, **poste**), **tri** par colonnes, **pagination** ; pour les ordinateurs, RAM/processeur/disque affichés ; **export Excel** de l'inventaire filtré ; **étiquettes** imprimables.
- **Fiche équipement** : caractéristiques par type, statut, observation, **affectation courante**, **historique des propriétaires**, **historique des relevés**.
- **Réaffectation (admin, historisée)** : depuis la fiche, réaffectation à un autre agent **du même TPR** (contrôlé côté liste **et** serveur), avec date d'effet ; clôture/ouverture de période.

## 5. Domaine « Missions »

- **Création** (admin/chef) : TPR (existant ou nouveau), objet, période (début → fin, fin ≥ début), **chef de mission** (désigné parmi les membres), **membres** (informaticiens, sélection multiple), **chef de poste facultatif** (figé sur la mission s'il est fourni). Génération du **N° de mission** (`MIS-AAAA-NNN`) puis d'un **canevas pré-estampillé**.
- **Chevauchement de périodes autorisé** : un agent peut être membre de plusieurs missions dont les périodes se chevauchent (aucun contrôle bloquant, ni à la création ni à l'édition).
- **Liste des missions** : **filtrable** par **poste**, **région**, **agent de mission** (informaticien membre) et **état**, avec recherche texte ; affiche les **agents en charge** (chef de mission en tête — mention « chef » — puis les membres) et les **ordres de mission** (cf. ci-dessous).
- **Ordres de mission (PDF)** : documents **facultatifs**, **plusieurs possibles par mission**, **joints** depuis la liste (admin/chef) puis **téléchargeables** par tout utilisateur connecté (ouverture dans le navigateur) ; **supprimables** individuellement. Seul le **PDF** est accepté ; les fichiers sont stockés en base.
- **Renseignement via le canevas** : à l'import, le **chef de poste** (s'il était inconnu) et les **dates de mission** saisis dans le canevas sont reportés à la mission. Le **chef de poste est obligatoire dans le canevas** : s'il est laissé vide, le **chargement est bloqué**.
- **Édition** : objet, dates, statut, observations, membres, chef de mission.
- **Téléchargement des canevas** : par mission (ZIP `Canevas-{codePoste}-{début}_{fin}.zip`, fichiers internes `…-{matricule}-{nom}.xlsx`) ou **en lot** depuis la liste (sélection multiple → un seul ZIP à plat ; admin/chef).
- **Statut de consolidation** : *en consolidation* → *clôturée* (bouton **Clôturer**, réservé admin/chef).
- **Statut temporel** dérivé des dates : Planifiée / En cours / Terminée.
- **Fiche mission** : en-tête, relevé réseau (état câblage, catégorie de câble), membres, inventaire relevé, **export Excel des relevés**, accès à la **consolidation** et au **canevas pré-rempli**.

## 6. Domaine « Hors-ligne : canevas et import »

1. **Canevas pré-rempli** généré pour la mission (en-tête, membres, agents du TPR, listes déroulantes, inventaire déjà connu du poste).
2. **Saisie hors-ligne** par les agents (un fichier par agent possible).
3. **Chargement** → **aperçu** + **contrôles** : champs obligatoires, format des adresses MAC (`AA:BB:CC:DD:EE:FF`), et **alerte anti-doublon** (ligne sans n° d'inventaire mais MAC/n° de série déjà connu en base → avertissement).
4. **Consolidation multi-fichiers** : chaque fichier devient un **lot** rattaché à la mission ; rapprochement par clé (n° d'inventaire, sinon MAC / n° de série) ; **détection des conflits** (valeurs divergentes) **arbitrés par le chef**.
5. **Intégration** (validation explicite) : photo datée figée, fiches matériel créées/mises à jour, **n° d'inventaire attribués** aux nouveaux, affectations historisées.

**Garde-fous du canevas** : indicateur conforme / incomplet / non conforme, mises en forme conditionnelles (obligatoires en rouge, MAC invalide en orange), validation de saisie des MAC, protection de la mise en forme contre le collage.

## 7. Restitutions et tableau de bord

- **Inventaire à une date** : composition et localisation du parc à une date donnée (à partir de l'historique des affectations) + **état observé** (photo datée du relevé au plus proche ≤ date).
- **Tableau de bord** (accueil), **filtrable par poste** : indicateurs (postes, matériel, **disponibilité** = en service / matériel statué, postes en alerte, missions) et graphiques (matériel par statut, par type, par poste, missions par état). **Exports** des statistiques en **Excel** (synthèse / par type / par poste) et **PDF** (impression).
- **Rapport annuel** (Admin / Manager) : revue annuelle des missions et du parc — synthèse avec **écart N‑1** et **prévision N+1**, **tendance sur ≤ 5 ans**, parc au 31/12 (statut reconstitué via le dernier relevé daté), incidents par type/poste, activité des agents ; **exports Excel / PDF**.

## 8. Référentiels et comptes

- **Référentiels** (admin) : logiciels et catégories de câble — ajout, renommage, suppression (bloquée si l'élément est utilisé).
- **Comptes** (admin) : création, modification, **réinitialisation** du mot de passe, activation/désactivation ; un compte chef de mission / agent est **rattaché à un agent informaticien** (identifiant = matricule, nom repris de l'agent) ; mot de passe **affichable** à la saisie.

## 8 bis. Sécurité et traçabilité

- **Journal d'audit** (admin) : trace horodatée des actions sensibles — **connexions** (réussies / refusées, avec IP) et **verrouillages**, **création / clôture de mission**, **réaffectation de matériel**, **création / modification de compte**, **réinitialisation de mot de passe**, **export / import de la base**. Écran consultable et **filtrable** (par action, par utilisateur), paginé.
- **Export / import des données** (admin) : écran `/donnees` pour **exporter** une sauvegarde complète de la base (format PostgreSQL « custom », horodatée, téléchargeable) et **importer** un fichier de sauvegarde (restauration **atomique** qui **remplace** les données ; confirmation requise). S'appuie sur `pg_dump`/`pg_restore` ; chaque opération est **tracée** dans le journal d'audit.
- **Durcissement** : protection **anti-force-brute** (verrouillage temporaire après échecs répétés), **en-têtes de sécurité** (CSP, HSTS, Referrer-Policy), **supervision** via Actuator (`/actuator/health`).

## 9. Règles de gestion transverses (rappel)

- **Historisation** (jamais d'écrasement) : chef de poste, rattachement agent↔TPR, affectation matériel↔agent.
- **Photo datée** : chaque relevé est figé à sa date (composition, localisation, attributs observés).
- **Clé matériel** : le n° d'inventaire ; case vide à l'import ⇒ nouveau matériel numéroté automatiquement.
- **Cohérence des rôles** : chef de mission et membres = informaticiens ; chef de poste et attributaire = agents de poste.
