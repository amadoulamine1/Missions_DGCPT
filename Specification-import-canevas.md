# Spécification de l'import des canevas Excel

*Comment l'application lit, contrôle, rapproche et consolide les canevas remplis hors-ligne, jusqu'à l'intégration validée par le chef de mission.*

## 1. Objectif

Transformer un canevas rempli hors-ligne en données fiables dans la base, en :

- contrôlant la saisie (formats, champs obligatoires, référentiels) ;
- rapprochant chaque matériel d'une fiche existante ou en créant une nouvelle ;
- consolidant les **plusieurs fichiers** d'une même mission en un **relevé unique** ;
- ne validant rien sans le **feu vert du chef de mission**.

## 2. Vue d'ensemble du flux

1. **Préparation** — le chef de mission crée la mission en ligne ; l'application génère le **N° de mission** et produit les canevas pré-estampillés (en-tête + référentiels du poste).
2. **Saisie hors-ligne** — chaque agent remplit son fichier.
3. **Upload** — l'agent charge son fichier ; l'application crée un **lot d'import** (brouillon) rattaché à la mission via le N° de mission.
4. **Contrôles automatiques** — structure, formats, obligatoires, référentiels, cohérence (§5).
5. **Rapprochement** — chaque ligne de matériel est identifiée : existante (mise à jour) ou nouvelle (§6).
6. **Consolidation** — le lot s'ajoute aux autres lots de la mission ; doublons et conflits inter-fichiers détectés (§7).
7. **Revue & validation** — le chef de mission voit le rapport, arbitre, puis valide (§8).
8. **Intégration** — écriture en base, attribution des numéros d'inventaire, photo datée figée (§9), puis restitutions (§10).

## 3. Pré-requis : le canevas pré-estampillé

Parce que le chef crée la mission en ligne d'abord, chaque canevas est distribué avec :

- le **N° de mission** et le **code poste** déjà renseignés (en-tête) ;
- l'en-tête de mission pré-remplie (objet, dates, chef de mission, chef de poste) ;
- les **listes déroulantes** alimentées avec les référentiels du poste (agents, catégories de câble, logiciels).

L'agent complète son **matricule de saisisseur**, éventuellement sa **zone**, le **relevé réseau** (une fois) et l'**inventaire**.

## 4. Lecture du fichier (correspondance onglets → tables)

| Onglet du canevas | Tables cibles | Remarques |
|---|---|---|
| 1-Mission et Réseau | `mission` | N° de mission → `mission.reference` ; relevé réseau → `etat_cablage`, `categorie_cable_id` ; agent saisisseur et zone repris sur chaque ligne du lot |
| 2-Membres mission | `mission_membre` | matricules d'informaticiens |
| 3-Ordinateurs | `materiel` + `ordinateur` (+ `ordinateur_logiciel`) | colonnes logiciels Oui/Non → lignes `ordinateur_logiciel` |
| 4-Imprimantes | `materiel` + `imprimante` | |
| 5-Switchs et AP | `materiel` + `equipement_reseau` | colonne **Type** → `materiel.type` (`SWITCH` ou `ACCESS_POINT`) |
| 6-Scanners chèque | `materiel` + `scanner_cheque` | clé physique = **numéro de série** |
| 7-Autres matériels | `materiel` (+ `categorie_materiel`) | **types paramétrables** (famille *Autre*) ; colonne **Type** → liste déroulante du référentiel ; MAC/IP portées par `materiel` |

## 5. Contrôles à l'import

