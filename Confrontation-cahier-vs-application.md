# Confrontation cahier des charges ⇄ application réalisée

*État au 24/06/2026 — cahier de référence : **v9**. Écarts entre le cahier des charges et l'application livrée, vérifiés dans le code.*

## 1. Globalement conforme

Le cœur métier est couvert : postes (TPR) et **chefs de poste historisés** ; agents (informaticiens / agents de poste) ; missions (création, membres, chef de mission **parmi les membres**, contrôle de chevauchement, édition, statut temporel) ; matériel et ses types avec caractéristiques ; **mode hors-ligne** (canevas Excel pré-rempli, contrôles, aperçu, **validation**, intégration) ; **consolidation multi-fichiers et arbitrage des conflits** ; **historisations** (chef de poste ; affectation matériel **avec réaffectation et historique des propriétaires** ; **rattachement agent↔TPR avec mutation**) ; restitutions (résultats de mission, inventaire d'un poste, **inventaire à une date**) ; recherche / filtres / tri / pagination ; **tableau de bord filtrable par poste** ; **exports** (parc, relevés, statistiques Excel + PDF) ; **administration des référentiels** ; authentification et **3 rôles**.

## 2. Écarts fonctionnels restants (vs cahier)

### 2.1 Inventaire « exact » à une date — *différé* (§9.5)
L'inventaire à une date reconstitue **composition et localisation**, pas l'**état exact des attributs** (MAC, logiciels…) tels qu'ils étaient à cette date.
→ *Impact* : faible. *Proposition* : figer une photo `etat_observe` à chaque intégration.

## 3. Robustesse / mise en production

- **CSRF désactivé** (intranet) → à réactiver pour une exposition réseau ; compte initial `admin / admin` à changer.
- **Politique de sauvegarde / sécurité** : document présent (`Politique-sauvegarde-securite.md`), **mise en œuvre opérationnelle** (HTTPS, sauvegardes PostgreSQL planifiées) à réaliser.
- **Tests** limités au pipeline d'import (`ImportIntegrationTest`) : rien sur consultation, sécurité, exports, réaffectation / mutation / référentiels.

## 4. Incohérences mineures / cosmétique

- **Export Excel du parc** : ne contient pas RAM / processeur / disque, alors que le tableau les affiche.
- **Cartes agents** : e-mail et téléphone **saisis** (formulaire + base) mais **non affichés** sur les fiches.
- **Clôture de mission** : possible en éditant le statut → *Clôturée*, sans bouton dédié « Clôturer ».
- **Rattachement créé hors formulaire** : un agent créé *via l'import* ou *via la désignation d'un chef* n'obtient sa ligne d'historique de rattachement qu'à sa première édition (les agents existants sont repris par le backfill de la migration V7).
- **Performance** : pagination et tri **en mémoire** — sans enjeu au volume cible (~millier d'équipements).

## 5. Résolus

- **Administration des référentiels** (§3.8) — écran admin pour **logiciels** et **catégories de câble** : ajout, renommage, suppression (avec garde si l'élément est utilisé).
- **Alerte anti-doublon à l'aperçu** (§4) — une ligne **sans n° d'inventaire** dont la **MAC / le n° de série** est déjà connu en base est signalée (**AVERTISSEMENT**) au chef avant validation.
- **Pages d'erreur 403 / 404 / 500** (+ générique) avec menu et message clair, à la place de la page « whitelabel ».
- **Réaffectation du matériel** (même TPR, historique des propriétaires) et **mutation d'un agent** (agent↔TPR, historisée) — le matériel reste dans son poste (§3.2 / §3.7).
- **Tableau de bord filtrable par poste** ; **disponibilité** recalculée sur le matériel statué.

---

**Restant, par priorité :** réactivation CSRF + durcissement sécurité (prod) · tests automatisés · snapshot exact à une date · finitions (export specs, e-mail/téléphone sur les cartes, bouton « Clôturer »).
