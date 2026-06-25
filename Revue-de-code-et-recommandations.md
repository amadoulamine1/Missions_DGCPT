# Revue de code & recommandations

Bilan de la revue du code et du projet, et suivi des actions par priorité.
Les priorités suivent la convention de la revue : **P1** = impact direct (performance / exactitude),
**P2** = sécurité, **P3** = qualité / dette maîtrisée.

---

## P1 — Performance & exactitude (fait)

| Item | État | Commit |
|------|------|--------|
| N+1 sur la liste du parc (un `findById` par ligne) → chargement par lot (`findAll` + `Map`) | ✅ Corrigé | `ad4ffcf` |
| Double parsing du canevas Excel à l'intégration → parse unique réutilisé pour conflits + intégration | ✅ Corrigé | `ad4ffcf` |

---

## P2 — Sécurité (fait)

| Item | État | Commit |
|------|------|--------|
| Forçage du changement de mot de passe à la première connexion (drapeau `mot_de_passe_a_changer`, V13, intercepteur, page self-service) | ✅ Implémenté | `cae93a1` |
| Réinitialisation par un administrateur repositionne le drapeau (l'utilisateur devra changer son mot de passe) | ✅ Implémenté | `cae93a1` |
| **Bean Validation** déclarative sur `PosteForm` et `AgentForm` (`@NotBlank`, `@Size`, `@Email`) ; contrôleurs `@Valid` + `BindingResult` ; erreurs affichées par champ (`th:errors`, `.field-err`) ; test `FormValidationTest` | ✅ Implémenté | — |
| **Création à la volée à l'import journalisée** (WARN « Import (création à la volée) ») pour poste / agent / agent de poste / catégorie de câble inconnus — audit a posteriori par un administrateur | ✅ Implémenté | — |

> Périmètre Bean Validation : les formulaires à logique inter-champs / conditionnelle
> (`UtilisateurForm` — compte lié à un agent vs compte système, mot de passe obligatoire à la création
> seulement ; formulaires mission — chef « existant/nouveau », liste des membres) conservent volontairement
> leur validation métier dans les services. La validation déclarative couvre la présence et le format des
> champs inconditionnels ; les règles métier et d'unicité (clés en base) restent au service (défense en profondeur).

---

## P3 — Qualité / dette maîtrisée

### Fait

- **Documentation des redondances de modèle (JavaDoc).** Les deux duplications volontaires du modèle sont
  désormais documentées au niveau de la classe, avec leur invariant :
  - `Materiel.type` (famille technique, enum, pilote les `switch` câblés) ↔ `Materiel.categorie`
    (type paramétrable : libellé + préfixe). Invariant `categorie.famille == type`, garanti à l'intégration.
  - `ReleveMateriel.etatObserve` / `statutObserve` / `agentTraitant` : instantanés (snapshots) figés de
    l'état du matériel à la date du relevé, jamais réécrits — duplication assumée pour l'historisation (§9.5).
- **Intégration continue (CI).** Workflow GitHub Actions `.github/workflows/ci.yml` : build + suite de tests
  (`mvn -B -ntp verify`) sur `ubuntu-latest` à chaque push / pull request sur `main`. Les runners disposant de
  Docker, **Testcontainers exécute réellement `ImportIntegrationTest`** (chaîne import → intégration + rejeu
  Flyway sur base `V1`…`V13` vierge) au lieu de l'ignorer comme sur un poste sans Docker. Verrou contre les
  régressions de migration / SQL natif avant la production.
- **Nettoyage des styles inline (partiel).** Les motifs répétés ont été factorisés en classes utilitaires
  d'`app.css` (`.flush`, `.inline-form`, `.toolbar.mt-1`, `.m-0`, `.mt-sm`, `.text-center`) et appliqués aux
  gabarits concernés. Les `style="…"` restants sont soit ponctuels (un seul usage), soit **dynamiques**
  (`th:style` calculé — jauges du tableau de bord), et restent volontairement inline.

### Préconisations P3 restantes (non implémentées)

- **Renforcer l'idempotence Flyway en local.** À défaut de Docker en poste de dev, vérifier régulièrement à
  la main que `mvn spring-boot:run` sur une base vide applique toute la chaîne sans erreur (régressions
  « colonne déjà existante » / « type incompatible » déjà rencontrées en V8/V10). En CI, c'est désormais
  couvert automatiquement par `ImportIntegrationTest`.

---

## Contraintes d'environnement à garder en tête

- **JDK 25 / ByteBuddy** : Mockito ne peut pas mocker les classes concrètes (mock inline). Convention du
  projet : ne mocker que des interfaces (dépôts) ou utiliser des doublures manuelles (sous-classes) ; exclure
  les beans de configuration concrets des slices `@WebMvcTest` (cf. `SecuriteAccesParRoleTest` excluant `WebConfig`).
- **Référence des tests** : voir `docs/05-Plan-de-tests-et-cahier-de-recette.md`.