Deux niveaux de sévérité : **Bloquant** (la ligne ou le fichier est rejeté tant que ce n'est pas corrigé) et **Avertissement** (signalé au chef de mission, qui tranche).

| Contrôle | Sévérité |
|---|---|
| Onglets et colonnes attendus présents | Bloquant |
| N° de mission présent, existant, et mission au statut *en consolidation* | Bloquant |
| Code poste du fichier cohérent avec celui de la mission | Bloquant |
| Agent saisisseur renseigné, connu, et de type `INFORMATICIEN` | Bloquant |
| Champs obligatoires (marqués `*`) renseignés sur chaque ligne | Bloquant |
| Format des adresses MAC `AA:BB:CC:DD:EE:FF` | Bloquant |
| Format IP, e-mail, dates | Bloquant |
| Valeurs des listes conformes (catégorie de câble, type, Oui/Non) | Bloquant |
| Agent attributaire = agent **de ce poste** | Bloquant |
| Agent installateur = agent `INFORMATICIEN` | Avertissement |
| N° d'inventaire renseigné mais inconnu en base | Bloquant |
| Doublon **intra-fichier** (même clé deux fois dans le fichier) | Bloquant |
| MAC ou n° de série déjà connu sous **un autre** numéro d'inventaire (ligne sans n°) | Avertissement (rapprochement proposé) |
| Onglet *7-Autres matériels* : **Type** et **Nom** renseignés | Bloquant |
| Onglet *7-Autres matériels* : **Type** correspondant à un type connu du référentiel | Bloquant (type inconnu) |

## 6. Rapprochement du matériel (clé d'identité)

Pour chaque ligne de matériel, l'application applique cette logique :

1. **N° d'inventaire renseigné ?**
   - **Oui** → on cherche le matériel. Trouvé → **mise à jour** de la fiche. Introuvable → **erreur bloquante** (numéro inconnu).
   - **Non** → matériel candidat à la **création**.
2. **Filet de sécurité (avant création)** — on recherche une fiche existante par **adresse MAC** (ordinateur, imprimante, équipement réseau) ou par **numéro de série** (scanner de chèque).
   - Correspondance trouvée → **avertissement** « ce matériel existe peut-être déjà » ; le chef de mission choisit : rattacher à l'existant (mise à jour) ou créer quand même.
   - Aucune correspondance → **nouveau matériel**.
3. **Attribution du numéro** — à l'intégration, chaque nouveau matériel reçoit un **numéro d'inventaire généré** : `CODEPOSTE-PRÉFIXE-SÉQUENCE` (ex. `DKR-PC-0042`). Le **préfixe** provient du **type de matériel** (référentiel paramétrable) : `PC`, `IMP`, `SW`, `AP`, `SCN`, `AUT` pour les types système, ou le préfixe saisi pour un type ajouté. Pour les types génériques (onglet *Autres*), la clé anti-doublon est la **MAC** portée par le matériel.

> La clé officielle reste le **numéro d'inventaire**. La MAC et le numéro de série ne servent que de garde-fou anti-doublon.

## 7. Consolidation des fichiers d'une même mission

Tous les lots portant le même N° de mission alimentent le **relevé unique** de la mission.

- **Rapprochement inter-fichiers** — par la même clé (numéro d'inventaire, sinon MAC / n° de série). Si deux agents ont saisi le même matériel, une seule entrée est retenue.
- **Conflit de valeurs** — si les deux saisies divergent (modèle, attributaire, logiciels…), l'application signale un **conflit** que le chef de mission arbitre.
- **Relevé réseau** — état du câblage et catégorie de câble sont retenus **une seule fois** pour la mission ; divergence entre fichiers = conflit signalé.
- **Traçabilité** — chaque ligne conserve son **agent saisisseur** et son **fichier source** (`releve_materiel.agent_saisisseur_matricule`, `source_fichier`).
- **Statut** — la mission reste *en consolidation* ; les agents peuvent charger leurs fichiers au fur et à mesure. Elle passe *clôturée* lorsque le chef valide la consolidation.

## 8. Revue et validation par le chef de mission

Avant intégration, le chef de mission dispose d'un **rapport de consolidation** présentant :

- les matériels **nouveaux**, **mis à jour**, et les lignes **en erreur** ;
- les **doublons** et **conflits** à arbitrer (avec la valeur de chaque agent) ;
- la **contribution de chaque agent** (qui a saisi quoi).

Ses actions : corriger ou exclure une ligne, **arbitrer un conflit** (choisir la bonne valeur), demander un rechargement, puis **valider**. La validation déclenche l'intégration.

## 9. Intégration en base (commit)

L'intégration s'effectue en **transaction** (tout ou rien sur le lot validé) :

- création ou mise à jour de `materiel` et de la table de sous-type correspondante ;
- attribution du **numéro d'inventaire** aux nouveaux matériels ;
- mise à jour des **affectations** : si l'attributaire ou le poste change, l'affectation en cours est **clôturée** (`date_fin`) et une **nouvelle** est ouverte (`affectation_materiel`) ;
- mise à jour des **logiciels installés** (`ordinateur_logiciel`) ;
- écriture des lignes `releve_materiel`, avec le **snapshot `etat_observe`** (photo des attributs observés) et la `date_releve` ;
- la photo datée est ainsi **figée** : elle ne change plus, même si la fiche matériel évolue ensuite.

**Idempotence** — recharger un fichier corrigé ne crée pas de doublon : le rapprochement par clé met à jour la même entrée.

## 10. Restitutions après import

- **Rapport d'import** : nombre de lignes lues, créées, mises à jour, en erreur, doublons/conflits.
- **Liste des nouveaux matériels avec leur numéro d'inventaire attribué** — à imprimer pour **étiqueter** physiquement les machines. À la mission suivante, l'agent lira l'étiquette et reportera le numéro.

## 11. Gestion des erreurs et rechargement

- Les lignes **valides** peuvent être intégrées même si d'autres sont en erreur ; les lignes **en erreur** sont listées avec leur motif pour correction.
- L'agent corrige son fichier et le **recharge** : grâce aux clés, les lignes déjà intégrées sont mises à jour, pas dupliquées.
- Un fichier entièrement rejeté (en-tête invalide, mauvais N° de mission) n'entre pas en consolidation.

## 12. Règles de gestion (récapitulatif)

| # | Règle |
|---|---|
| R1 | Un fichier se rattache à une mission par son **N° de mission**. |
| R2 | Seules les missions **en consolidation** acceptent des imports. |
| R3 | Clé d'identité du matériel = **numéro d'inventaire** ; à défaut, MAC ou n° de série (anti-doublon). |
| R4 | N° d'inventaire vide ⇒ **nouveau** matériel, numéro généré à l'intégration. |
| R5 | Le **chef de mission** valide ; rien n'est intégré sans sa validation. |
| R6 | Chaque ligne conserve son **agent saisisseur** et son **fichier source**. |
| R7 | Le **relevé réseau** est renseigné une seule fois par mission. |
| R8 | L'intégration **fige** une photo datée (`releve_materiel`) et historise les affectations. |

## 13. Cas limites à prévoir

- **Numéro d'inventaire saisi mais inexistant** → erreur (probable faute de frappe sur l'étiquette).
- **Même MAC sous deux numéros différents** → conflit d'identité à arbitrer.
- **Matériel attendu mais non relevé** lors d'une mission → non traité comme une suppression ; un retrait éventuel reste une action explicite.
- **Agent saisisseur absent du référentiel** → fichier rejeté (mettre l'agent à jour d'abord).
- **Deux fichiers, valeurs divergentes pour le même matériel** → conflit présenté au chef avec les deux versions.
