# 08 — Guide de déploiement (production)

Procédure complète de mise en production de l'application **Missions & Parc DGCPT** (intranet) :
build, base de données, configuration, **HTTPS avec ou sans reverse-proxy** (Nginx/Apache **durci**),
service, sauvegardes, recette et checklist de sécurité.

> Ce guide fusionne l'ancien « guide de déploiement » et le « guide de déploiement sécurisé ».
> Pour l'exploitation courante (supervision, journaux, restauration), voir
> `06-Guide-installation-exploitation.md`.

---

## 1. Pré-requis serveur

| Composant | Version | Remarque |
|---|---|---|
| JDK | 17+ | exécution du JAR (compatible JDK 21/25) |
| PostgreSQL | 12+ | base dédiée `missions_parc` |
| Maven | 3.9+ | **build uniquement** (poste de build/CI) |
| Reverse-proxy *(option)* | nginx / Apache | terminaison TLS — voir **Mode B** (§6) |

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

> **En-têtes de sécurité déjà posés par l'application** (`config/SecurityConfig`) : **CSP**, **HSTS**,
> `Referrer-Policy: same-origin`, `X-Content-Type-Options: nosniff`, anti-clickjacking
> (`frame-ancestors 'self'`). Le **CSRF** est actif (jeton dans tous les formulaires) et un
> **anti-force-brute** verrouille le compte après 5 échecs.

---

## 5. Choisir le mode de déploiement HTTPS

Deux façons d'assurer le HTTPS, selon que l'on dispose ou non d'un reverse-proxy :

```
Mode A (sans Nginx)                       Mode B (avec Nginx/Apache)
Navigateur ──HTTPS──> App (keystore)      Navigateur ──HTTPS──> Proxy ──HTTP(127.0.0.1)──> App
```

| Critère | Mode A — Sans proxy | Mode B — Avec proxy |
|---|---|---|
| Composants à installer | aucun (juste le JAR) | Nginx ou Apache en plus |
| Terminaison TLS | par l'application (keystore PKCS12) | par le proxy |
| Redirection HTTP→HTTPS automatique | non (sauf ajout manuel) | **oui** (301) |
| En-têtes / HSTS renforcés | ceux de l'app | app **+** proxy |
| Mutualiser plusieurs apps / certificats | non | **oui** |
| Simplicité de mise en place | **la plus simple** | une brique de plus |

> **Recommandation.** Pour un **serveur unique en intranet** sans autre service web, le **Mode A** suffit
> et reste le plus simple. Dès qu'il faut mutualiser un certificat, héberger d'autres applications,
> forcer la redirection HTTP→HTTPS ou centraliser les journaux d'accès, préférez le **Mode B**.

---

## 5 bis. Mode A — Sans Nginx (application autonome)

L'application écoute directement et porte le certificat. **Pas besoin de reverse-proxy pour faire du HTTPS.**

### A.1 — Générer le keystore PKCS12

Le HTTPS de Spring Boot s'appuie sur un **keystore PKCS12** (`.p12`). Deux cas :

**a) Certificat fourni par la PKI interne (recommandé en intranet)** — convertir `.crt` + `.key` en PKCS12 :

```bash
openssl pkcs12 -export -name dgcpt \
  -in dgcpt.crt -inkey dgcpt.key \
  -out /etc/dgcpt/keystore.p12 -passout pass:"$SSL_KEYSTORE_PASSWORD"
```

**b) Auto-signé (test / labo uniquement — avertissement navigateur)** — via `keytool` (livré avec le JDK) :

```bash
keytool -genkeypair -alias dgcpt -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore /etc/dgcpt/keystore.p12 \
  -validity 825 -dname "CN=dgcpt.intranet, O=DGCPT, C=SN" \
  -storepass "$SSL_KEYSTORE_PASSWORD"
```

> Sous **Windows**, utilisez un chemin comme `C:\dgcpt\keystore.p12`. Protégez le fichier (lecture
> réservée au compte de service) ; le mot de passe passe par `SSL_KEYSTORE_PASSWORD`, jamais en clair.

### A.2 — Activer le HTTPS dans le profil `prod`

Le bloc « Option B » est déjà présent (commenté) dans `application-prod.properties` — le **décommenter** :

```properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=file:/etc/dgcpt/keystore.p12
server.ssl.key-store-type=PKCS12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-alias=dgcpt

server.servlet.session.cookie.secure=true     # HTTPS effectif -> cookie de session sécurisé
```

Puis lancer (avec `SSL_KEYSTORE_PASSWORD` en plus des `DB_*`). L'app est accessible en
**`https://<serveur>:8443/`**.

> **Port 443 directement** : sous Linux, les ports < 1024 sont privilégiés. Garder **8443** (le plus
> simple en intranet), ou rediriger 443→8443 au pare-feu. Sous Windows, 443 est utilisable directement
> si aucun autre service ne l'occupe.

