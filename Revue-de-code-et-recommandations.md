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

### Préconisations P2 restantes (non implémentées)

- **Bean Validation** sur les paramètres/DTO des formulaires (`@NotBlank`, `@Size`, `@Email`…) plutôt que des
  contrôles manuels dispersés dans les services. Gain : messages d'erreur homogènes et validation déclarative.
- **Durcir la création à la volée à l'import.** Aujourd'hui certains référentiels peuvent être créés
  implicitement lors d'une intégration ; tracer ces créations (journal) et/ou les soumettre à validation
  d'un administrateur évite l'introduction silencieuse de données de mauvaise qualité.

---

## P3 — Qualité / dette maîtrisée

### Fait

- **Documentation des redondances de modèle (JavaDoc).** Les deux duplications volontaires du modèle sont
  désormais documentées au niveau de la classe, avec leur invariant :
  - `Materiel.type` (famille technique, enum, pilote les `switch` câblés) ↔ `Materiel.categorie`
    (type paramétrable : libellé + préfixe). Invariant `categorie.famille == type`, garanti à l'intégration.
  - `ReleveMateriel.etatObserve` / `statutObserve` / `agentTraitant` : instantanés (snapshots) figés de
    l'état du matériel à la date du relevé, jamais réécrits — duplication assumée pour l'historisation (§9.5).

### Préconisations P3 restantes (non implémentées)

- **Intégration continue (CI) exécutant le test d'intégration existant.** `ImportIntegrationTest`
  (Testcontainers, `@SpringBootTest`) couvre déjà la chaîne import → intégration sur une vraie PostgreSQL
  **et**, de fait, le rejeu Flyway sur base vierge (le conteneur neuf applique `V1`…`V13` au démarrage : une
  migration cassée fait échouer le test). Mais il est **ignoré automatiquement quand Docker n'est pas
  disponible** (`@EnabledIf("dockerDisponible")`) — c'est le cas en poste de dev sans Docker Desktop. Le
  manque réel est une CI avec service Docker qui l'exécute systématiquement, afin qu'il cesse d'être ignoré
  et garde les régressions de migration / SQL natif hors production. Les tests unitaires restent mockés
  (contrainte JDK 25 : ByteBuddy n'instrumente pas les classes concrètes), d'où l'intérêt de ce test réel.
- **Renforcer l'idempotence Flyway en local.** À défaut de Docker, vérifier régulièrement à la main que
  `mvn spring-boot:run` sur une base vide applique toute la chaîne sans erreur (régressions « colonne déjà
  existante » / « type incompatible » déjà rencontrées en V8/V10).
- **Nettoyage des styles inline.** Plusieurs gabarits Thymeleaf portent encore des attributs `style="…"`
  ponctuels (ex. largeurs, marges). Les migrer vers des classes utilitaires d'`app.css` améliore la cohérence
  visuelle et la maintenabilité (point cosmétique, sans urgence).

---

## Contraintes d'environnement à garder en tête

- **JDK 25 / ByteBuddy** : Mockito ne peut pas mocker les classes concrètes (mock inline). Convention du
  projet : ne mocker que des interfaces (dépôts) ou utiliser des doublures manuelles (sous-classes) ; exclure
  les beans de configuration concrets des slices `@WebMvcTest` (cf. `SecuriteAccesParRoleTest` excluant `WebConfig`).
- **Référence des tests** : voir `docs/05-Plan-de-tests-et-cahier-de-recette.md`.
