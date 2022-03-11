ALTER TABLE barcode_events ADD COLUMN ip_address varchar(50);

UPDATE barcode_events SET ip_address = '';

ALTER TABLE barcode_events ALTER COLUMN ip_address SET NOT NULL;
