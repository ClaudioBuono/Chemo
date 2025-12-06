#!/bin/bash
DB_NAME=${1:-Chemo_TEST}
CONTAINER_NAME="chemo_mongo"

export MSYS_NO_PATHCONV=1

echo "🧹 RESET DATABASE: $DB_NAME..."

# NOTA IL PERCORSO AGGIORNATO: /data/seed/init/...
docker exec $CONTAINER_NAME mongoimport --db $DB_NAME --collection user --drop --file /data/seed/init/user.json --jsonArray
docker exec $CONTAINER_NAME mongoimport --db $DB_NAME --collection medicine --drop --file /data/seed/init/medicine.json --jsonArray
docker exec $CONTAINER_NAME mongoimport --db $DB_NAME --collection patient --drop --file /data/seed/init/patient.json --jsonArray
docker exec $CONTAINER_NAME mongoimport --db $DB_NAME --collection planner --drop --file /data/seed/init/planner.json --jsonArray

echo "✅ FATTO."