#!/usr/bin/env bash
# Loads scripts/seed-mock-data.sql into the local IMS Postgres container.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/seed-mock-data.sql"
CONTAINER="${IMS_PG_CONTAINER:-my-postgres}"
DB_USER="${IMS_PG_USER:-anikettcodes}"
DB_NAME="${IMS_PG_DB:-ims}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "SQL file not found: $SQL_FILE" >&2
  exit 1
fi

if [[ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null || true)" != "true" ]]; then
  echo "Container '$CONTAINER' is not running. Start it with: docker compose up -d (from ims/)" >&2
  exit 1
fi

echo "Seeding mock data into ${DB_NAME} on ${CONTAINER} ..."
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" < "$SQL_FILE"
echo "Done. GET http://localhost:8080/api/v1/categories should now return seeded categories."
