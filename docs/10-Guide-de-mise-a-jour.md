# 10 — Guide de mise à jour et de maintenance

Comment faire évoluer l'application en sécurité (nouvelle version, migration de schéma, dépendances).

## 1. Mettre à jour l'application en production

```bash
# 1. Sauvegarder AVANT toute mise à jour
./scripts/sauvegarde-postgres.sh            # (ou la tâche Windows / le timer systemd)

# 2. Récupérer et construire la nouvelle version
git pull
mvn -B clean package                        # build + tests (la CI doit être verte)

# 3. Déployer le nouveau JAR et redémarrer le service
#    Au démarrage, Flyway applique automatiquement les nouvelles migrations (V…),
#    puis l'application est disponible.
```

> **Migrations automatiques** : aucune commande SQL manuelle. Si une migration échoue, **l'application
> refuse de démarrer** — lire le message, corriger, redémarrer. C'est pourquoi on **sauvegarde avant**.

### Vérifications post-déploiement
- `GET /actuator/health` → `UP`.
- Les nouvelles migrations apparaissent dans `flyway_schema_history` (`success = true`).
- Smoke test (cf. `08 §8`) : login, une liste, une action.

### Revenir en arrière (rollback)
1. Redéployer le **JAR précédent**.
2. Si une migration a modifié le schéma de façon incompatible : **restaurer la sauvegarde** prise à
   l'étape 1 (`./scripts/restauration-postgres.sh <dump>`). Flyway ne défait pas une migration ; la
   sauvegarde est le filet de sécurité.

## 2. Ajouter une migration de schéma (Flyway)

Règle d'or : **une migration appliquée est immuable** — ne jamais la modifier ; en créer une nouvelle.

1. Créer `src/main/resources/db/migration/V<N+1>__description.sql` (numéro **strictement croissant** ;
   un « trou » est toléré, mais pas de doublon ni de retouche d'une version existante).
2. Écrire du SQL **idempotent** quand c'est pertinent (`ADD COLUMN IF NOT EXISTS`, `CREATE TABLE IF NOT EXISTS`)
   pour résister aux rejeux.
3. Mettre à jour l'entité JPA correspondante (`domain/`) et, si besoin, le dépôt / service / vue.
4. Tester : `mvn test` (le rejeu Flyway sur base vierge est couvert par `ImportIntegrationTest` en CI).
5. Mettre à jour la doc `04-Modele-de-donnees.md` (table des migrations).

> *Checksum mismatch* après retouche d'un commentaire/idempotence : exécuter une fois `flyway repair`
> sur l'environnement concerné pour réaligner l'historique (à éviter en modifiant le moins possible).

## 3. Ajouter une fonctionnalité (rappel des points d'accroche)

- **Nouvel écran** : contrôleur dans `web/`, service dans le package métier, template dans `templates/`
  (réutiliser `fragments/layout.html`), règle d'accès dans `config/SecurityConfig`, lien de menu dans le
  fragment de navigation.
- **Nouveau champ métier** : migration + entité + DTO de vue + formulaire + service. Respecter
  l'**historisation** (jamais d'écrasement) si la donnée est datée.
- **Action sensible** : la **tracer** via `AuditService` (connexion, clôture, réaffectation, comptes…).

## 4. Mettre à jour les dépendances

- **Version Spring Boot** : bumper `<parent>` dans `pom.xml`, relire les notes de version, lancer
  `mvn -U clean verify`. Surveiller : Spring Security (DSL), Flyway, dépréciations (`@MockBean`).
- **Apache POI** : version figée (`5.2.5`) — tester la génération/lecture du canevas après tout changement.
- **JDK** : cible 17 ; vérifier la contrainte **Mockito / classes concrètes** sur JDK récents (cf. `09 §5`).

## 5. Maintenance courante

| Tâche | Comment |
|---|---|
| Consulter qui a fait quoi | Écran `/journal` (ADMIN) ou table `audit_event` |
| Vérifier la santé | `GET /actuator/health` |
| Réinitialiser un mot de passe | Écran **Comptes** (mot de passe temporaire, changement forcé) |
| Vérifier les sauvegardes | Lister les dumps + lancer `test-restauration-postgres.sh` |
| Purge/rotation des sauvegardes | Automatique (`RETENTION_JOURS`, défaut 30) |

## 6. Dépannage rapide

| Symptôme | Cause probable | Action |
|---|---|---|
| Refus de démarrer, erreur Flyway | Migration en échec / schéma divergent | Lire le SQL ; corriger la migration ; restaurer si nécessaire |
| « colonne existe déjà » au rejeu | Schéma modifié hors Flyway | Migration idempotente (`IF NOT EXISTS`) ou `flyway repair` |
| Login refusé malgré bon mot de passe | Verrou anti-force-brute (5 échecs) | Attendre 15 min ou redémarrer (le verrou est en mémoire) |
| 403 inattendu | Règle de rôle dans `SecurityConfig` | Vérifier le `requestMatcher` et le rôle du compte |
