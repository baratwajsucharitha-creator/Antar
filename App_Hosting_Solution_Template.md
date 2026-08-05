# App Hosting Solution — Reusable Template

Adapted from `Agent_Hosting_Solution.docx` (the architect's agent-hosting decision framework) for **conventional applications** — i.e. no LLM reasoning loop. Use this as the starting skeleton whenever a new non-agent app/service needs a hosting decision. See `Antar Solution.md` for a worked example.

## When to use this vs the agent-hosting doc

Use this template if the thing being hosted has **no LLM in the loop** — no reasoning/orchestration step, no token spend, no retrieval/tools layer. If the workload plans → calls tools → observes → iterates with a model driving it, use the original agent-hosting framework instead.

## 1. Context

One paragraph: what the app does, who uses it, and why the agent-hosting doc's LLM-specific sections don't apply (no reasoning loop, no token spend).

## 2. The layers (collapsed from 7 to ~4)

The agent doc's seven layers exist because an agent has a reasoning loop and token spend. A conventional app only carries cost/risk in these:

| Layer | What it does | Where the money / risk sits |
|---|---|---|
| Interface | Web UI / API / scheduled job | Usually cheap. Risk = access control. |
| Compute host | Where the app runs | Recurring infra. Scale-to-zero vs always-on. |
| Data tier | DB, migrations, backup/HA | Managed DB tier + storage + backup. |
| Cross-cutting | Identity, secrets, TLS, logging | Small individually; mandatory for production. |

No LLM tier, no vector store, no token line, no connector-licence line.

## 3. The decision scorecard (10 lenses, re-cast)

| Lens | The question to answer | For this app |
|---|---|---|
| 1. Options | Which archetypes (§4) can host this? | |
| 2. Cost | Monthly compute + data-tier cost at expected volume | |
| 3. Speed of deployment | Days to production once approved | |
| 4. Guardrails | Conventional appsec: auth, input validation, secrets, dependency scanning | |
| 5. Efficiency | Right-sized tier + scale-to-zero where traffic allows | |
| 6. Feasibility | Do the DB, network, and dependencies exist? | |
| 7. Human-in-the-loop | Only relevant if the app has an irreversible write step a human should approve — mark N/A otherwise | |
| 8. Data policy | Any residency/compliance constraint on where data or the DB can live? | |
| 9. Platform + host | Named runtime, named DB tier | |
| 10. KT / handover | Runbook, diagram, cost owner (see §6) | |

## 4. The hosting archetype spectrum

| Archetype | Examples | Speed vs control | Where money goes |
|---|---|---|---|
| A. Fully managed PaaS | Azure App Service, AWS Elastic Beanstalk | Fastest / least control | Platform tier fee |
| B. Serverless compute | Azure Functions, AWS Lambda | Fast, best for low/bursty traffic | Pay-per-execution; near-$0 idle |
| C. Containers you orchestrate | Azure Container Apps, Fargate, AKS/EKS | Medium / high control | Your compute + your ops |
| D. VM / IaaS | Plain VM, on-prem VMware | Slow / high control | Full OS patching burden |
| E. On-prem / self-hosted | Owned hardware | Slowest / total control | Capex-heavy, near-zero recurring |

## 5. Best case vs Moderate case

| | Best case (most capable/managed) | Moderate case (cost-efficient) |
|---|---|---|
| Archetype | | |
| Compute | | |
| Database | | |
| Guardrails | | |
| Deploy speed | | |
| Fit for this project | | |

## 6. Final recommendation

One paragraph: which case, and why — tie back to the scorecard rows that decided it (usually cost + fit, per the "cheapest option that passes the eval" rule).

## 7. Next steps to deploy

Numbered, concrete: resource creation → config/secrets wiring → CI/CD wiring → push → verify.

## 8. KT & handover checklist

- Architecture diagram (the layers in §2, plus external dependencies)
- Runbook (deploy, roll back, rotate secrets, what to do when the host or DB is down)
- Cost sheet & owner (the §2 line items, current spend, named owner)
- Decision record (the completed §3/§5 scorecard — why this archetype/host was chosen)