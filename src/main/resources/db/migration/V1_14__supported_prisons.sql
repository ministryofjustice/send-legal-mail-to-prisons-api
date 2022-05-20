CREATE TABLE supported_prisons(
  code varchar(3) NOT NULL constraint supported_prisons_pk PRIMARY KEY,
  active boolean NOT NULL,
  updated_by varchar(64) NOT NULL,
  updated timestamp NOT NULL
);