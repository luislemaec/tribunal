# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Tribunal Electoral System (`tec` / `TEC`), Maven artifact `ec.com.antenasur.tec:tec`, packaged as a WAR.
Stack: Java 17, Jakarta EE 10, WildFly 39, JSF/Jakarta Faces 4, PrimeFaces 15 (+Extensions), JPA/Hibernate 6, Hibernate Envers (audit), Flyway 11, PostgreSQL.
Auth: FORM login via WildFly Elytron (`request.login(...)`), security domain `TribunalSecurityDomain`, passwords hashed with BCrypt (`at.favre.lib:bcrypt`, cost 12).

> **See also [`Agents.md`](Agents.md)** — a detailed (Spanish) agent guide with the full package map, the electoral domain model, login/QR-access security flows, document/report generation rules, and a list of sensitive files. Read it before making non-trivial changes to login, escrutinio/JRV/padron, documents/reports, or the QR partial-acta feature; this file only summarizes what's needed to get building and testing.

## Build & Test

```bash
mvn -DskipTests compile      # compile only, run before finishing any code change
mvn clean package            # build the WAR
mvn clean package wildfly:deploy
mvn wildfly:undeploy
```

Tests are **not** run by default — Surefire is configured with `<skip>true</skip>` in `pom.xml`. Test sources exist under `src/test/java/ec/com/antenasur/` (service/`tec` and `security/qr` packages) but only run via the dedicated profile:

```bash
mvn -Pqr-tests test          # runs **/security/qr/*Test.java only
```

Arquillian profiles exist for container-based tests but need a WildFly instance:

```bash
mvn clean test -Parq-wildfly-managed
mvn clean test -Parq-wildfly-remote
```

To run a single test class, add `-Dtest=ClassName` to a Surefire-enabled invocation (e.g. `mvn -Pqr-tests test -Dtest=AlcanceSesionQrServiceTest`).

## Architecture

Standard layered backend under `src/main/java/ec/com/antenasur/`:

- `controller/` — JSF/CDI screen controllers: UI events, validation, navigation, calls into `service/`. Keep thin.
- `service/` (+ `service/tec/`) — EJB/CDI business rules and transactions. This is where domain logic belongs, not in controllers or util.
- `facade/` (+ `facade/tec/`) — JPA queries and persistence.
- `model/` (+ `model/tec/`) — JPA entities; several are audited via Hibernate Envers (`audit/`).
- `dto/` — flat view objects; prefer these over full entities in JSF/session state.
- `bean/` — session/conversation state, notably `LoginBean`.
- `security/menu/` — page/menu authorization (`AccesoPaginaInterceptor`, `AutorizacionMenuService`).
- `security/qr/` — the QR partial-acta access feature (opaque-token exchange, Elytron bridge, rate limiting); disabled by default behind `tec.qr.enabled`.
- `itext/` — PDF (iText 5) and XLSX (Apache POI) report generation.
- `util/` — cross-cutting utilities, filters, mail, file handling, `Constantes`; avoid adding new business rules here.

Webapp (JSF, `src/main/webapp/`) has ~30 top-level `*.xhtml` screens plus `WEB-INF/` (Faces Servlet/FORM login config, `template.xhtml`/`menu.xhtml` layout, `globals.xhtml` for the global growl message component). URL mappings: `/faces/*`, `*.jsf`, `*.faces`, `*.xhtml`.

### Electoral domain core

The system is centered on `ProcesoElectoral` (not the legacy `Periodo`) for the electoral module. Key entities: `Padron` (persona/iglesia roll per mesa+proceso), `MiembroJRV` (Junta Receptora del Voto designations), `EscrutinioCabecera`/`Escrutinio` (mesa open/count/close state and vote totals). Padron and escrutinio queries must always filter by `proce_id`, not just `mesa_id`, since historical data from prior processes coexists in the same tables. See `Agents.md` for the full rule set (padron eligibility, JRV designation, Presidente de Mesa flow, acta PDF generation gating mesa closure).

### Persistence

`persistence.xml` uses `hibernate.hbm2ddl.auto=none`; **Flyway is the sole authority for schema**, in `src/main/resources/db/migration/` (`V1__baseline_inicial.sql` through `V6__...`). The in-app Flyway runner (`FlywayMigrationRunner`) is disabled by default — enable via `-Dtec.flyway.enabled=true` (see [`docs/flyway.md`](docs/flyway.md) for the full flag set and safe rollout sequence). `V1`/`V2` are for a clean database only — never run them against an existing one. All user-facing text lives in `messages_es.properties` (escaped Unicode for accented/special characters).

Document/report storage path resolves through `Constantes` (JVM prop `rpm.files.path` → `messages_es.properties` key → WildFly data dir → tmpdir fallback) — never hardcode absolute paths for generated PDFs/Excel.

### Login flow

`login.xhtml` → `LoginController.login()` → `request.login(...)` (Elytron/`TribunalSecurityDomain`) → on success, `UsuarioService.cargarContextoUsuarioAutenticado(...)` loads session context (user, roles, menu, timeout, access audit) → routing to `dashboard.jsf` or `actaE.jsf` (role `SITEC-Presidente-mesa`) for permanent users, `cambioClave.jsf` for non-permanent ones. Never validate passwords manually in controllers or log them; use `PasswordService.hashBcrypt(...)` for any new password flow.

## Sensitive data

`db_tribunal.backup` and `V2__datos_iniciales.sql` contain real personal data and BCrypt password hashes — treat the repo and built WAR as restricted artifacts.
