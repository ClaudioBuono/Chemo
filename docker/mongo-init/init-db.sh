#!/bin/bash
echo "🐳 STARTING DATA IMPORT..."

mongoimport --host localhost --db Chemo --collection user --drop --file /data/seed/init/user.json --jsonArray
mongoimport --host localhost --db Chemo --collection medicine --drop --file /data/seed/init/medicine.json --jsonArray
mongoimport --host localhost --db Chemo --collection patient --drop --file /data/seed/init/patient.json --jsonArray
mongoimport --host localhost --db Chemo --collection planner --drop --file /data/seed/init/planner.json --jsonArray

echo "✅ IMPORT COMPLETED."