# 07 — Manuel utilisateur

## 1. Connexion et navigation

Se connecter avec son **identifiant** (matricule pour les comptes liés à un agent) et son mot de passe. Le **menu de gauche** donne accès aux écrans selon le rôle : Accueil, Postes, Agents, Parc, Missions, **Rapport annuel**, Importer, Guide, Référentiels, Comptes. La page courante est mise en évidence ; les actions réservées sont masquées. Quatre rôles : **Administrateur**, **Chef de mission**, **Agent**, et **Manager** (pilotage en **lecture seule** — voir §9).

## 2. Tableau de bord (Accueil)

Indicateurs clés (postes, matériel, **disponibilité**, postes en alerte, missions) et graphiques (matériel par statut, par type, par poste ; missions par état). Un **sélecteur de poste** recentre tout le tableau de bord sur un TPR. Deux exports : **Excel** (synthèse) et **PDF** (impression). La *disponibilité* = matériels « En service » rapportés au matériel **dont le statut est renseigné**.

## 3. Postes (TPR)

Liste triable et paginée. La **fiche d'un poste** montre le chef de poste, les agents rattachés et le matériel (avec affectataire et caractéristiques). L'administrateur peut créer/modifier un poste et **désigner son chef** (avec date d'effet, historisé).

## 4. Agents

Fiches séparées : **informaticiens** (effectuent les missions) et **agents de poste** (utilisent le matériel). Utiliser la **recherche** et, pour les agents de poste, les **boutons de TPR**. L'administrateur peut créer/modifier un agent et, depuis sa fiche, le **muter vers un autre TPR** (date d'effet, historisé) — le matériel n'est pas déplacé.

## 5. Parc

- **Rechercher** (n°, nom, modèle), **filtrer** par type/statut (pastilles) et par poste, **trier** par colonnes.
- Pour les ordinateurs : RAM, processeur, disque affichés.
- **Export Excel** de l'inventaire filtré, **étiquettes** imprimables, **inventaire à une date** passée.
- La **fiche d'un équipement** montre les caractéristiques, le statut, l'**affectation courante**, l'**historique des propriétaires** et les **relevés**. L'administrateur peut **réaffecter** l'équipement à un autre agent **du même TPR** (date d'effet).

## 6. Workflow d'une mission (de bout en bout)

1. **Créer la mission** (admin/chef) : TPR, objet, période, membres (informaticiens), chef de mission. Le **chef de poste est facultatif** (laissé vide s'il est inconnu, il sera renseigné via le canevas). Un même agent peut être membre de **missions aux périodes qui se chevauchent** (aucun blocage). L'application génère le **N° de mission**.
2. **Télécharger le canevas pré-rempli** et le distribuer aux agents.
3. **Saisir hors-ligne** dans Excel (un fichier par agent possible). Respecter les champs obligatoires et le format des **MAC** (`AA:BB:CC:DD:EE:FF`). Laisser le **n° d'inventaire vide** pour un matériel nouveau. Le **chef de poste** et les **dates de mission** renseignés dans le canevas sont **reportés à la mission** à l'import. Logiciels relevés sur les ordinateurs : Aster, Antivirus, SicCDD, CIC, Sysbudget, **AD** (colonnes Oui/Non).
4. **Recharger** chaque fichier (Importer) : l'application contrôle et affiche un **aperçu** (anomalies bloquantes à corriger, **avertissements** anti-doublon à vérifier).
5. **Consolidation** : le chef de mission rapproche les fichiers, **arbitre les conflits**, puis **valide l'intégration** (les n° sont attribués aux nouveaux matériels).
6. **Clôturer** la mission une fois la consolidation terminée.

## 7. Référentiels et comptes (administrateur)

- **Référentiels** : ajouter/renommer/supprimer logiciels et catégories de câble (suppression refusée si l'élément est utilisé).
- **Comptes** : créer, modifier, activer/désactiver, **réinitialiser** un mot de passe. Un compte de chef de mission ou d'agent est rattaché à un agent informaticien (identifiant = matricule). À la **première connexion** (compte initial) et **après une réinitialisation**, l'utilisateur est **obligé de changer son mot de passe** avant de continuer (page « Mot de passe »).

## 8. Bonnes pratiques

- **Étiqueter** physiquement les nouveaux matériels avec le n° d'inventaire attribué, pour les missions suivantes.
- Répartir la saisie d'une mission **par type de matériel ou par zone** pour éviter les doublons.
- Renseigner le **statut** du matériel (en service / en panne / à changer) pour fiabiliser la disponibilité.
- Changer le **mot de passe administrateur** par défaut dès la première connexion.

## 9. Rapport annuel et profil Manager (pilotage)

- **Profil Manager** : compte de **décision en lecture seule**. Il voit le tableau de bord, les postes,
  le parc, les missions, les agents et le **rapport annuel**, mais ne peut **rien modifier ni importer**.
  Création d'un compte Manager : *Comptes → Nouveau compte → rôle « Manager (pilotage) »* (administrateur).
- **Rapport annuel** (menu *Rapport annuel*, réservé Administrateur / Manager) : revue de l'année pour le
  pilotage.
  - Choisir l'**année** et la **fenêtre de comparaison** (2 à 5 ans) en haut de page.
  - **Synthèse** : missions, relevés, incidents, nouveau matériel, taille du parc et **disponibilité au
    31/12**, avec l'**écart par rapport à l'année précédente** et une **prévision** de l'année suivante.
  - **Tendance** : évolution sur jusqu'à 5 ans, avec une barre de **prévision** (extrapolation, indicative).
  - **Détails** : missions de l'année (par mois, par poste), parc au 31/12 (par statut / type / poste),
    incidents (équipements concernés), activité des agents.
  - **Exporter** : bouton **Excel** (classeur détaillé) ou **PDF** (impression).
