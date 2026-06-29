# Guide de remplissage du canevas Excel (mission d'inventaire)

Ce guide explique comment renseigner le **fichier canevas** (`.xlsx`) téléchargé pour une mission, avant
de le **recharger** dans l'application pour consolidation et intégration au parc.

> **Règle d'or** : les champs suivis d'un **astérisque (`*`)** sont **obligatoires**. Une ligne devient
> « saisie » dès que **n'importe quelle** cellule y est renseignée — y compris une case **Oui/Non** comme
> **AD** : les obligatoires encore vides apparaissent alors en **rouge** (mise en forme conditionnelle) et
> **bloquent** le chargement tant qu'ils ne sont pas complétés. Un format d'adresse **MAC** invalide
> apparaît en **orange**. Tant qu'une anomalie **bloquante** subsiste, **le chargement ne passe pas**.

---

## 1. Obtenir le canevas

1. Ouvrir la **mission** (menu *Missions* → la mission concernée) puis **Télécharger le canevas**.
2. L'application génère **un fichier par agent membre**, regroupés dans un **ZIP** nommé par
   **code poste + période** (ex. `Canevas-DKR-2026-06-01_2026-06-05.zip`). Chaque fichier reprend ce nom
   suivi du matricule et du nom de l'agent.
3. Chaque agent remplit **son** fichier (matricule déjà pré-rempli comme *agent saisisseur*).

Le matériel déjà connu du poste est **pré-chargé** : il faut le **vérifier** et le **compléter**.

---

## 2. Onglets du canevas

| Onglet | Contenu |
|---|---|
| **1-Mission et Réseau** | En-tête de la mission + relevé réseau (à remplir **une fois**) |
| **Agents TPR** | Agents du poste (attributaires possibles) — pré-chargés, complétables |
| **2-Membres mission** | Membres informaticiens de la mission (informatif) |
| **3-Ordinateurs** | Postes de travail / unités centrales |
| **4-Imprimantes** | Imprimantes |
| **5-Switchs et AP** | Switchs et points d'accès |
| **6-Scanners chèque** | Scanners de chèque |
| **7-Autres matériels** | Types de matériel paramétrables (onduleurs, etc.) |
| **Referentiels** | Listes internes (ne pas modifier) |

---

## 3. Onglet « 1-Mission et Réseau »

En-tête de la mission. Champs **obligatoires** (`*`) à vérifier / compléter :

- **N° de mission \*** — pré-rempli, ne pas modifier.
- **Code poste \*** — pré-rempli.
- **Objet de la mission \***.
- **Date de début / Date de fin** — si elles n'étaient pas connues à la création, les renseigner ici
  (format `JJ/MM/AAAA`) ; elles seront **reportées à la mission** à l'import.
- **Chef de mission \*** — informaticien (pré-rempli).
- **Chef de poste \*** — **OBLIGATOIRE**. S'il était inconnu à la création de la mission, **le renseigner
  ici** (agent de poste). **Sans chef de poste, le chargement est refusé.**
- **Agent saisisseur \*** — celui qui remplit *ce* fichier (pré-rempli).
- **État du câblage réseau \*** — liste : **Neuf**, **Bon** ou **Pas bon**.
- **Catégorie de câble \***.

Le chef de poste et les dates renseignés ici sont **reportés à la mission** lors de l'intégration.

---

## 4. Onglets matériel (3 à 6 et « 7-Autres »)

### Règles communes

- **N° d'inventaire** : colonne **verrouillée** (lecture seule). Pour un **matériel nouveau**, **laisser
  vide** : l'application lui attribuera un numéro à l'intégration. Pour un matériel existant, il est déjà
  pré-rempli (ne pas le modifier).
- **Statut \*** — **obligatoire** : **En service**, **En panne** ou **À changer** (liste déroulante).
- **Adresses MAC** : format `AA:BB:CC:DD:EE:FF` (deux caractères hexadécimaux séparés par `:`).
  Une MAC mal formée est signalée en **orange**.
- Utiliser les **menus déroulants** proposés (agents, statut, type, Oui/Non) plutôt que la saisie libre.

### 3-Ordinateurs

- **Nom machine \***, **Agent attributaire \*** (agent de poste), **Agent traitant \*** (informaticien).
- **MAC ethernet \*** obligatoire ; **MAC wifi** facultative (contrôlée si présente).
- **Logiciels** (cases **Oui/Non**) : **Aster, Antivirus, SicCDD, CIC, Sysbudget, AD**.
- **Agent traitant** : à **ressaisir à chaque mission** (il n'est pas reporté de la précédente, car
  historisé par mission).

### 4-Imprimantes

- **Nom \***, **MAC \*** obligatoire, **MAC wifi** et **IP** facultatives.

### 5-Switchs et AP

- **Type \*** (switch / point d'accès), **Nom \***, **MAC \*** obligatoire, **IP** facultative.

### 6-Scanners chèque

- **Numéro de série \*** obligatoire (pas de MAC), **marque**.

### 7-Autres matériels (types paramétrables)

- **Type \*** : à choisir dans la **liste déroulante** (le type doit exister dans les *Référentiels* ;
  sinon, le faire créer par l'administrateur avant l'import).
- **Nom \*** et **Statut \*** obligatoires ; **MAC** contrôlée si présente.

---

## 5. Plusieurs agents sur une même mission

Chaque agent remplit **son propre fichier** ; l'application les **consolide** grâce au N° de mission
commun. Répartir le travail par **type de matériel** ou par **zone** pour éviter les doublons. En cas de
valeurs divergentes sur un même matériel, le **chef de mission arbitre les conflits** avant l'intégration.

---

## 6. Recharger et intégrer

1. **Importer** le fichier rempli (menu *Importer*).
2. L'application affiche un **aperçu** avec les **contrôles** :
   - **Anomalies bloquantes** (rouge) : champ obligatoire manquant (dont **chef de poste**, **statut**,
     **état du câblage**, **catégorie de câble**), MAC invalide, type inconnu… → **à corriger** dans le
     canevas, puis recharger. **Tant qu'il en reste, l'ajout à la consolidation est refusé.**
   - **Avertissements** (anti-doublon) : ligne sans n° d'inventaire mais MAC / n° de série déjà connu →
     **à vérifier** avant de valider.
3. Le **chef de mission** rapproche les fichiers, **arbitre** les conflits éventuels, puis **valide
   l'intégration** : les fiches matériel sont créées/mises à jour, les **n° d'inventaire attribués** aux
   nouveaux, l'état du parc figé (photo datée).

---

## 7. Mémo des contrôles bloquants

| Champ | Onglet | Règle |
|---|---|---|
| N° de mission, Code poste, Objet | 1-Mission et Réseau | Obligatoires |
| **Chef de mission**, **Chef de poste**, **Agent saisisseur** | 1-Mission et Réseau | **Obligatoires** |
| **État du câblage**, **Catégorie de câble** | 1-Mission et Réseau | Obligatoires (câblage ∈ Neuf/Bon/Pas bon) |
| **Statut** du matériel | tous onglets matériel | Obligatoire (En service / En panne / À changer) |
| **MAC** | ordinateurs/imprimantes/switchs | Obligatoire et format `AA:BB:CC:DD:EE:FF` |
| **Numéro de série** | scanners | Obligatoire |
| **Type** | 7-Autres matériels | Doit exister dans les référentiels |
