# Deployment — Bundesliga aktuell mit KI-Vorschau (rasenradar)

Zielplattform: **eigener Server** (SSH-Zugriff) mit Traefik als Reverse Proxy.
Container-Registry: **GitHub Container Registry (GHCR)**.
CI/CD: **GitHub Actions** (`.github/workflows/build.yml` + `deploy.yml`).

Öffentliches Hobby-/Showcase-Projekt: die Ansicht bleibt frei zugänglich, nur die
KI-Vorschau-auslösenden POST-Routen sind per Basic Auth geschützt (Kostenschutz).

---

## Architektur

```
Internet → Traefik (proxy-Netz, :443) → rasenradar (:8080)
                                              ↓
                                         postgres (internal-Netz, :5432)
```

- `proxy`-Netz: extern, wird von Traefik verwaltet — muss auf dem Server existieren.
- `internal`-Netz: bridge, isoliert Postgres vom Internet.
- TLS: Let's Encrypt via Traefik (`letsencrypt`-certresolver).
- Domain: `rasenradar.markserver.de`
- Die Ansicht (alle GET-Routen) bleibt frei zugänglich. Nur POST-Routen — KI-Vorschau/Rücktest
  auslösen, Bewertungsmaßstäbe ändern — sind per Basic Auth geschützt (zweiter Traefik-Router mit
  `Method(\`POST\`)`, siehe `docker-compose.yml`). Kostenschutz, kein Login-System in der App.

**Bekannte Einschränkung:** Anders als im Dev-Modus läuft auf dem Server kein lokales Ollama.
Schlägt der Cloud-Aufruf (Anthropic) fehl, greift `FallbackChatModel` ins Leere — der betroffene
Bewerter bzw. Prognoseschritt schlägt fehl, statt auf ein lokales Modell auszuweichen. Die drei
Bewerter sind dank `ResilientAssessors` (ein Retry, dann Ausfall statt Abbruch) robust dagegen;
ein Ausfall des Prognostikers oder Prüfers lässt die einzelne Prognose fehlschlagen — der Nutzer
sieht das im Fortschritt und kann es erneut versuchen.

---

## Secrets-Strategie

**Gewählter Ansatz: `.env`-Datei auf dem Server** (wie beim Referenzprojekt).

- DB-Passwort und Anthropic-Key leben in `/opt/markserver-cloud/rasenradar/.env`
- `docker compose` liest die Datei automatisch ein
- Datei niemals ins Git-Repository committen

GitHub Secrets werden nur für **SSH-Zugang, GHCR-Login und den Cache-Purge** benötigt:

| Secret | Beschreibung |
|--------|-------------|
| `DEPLOY_HOST` | IP oder FQDN des Zielservers |
| `DEPLOY_USER` | SSH-Benutzername |
| `DEPLOY_SSH_KEY` | Privater SSH-Key (PEM-Format; passender Public Key muss in `~/.ssh/authorized_keys` stehen) |
| `GHCR_TOKEN` | GitHub PAT mit `read:packages`-Scope — für `docker login ghcr.io` auf dem Server |
| `CF_ZONE_ID` | Cloudflare Zone-ID von `markserver.de` (für den Cache-Purge nach dem Deploy) |
| `CF_API_TOKEN` | Cloudflare API-Token mit `Zone.Cache Purge`-Recht |

`DEPLOY_HOST`/`_USER`/`_SSH_KEY` und `GHCR_TOKEN` existieren vermutlich schon als Secrets in
anderen Repos auf demselben Server/Account — ggf. dieselben Werte wiederverwenden.
Namen bewusst generisch gehalten, damit der Hosting-Anbieter nicht aus dem Repo hervorgeht.

---

## `.env`-Datei auf dem Server

Vorlage: `deployment/.env.example`. Die Datei liegt auf dem Server unter:
`/opt/markserver-cloud/rasenradar/.env`

```bash
MATCHORACLE_DB_PASSWORD=<sicheres-passwort>
ANTHROPIC_API_KEY=<api-key>
```

Berechtigungen setzen:
```bash
chmod 600 /opt/markserver-cloud/rasenradar/.env
```

---

## Zugriffskontrolle für die schreibenden Routen (Basic Auth)

Nur POST-Anfragen (KI-Vorschau/Rücktest starten, Bewertungsmaßstäbe ändern) verlangen ein
Passwort — geprüft von Traefik, bevor die Anfrage die Anwendung überhaupt erreicht.

**Nutzer anlegen:**

