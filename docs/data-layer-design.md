# Data layer — implementation notes

Running log of decisions and follow-ups made while implementing
`Antar_Data_Layer_Design_v2.md` steps 1-2, kept separate from that doc since
it's the authoritative spec and shouldn't be edited to record after-the-fact
implementation detail. Entries are added as they come up, not backfilled.

## V3: NVARCHAR for transcribed free-text columns

`insurer.legal_name`/`display_name`, `insurance_product.product_name`/`notes`,
and `data_import_run.run_by` are `NVARCHAR`, not `VARCHAR`. Real SQL Server
`VARCHAR` is codepage-limited and non-Unicode - it silently substitutes any
character outside that codepage with `?` instead of erroring. Every value in
these columns is a human-transcribed name (an insurer's legal name, a
product's marketing name, a free-text note), so the risk isn't hypothetical:
one accented character or currency symbol in a transcribed name would be
quietly corrupted with no failure to notice - exactly the "wrong but
plausible" failure mode this project treats as worse than an outright error.
Codes, enums, UINs and URLs (`irdai_registration_no`, `uin`,
`verification_status`, `source_url`, etc.) stay `VARCHAR` - they're
constrained to ASCII by IRDAI/URL syntax regardless of storage type.

**Follow-up, not yet done:** `policy.insurer_name` and `policy.product_name`
(from V1) are plain `VARCHAR` and carry the exact same risk - they're
free-text fields a user types directly into the policy form, with no
constraint on character set at all. A future migration should widen both to
`NVARCHAR` for the same reason (V4 ended up being used for something else -
see below - so this is now V5+). Not done as part of this catalogue work
because it touches the existing `policy` table, which is out of scope for
the insurer/product/version catalogue - but it's the same bug, just in an
older table, and should not be forgotten.

## V4: irdai_registration_no nullable - the filtered unique index is still owed

V4 dropped `insurer`'s `UNIQUE (irdai_registration_no)` constraint (needed
because the seed data - 19 real, publicly-known insurer names - has no
verified registration numbers at all) and replaced it with a plain,
non-unique index. Real duplicate prevention for insurers that *do* have a
registration number is enforced only at the application layer
(`CatalogueImportService` looks up the existing row before inserting) - there
is no database constraint stopping two rows from sharing a real registration
number if something outside the import path ever inserts one.

**This has not been a problem in practice yet only because it doesn't need to
be**: all 19 seeded rows have a blank `irdai_registration_no`, so there is
currently nothing for a uniqueness constraint to enforce. That will stop
being true the moment someone populates real registration numbers from
IRDAI's list (design-doc step 3).

**Required follow-up, before that happens:** add a real filtered/partial
unique index - `CREATE UNIQUE INDEX ux_insurer_registration_no ON insurer
(irdai_registration_no) WHERE irdai_registration_no IS NOT NULL` - which
real SQL Server supports natively but H2's `MSSQLServer` compatibility mode
cannot even parse (confirmed directly against the H2 driver while writing
V4; this is why V4 uses a plain index instead of solving it properly).

**The clean solution, not yet implemented:** Flyway's `{vendor}` placeholder
in `spring.flyway.locations`. Instead of one shared `classpath:db/migration`
folder, migrations can live under vendor-specific subfolders
(`db/migration/{vendor}`, where `{vendor}` resolves to `h2` or `sqlserver`
automatically from the JDBC URL) alongside a shared vendor-neutral folder for
everything both engines already agree on. That lets H2 keep the plain index
it can actually parse while the real migration that reaches Azure gets the
correct filtered unique index - the two engines are allowed to diverge
exactly where they must, instead of the whole migration being limited to
the lowest common syntax denominator. This project already depends on
`flyway-sqlserver` for exactly this kind of vendor-specific behavior but
hasn't split `spring.flyway.locations` out to use it yet.
