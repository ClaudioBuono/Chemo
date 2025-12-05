// Select the target database
const db = db.getSiblingDB('Chemo');

// Configuration constants
const WEEKS_TO_GENERATE = 24;
const PATIENTS_TO_AGGREGATE = 5000;

//  Drop the planner collection to start fresh
db.planner.drop();

print("🗓️ Start generating planners...");

// Fetch Dependencies
const patients = db.patient.aggregate([
    { $sample: { size: PATIENTS_TO_AGGREGATE } },
    { $project: { _id: 1 } }
]).toArray();

const medicines = db.medicine.find({}, { _id: 1 }).toArray();

// Validation: Ensure we have data to link to
if (patients.length === 0 || medicines.length === 0) {
    print("❌ ERROR: No patient or medicines found");
    quit();
}

print("✅ Generating " + WEEKS_TO_GENERATE + " weeks...");

// Initialize bulk operation for performance
const bulk = db.planner.initializeUnorderedBulkOp();

// Date Setup
// Set start date to 3 months ago to simulate historical data
const startDate = new Date();
startDate.setMonth(startDate.getMonth() - 3);

// Logic to "Rewind" to the previous Monday to align weeks correctly
startDate.setDate(startDate.getDate() - (startDate.getDay() + 6) % 7);
startDate.setHours(8, 0, 0, 0); // Start of working day

// List of available resources
const chairs = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "VIP_1", "VIP_2"];

// --- MAIN LOOP: WEEKS ---
for (let w = 0; w < WEEKS_TO_GENERATE; w++) {

    // Calculate the Monday of the current iteration's week
    const weekStart = new Date(startDate);
    weekStart.setDate(weekStart.getDate() + (w * 7));

    // Calculate the Friday of that week (Work week end)
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekEnd.getDate() + 4);
    weekEnd.setHours(17, 0, 0, 0);

    const appointments = [];

    // --- INNER LOOP: DAYS (Monday to Friday) ---
    for (let d = 0; d < 5; d++) {
        const currentDay = new Date(weekStart);
        currentDay.setDate(currentDay.getDate() + d);

        // --- INNER LOOP: CHAIRS ---
        for (let c = 0; c < chairs.length; c++) {

            // Simulate realistic density: Randomly decide how many slots this chair has today (1 to 3)
            const slots = Math.floor(Math.random() * 3) + 1;

            for (let s = 0; s < slots; s++) {
                // Random start time between 08:00 and 16:00
                const hour = 8 + Math.floor(Math.random() * 8);
                const apptDate = new Date(currentDay);
                apptDate.setHours(hour, 0, 0, 0);

                // Pick random Patient and Medicine
                const randomPat = patients[Math.floor(Math.random() * patients.length)];
                const randomMed = medicines[Math.floor(Math.random() * medicines.length)];

                // Duration variability (Short vs Long infusions)
                const duration = [30, 60, 60, 120, 360][Math.floor(Math.random() * 5)];

                // Push appointment object to the array (Embed data pattern)
                appointments.push({
                    "chair": chairs[c],
                    "date": apptDate,
                    "duration": NumberInt(duration), // Enforce Integer type
                    "idMedicine": randomMed._id.str, // Store OID as String (Reference)
                    "idPatient": randomPat._id.str   // Store OID as String (Reference)
                });
            }
        }
    }


    const weekDoc = {
        "start": weekStart,
        "end": weekEnd,
        "appointments": appointments // Array of all appointments for this week
    };

    bulk.insert(weekDoc);
}

// Execute all inserts
bulk.execute();
print("🎉 Finished! Inserted " + WEEKS_TO_GENERATE + " weeks of work plans.");