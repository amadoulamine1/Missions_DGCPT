# Plan d'amélioration du design — Missions & Parc DGCPT

*Document de travail. Base : `src/main/resources/static/css/app.css` (système de design existant) et les
templates Thymeleaf. Objectif : passer d'un rendu « propre » à un rendu « distinctif », sans casser
l'identité institutionnelle (or DGCPT) ni la stack (Thymeleaf + CSS, sans framework JS lourd).*

## 1. État des lieux

Le design est déjà solide : design tokens (`:root`), palette or maîtrisée, sidebar en dégradé,
composants métier (KPI, barres, pastilles de statut, cartes agents à avatars, glyphes typés par
matériel), responsive (sidebar → barre horizontale < 860 px) et impression (sidebar masquée).

**Faiblesses identifiées :** contraste insuffisant de certains éléments (accessibilité), nombreux
styles **inline** dans les templates, échelle de rayons/ombres/typo peu harmonisée, identité
typographique limitée, densité parfois fatigante, tableaux denses à l'étroit sur grand écran.

## 2. Priorités

### P1 — Accessibilité & corrections rapides *(fort impact, faible coût)*

- **Contraste des en-têtes de tableau** : texte blanc sur or `#B8860B` ≈ **3.3:1** (sous WCAG AA 4.5:1
  pour le petit texte `.74rem`). → Passer le fond d'en-tête sur `--gold-dark` (#705600 ≈ **6.9:1**), ou
  agrandir/épaissir le texte. Fichier : `app.css` (`.table th`).
- **Audit de contraste systématique** : `--muted` (#6b7280) sur `--bg` (~4.3:1) est limite ; vérifier
  badges, chips actives, liens dorés. Viser AA partout (4.5:1 texte normal, 3:1 large/UI).
- **États de focus** : confirmer un focus visible sur **tous** les éléments interactifs (liens de menu,
  chips, boutons, lignes de tableau cliquables), pas seulement les inputs.
- **Sémantique** : `aria-current="page"` sur le lien de menu actif ; `scope="col"` sur les `th` ;
  `aria-label` sur les boutons icônes.

### P2 — Maintenabilité & cohérence *(impact structurel)*

- **Extraire les styles inline → classes utilitaires.** Beaucoup de `style="..."` dans
  `referentiels.html`, `parc.html`, `agents.html`, etc. Créer un petit jeu d'utilitaires
  (`.input`, `.input-sm`, `.field-prefixe`, `.row`, `.gap-sm`, `.muted-xs`…) et remplacer les inline.
  Bénéfice : cohérence visuelle + maintenance + diffs lisibles.
- **Échelle de rayons** : harmoniser (`--r-sm:6px`, `--r:8px`, `--r-pill:999px`) — aujourd'hui 6/7/8/10/20.
- **Échelle d'élévation** : 2–3 niveaux d'ombre tokenisés (`--e1`, `--e2`) au lieu d'ombres ad hoc.
- **Échelle typographique** : tokens de taille (`--fs-xs`…`--fs-xl`) et de graisse, appliqués
  uniformément (tableaux, cartes, formulaires).

### P3 — Identité & raffinement *(valeur perçue)*

- **Typographie de titre distinctive** : une police de titre (ex. une sans-serif marquée, ou une serif
  institutionnelle) pour les `h1`/`brand`, le corps restant en police système. Charger en local
  (pas de CDN, intranet).
- **Confort de lecture** : remonter d'un cran le corps de texte (tableaux `.92rem`→`.95rem`, en-têtes
  `.74rem`→`.8rem`) et les hauteurs de ligne sur les pages denses.
- **Mode large pour les tableaux denses** : le **Parc** (≈9 colonnes) est contraint par
  `max-width:1080px`. Prévoir une largeur étendue (ou pleine largeur) pour les pages « tableau ».
- **Graphiques du tableau de bord** : raffiner les barres (axes/légendes plus nets, libellés,
  arrondis cohérents) tout en restant en CSS, sans dépendance JS.
- **Micro-interactions & états** : uniformiser hovers/transitions ; soigner les états **vides**
  (`.empty-box`) et de **chargement** ; cohérence des pastilles et icônes.

## 3. Pages de référence (ordre suggéré)

1. **Tableau de bord** (accueil) — vitrine : KPI, graphiques, cartes. Plus fort effet « avant/après ».
2. **Parc** — page la plus dense : valide l'échelle typo, le mode large, les filtres/chips.
3. **Fiche poste / fiche équipement** — mise en page des caractéristiques et historiques.
4. **Référentiels / formulaires** — cible principale du nettoyage des styles inline.

Méthode : pour chaque page, capture **avant**, refonte ciblée (avec le pluge `frontend-design`),
capture **après**, puis remontée des motifs réutilisables dans `app.css`.

## 4. Travaux transverses (à faire une fois)

- Centraliser les **tokens** (couleurs déjà faites ; ajouter rayons, ombres, typo, espacement).
- Créer une mini **feuille d'utilitaires** pour éliminer les inline.
- Passe **accessibilité** globale (contraste, focus, ARIA, ordre de tabulation).
- Vérifier **impression** et **responsive** après chaque lot (déjà bien gérés, ne pas régresser).

## 5. Vérification

- **Visuel** : lancer `mvn spring-boot:run`, comparer chaque page avant/après (desktop + < 860 px + impression).
- **Accessibilité** : contrôler les contrastes clés (en-têtes, muted, badges, chips) ≥ AA ; navigation
  clavier complète ; lecteur d'écran sur le menu et les tableaux.
- **Non-régression** : la suite de tests reste verte (le design ne touche pas la logique) ;
  vérifier que les pages se rendent sans erreur Thymeleaf.

## 6. Hors-scope

- Pas de framework front (React/Vue) ni de dépendance JS lourde : on reste en Thymeleaf + CSS.
- Pas de refonte fonctionnelle (les écrans et parcours restent identiques).
- Mode sombre : possible plus tard via les tokens, non prioritaire.

---

**Lots livrables proposés**
1. *Lot A (P1)* — accessibilité & quick wins sur `app.css` (contraste en-têtes, focus, ARIA). **Faible risque.**
2. *Lot B (P2)* — tokens (rayons/ombres/typo) + utilitaires + nettoyage des styles inline page par page.
3. *Lot C (P3)* — typographie de titre, confort de lecture, mode large tableaux, raffinement des graphiques.
