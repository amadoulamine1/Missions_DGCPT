# 01 — Note de cadrage

## 1. Contexte

La DGCPT est organisée en **postes régionaux (TPR)**. Chaque TPR possède un chef de poste, des agents et un parc de matériel informatique (ordinateurs, imprimantes, switchs, points d'accès, scanners de chèque). La **Direction Informatique** mène des **missions** sur ces postes pour relever l'état du réseau et inventorier le matériel. Les données étaient jusqu'ici dispersées ; l'inventaire du parc n'est pas consolidé.

## 2. Objectifs

- Centraliser la gestion des **postes, agents et matériel**.
- Créer et suivre les **missions** et leurs **relevés datés**.
- Permettre la **saisie hors-ligne** (canevas Excel) en cas de coupure réseau, puis l'import et la validation.
- Restituer : **résultats d'une mission**, **inventaire d'un poste**, **inventaire du parc à une date donnée**.
- Fournir un **tableau de bord** de pilotage et des **exports**.

## 3. Périmètre

**Inclus :** postes/TPR, agents (informaticiens et agents de poste), parc et caractéristiques, affectations historisées, missions (cycle de vie complet), canevas hors-ligne + consolidation multi-fichiers + arbitrage des conflits, restitutions et inventaire à une date, tableau de bord et exports, référentiels (logiciels, catégories de câble), comptes et rôles.

**Exclus (à ce stade) :** gestion budgétaire/achats, supervision réseau temps réel, mobilité native, interfaçage avec d'autres SI.

## 4. Acteurs et rôles

| Rôle | Description |
|---|---|
| **Administrateur** | Gère postes, agents, comptes et référentiels ; accès complet. |
| **Chef de mission** | Crée et édite ses missions, valide les imports, clôture. |
| **Agent** | Consulte, télécharge les canevas, téléverse les fichiers remplis. |

Distinction métier : **agent informaticien** (Direction Informatique, effectue les missions) vs **agent de poste** (rattaché à un TPR, utilise le matériel).

## 5. Contraintes

- **Réseau intranet** avec coupures possibles → mode hors-ligne natif (canevas Excel) obligatoire.
- **Base PostgreSQL** (gratuit, adapté à l'historisation et aux requêtes à une date).
- **Réalisation et maintenance internes** (Direction Informatique) → stack standard Java/Spring.
- **Historisation** systématique (chef de poste, affectation de matériel, rattachement d'agent) ; on n'écrase jamais l'historique.
- **Photos datées** : chaque relevé de mission est figé à sa date.

## 6. Livrables

1. Application web déployable (intranet) + base PostgreSQL versionnée (Flyway).
2. Canevas Excel de saisie hors-ligne.
3. Dossier de projet (le présent dossier `docs/`).
4. Scripts d'exploitation (sauvegarde/restauration) et profil de production.

## 7. Volumétrie cible

~20 TPR ; ~2 000 agents (dont ~700 en poste) ; parc de l'ordre du **millier d'équipements**. Volume modéré : la stack retenue est largement dimensionnée.

## 8. Macro-planning (phases)

1. **Cadrage** — besoin, périmètre, modèle métier.
2. **Conception** — spécifications fonctionnelles + architecture + modèle de données.
3. **Réalisation** — itérative, par domaine (postes, agents, parc, missions, import, restitutions, sécurité).
4. **Tests & recette** — unitaires, intégration, recette fonctionnelle.
5. **Déploiement** — installation intranet, sauvegardes, HTTPS.
6. **Exploitation & évolutions**.

## 9. Risques et mitigations

| Risque | Impact | Mitigation |
|---|---|---|
| Coupures réseau pendant les missions | Saisie impossible | Mode hors-ligne (canevas) + consolidation à l'import |
| Doublons de matériel (saisies multiples) | Données incohérentes | Clé n° d'inventaire + alerte MAC/série déjà connu + arbitrage des conflits |
| Perte de l'historique sur changements | Traçabilité perdue | Tables historisées (périodes datées) |
| Perte du serveur | Perte de données | Sauvegardes PostgreSQL planifiées + copie hors-site |
| Exposition réseau | Sécurité | Authentification + rôles, CSRF, HTTPS en production |

## 10. Critères de succès

- Un inventaire **consolidé et fiable** du parc, reconstituable à n'importe quelle date.
- Des missions **traçables** de bout en bout (création → saisie → validation → clôture).
- Une **adoption** facilitée par le mode hors-ligne et des écrans simples.
