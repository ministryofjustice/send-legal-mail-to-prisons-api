CREATE TABLE barcode_events (
  id serial NOT NULL constraint barcode_events_pk PRIMARY KEY,
  barcode varchar(12) references barcodes(code),
  user_id varchar(320),
  status varchar(10),
  date_time timestamp with time zone default CURRENT_TIMESTAMP
);