```bash
openssl passwd -apr1 'passwort-von-alice'
# Ausgabe z.B.: $apr1$xK3s1234$AbCdEfGhIjKlMnOpQrStUv0
```

In der `.env` auf dem Server, mehrere Nutzer kommagetrennt. **Wichtig: jedes `$` im Hash muss als
`$$` geschrieben werden** — Docker Compose interpoliert `.env`-Werte sonst als Variable:

```
BASIC_AUTH_USERS=alice:$$apr1$$xK3s1234$$AbCdEfGhIjKlMnOpQrStUv0
```

Danach `docker compose up -d` — Traefik übernimmt es sofort, kein Neustart von Traefik nötig.

## Server-Ersteinrichtung (einmalig)

### 1. Verzeichnis anlegen

```bash
mkdir -p /opt/markserver-cloud/rasenradar
```

### 2. Traefik-Netzwerk sicherstellen

```bash
docker network inspect proxy >/dev/null 2>&1 || docker network create proxy
```

### 3. `.env`-Datei anlegen und befüllen

Siehe Abschnitt oben. Werte aus dem Passwort-Manager übernehmen.

### 4. Deployment-Dateien initial hochladen

Beim ersten Mal manuell — danach übernimmt CI/CD automatisch:

```bash
scp -r deployment/. <user>@<host>:/opt/markserver-cloud/rasenradar/
```

### 5. Ersten Start testen

```bash
ssh <user>@<host>
cd /opt/markserver-cloud/rasenradar
docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f rasenradar
```

---

## Deployment-Workflow (CI/CD)

### Automatischer Build & Test (bei jedem Push und jeder PR auf `main`)

`.github/workflows/build.yml`:
1. `./mvnw test` — alle 48 Tests müssen grün sein (ArchUnit eingeschlossen), sonst bricht der Workflow ab.
2. Nur bei einem Push (nicht bei PRs): baut das Docker-Image via Quarkus JIB und pusht es nach GHCR.
   - Branch-Push → `ghcr.io/mgoericke/rasenradar:latest`
   - Release-Tag (`v*.*.*`) → `ghcr.io/mgoericke/rasenradar:v1.2.3` + `:latest`

### Manuelles Deployment (GitHub Actions → Actions → "Deploy to Production")

1. GitHub → **Actions** → **Deploy to Production** → **Run workflow**
2. Optional: Image-Tag angeben (Standard: `latest`)
3. Der Workflow:
   - Kopiert `deployment/`-Verzeichnis via SCP auf den Server (aktualisiert `docker-compose.yml` etc.)
   - Führt `docker compose pull` + `docker compose up -d` aus
   - Räumt alte Images auf (`docker image prune -f`)
   - Leert den Cloudflare-Cache für die statischen Dateien (`app.css`, `match.js`, `accuracy.js`)

Die `.env`-Datei auf dem Server bleibt unberührt — sie wird nur manuell gepflegt.

---

## Datenbank

- Image: `postgres:18-alpine` (dieselbe Version wie im Dev-Modus via Compose Dev Services)
- Datenbank/User werden beim ersten Start über `POSTGRES_DB`/`POSTGRES_USER` automatisch angelegt
- Schema: **Flyway** migriert beim Start (`quarkus.flyway.migrate-at-start=true`), Hibernate validiert nur
- Daten persistiert in Docker Volume: `rasenradar-postgres-data`

Beim allerersten Start importiert `MatchdaySynchronizer` die laufende Saison plus zwei Vorsaisons
je Liga von OpenLigaDB (`importMissingSeasons`) — das kann beim ersten Hochfahren einen Moment dauern.

---

## Rollback

```bash
# Auf dem Server
cd /opt/markserver-cloud/rasenradar
IMAGE_TAG=v1.2.2 docker compose up -d
```

Oder über den "Deploy to Production"-Workflow mit explizitem Tag.

---

## Monitoring & Health

| Endpunkt | Beschreibung |
|----------|-------------|
| `https://rasenradar.markserver.de/q/health/live` | Liveness-Check |
| `https://rasenradar.markserver.de/q/health/ready` | Readiness-Check (wartet auf DB) |

Der Readiness-Check ist auch als Docker-Healthcheck konfiguriert.

---

## Fehlerbehebung

```bash
# Logs der Anwendung
docker compose logs -f rasenradar

# Logs der Datenbank
docker compose logs -f postgres

# Container-Status
docker compose ps

# In laufenden Container einloggen
docker compose exec rasenradar /bin/bash

# Postgres direkt abfragen
docker compose exec postgres psql -U matchoracle -d matchoracle
```
