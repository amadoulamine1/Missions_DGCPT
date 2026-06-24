# Politique de sauvegarde et de sécurité — recommandations

*Application de gestion des missions et du parc informatique (DGCPT). Document opérationnel, à adapter aux règles internes de la Direction Informatique.*

## 1. Sauvegarde des données

- **Base PostgreSQL** : sauvegarde quotidienne par `pg_dump` (format `custom`), conservée hors du serveur applicatif.
  - Exemple : `pg_dump -Fc -d missions_parc -f /backups/missions_parc_$(date +%F).dump`
  - **Rétention** : 7 sauvegardes quotidiennes + 4 hebdomadaires + 12 mensuelles (à ajuster).
  - **Test de restauration** périodique (au moins trimestriel) sur un environnement séparé — une sauvegarde non testée n'est pas une sauvegarde.
- **Secrets et configuration** : sauvegarder `application-local.properties` (identifiants de base) dans un coffre/sauvegarde chiffrée, **séparément** des dumps.
- **Fichiers canevas** téléversés : si conservés sur disque, les inclure dans la sauvegarde ; sinon, la base suffit (les données sont intégrées).
- **Externalisation** : copier les sauvegardes sur un second support/site (règle 3-2-1 : 3 copies, 2 supports, 1 hors site).

## 2. Sécurité applicative

- **Changer immédiatement** le mot de passe du compte `admin` initial (`admin` / `admin`).
- **Réactiver la protection CSRF** avant toute exposition au-delà d'un intranet de confiance (actuellement désactivée pour simplifier l'intranet).
- **HTTPS/TLS** : placer l'application derrière un reverse proxy (Nginx/Apache) terminant le TLS ; ne pas exposer le port applicatif en clair.
- **Mots de passe** : politique de complexité minimale, changement à la première connexion (mots de passe temporaires de réinitialisation), et rotation périodique pour les comptes sensibles.
- **Moindre privilège** : n'attribuer le rôle *Administrateur* qu'au strict nécessaire ; les chefs de mission et agents reçoivent leur rôle propre.
- **Cycle de vie des comptes** : désactiver (plutôt que supprimer) les comptes des agents qui quittent leurs fonctions ; revue périodique des comptes actifs.

## 3. Sécurité de l'infrastructure

- **Réseau** : maintenir l'application sur l'**intranet** ; restreindre l'accès par pare-feu aux seuls postes/sous-réseaux autorisés.
- **Base de données** : compte applicatif dédié avec droits limités à la base `missions_parc` ; pas d'accès distant non chiffré.
- **Mises à jour** : appliquer régulièrement les correctifs de PostgreSQL, de la JVM et des dépendances de l'application.
- **Sauvegarde du serveur** : image/snapshot de la machine hôte en complément des dumps de base.

## 4. Journalisation et traçabilité

- L'application **trace déjà** l'agent saisisseur et le fichier source de chaque ligne d'inventaire, ainsi que l'historique des affectations et des chefs de poste.
- Recommandé en complément : journaliser les **connexions**, les **validations d'import** et les **modifications de comptes** (qui, quoi, quand) pour l'audit.

## 5. Continuité

- Documenter la **procédure de restauration** (base + configuration) et le **temps de reprise** visé.
- Conserver une **copie de ce dépôt de code** (source) sauvegardée, afin de pouvoir reconstruire l'application.
