#!/bin/sh

DB_NAME="ledger"
DB_USER="ist190770"
DB_PWD="ist190770"

CMD="
dropdb --if-exists ${DB_NAME};
createdb ${DB_NAME};
echo \"
  DROP ROLE IF EXISTS ${DB_USER};
  CREATE ROLE ${DB_USER} LOGIN SUPERUSER PASSWORD '${DB_PWD}';
\" | psql ${DB_NAME};
"

sudo service postgresql start
echo "${CMD}" | sudo su -l postgres
