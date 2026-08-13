# Antar catalogue data — how to populate it

These CSVs are the single source of truth for the health-insurer catalogue
(`insurer`, `insurance_product`, `product_version`). Edit them by hand, commit
the diff, and the import runner upserts them into the database. Nothing here
is scraped or auto-fetched — IRDAI's site disallows automated access
(`robots.txt`, verified 12 Aug 2026), so every row is transcribed by a person
reading an IRDAI page.

## The example rows

Each CSV ships with **at most 2 rows** whose `source` column is the literal
string `EXAMPLE - REPLACE`. Those rows exist only to show the column shapes.
The import runner **skips any row with that exact source value** — they will
never be written to the database — so they are safe to leave in place until
you replace them, but they teach you nothing about real insurers. Delete them
once you've added real rows, or leave them; either way they're inert.

**No row in this repo may contain a fabricated IRDAI registration number or
UIN.** If you don't have a confirmed value, leave the cell blank rather than
guess — a blank is `NULL` after import (unknown), which is correct; an
invented number is wrong and looks correct, which is worse.

`insurer.irdai_registration_no` is nullable (V4) for exactly this reason: the
current seed data is publicly known insurer/product names with registration
numbers and UINs still pending verification, and that's an honest, normal
state for this table to be in, not a placeholder to feel bad about.

## Natural keys when the registration number is blank

Every import (insurers, then products, then versions) resolves its parent
row the same way: **prefer the registration number when the row has one - it's
authoritative - otherwise fall back to an exact match on the insurer's
`display_name`.** So `products.csv` and `product-versions.csv` both carry an
`insurer_registration_no` column *and* an `insurer_display_name` column; fill
in whichever one you actually have (registration number if you have it,
display name otherwise), matching `insurers.csv`'s `display_name` for that
insurer exactly (case-sensitive, exact string match - no fuzzy matching).

## Where to get each value

### `insurers.csv`

Source: IRDAI's List of Registered Insurance Entities.
- `irdai.gov.in` → Regulation → Registration → List of Registered Insurers
- Mirror: `policyholder.gov.in/registered-insurers-re-insurers`

**Verify directly against IRDAI, not a comparison/aggregator site.** Four
aggregators gave four different counts of standalone health insurers (5, 6,
7, 8) on the same day (12 Aug 2026) — they are not reliable even for a simple
count.

Scope: **standalone health insurers (SAHI) and general insurers that write
health business only.** Do not add life insurers — they don't pay hospital
bills, so they're out of scope for Antar entirely.

| Column | Expects |
|---|---|
| `irdai_registration_no` | The registration number exactly as printed on the IRDAI list. Never invented. |
| `legal_name` | Full registered legal name. |
| `display_name` | The name people actually recognise (e.g. "Star Health and Allied Insurance" vs. the longer legal name). |
| `insurer_type` | `STANDALONE_HEALTH` or `GENERAL` — nothing else. |
| `is_active` | `TRUE` unless IRDAI shows the licence as ceased/withdrawn. |
| `ceased_date` | Blank unless the insurer has formally exited. |
| `succeeded_by_insurer_registration_no` | If this insurer merged into another (e.g. Apollo Munich → HDFC ERGO), the *successor's* `irdai_registration_no`. Blank otherwise. Must reference a registration number that also appears as a row in this file. |
| `source` | The exact page you read this from (URL). Never `EXAMPLE - REPLACE` for a real row. |
| `last_verified_date` | The date (`YYYY-MM-DD`) you read that page. |

### `products.csv`

Source: IRDAI health product clearance lists, by financial year (same IRDAI
site, "List of Products" / "Approved Products" sections).

| Column | Expects |
|---|---|
| `insurer_registration_no` | The parent insurer's registration number, if known. Blank if not - see "Natural keys" above. |
| `insurer_display_name` | The parent insurer's `display_name`, exactly as it appears in `insurers.csv`. Required when `insurer_registration_no` is blank. |
| `product_name` | The product name as cleared, without the version suffix. |
| `product_category` | One of `INDEMNITY`, `TOP_UP`, `SUPER_TOP_UP`, `FIXED_BENEFIT`, `CRITICAL_ILLNESS`, `PERSONAL_ACCIDENT`, `GOVERNMENT_SCHEME`. |
| `segment` | `RETAIL` or `GROUP`. An insurer can sell the same product name as both — that's why the natural key includes this column. |
| `availability_status` | `OPEN_TO_NEW`, `CLOSED_TO_NEW`, `WITHDRAWN`, or `UNKNOWN`. **Leave `UNKNOWN` unless you have a specific source saying otherwise** — insurer product pages rarely publish closure dates, so `UNKNOWN` is usually the honest answer, not a placeholder to fix later. |
| `first_cleared_date` | Earliest clearance date you can find, if any. Blank is fine. |
| `notes` | Free text — e.g. "renamed after HDFC ERGO merger". Optional. |
| `source` | The exact page/document you read this from. |
| `last_verified_date` | The date you read it. |

