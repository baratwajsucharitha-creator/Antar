# Antar Solution — Hosting Scorecard & Deployment Plan

Applying the lessons from `Agent_Hosting_Solution.docx` (the architect's agent-hosting decision framework) to the Antar project.

## Context

The reference document is scoped to hosting **LLM agents** (interface → orchestration → reasoning/LLM → retrieval → tools → compute → guardrails). Antar is **not** an LLM agent — `GapEngine` is a deterministic, rule-based deduction engine with no LLM calls. So the framework is mapped onto **hosting Antar's Spring Boot API**, which is the actual analogous decision here. Only two of the doc's seven layers are in play — **Interface** and **Compute host** — since there's no reasoning/retrieval/tools layer to host or price.

## Antar — hosting scorecard (§10 intake, applied)

| Lens | Antar's answer |
|---|---|
| What it does | Spring Boot API + Thymeleaf UI: clause-by-clause deduction ledger over JPA/SQL data |
| Interface | Web UI + REST API, low traffic (personal/demo use) |
| Data sources & sensitivity | Own Azure SQL DB via Flyway migrations; no regulated/external data |
| "Permitted-LLM" equivalent | N/A — no LLM in the loop, so Layers 3–5 (reasoning/retrieval/tools) of the doc's framework don't apply at all |
| Actions | Read/write to its own DB — reversible, no external system writes |
| Volume | Low (dev/demo scale) |
| Latency need | Live, interactive (<2s), not batch |
| Guardrails required | Standard web hygiene (no secrets in code, managed identity for DB) — already done in `application-azure.yml` |
| HITL | N/A — not agentic, no approval gates needed |
| Archetype (from §4) | **C — Framework + your own compute** (you own the code/host, deploy via CI/CD) — the doc's Copilot/Bedrock/Foundry archetypes (A/B) don't apply since there's no LLM runtime to host |

## Best case vs Moderate case (§6 cost-block method)

| | **Best case** (most capable/managed) | **Moderate case** (cost-efficient) — ✅ what you're set up for |
|---|---|---|
| Archetype | A-equivalent: Azure App Service **Premium (P1v3, HA, always-on)** + Azure SQL Standard | C: App Service **F1 Free** + Azure SQL free/serverless (auto-pause) |
| Compute | ~$300–350/mo (per §6.1 App Service P1v3×2) | **$0/mo** (F1 free tier, 60 CPU-min/day cap, sleeps when idle) |
| Database | Azure SQL Standard tier, always-on, ~$5–15/mo+ | Azure SQL free/serverless, auto-pause after idle — **$0/mo** (with wake latency, which the Hikari config already tolerates: `connection-timeout: 60000`) |
| Guardrails | Managed identity + Key Vault + App Service auth | Managed identity + Key Vault (already wired, no extra cost either way) |
| Deploy speed | Days (same CI/CD either way) | Days — already wired via `deploy.yml` |
| Fit for this project | Overkill — no production traffic/SLA need | **Matches the brief**: personal Azure free-tier project, demo/dev scale |

## Final recommendation: Moderate case

Free F1 App Service + free-tier Azure SQL is the right call — it costs $0 in tooling/access (per the doc's own rule of "cheapest option that passes the eval"), and the codebase is already built for it (cold-start-tolerant Hikari settings, managed-identity DB connection, CI/CD deploy workflow). Upgrading to the Best case only makes sense later if there's real traffic and a need for always-on/no-cold-start behavior.

## Next steps to deploy

1. **Create the resources** — in Azure Portal, use **"Web App + Database"**: Runtime Java 17/Linux, App Service plan **F1 Free**, SQL Database on the free/serverless tier.
2. **Confirm the DB connection variable** — check what Azure named the connection string/app setting; the app expects `ANTAR_DB_URL` (`application-azure.yml`), so rename or add it if Azure used a different key.
3. **Set `SPRING_PROFILES_ACTIVE=azure`** in App Service → Configuration → Application settings.
4. **Wire GitHub Actions**: repo secret `AZURE_WEBAPP_PUBLISH_PROFILE` (download from App Service → Overview), repo variable `AZURE_WEBAPP_NAME`.
5. **Push to `main`** — build → deploy workflows fire automatically.
6. **Verify**: hit `https://<app-name>.azurewebsites.net/actuator/health`, then the UI, and check the SQL free-tier's cold-start delay is acceptable (~a few seconds after idle, per the Hikari `connection-timeout`).