#!/bin/bash
echo "🐳 STARTING DATA IMPORT..."

mongoimport --host localhost --db Chemo --collection users --drop --file init/users.json --jsonArray
mongoimport --host localhost --db Chemo --collection medicine --drop --file init/medicine.json --jsonArray
mongoimport --host localhost --db Chemo --collection patient --drop --file init/patient.json --jsonArray
mongoimport --host localhost --db Chemo --collection planner --drop --file init/planner.json --jsonArray

echo "✅ IMPORT COMPLETED."