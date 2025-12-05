// Select the target database
var db = db.getSiblingDB('Chemo');

// Configuration constants
const TARGET_PATIENTS = 10000;
const BATCH_SIZE = 1000;

// Drop the existing collection before reseeding
db.patient.drop();

print("🔗 Get medicines ID ...");

// Fetch all Medicine IDs into memory first.
const allMeds = db.medicine.find({}, {_id: 1}).toArray();

// Stop the script if no medicines exist to link to
if (allMeds.length === 0) {
    print("❌ ERROR: No medicines found!");
    quit();
}

// Initialize Bulk Operation to optimize write performance
let bulk = db.patient.initializeUnorderedBulkOp();
let count = 0;

// Source data arrays
const names = ["Marco", "Luca", "Giulia", "Anna", "Sofia", "Matteo", "Giovanni"];
const surnames = ["Verdi", "Rossi", "Bianchi", "Esposito", "Romano", "Costa"];
const cities = ["Napoli", "Roma", "Milano", "Torino"];
const conditions = ["Tumore ai polmoni", "Linfoma", "Carcinoma", "Melanoma"];

// Main loop to generate patients
for (let i = 0; i < TARGET_PATIENTS; i++) {

    // Pick a random medicine from the array fetched at the start
    const randomMed = allMeds[Math.floor(Math.random() * allMeds.length)];

    // Create the embedded medicines list
    const medsList = [];
    medsList.push({
        "dose": NumberInt(Math.floor(Math.random() * 500) + 50),
        "medicineId": randomMed._id.toString()
    });

    // Construct the Patient document
    const doc = {
        // Generate a fake unique Tax Code (Codice Fiscale)
        "taxCode": "CF_FAKE_" + i + "X",
        "name": names[i % names.length] + "_" + i,
        "surname": surnames[i % surnames.length],
        // Generate birth date (sequentially varied by index)
        "birthDate": new Date(1950 + (i % 50), 5, 15),
        "city": cities[Math.floor(Math.random() * cities.length)],
        "phoneNumber": "333" + (1000000 + i),
        "status": (Math.random() > 0.5),
        "notes": "Paziente generato per test. " + "Note cliniche lunghe... ".repeat(5),
        "condition": conditions[Math.floor(Math.random() * conditions.length)],

        // Embedded Therapy Object
        "therapy": {
            "sessions": NumberInt(Math.floor(Math.random() * 12) + 1),
            "duration": NumberInt([30, 45, 60, 90][Math.floor(Math.random() * 4)]),
            "frequency": NumberInt(1),
            "medicines": medsList // Contains the linked medicine ID
        }
    };

    // Queue the insert operation
    bulk.insert(doc);
    count++;

    // Execute batch when the size limit is reached
    if (count % BATCH_SIZE === 0) {
        bulk.execute();
        bulk = db.patient.initializeUnorderedBulkOp(); // Reset bulk op
        print("👤 Inserted bulk: " + count + "...");
    }
}

// Process any remaining documents in the queue
if (count % BATCH_SIZE !== 0) { bulk.execute(); }

print("✅ Finished generating patients");