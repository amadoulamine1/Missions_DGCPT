# Confrontation cahier des charges ⇄ application réalisée

*État au 24/06/2026 — cahier de référence : **v9**. Écarts entre le cahier des charges et l'application livrée, vérifiés dans le code.*

## 1. Globalement conforme

Le cœur métier est couvert : postes (TPR) et **chefs de poste historisés** ; agents (informaticiens / agents de poste) ; missions (création, membres, chef de mission **parmi les membres**, contrôle de chevauchement, édition, statut temporel) ; matériel et ses types avec caractéristiques ; **mode hors-ligne** (canevas Excel pré-rempli, contrôles, aperçu, **validation**, intégration) ; **consolidation multi-fichiers et arbitrage des conflits** ; **historisations** (chef de poste ; affectation matériel **avec réaffectation et historique des propriétaires** ; **rattachement agent↔TPR avec mutation**) ; restitutions (résultats de mission, inventaire d'un poste, **inventaire à une date**) ; recherche / filtres / tri / pagination ; **exports** (parc, relevés, statistiques Excel + PDF) ; authentification et **3 rôles**.

## 2. Écarts fonctionnels (vs cahier)

### 2.1 Administration des référentiels — *manquant* (§3.8, §9.7)
Le cahier prévoit des référentiels **paramétrables / extensibles** gérés par l'admin : logiciels, catégories de câble, types de matériel. Les entités `Logiciel` et `CategorieCable` existent, mais **aucun écran de gestion** n'est fourni : impossible d'ajouter un logiciel ou une catégorie sans intervention technique. Les **types de matériel** sont une énumération Java figée.
→ *Impact* : moyen. *Proposition* : CRUD admin (liste + ajout/édition) pour logiciels et catégories de câble.

### 2.2 Alerte anti-doublon à l'aperçu — *partiel* (§4)
Le rapprochement par **MAC / n° de série déjà connus** se fait **silencieusement à l'intégration** (fusion dans la fiche existante). L'**alerte explicite au chef avant validation** (« ce matériel existe peut-être déjà ») n'est pas affichée : `ControleImport` n'interroge pas la base.
→ *Impact* : moyen. *Proposition* : avertissement (non bloquant) dans l'aperçu quand un MAC/série existe déjà sous un autre numéro.

### 2.3 Inventaire « exact » à une date — *différé* (§9.5)
L'inventaire à une date reconstitue **composition et localisation**, pas l'**état exact des attributs** (MAC, logiciels…) tels qu'ils étaient à cette date.
→ *Impact* : faible. *Proposition* : figer une photo `etat_observe` à chaque intégration.

## 3. Robustesse / mise en production

- **Pages d'erreur 403 / 404 / 500** absentes → page « whitelabel » par défaut.
- **CSRF désactivé** (intranet) → à réactiver pour une exposition réseau ; compte initial `admin / admin` à changer.
- **Politique de sauvegarde / sécurité** : document présent (`Politique-sauvegarde-securite.md`), **mise en œuvre opérationnelle** (HTTPS, sauvegardes PostgreSQL planifiées) à réaliser.
- **Tests** limités au pipeline d'import (`ImportIntegrationTest`) : rien sur consultation, sécurité, exports, réaffectation / mutation.

## 4. Incohérences mineures / cosmétique

- **Export Excel du parc** : ne contient pas RAM / processeur / disque, alors que le tableau les affiche désormais.
- **Cartes agents** : e-mail et téléphone **saisis** (formulaire + base) mais **non affichés** sur les fiches.
- **Clôture de mission** : possible en éditant le statut → *Clôturée*, sans bouton dédié « Clôturer ».
- **Rattachement créé hors formulaire** : un agent créé *via l'import* ou *via la désignation d'un chef* n'obtient sa ligne d'historique de rattachement qu'à sa première édition (les agents existants sont repris par le backfill de la migration V7).
- **Performance** : pagination et tri **en mémoire** — sans enjeu au volume cible (~millier d'équipements), à revoir si le parc grossit fortement.

## 5. Récemment résolus (pour mémoire)

- **Réaffectation du matériel** à un autre agent du **même TPR**, datée, avec **historique des propriétaires** (§3.7).
- **Mutation d'un agent** vers un autre TPR, datée, **historisée** — le matériel reste dans son poste (§3.2 / §3.7).

---

**Priorités suggérées :** 2.1 (référentiels) · 2.2 (alerte anti-doublon) · pages d'erreur (3).
