# 08 — Guide de déploiement (production)

Procédure de mise en production de l'application **Missions & Parc DGCPT** (intranet). Complète
`06-Guide-installation-exploitation.md` (qui détaille HTTPS, sauvegardes, supervision).

## 1. Pré-requis serveur

| Composant | Version | Remarque |
|---|---|---|
| JDK | 17+ | exécution du JAR (compatible JDK 21/25) |
| PostgreSQL | 12+ | base dédiée `missions_parc` |
| Maven | 3.9+ | **build uniquement** (poste de build/CI) |
| Reverse-proxy *(option)* | nginx / Apache | terminaison TLS recommandée en intranet |

Le schéma est créé **automatiquement par Flyway** au premier démarrage (`ddl-auto=none`).

## 2. Construire l'artefact

Sur le poste de build / la CI :

```bash
mvn -B clean package          # produit target/missions-parc-*.jar (tests inclus ; -DskipTests pour les ignorer)
```

Copier le JAR sur le serveur (ex. `/opt/dgcpt/app.jar` ou `C:\dgcpt\app.jar`).

## 3. Base de données

```sql
CREATE DATABASE missions_parc;
CREATE USER missions_parc WITH PASSWORD '<mot de passe fort>';
GRANT ALL PRIVILEGES ON DATABASE missions_parc TO missions_parc;
```

## 4. Configuration (profil `prod`)

Le profil `prod` (`application-prod.properties`) lit les **secrets via variables d'environnement** — ne
rien committer en clair :

```bash
export DB_URL=jdbc:postgresql://localhost:5432/missions_parc
export DB_USER=missions_parc
export DB_PASSWORD=<mot de passe fort>
java -jar app.jar --spring.profiles.active=prod
```

Au démarrage, Flyway applique `V1…V20`. **Si une migration échoue, l'application refuse de démarrer**
(sécurité) — corriger puis relancer.

## 5. HTTPS

Deux options (cf. `06 §5`) :
- **Reverse-proxy** (recommandé) : TLS au proxy, app en HTTP derrière ; le profil prod active déjà
  `server.forward-headers-strategy=framework`.
- **Keystore PKCS12** porté par l'application (`server.ssl.*`).

Dans les deux cas, activer le cookie sécurisé une fois l'accès en https :
`server.servlet.session.cookie.secure=true`.

> **Configuration complète et durcie** (Nginx **et** Apache, HTTPS forcé, TLS durci, en-têtes de
> sécurité, pare-feu, vérifications) : voir **[11 — Guide de déploiement sécurisé](11-Guide-de-deploiement-securise.md)**.

## 6. Exécuter comme service

- **Linux (systemd)** : unité `app.service` (`ExecStart=java -jar /opt/dgcpt/app.jar --spring.profiles.active=prod`,
  `Restart=always`, `EnvironmentFile=/etc/dgcpt/app.env`).
- **Windows** : exécuter le JAR au démarrage (tâche planifiée « au démarrage » ou service via NSSM).

## 7. Sauvegardes (à activer)

- **Linux** : `scripts/systemd/dgcpt-sauvegarde.{service,timer}` → `systemctl enable --now dgcpt-sauvegarde.timer`.
- **Windows** : `scripts/installer-sauvegarde-planifiee.ps1` (tâche quotidienne 01h00).
- **Copier les dumps hors-serveur** et lancer périodiquement le **test de restauration**
  (`scripts/test-restauration-postgres.sh`).

## 8. Recette de mise en service (smoke test)

| # | Vérification | Attendu |
|---|---|---|
| 1 | `GET /actuator/health` | `{"status":"UP"}` (avec PostgreSQL) |
| 2 | Page de login accessible en **https** | cadenas, pas d'alerte mixte |
| 3 | Connexion `admin/admin` | redirection vers le tableau de bord |
| 4 | **Changer** le mot de passe initial | imposé à la première connexion |
| 5 | Charger une page liste (Parc / Missions) | données + pagination OK |
| 6 | Lancer une sauvegarde test | dump produit, restaurable |

## 9. Checklist sécurité avant ouverture

- [ ] HTTPS effectif + cookie `Secure` activé.
- [ ] Compte initial `admin/admin` **changé** (ou désactivé après création d'un autre admin).
- [ ] `DB_PASSWORD` fort, jamais en clair dans un fichier versionné.
- [ ] Accès réseau **restreint** à l'intranet (pare-feu).
- [ ] Sauvegardes planifiées **actives** + copie hors-serveur.
- [ ] Journal d'audit consulté (`/journal`) ; supervision branchée sur `/actuator/health`.
