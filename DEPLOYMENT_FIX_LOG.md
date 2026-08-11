# Azure App Service Deployment — Diagnosis & Fix Log

Date: 2026-08-11
App: `antarv1` (Azure App Service, Linux, Java 17 SE), resource group `antar-rg`, Central India.

## Context

The Spring Boot app (`antar-api`) was not running on Azure App Service. The
CI/CD pipeline (`.github/workflows/deploy.yml`) deploys a renamed `app.jar`
via `azure/webapps-deploy@v3` using a repo secret (`AZURE_WEBAPP_PUBLISH_PROFILE`)
and a repo variable (`AZURE_WEBAPP_NAME`).

## Investigation

1. **Repo state** — confirmed `main` was fully committed and pushed, remote
   pointed at `baratwajsucharitha-creator/Antar`, and the git author matched.
2. **Local build** — `mvn clean package` produced a correct executable fat jar
   (`spring-boot-maven-plugin` repackage goal bound correctly, manifest showed
   `Main-Class: JarLauncher`, `Start-Class: AntarApplication`). Ran locally
   with `java -jar` and confirmed Spring, Flyway, and Tomcat started cleanly
   against the local H2 profile.
3. **Pipeline history** — `gh run list` showed the last `deploy` run failing
   with `403 Site Disabled`. The failing run's log showed the pipeline
   deploying to app name `projectantar`.
4. **Azure inventory** — `az webapp list` showed `projectantar` **no longer
   exists**. The only app service in `antar-rg` was `antarv1`, running on
   plan `ASP-antarrg-9c8e` with SKU **B1 Basic (paid)**, not the Free F1 tier
   the project's hosting plan (`Antar Solution.md`) assumed.
5. **App config on `antarv1`** was hand-set and diverged from the intended
   design:
   - `SPRING_PROFILES_ACTIVE=prod` — there is no `application-prod.yml`, so
     the app silently fell back to the default profile.
   - `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` were set directly as
     **plaintext** app settings (password `Chathu1@` exposed in the
     Portal/CLI), bypassing the Key Vault design entirely.
   - No `ANTAR_DB_URL` setting existed.
   - No managed identity was assigned to the app, so even a correct Key
     Vault reference could not have resolved.
6. **Startup logs** (`az webapp log tail` / log download) showed the real
   crash: Flyway/Hikari trying to open a `jdbc:sqlserver://...` URL using
   the **H2 driver**, because the default profile's
   `driver-class-name: org.h2.Driver` was still active (profile `prod`
   didn't exist) while `SPRING_DATASOURCE_URL` env var overrode only the URL.

## Fixes applied

1. **Repointed CI/CD at the correct app** — `AZURE_WEBAPP_NAME` set to
   `antarv1`; refreshed the `AZURE_WEBAPP_PUBLISH_PROFILE` repo secret with
   `antarv1`'s current publish profile (the old one was stale/possibly tied
   to the deleted `projectantar`).
2. **Enabled managed identity** on `antarv1`
   (`az webapp identity assign`) and granted it the **Key Vault Secrets
   User** RBAC role on `antar-kv-sb2026` (the vault uses RBAC authorization,
   not access policies — `az role assignment create` had a client-side CLI
   bug on this machine returning `MissingSubscription`, so the role
   assignment was created via a direct ARM REST call instead).
3. **Removed the plaintext datasource app settings** and set:
   ```
   SPRING_PROFILES_ACTIVE = azure
   ANTAR_DB_URL = @Microsoft.KeyVault(SecretUri=https://antar-kv-sb2026.vault.azure.net/secrets/antar-db-url/)
   ```
4. **Found and fixed the actual root cause of the startup crash** (code
   change, not infra): `application.yml` (base profile, always loaded) set
   `spring.datasource.username: sa` / `password: ""` for local H2 use.
   `application-azure.yml` only overrode `url` and `driver-class-name`, never
   `username`/`password`. Spring's Hikari auto-configuration prefers explicit
   `spring.datasource.username`/`password` over credentials embedded in the
   JDBC URL, so the app always tried to log in as `sa` with a blank password
   regardless of what `ANTAR_DB_URL` contained. Fixed by blanking
   `username`/`password` in `application-azure.yml`:
   ```yaml
   spring:
     datasource:
       url: ${ANTAR_DB_URL}
       username:
       password:
       driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
   ```
   Committed as `816bbef`, pushed to `main`, built and deployed through the
   pipeline — first successful end-to-end pipeline deploy.
5. **Verified live**: Flyway applied `V1__create policy` against the real
   `antar-db`, app fully started ("Started AntarApplication in ~113s" —
   mostly SQL free-tier auto-pause wake time), `/actuator/health` returned
   `200 {"status":"UP"}`.
6. **Cost fix** — downgraded the App Service plan from paid B1 Basic to
   **F1 Free** (`az appservice plan update --sku F1`), after disabling
   `alwaysOn` (required, since F1 doesn't support it). Verified the app still
   started and responded healthy afterward. Confirmed no B1-only features
   (deployment slots, custom domain/SSL, backups) were in use, so this is a
   pure cost saving with no functional loss — cold starts after ~20 minutes
   of idle are now expected, on top of the SQL free tier's own auto-pause.
7. **Security cleanup**:
   - Rotated the exposed SQL admin password (`Chathu1@` → new random
     password) via `az sql server update --admin-password`, updated the
     `antar-db-url` Key Vault secret to match, restarted the app, and
     verified it reconnected successfully with the new credential.
   - Deleted a stray Key Vault Secrets User role assignment left over from
     the deleted `projectantar` app's managed identity.

## Result

- `antarv1` is running on Azure App Service, Free F1 tier, Linux, Java 17.
- CI/CD (`build.yml` → `deploy.yml`) deploys to the correct app on every
  push to `main`.
- DB credentials are sourced from Key Vault via managed identity — no
  secrets in app settings or source control.
- `/actuator/health` returns `200 {"status":"UP"}`.

## Still open / good next steps

- Monitor the first few cold starts under F1 + SQL auto-pause to confirm
  the combined wake time stays within acceptable bounds for real usage.
- Consider adding a `WEBSITE_HTTPLOGGING_RETENTION_DAYS` review and log
  retention policy now that the app is expected to run long-term.