### A.3 — Limite : pas de redirection HTTP→HTTPS automatique

En mode autonome, l'application **n'écoute que le port HTTPS** : une visite en `http://…` n'est pas
redirigée. En intranet, **publiez l'URL `https://`** (favori, portail). Forcer la redirection 80→443
sans proxy demande d'ajouter un connecteur HTTP secondaire — c'est précisément un avantage du **Mode B**.

### A.4 — Pare-feu (Mode A)

```bash
# Linux (ufw)
ufw allow 8443/tcp && ufw enable
```
```powershell
# Windows
New-NetFirewallRule -DisplayName "DGCPT HTTPS" -Direction Inbound -Protocol TCP -LocalPort 8443 -Action Allow
```

---

## 6. Mode B — Avec Nginx ou Apache (reverse-proxy, durci)

Le proxy termine le TLS et relaie vers l'application, qui **n'écoute que sur `127.0.0.1`**.

```
Navigateur ──HTTPS(443)──>  Reverse-proxy (Nginx ou Apache)  ──HTTP(127.0.0.1:8080)──>  App Spring Boot
                            · certificat TLS              · l'app n'écoute QUE sur localhost
                            · redirection 80 -> 443        · forward-headers-strategy=framework
                            · en-têtes de sécurité, HSTS
```

L'application **ne doit jamais être joignable directement** depuis le réseau : seul le proxy l'atteint
en local.

### 6.1 — Préparer l'application

