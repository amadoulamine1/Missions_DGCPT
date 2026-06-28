# 11 — Guide de déploiement sécurisé (reverse-proxy HTTPS : Nginx & Apache)

Déploiement **durci** de l'application **Missions & Parc DGCPT** derrière un reverse-proxy assurant la
**terminaison TLS** et le **HTTPS forcé**. Complète `08-Guide-de-deploiement.md`.

## 0. Principe

```
Navigateur ──HTTPS(443)──>  Reverse-proxy (Nginx ou Apache)  ──HTTP(127.0.0.1:8080)──>  App Spring Boot
                            · certificat TLS              · l'app n'écoute QUE sur localhost
                            · redirection 80 -> 443        · forward-headers-strategy=framework
                            · en-têtes de sécurité, HSTS
```

L'application **ne doit jamais être joignable directement** depuis le réseau : seul le proxy l'atteint
en local. Le proxy impose le HTTPS et porte le certificat.

## 1. Préparer l'application

Dans `application-prod.properties` (ou variables d'environnement) :

```properties
# N'écouter que sur la boucle locale : seul le reverse-proxy peut joindre l'app
server.address=127.0.0.1
server.port=8080

# Respecter les en-têtes du proxy (https, hôte, IP réelle) — déjà présent dans le profil prod
server.forward-headers-strategy=framework

# Cookie de session : HttpOnly + SameSite=Lax (déjà actifs) + Secure une fois en HTTPS
server.servlet.session.cookie.secure=true

# Ne pas divulguer la version du serveur
server.error.whitelabel.enabled=false
```

> **En-têtes déjà posés par l'application** (`config/SecurityConfig`) : **CSP**, **HSTS**,
> `Referrer-Policy: same-origin`, `X-Content-Type-Options: nosniff`, anti-clickjacking (`frame-ancestors 'self'`).
> Pour éviter les **doublons**, le proxy ci-dessous **n'ajoute pas** de second CSP/Referrer ; il complète
> avec `Permissions-Policy`, renforce **HSTS** (présent même sur les erreurs) et masque sa propre version.
> *(Variante : centraliser tous les en-têtes au proxy et les retirer de l'app — ne pas faire les deux.)*

## 2. Certificat TLS

- **Public / accessible depuis Internet** : Let's Encrypt (`certbot`), renouvellement automatique.
- **Intranet** : certificat émis par l'**autorité interne** (AD CS / PKI d'entreprise) — à privilégier
  pour la confiance navigateur sans avertissement.
- **Test uniquement** : auto-signé (génère un avertissement navigateur) :

```bash
openssl req -x509 -newkey rsa:2048 -nodes -days 825 \
  -keyout /etc/ssl/dgcpt/dgcpt.key -out /etc/ssl/dgcpt/dgcpt.crt \
  -subj "/CN=dgcpt.intranet"
chmod 600 /etc/ssl/dgcpt/dgcpt.key
```

Paramètres Diffie-Hellman (Nginx, pour les suites DHE) : `openssl dhparam -out /etc/ssl/dgcpt/dhparam.pem 2048`.

## 3. Nginx — configuration complète

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
    client_max_body_size 10m;                   # = limite multipart de l'app (10 Mo)

    # --- Reverse-proxy vers l'app ---
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;   # -> l'app sait qu'on est en https
        proxy_set_header X-Forwarded-Host  $host;
        proxy_read_timeout 300s;                # imports / exports Excel un peu longs
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

## 4. Apache (httpd) — configuration complète

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
    LimitRequestBody 10485760                     # 10 Mo = limite multipart de l'app

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

## 5. Récapitulatif des paramètres de sécurité

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
| **Taille des téléversements** | app + proxy | 10 Mo |
| **Écoute restreinte** | app | `127.0.0.1` (jamais exposée directement) |
| **Masquage des versions** | proxy | `server_tokens off` / `ServerTokens Prod` |
| **Supervision protégée** | proxy | `/actuator/**` bloqué sauf `/health` |

## 6. Pare-feu

N'ouvrir que **80** (redirection) et **443** ; **bloquer 8080** depuis l'extérieur.

```bash
# Linux (ufw)
ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
# (8080 reste fermé ; l'app n'écoute que sur 127.0.0.1 de toute façon)

# Linux (firewalld)
firewall-cmd --permanent --add-service=http --add-service=https && firewall-cmd --reload
```
```powershell
# Windows (si proxy sur Windows)
New-NetFirewallRule -DisplayName "DGCPT HTTPS" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow
New-NetFirewallRule -DisplayName "DGCPT HTTP redirect" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
```

## 7. Vérification

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
Audit complémentaire : **SSL Labs** (si exposé), ou `testssl.sh` en interne (note A attendue).

## 8. Renouvellement du certificat

- **Let's Encrypt** : `certbot renew` (tâche planifiée) recharge le proxy automatiquement.
- **CA interne** : suivre l'échéance (ex. 1 an) ; remplacer `.crt`/`.key` puis `reload` du proxy.

## 9. Checklist de mise en ligne

- [ ] App liée à `127.0.0.1` uniquement ; **8080 non exposé**.
- [ ] Redirection **HTTP → HTTPS** active (301).
- [ ] TLS 1.2/1.3 seulement ; TLS 1.0/1.1 **refusés** ; suites fortes.
- [ ] **HSTS**, `nosniff`, anti-clickjacking, Referrer-Policy, Permissions-Policy présents.
- [ ] Cookie de session **`Secure`** activé.
- [ ] `/actuator/**` bloqué au proxy (sauf `/health`).
- [ ] Pare-feu : 80/443 ouverts, reste fermé.
- [ ] Versions serveur masquées (`server_tokens` / `ServerTokens`).
- [ ] Compte initial `admin/admin` **changé** ; sauvegardes **actives** + copie hors-serveur.
