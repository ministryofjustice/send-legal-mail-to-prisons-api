ALTER TABLE contacts RENAME COLUMN prisonCode TO prison_code;
ALTER TABLE contacts RENAME COLUMN prisonNumber TO prison_number;
CREATE UNIQUE INDEX contacts_uni_idx_owner_prison_number ON contacts (owner, prison_number) WHERE prison_number IS NOT NULL
