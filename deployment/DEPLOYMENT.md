# Deployment — Rasen-Radar mit KI-Vorschau (rasenradar)

Zielplattform: **eigener Server** (SSH-Zugriff) mit Traefik als Reverse Proxy.
Container-Registry: **GitHub Container Registry (GHCR)**.
CI/CD: **GitHub Actions** (`.github/workflows/build.yml` + `deploy.yml`).

Öffentliches Hobby-/Showcase-Projekt: die Ansicht bleibt frei zugänglich, nur die
KI-Vorschau-auslösenden POST-Routen sind per Basic Auth geschützt (Kostenschutz).

---

## Architektur

```
Internet → Traefik (proxy-Netz, :443) → rasenradar (:8080)   [rasen-radar.de]
                                              ↓
                                         postgres (internal-Netz, :5432)
                                              ↑
                                        umami (:3000)        [stats.rasen-radar.de]
```

- `proxy`-Netz: extern, wird von Traefik verwaltet — muss auf dem Server existieren.
- `internal`-Netz: bridge, isoliert Postgres vom Internet.
- TLS: Let's Encrypt via Traefik (`letsencrypt`-certresolver).
- Domain: `rasen-radar.de` (kanonisch). `rasenradar.markserver.de` ist der alte Host und
  leitet per 301 (`redirectregex`-Middleware) 1:1 auf `rasen-radar.de` weiter — kein doppelt
  indexierter Content unter zwei Hosts.
- Die Ansicht (alle GET-Routen) bleibt frei zugänglich. Nur POST-Routen — KI-Vorschau/Rücktest
  auslösen, Bewertungsmaßstäbe ändern — sind per Basic Auth geschützt (zweiter Traefik-Router mit
  `Method(\`POST\`)`, siehe `docker-compose.yml`). Kostenschutz, kein Login-System in der App.

**Bekannte Einschränkung:** Anders als im Dev-Modus läuft auf dem Server kein lokales Ollama.
Das `prod`-Profil schaltet den lokalen Fallback deshalb ab (`matchoracle.forecast.local-fallback-enabled=false`):
Schlägt der Cloud-Aufruf (Anthropic) fehl — etwa weil die Credits aufgebraucht sind —, schlägt der
betroffene Bewerter bzw. Prognoseschritt mit dem echten Grund fehl. Die drei Bewerter sind dank
`ResilientAssessors` (ein Retry, dann Ausfall statt Abbruch) robust dagegen; ein Ausfall des
Prognostikers oder Prüfers lässt die einzelne Prognose fehlschlagen. Der Betrachter sieht einen
kurzen Hinweis mit dem Grund; die Terminplanung holt anstehende Spiele nach einer Stunde nach
(`matchoracle.forecast.auto.retry-after`). Die Antworten des Cloud-Modells werden in Prod geloggt
(`log-responses=true`), damit sich abgeschnittene oder unparsbare Antworten nachvollziehen lassen.

---

## Domain-Umzug auf `rasen-radar.de`

Einmalig nötig, bevor der nächste Deploy live geht:

1. Bei der Registrierung von `rasen-radar.de`: A-Record (und AAAA, falls IPv6) auf die Server-IP setzen,
   genau wie für `rasenradar.markserver.de` — Traefik entscheidet rein über den `Host()`-Header, DNS
   muss für beide Hosts auf denselben Server zeigen.
2. `docker compose up -d` ausführen (bzw. den "Deploy to Production"-Workflow anstoßen) — Traefik zieht
   sich für `rasen-radar.de` automatisch ein neues Let's-Encrypt-Zertifikat.
3. Prüfen: `https://rasen-radar.de` liefert die Seite aus, `https://rasenradar.markserver.de` leitet
   (Status 301) auf denselben Pfad unter `rasen-radar.de` weiter.
4. Sitemap/robots.txt (siehe unten) referenzieren bereits `rasen-radar.de` — nach dem Umzug bei Google
   Search Console als neue Property anlegen und die alte Domain dort als "Adressänderung" markieren.

---

## Secrets-Strategie

**Gewählter Ansatz: `.env`-Datei auf dem Server** (wie beim Referenzprojekt).

- DB-Passwort und Anthropic-Key leben in `<deploy-path>/.env`
- `docker compose` liest die Datei automatisch ein
- Datei niemals ins Git-Repository committen

GitHub Secrets werden nur für **SSH-Zugang, GHCR-Login und den Cache-Purge** benötigt:

| Secret | Beschreibung |
|--------|-------------|
| `DEPLOY_HOST` | IP oder FQDN des Zielservers |
| `DEPLOY_USER` | SSH-Benutzername |
| `DEPLOY_SSH_KEY` | Privater SSH-Key (PEM-Format; passender Public Key muss in `~/.ssh/authorized_keys` stehen) |
| `DEPLOY_PATH` | Absoluter Pfad auf dem Server, in den `deployment/` kopiert wird, z. B. `<dein-pfad>/rasenradar` |
| `GHCR_TOKEN` | GitHub PAT mit `read:packages`-Scope — für `docker login ghcr.io` auf dem Server |

