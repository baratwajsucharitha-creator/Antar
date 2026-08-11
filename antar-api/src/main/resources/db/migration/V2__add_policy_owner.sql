-- Ownership. Until now any caller could read any policy by guessing an integer id.
-- owner_id holds the stable subject identifier from the identity provider,
-- never the display name or email, both of which can change.

ALTER TABLE policy ADD owner_id VARCHAR(100) NOT NULL DEFAULT 'legacy-unowned';

CREATE INDEX ix_policy_owner ON policy (owner_id);
