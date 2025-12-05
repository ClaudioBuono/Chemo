// Select target database
const db = db.getSiblingDB('Chemo');

// Configuration constants
const TARGET_MEDICINES = 1000;
const BATCH_SIZE = 1000; // Number of operations to execute at once

// Drop the existing collection to start fresh
db.medicine.drop();

print("💊 Start generating " + TARGET_MEDICINES + " medicines...");

// Initialize an Unordered Bulk Operation for performance efficiency
// (This prevents sending 1000 separate insert requests to the server)
let bulk = db.medicine.initializeUnorderedBulkOp();
let count = 0;

// Source data arrays for randomization
let molecules = ["Docetaxel", "Paclitaxel", "Cisplatino", "Oxaliplatino", "Doxorubicina", "Gemcitabina", "Vinorelbina"];
let brands = ["Sandoz", "Teva", "Pfizer", "Accord", "Mylan"];
let forms = ["(anidro)", "(idrato)", "concentrato", "soluzione iniettabile"];

// Main loop to generate documents
for (let i = 0; i < TARGET_MEDICINES; i++) {

    // Select attributes: Cycle through molecules, pick random brand/form
    let mol = molecules[i % molecules.length];
    let brand = brands[Math.floor(Math.random() * brands.length)];
    let form = forms[Math.floor(Math.random() * forms.length)];

    // Generate random sub-documents (packages)
    let packages = [];
    let numPackages = Math.floor(Math.random() * 5) + 1; // Random number between 1 and 5

    for (let p = 1; p <= numPackages; p++) {
        // Generate a random future expiry date (between 2028 and 2034)
        let year = 2028 + Math.floor(Math.random() * 6);
        let month = Math.floor(Math.random() * 12);

        packages.push({
            "packageId": p.toString(),
            "status": true,
            "capacity": NumberInt(Math.floor(Math.random() * 1500) + 100),
            "expiryDate": new Date(year, month, 10)
        });
    }

    // Construct the main medicine document
    let doc = {
        "name": mol + " - " + brand,
        "ingredients": mol + " " + form,
        "amount": NumberInt(packages.length),
        "package": packages,
    };

    // Add document to the bulk queue
    bulk.insert(doc);
    count++;

    // Execute the batch write when the limit is reached to manage memory
    if (count % BATCH_SIZE === 0) {
        bulk.execute();
        bulk = db.medicine.initializeUnorderedBulkOp(); // Reset the bulk op
        print("📦 Inserted batch: " + count + "...");
    }
}

// Execute any remaining operations in the queue (e.g., if total is not a multiple of BATCH_SIZE)
if (count % BATCH_SIZE !== 0) { bulk.execute(); }

print("✅ Finished generating medicines");