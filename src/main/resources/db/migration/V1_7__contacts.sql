CREATE TABLE contacts(
  id serial NOT NULL constraint contacts_pk PRIMARY KEY,
  owner varchar(320) NOT NULL,
  name varchar(64) NOT NULL,
  prisonCode varchar(3) NOT NULL,
  dob date,
  prisonNumber varchar(7),
  created timestamp NOT NULL,
  updated timestamp NOT NULL
);