Cloudflare-Cache-Purge nach dem Deploy ist vorerst nicht eingerichtet — bei Bedarf später mit
`CF_ZONE_ID`/`CF_API_TOKEN` nachrüsten (Zone-ID und ein API-Token mit `Zone.Cache Purge`-Recht).

`DEPLOY_HOST`/`_USER`/`_SSH_KEY` und `GHCR_TOKEN` existieren vermutlich schon als Secrets in
anderen Repos auf demselben Server/Account — ggf. dieselben Werte wiederverwenden.
Namen bewusst generisch gehalten, damit der Hosting-Anbieter nicht aus dem Repo hervorgeht.

---

## `.env`-Datei auf dem Server

Vorlage: `deployment/.env.example`. Die Datei liegt auf dem Server unter:
`<deploy-path>/.env`

```bash
MATCHORACLE_DB_PASSWORD=<sicheres-passwort>
ANTHROPIC_API_KEY=<api-key>
```

Berechtigungen setzen:
```bash
chmod 600 <deploy-path>/.env
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
mkdir -p <deploy-path>
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
scp -r deployment/. <user>@<host>:<deploy-path>/
```

### 5. Ersten Start testen

```bash
ssh <user>@<host>
cd <deploy-path>
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

## Besucherstatistik (Umami)

Selbst gehostet, deshalb bleiben die Daten auf demselben Server. Umami setzt keine Cookies und
speichert keine IP-Adressen: ein Besucher wird über einen täglich wechselnden Hash erkannt. Damit
braucht die Seite kein Zustimmungsbanner — und die Zahlen stimmen, weil niemand wegklicken kann.
Erfasst werden Seitenaufruf, Referrer, Land, Browser, Betriebssystem und Bildschirmgröße.

- Dashboard: `https://stats.rasen-radar.de` (eigener Traefik-Router, eigenes Zertifikat)
- Image: `ghcr.io/umami-software/umami:3.4.0`, Schema-Migrationen laufen beim Start selbst
- Datenbank: `umami` in derselben Postgres-Instanz, User `matchoracle`

### Einmalige Einrichtung

**1. DNS:** `stats.rasen-radar.de` als A-Record auf dieselbe Server-IP wie `rasen-radar.de`.
Ohne den Eintrag bekommt Traefik kein Let's-Encrypt-Zertifikat.

**2. Secrets in die `.env` auf dem Server** (`UMAMI_WEBSITE_ID` bleibt zunächst leer):

```bash
openssl rand -hex 32   # → UMAMI_APP_SECRET
openssl rand -hex 32   # → UMAMI_TWO_FACTOR_KEY
```

**3. Datenbank anlegen.** Das Postgres-Volume existiert bereits, `POSTGRES_DB` greift also nicht
mehr — die zweite Datenbank muss einmalig von Hand angelegt werden:

```bash
cd /opt/rasenradar   # bzw. DEPLOY_PATH
docker compose exec postgres createdb -U matchoracle umami
```

**4. Umami starten** und im Dashboard anmelden:

```bash
docker compose up -d umami
docker compose logs -f umami   # wartet auf "Server started"
```

Erstanmeldung mit `admin` / `umami` — **Passwort sofort ändern**, die Instanz ist öffentlich
erreichbar.

**5. Website anlegen:** Settings → Websites → Add website, Name „Rasen-Radar", Domain
`rasen-radar.de`. Umami zeigt danach die Website-ID (UUID).

**6. ID in die `.env` eintragen** und die Anwendung neu starten, damit sie das Tracking-Script
ausliefert:

```bash
# UMAMI_WEBSITE_ID=<die UUID aus dem Dashboard>
docker compose up -d rasenradar
```

Ohne ID bindet die Anwendung gar kein Script ein (`matchoracle.analytics.website-id` ist leer) —
die Seite funktioniert also in jedem Zwischenzustand. Prüfen lässt sich das im Seitenquelltext:
`<script src="https://stats.rasen-radar.de/script.js" data-website-id="…">`.

### Warum die Zahlen von Server-Logs abweichen

Umami zählt nur Aufrufe, bei denen JavaScript läuft — Suchmaschinen-Crawler und Bots fehlen
deshalb, Besucher mit Script-Blocker ebenfalls. Das ist gewollt: gezählt werden Menschen, nicht
Requests.

---

## Rollback

```bash
# Auf dem Server
cd <deploy-path>
IMAGE_TAG=v1.2.2 docker compose up -d
```

Oder über den "Deploy to Production"-Workflow mit explizitem Tag.

---

## Monitoring & Health

| Endpunkt | Beschreibung |
|----------|-------------|
| `https://rasen-radar.de/q/health/live` | Liveness-Check |
| `https://rasen-radar.de/q/health/ready` | Readiness-Check (wartet auf DB) |

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
