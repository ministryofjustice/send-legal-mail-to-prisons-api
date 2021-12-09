ALTER TABLE barcode_events ADD COLUMN location varchar(45);

UPDATE barcode_events SET location = '';

ALTER TABLE barcode_events ALTER COLUMN location SET NOT NULL;
