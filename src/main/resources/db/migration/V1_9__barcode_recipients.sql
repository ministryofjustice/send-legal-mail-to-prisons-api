CREATE TABLE barcode_recipients(
  id serial NOT NULL constraint barcode_recipients_pk PRIMARY KEY,
  barcode varchar(12) constraint barcode_recipients_barcode_unique UNIQUE REFERENCES barcodes(code),
  name varchar(64) NOT NULL,
  prison_code varchar(3) NOT NULL,
  dob date,
  prison_number varchar(7)
);
