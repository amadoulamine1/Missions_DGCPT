# 05 — Plan de tests et cahier de recette

## 1. Stratégie de test

| Niveau | Outils | Objet |
|---|---|---|
| **Unitaire** | JUnit 5, Mockito, AssertJ | Logique métier isolée (ex. agrégations statistiques, règle de réaffectation même-poste) |
| **Intégration** | Spring Boot Test, **Testcontainers** (PostgreSQL) | Pipeline d'import de bout en bout sur une vraie base |
| **Recette fonctionnelle** | Manuelle (ce cahier) | Validation des cas d'usage par rôle |
| **Sécurité** | Revue + tests d'accès | Règles de rôles, CSRF |

Exécution des tests automatisés : `mvn test` (Testcontainers nécessite Docker).

## 2. Environnement de recette

- Base PostgreSQL dédiée (vide), schéma appliqué par Flyway au démarrage.
- Compte administrateur initial `admin/admin` (à changer).
- Jeu de données minimal : 2 TPR, quelques agents (informaticiens et de poste), 1 mission.

## 3. Cahier de recette (cas de test)

Statuts attendus : **OK** si le résultat observé = attendu.

| ID | Domaine | Objectif | Étapes | Résultat attendu |
|---|---|---|---|---|
| R01 | Sécurité | Connexion | Se connecter avec `admin/admin` | Accès au tableau de bord ; menu admin complet |
| R02 | Sécurité | Accès refusé | En tant qu'AGENT, ouvrir `/utilisateurs` | Page **403** (accès refusé) |
| R03 | Sécurité | Déconnexion | Cliquer « Déconnexion » | Retour à l'écran de login |
| R04 | Postes | Créer un TPR | Postes → + Nouveau → saisir code/nom/région | TPR créé, visible et trié dans la liste |
| R05 | Postes | Chef de poste daté | Fiche TPR → désigner un chef avec date d'effet | Période précédente clôturée, nouvelle ouverte |
| R06 | Agents | Créer un agent de poste | Agents → + Nouvel agent (type POSTE, TPR) | Agent visible dans « Agents de poste » |
| R07 | Agents | Filtre TPR | Onglet « Agents de poste » → choisir un TPR | Liste filtrée sur ce TPR |
| R08 | Agents | Muter un agent | Fiche agent → Mutation vers un autre TPR + date | Historique des rattachements mis à jour ; matériel inchangé |
| R09 | Mission | Créer une mission | Nouvelle mission : TPR, objet, dates, membres, chef | N° `MIS-AAAA-NNN` généré ; canevas téléchargeable |
| R10 | Mission | Chevauchement | Ajouter un agent déjà membre d'une mission concomitante | Blocage + signalement de la mission en conflit |
| R11 | Hors-ligne | Canevas pré-rempli | Mission → Télécharger le canevas pré-rempli | Fichier `.xlsx` avec en-tête, membres, agents TPR, inventaire connu |
| R12 | Import | Contrôles | Charger un canevas avec un champ obligatoire vide / MAC invalide | Anomalies **bloquantes** signalées dans l'aperçu |
| R13 | Import | Anti-doublon | Charger une ligne sans n° mais avec une MAC déjà connue | **Avertissement** « ce matériel existe peut-être déjà » |
| R14 | Consolidation | Conflit | Charger deux fichiers divergents pour un même matériel | Conflit listé ; arbitrage par le chef puis intégration |
| R15 | Import | Intégration | Valider la consolidation | Matériels créés/mis à jour ; n° attribués aux nouveaux |
| R16 | Parc | Recherche/filtres | Parc → recherche + chips type/statut + poste | Liste filtrée, triable, paginée ; export Excel cohérent |
| R17 | Parc | Specs ordinateur | Filtrer type = Ordinateur | Colonnes RAM/Processeur/Disque renseignées |
| R18 | Parc | Réaffectation | Fiche équipement → réaffecter à un agent du **même** TPR | Période close/ouverte ; historique des propriétaires mis à jour |
| R19 | Parc | Réaffectation hors poste | Tenter via requête forgée un agent d'un autre TPR | Refus serveur (« même poste (TPR) ») |
| R20 | Restitution | Inventaire à une date | Saisir une date passée | Composition/localisation + **état observé** à la date |
| R21 | Mission | Clôture | Fiche mission → Clôturer | Statut « Clôturée » ; bouton masqué ensuite |
| R22 | Tableau de bord | Filtre par poste | Accueil → choisir un TPR | Indicateurs et graphiques recalculés pour ce TPR |
| R23 | Tableau de bord | Exports | Export Excel / Export PDF | Classeur de synthèse / impression PDF |
| R24 | Référentiels | Ajout/suppression | Référentiels → ajouter un logiciel, supprimer | Ajout OK ; suppression refusée si utilisé |
| R25 | Comptes | Réinitialiser MdP | Comptes → réinitialiser un mot de passe | Mot de passe temporaire fixé |
| R26 | Robustesse | Page 404 | Ouvrir une URL inexistante | Page **404** habillée + lien retour |

## 4. Critères d'acceptation

- 100 % des cas **bloquants** (R01–R03, R09–R15, R18–R19, R21) en statut **OK**.
- Aucune régression sur les restitutions (R16, R20, R22).
- Aucune anomalie de sécurité (accès non autorisés).

## 5. Procès-verbal de recette (gabarit)

```
PV de recette — Application Missions & Parc DGCPT
Version testée : ______   Date : ______   Environnement : ______
Testeur(s) : __________________________________

Cas exécutés : __ / 26      Réussis : __      Échoués : __
Anomalies ouvertes (id, sévérité, description) :
  - ____________________________________________
Décision : [ ] Recette prononcée   [ ] Recette sous réserves   [ ] Refusée
Réserves / actions correctives : ______________
Signatures : Maîtrise d'ouvrage ______   Maîtrise d'œuvre ______
```
