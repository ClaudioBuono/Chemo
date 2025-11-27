@echo off
echo [INFO] Inizio importazione dati in Chemo

:: Ci spostiamo nella cartella dove si trova questo script (es. .../TuoProgetto/scripts)
cd /d "%~dp0"

:: Assicurati che mongoimport sia nel PATH
mongoimport --db Chemo --collection user --drop --file seed-data/init/user.json --jsonArray
mongoimport --db Chemo --collection medicine --drop --file seed-data/init/medicine.json --jsonArray
mongoimport --db Chemo --collection patient --drop --file seed-data/init/patient.json --jsonArray
mongoimport --db Chemo --collection planner --drop, --file seed-data/init/planner.json --jsonArray

:: Verifica errori
IF %ERRORLEVEL% NEQ 0 (
    echo [ERRORE] Impossibile trovare i file o connettersi a Mongo.
    echo Controlla che i file JSON siano in: %~dp0seed-data\init\
    pause
    exit /b
)

echo [SUCCESS] Importazione terminata.
pause