Dans `application-prod.properties` (ou variables d'environnement) :

```properties
server.address=127.0.0.1            # n'écouter que sur la boucle locale
server.port=8080
server.forward-headers-strategy=framework    # respecter les en-têtes X-Forwarded-* (déjà actif)
server.servlet.session.cookie.secure=true     # HTTPS assuré par le proxy
server.error.whitelabel.enabled=false         # ne pas divulguer la version du serveur
```

> **Pas de CSP en double** : l'application pose déjà CSP, HSTS, Referrer-Policy, anti-MIME/clickjacking.
> Le proxy ci-dessous **complète** (Permissions-Policy, HSTS même sur les erreurs, masquage de version)
> sans réémettre de CSP. *(Variante : tout centraliser au proxy et le retirer de l'app — pas les deux.)*

### 6.2 — Certificat TLS

- **Intranet** : certificat émis par l'**autorité interne** (AD CS / PKI d'entreprise) — à privilégier
  pour la confiance navigateur sans avertissement.
- **Public** : Let's Encrypt (`certbot`), renouvellement automatique.
- **Test uniquement** : auto-signé (avertissement navigateur) :

```bash
openssl req -x509 -newkey rsa:2048 -nodes -days 825 \
  -keyout /etc/ssl/dgcpt/dgcpt.key -out /etc/ssl/dgcpt/dgcpt.crt \
  -subj "/CN=dgcpt.intranet"
chmod 600 /etc/ssl/dgcpt/dgcpt.key
```

Paramètres Diffie-Hellman (Nginx, suites DHE) : `openssl dhparam -out /etc/ssl/dgcpt/dhparam.pem 2048`.

### 6.3 — Nginx — configuration complète

`/etc/nginx/sites-available/dgcpt.conf` :

```nginx
# ---- HTTP : redirection forcée vers HTTPS ----
server {
    listen 80;
    listen [::]:80;
    server_name dgcpt.intranet;
    return 301 https://$host$request_uri;       # HTTPS forcé
}

# ---- HTTPS ----
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name dgcpt.intranet;

    # --- TLS durci ---
    ssl_certificate     /etc/ssl/dgcpt/dgcpt.crt;
    ssl_certificate_key /etc/ssl/dgcpt/dgcpt.key;
    ssl_protocols TLSv1.2 TLSv1.3;              # SSLv3/TLSv1.0/1.1 désactivés
    ssl_prefer_server_ciphers on;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305;
    ssl_dhparam /etc/ssl/dgcpt/dhparam.pem;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;
    ssl_stapling on;                            # OCSP (certificats d'une CA publique)
    ssl_stapling_verify on;

    # --- En-têtes de sécurité (complément à ceux de l'app ; pas de CSP en double) ---
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header Referrer-Policy "same-origin" always;
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

    server_tokens off;                          # masque la version Nginx
    client_max_body_size 100m;                  # >= limite multipart de l'app (100 Mo : import de base, canevas, PDF)

    # --- Reverse-proxy vers l'app ---
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;   # -> l'app sait qu'on est en https
        proxy_set_header X-Forwarded-Host  $host;
        proxy_read_timeout 300s;                # imports / exports un peu longs
        proxy_buffering on;
    }

    # Bloquer l'accès direct aux endpoints d'admin Actuator depuis l'extérieur (sauf /health)
    location /actuator/ { return 403; }
    location = /actuator/health { proxy_pass http://127.0.0.1:8080; }
}
```

Activer + recharger :
```bash
ln -s /etc/nginx/sites-available/dgcpt.conf /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

> Version **minimale** suffisante en labo : les deux blocs `server` (redirection 80→443 + `location /`
> avec les `proxy_set_header` `Host`/`X-Forwarded-*` et `client_max_body_size 100m`). Les directives TLS
> durcies et en-têtes ci-dessus sont recommandées en production.

### 6.4 — Apache (httpd) — configuration complète

Modules requis : `mod_ssl mod_proxy mod_proxy_http mod_headers mod_rewrite`.
```bash
a2enmod ssl proxy proxy_http headers rewrite      # Debian/Ubuntu
```

`/etc/apache2/sites-available/dgcpt.conf` :

```apache
# ---- HTTP : redirection forcée vers HTTPS ----
<VirtualHost *:80>
    ServerName dgcpt.intranet
    RewriteEngine On
    RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [R=301,L]
</VirtualHost>

# ---- HTTPS ----
<VirtualHost *:443>
    ServerName dgcpt.intranet

    SSLEngine on
    SSLCertificateFile      /etc/ssl/dgcpt/dgcpt.crt
    SSLCertificateKeyFile   /etc/ssl/dgcpt/dgcpt.key
    SSLProtocol             -all +TLSv1.2 +TLSv1.3      # TLS 1.0/1.1 désactivés
    SSLHonorCipherOrder     on
    SSLCipherSuite          ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305
    SSLUseStapling          on

    # En-têtes de sécurité (complément ; pas de CSP en double avec l'app)
    Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains"
    Header always set X-Content-Type-Options "nosniff"
    Header always set X-Frame-Options "DENY"
    Header always set Referrer-Policy "same-origin"
    Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"

    ServerTokens Prod
    ServerSignature Off
    LimitRequestBody 104857600                    # 100 Mo = limite multipart de l'app

    # Reverse-proxy vers l'app (l'app n'écoute que sur 127.0.0.1)
    ProxyPreserveHost On
    RequestHeader set X-Forwarded-Proto "https"
    ProxyPass        /actuator/health http://127.0.0.1:8080/actuator/health
    ProxyPassReverse /actuator/health http://127.0.0.1:8080/actuator/health
    # Bloquer le reste d'Actuator depuis l'extérieur
    <Location /actuator/>
        Require all denied
    </Location>
    ProxyPass        / http://127.0.0.1:8080/
    ProxyPassReverse / http://127.0.0.1:8080/
    ProxyTimeout 300
</VirtualHost>
```
> `ServerTokens Prod` se met dans la config globale (`/etc/apache2/conf-available/security.conf`).

Activer + recharger :
```bash
a2ensite dgcpt && apachectl configtest && systemctl reload apache2
```

### 6.5 — Récapitulatif des paramètres de sécurité (Mode B)

| Paramètre | Où | Valeur |
|---|---|---|
| **HTTPS forcé** | proxy | redirection 80 → 443 (301) |
| **HSTS** | app + proxy | `max-age=31536000; includeSubDomains` |
| **TLS** | proxy | TLS 1.2 / 1.3 uniquement, suites ECDHE/GCM/CHACHA20 |
| **CSP** | app | `default-src 'self'…` (une seule source) |
| **Anti-MIME / clickjacking** | app + proxy | `nosniff`, `frame-ancestors 'self'` / `X-Frame-Options: DENY` |
| **Referrer / Permissions** | app + proxy | `same-origin` / `Permissions-Policy` restrictive |
| **Cookie de session** | app | `HttpOnly`, `SameSite=Lax`, `Secure` |
| **CSRF** | app | actif (jeton dans tous les formulaires) |
| **Anti-force-brute** | app | verrouillage temporaire après 5 échecs |
| **Taille des téléversements** | app + proxy | 100 Mo |
| **Écoute restreinte** | app | `127.0.0.1` (jamais exposée directement) |
| **Masquage des versions** | proxy | `server_tokens off` / `ServerTokens Prod` |
| **Supervision protégée** | proxy | `/actuator/**` bloqué sauf `/health` |

### 6.6 — Pare-feu (Mode B)

Ouvrir **80** (redirection) et **443** ; **bloquer 8080** depuis l'extérieur (l'app n'écoute de toute
façon que sur `127.0.0.1`).

```bash
# Linux (ufw)
ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
# Linux (firewalld)
firewall-cmd --permanent --add-service=http --add-service=https && firewall-cmd --reload
```
```powershell
# Windows (si proxy sur Windows)
New-NetFirewallRule -DisplayName "DGCPT HTTPS" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow
New-NetFirewallRule -DisplayName "DGCPT HTTP redirect" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
```

### 6.7 — Vérification TLS

```bash
# 1. Redirection HTTP -> HTTPS (301)
curl -sI http://dgcpt.intranet | grep -i location
# 2. HSTS et en-têtes présents
curl -sI https://dgcpt.intranet | grep -iE "strict-transport|content-type-options|frame-options|referrer|permissions"
# 3. Version TLS (doit refuser TLS 1.0/1.1)
openssl s_client -connect dgcpt.intranet:443 -tls1_1 </dev/null   # doit ÉCHOUER
openssl s_client -connect dgcpt.intranet:443 -tls1_2 </dev/null   # doit réussir
# 4. App non joignable en direct
curl -s http://<ip-serveur>:8080/    # doit échouer (connexion refusée / filtrée)
# 5. Santé applicative au travers du proxy
curl -s https://dgcpt.intranet/actuator/health      # {"status":"UP"}
```
Audit complémentaire : **SSL Labs** (si exposé) ou `testssl.sh` en interne (note A attendue).

### 6.8 — Renouvellement du certificat

- **Let's Encrypt** : `certbot renew` (tâche planifiée) recharge le proxy automatiquement.
- **CA interne** : suivre l'échéance ; remplacer `.crt`/`.key` puis `reload` du proxy.

---

## 7. Exécuter comme service

- **Linux (systemd)** — `/etc/systemd/system/dgcpt.service` :

```ini
[Unit]
Description=Missions & Parc DGCPT
After=network.target postgresql.service

[Service]
EnvironmentFile=/etc/dgcpt/app.env          # DB_*, SSL_KEYSTORE_PASSWORD (Mode A), LOG_FILE
ExecStart=/usr/bin/java -jar /opt/dgcpt/app.jar --spring.profiles.active=prod
Restart=always
User=dgcpt

[Install]
WantedBy=multi-user.target
```
```bash
systemctl daemon-reload && systemctl enable --now dgcpt
```

- **Windows** : exécuter le JAR au démarrage (tâche planifiée « au démarrage » ou service via **NSSM**).
  Définir `LOG_FILE` (ex. `C:\ProgramData\DGCPT\logs\app.log`) et les `DB_*` au niveau du service.

## 8. Sauvegardes (à activer)

- **Linux** : `scripts/systemd/dgcpt-sauvegarde.{service,timer}` → `systemctl enable --now dgcpt-sauvegarde.timer`.
- **Windows** : `scripts/installer-sauvegarde-planifiee.ps1` (tâche quotidienne 01h00).
- **Copier les dumps hors-serveur** et lancer périodiquement le **test de restauration**
  (`scripts/test-restauration-postgres.sh`).

## 9. Recette de mise en service (smoke test)

| # | Vérification | Attendu |
|---|---|---|
| 1 | `GET /actuator/health` | `{"status":"UP"}` (avec PostgreSQL) |
| 2 | Page de login accessible en **https** | cadenas, pas d'alerte mixte |
| 3 | Redirection HTTP→HTTPS | Mode B : `301` ; Mode A : non assurée (publier l'URL https) |
| 4 | Connexion `admin/admin` | redirection vers le tableau de bord |
| 5 | **Changer** le mot de passe initial | imposé à la première connexion |
| 6 | Charger une page liste (Parc / Missions) | données + pagination OK |
| 7 | Export de base (`/donnees`, ADMIN) | dump téléchargé (proxy `client_max_body_size` OK) |
| 8 | Lancer une sauvegarde test | dump produit, restaurable |
| 9 | *(Mode B)* App non joignable en direct | `curl http://<ip>:8080/` doit échouer |

## 10. Checklist sécurité avant ouverture

- [ ] **HTTPS effectif** (keystore en Mode A, certificat au proxy en Mode B) + cookie `Secure` activé.
- [ ] *(Mode B)* App liée à `127.0.0.1` ; redirection **HTTP→HTTPS** active ; **8080 non exposé** ;
      TLS 1.2/1.3 seulement (1.0/1.1 refusés) ; HSTS et en-têtes présents ; `/actuator/**` bloqué sauf `/health`.
- [ ] Compte initial `admin/admin` **changé** (ou désactivé après création d'un autre admin).
- [ ] `DB_PASSWORD` (et `SSL_KEYSTORE_PASSWORD` en Mode A) **forts**, jamais en clair dans un fichier versionné.
- [ ] Accès réseau **restreint** à l'intranet (pare-feu) ; versions serveur masquées (Mode B).
- [ ] Sauvegardes planifiées **actives** + copie hors-serveur.
- [ ] **Journalisation fichier** active (profil `prod` ; `LOG_FILE` sous Windows) + dossier accessible en écriture.
- [ ] Journal d'audit consulté (`/journal`) ; supervision branchée sur `/actuator/health`.
