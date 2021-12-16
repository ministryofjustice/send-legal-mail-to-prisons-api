CREATE TABLE cjsm_directory (
  id serial NOT NULL constraint cjsm_directory_pk PRIMARY KEY,
  secure_email varchar(320) NOT NULL,
  first_name varchar(50),
  last_name varchar(50),
  organisation varchar(100),
  town_city varchar(50),
  business_type varchar(50)
);

CREATE UNIQUE INDEX ON cjsm_directory (lower(secure_email))