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
constraint on character set at all. A future V4 should widen both to
`NVARCHAR` for the same reason. Not done as part of this catalogue work
because it touches the existing `policy` table, which is out of scope for
the insurer/product/version catalogue - but it's the same bug, just in an
older table, and should not be forgotten.