### `product-versions.csv`

Source: the same IRDAI FY clearance lists — each cleared UIN is a version.
The UIN encodes the version number, e.g. `NBHHLIP22156V032122` is version 03.

| Column | Expects |
|---|---|
| `insurer_registration_no` / `insurer_display_name`, `product_name`, `segment` | Together locate the parent row in `products.csv`. Same rule as `products.csv`: registration number if you have it, display name otherwise. Must match exactly. |
| `uin` | The IRDAI Unique Identification Number exactly as published. Never invented — if you can't find it, don't add the row at all. |
| `version_label` | Parsed from the UIN if you can (e.g. `V03`). Optional. |
| `irdai_cleared_date` | The FY clearance date. Optional. |
| `effective_from` / `effective_to` | The window this version was sold new. Leave both blank if unknown — do not guess. `effective_to` blank means "still current," which is a real assertion, not a lazy default, so only leave it blank when that's actually true. |
| `verification_status` | `VERIFIED_IRDAI`, `VERIFIED_INSURER`, or `UNVERIFIED`. Use `UNVERIFIED` unless you've actually cross-checked the UIN against IRDAI or the insurer's own site. |
| `source_url` | The clearance list page or insurer page you read this from. Required. |
| `wording_pdf_url` | Link to the policy wording PDF, if you have one. This is **not** where terms get read into the database — that's a later step (design doc step 5) — this column just saves the link for whoever does that later. |
| `source`, `last_verified_date` | As above. |

`version_terms_template` (the actual room-rent/co-pay/sub-limit numbers) is
**not** built yet. It's design-doc step 5 and requires manually reading policy
wording PDFs one at a time — deliberately out of scope here.

## Refresh cost — read this before you assume an edit is live

**These CSVs are packaged into the `antar-api` jar at build time** (Maven
picks them up from this folder as a resource, at `data/*.csv` inside the
jar). Editing a CSV in this folder does **nothing** to the running app on its
own — it takes effect only after the next `mvn package` and a redeploy.

**Editing a catalogue CSV is a deploy, not a hot file edit.** If you fix a
typo in `insurers.csv` at 4pm, the production app is still serving the old
data until you rebuild and redeploy. Plan accordingly — this is not a live
content-management system, it's version-controlled reference data that ships
with each release.

This file (`README.md`) is documentation only and is **not** packaged into
the jar — only `*.csv` files in this folder are bundled as a build resource.

## Import behaviour

The import runner (`CsvCatalogueImportRunner`) is:

- **On by default for local/default-profile runs** (`application.yml`):
  H2 is in-memory, so without this the catalogue would be empty every time
  you start the app locally.
- **Explicitly off for the azure profile** (`application-azure.yml` pins
  `antar.catalogue.import-on-startup: false` itself - it does not just rely
  on inheriting a default). Azure SQL is a persisted database, not
  in-memory, and Antar runs on an Azure App Service Free (F1) tier that
  cold-starts often. CPU-quota exhaustion has already taken the app down
  once - an import running on every cold start against an already-populated
  table would be pure waste.

Override either way with `-Dantar.catalogue.import-on-startup=true|false`.

- `antar.catalogue.import-force` — default `false`. If a target table
  already has rows, the importer skips that whole dataset and leaves it
  alone, *unless* this is `true`, in which case it re-upserts on the natural
  key regardless of what's already there.

Import is idempotent: re-running it against the same CSVs does not create
duplicate rows. Upsert key per dataset: insurers - registration number if
present, else `display_name`; products - registration number/display name
resolves the parent insurer, then `(insurer_id, product_name, segment)`;
versions - `uin`. Every run writes a `data_import_run` row recording the
dataset, source file, and inserted/updated/skipped counts.
