# Antar — Health Insurance Coverage Gap Analysis

> *अंतर — "the gap"*

**Indian families discover the limits of their health cover at the hospital billing counter, not before.**

A ₹5 lakh policy does not mean ₹5 lakh of protection. After room-rent proportionate deduction, disease sub-limits, co-pay and non-medical exclusions, a ₹3 lakh hospital bill on a ₹5 lakh policy can still leave ₹1.24 lakh on the family.

Antar computes that number — before the hospitalisation, not after — and explains every rupee of it.

---

## Status

**Week 1 — domain model and rules engine.** The calculation core is complete and tested. Persistence, REST API and the web UI follow in Weeks 2–3.

| Module | State |
|---|---|
| `antar-engine` | Domain model + `GapEngine`, 6 passing tests |
| `antar-api` | Week 2 — Spring Boot 3, JPA, REST |
| Web UI | Week 3 — Thymeleaf |
| Azure deployment | Week 3 — App Service, APIM, Key Vault |

---

## The worked example

| Input | Value |
|---|---|
| Sum insured (family floater) | ₹5,00,000 |
| Room rent limit (1% of SI/day) | ₹5,000/day |
| Room actually taken | ₹8,000/day × 5 days |
| Total hospital bill | ₹3,00,000 |
| Non-medical items (IRDAI Annexure I) | ₹18,000 |
| Pharmacy + implants | ₹52,000 |
| Associated charges | ₹1,90,000 |
| Co-pay | 10% |

```
roomProportion   = 5,000 / 8,000              = 0.625
eligibleRoom     = 5 × 5,000                  = ₹25,000
associated       = 1,90,000 × 0.625           = ₹1,18,750
pharmacy/implant = not proportioned           = ₹52,000
                                                ─────────
subtotal                                        ₹1,95,750
after co-pay     = × 0.90                       ₹1,76,175
payout           = min(1,76,175 , 5,00,000)     ₹1,76,175

GAP              = 3,00,000 − 1,76,175          ₹1,23,825   (41.3% of the bill)
```

The family believed they had ₹5 lakh of cover. On a ₹3 lakh bill they paid ₹1.24 lakh themselves — and most of that traces back to a single decision at admission: an ₹8,000 room instead of a ₹5,000 one.

### The deduction trace for that scenario

| Clause | Amount removed |
|---|---|
| `NON_PAYABLE_ITEMS` | ₹18,000 |
| `ROOM_RENT_CAP` | ₹15,000 |
| `ROOM_RENT_PROPORTION` | ₹71,250 |
| `CO_PAY` | ₹19,575 |
| **Total** | **₹1,23,825** |

---

## Design principles

**Every rupee is traceable to a clause.** Each deduction the engine applies emits a `DeductionTrace` naming the policy clause that authorised it. A test asserts that the sum of all traces equals the computed gap — if the engine ever removes money without explaining it, the build fails.

**`BigDecimal` everywhere, `double` nowhere.** The `Money` type enforces scale 2 and `HALF_UP` in its compact constructor, so an unrounded value cannot be constructed. The one exception is `ratioTo()`, which returns scale 6 — the room proportion factor must not be rounded to two places before multiplying a large amount.

**The engine has no framework dependency.** `antar-engine` declares test-scope dependencies only. It is runnable, testable and reviewable without Spring. When `antar-api` arrives in Week 2, the dependency arrow points one way.

**Rule ordering is modelled, not hardcoded.** Insurers genuinely differ on whether co-pay applies before or after the room-rent proportion, and the order changes the final number. `CoPayOrder` makes that variation a first-class part of the model rather than a hidden assumption.

**Nothing probabilistic touches the calculation.** Document parsing will use an LLM (Week 6). Computation never does. Same inputs, same rupee figure, every time.

**Analysis only, never advice.** Antar computes a gap and explains it. It does not recommend insurance products — that requires an IRDAI licence.

---

## Domain model

| Type | Purpose |
|---|---|
| `Money` | BigDecimal wrapper, scale 2, HALF_UP, non-negative |
| `BillCategory` | ROOM, ASSOCIATED, PHARMACY, IMPLANT, NON_PAYABLE — behaviour, not labels |
| `BillLine` / `HospitalBill` | The bill as the hospital raised it |
| `RoomRentRule` | Eligibility as a % of sum insured per day |
| `SubLimit` | Per-procedure caps |
| `CoPayRule` / `CoPayOrder` | Co-pay percentage and where it sits in the pipeline |
| `PolicyTerms` / `Policy` | The clauses, and the policy's current remaining cover |
| `DeductionTrace` | Why a rupee was removed, and under which clause |
| `GapResult` | Payout, gap, gap as % of bill, full trace |

---

## The calculation pipeline

```
1. nonPayables        → removed entirely (IRDAI Annexure I)
2. roomCap            → min(actual, eligibleRoomRent × days)
3. roomProportion     → eligible / actual, applied to ASSOCIATED charges
4. nonProportioned    → pharmacy and implants pass through untouched
5. coPay              → × (1 − coPayPercent)
6. subLimit           → min(payable, procedureCap)
7. sumInsured         → min(payable, remainingSumInsured)
8. gap                → totalBill − payout
```

---

## Tech stack

**Now:** Java 17 · Maven (multi-module) · JUnit 5 · AssertJ

**Weeks 2–8:** Spring Boot 3 · Spring Data JPA · Azure SQL · Flyway · springdoc OpenAPI · Thymeleaf · Docker · Azure App Service · Azure API Management · Azure Key Vault (managed identity) · GitHub Actions · Application Insights · Python + pdfplumber (policy PDF extraction)

---

## Running it

Requires JDK 17+ and Maven 3.8+.

```bash
git clone https://github.com/baratwajsucharitha-creator/Capstone_Project.git antar
cd antar
mvn clean install
```

Run the domain model smoke test:

```bash
cd antar-engine
mvn exec:java
```

Run the test suite:

```bash
mvn test
```

### Current test coverage

| Scenario | Expected payout | Expected gap |
|---|---|---|
| Room within limit, no co-pay | ₹2,67,000 | ₹18,000 |
| Room above limit + 10% co-pay | ₹1,76,175 | ₹1,23,825 |
| Pharmacy survives the proportion | ₹1,95,750 | — |
| Cataract sub-limit applied | ₹40,000 | ₹40,000 |
| Sum insured exhausted | ₹1,00,000 | — |
| Deductions sum exactly to the gap | ✓ | — |

---

## Repository layout

```
antar/
├── pom.xml                      parent (packaging: pom)
└── antar-engine/
    ├── pom.xml                  test-scope dependencies ONLY
    └── src/
        ├── main/java/com/antar/engine/
        │   ├── GapEngine.java
        │   ├── DomainModelDemo.java
        │   └── model/           11 domain records
        └── test/java/com/antar/engine/
            └── GapEngineTest.java
```

---

## Known gaps

- `CoPayOrder.BEFORE_PROPORTION` is modelled but not yet honoured by the engine — Week 5, alongside versioned per-insurer rule sets.
- Waiting periods and no-claim bonus are not yet part of the calculation.
- Multi-policy stacking (base + top-up with deductible) is Week 7.

---

Built by [Sucharitha Baratwaj](https://github.com/baratwajsucharitha-creator).
