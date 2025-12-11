function validateMedicineData(medicine){
    let validity = true;
    //validazione del formato
    if (!medicineNameValidity(medicine.name)) {
        document.getElementById("name-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("name-validity").innerHTML = "";
    }
    if (!ingredientsValidity(medicine.ingredients)) {
        document.getElementById("ingredients-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("ingredients-validity").innerHTML = "";
    }
    return validity;
}

function validatePackageData(medicinePackage, id){
    let validity = true;
    //validazione del formato
    if (!capacityValidity(medicinePackage.capacity)) {
        document.getElementById("package-" + id + "-capacity-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("package-" + id + "-capacity-validity").innerHTML = "";
    }
    if (!dateValidity(medicinePackage.expiryDate)) {
        document.getElementById("package-" + id + "-expiry-date-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("package-" + id + "-expiry-date-validity").innerHTML = "";
    }
    return validity;
}



function findAllMedicines(select) {
    var request = new XMLHttpRequest();
    request.open('POST', "MedicineServlet", false);
    request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    request.setRequestHeader('Authorization', 'Basic ');
    request.setRequestHeader('Accept', 'application/json');
    var body = "action=findAllMedicines";
    request.send(body);
    if (request.status === 200) {
        if (request.getResponseHeader('OPERATION_RESULT')) {
            const medicineNumber = request.getResponseHeader('medicineNumber');
            var medicineId = [];
            var medicineName = [];
            for (let i = 0; i < medicineNumber; i++) {
                medicineId[i] = request.getResponseHeader('medicineId' + i);
                medicineName[i] = request.getResponseHeader('medicineName' + i);
            }

            const option0 = document.createElement("option");
            option0.setAttribute("value", "null");
            option0.innerHTML = "Seleziona medicinale";
            select.appendChild(option0);

            for (let i = 0; i < medicineNumber; i++){
                const option = document.createElement("option");
                option.setAttribute("value", medicineId[i]);
                option.innerHTML = medicineName[i];
                select.appendChild(option);
            }
            return select;
        } else {
            //errore modifica select
            return null;
        }
    }
}

function addPackageForm() {
    document.getElementById("new-package-button").className = "hidden";
    document.getElementById("new-package-form").className = "box";
}

function editMedicineButton(id) {
    editToSaveButton("save-medicine-button", "medicine-data-buttons", "edit-medicine-data-button", "submitUpdatedMedicine('" + id + " ')");
    document.getElementById("name").className = "input-field";
    document.getElementById("ingredients").className = "input-field";
}

function editPackageButton(id) {
    editToSaveButton("save-package-button", "package-data-buttons", "edit-package-data-button", "submitUpdatedPackage('" + id + " ')");
    document.getElementById(id + "-package-capacity").className = "input-field";
    document.getElementById(id + "-package-expiry-date").className = "input-field";
    document.getElementById(id + "-package-status").className = "input-field";
}

function addMedicine() {
    const name = document.getElementById("name").value;
    const ingredients = document.getElementById("ingredients").value;

    const medicine = {
        name: name,
        ingredients: ingredients
    };

    if (validateMedicineData(medicine)) {
        var request = new XMLHttpRequest();
        request.open('POST', "MedicineServlet", true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.setRequestHeader('Authorization', 'Basic ');
        request.setRequestHeader('Accept', 'application/json');
        var body = "action=insertMedicine&name=" + medicine.name + "&ingredients=" + medicine.ingredients;
        request.send(body);
        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200) {
                if (request.getResponseHeader('OPERATION_RESULT')) {
                    const medicineID = request.getResponseHeader('MEDICINE_ID');
                    //recupero id dalla risposta
                    redirectToMedicineDetails(medicineID);
                } else {
                    //errore creazione paziente
                    showAlertDanger(request.getResponseHeader('ERROR_MESSAGE'));
                }
            }
        };
    }
}

function addPackage(id) {
    const capacity = document.getElementById("package-new-capacity").value;
    const expiryDate = document.getElementById("package-new-expiry-date").value;

    const medicinePackage = {
        capacity: capacity,
        expiryDate: expiryDate
    };

    if (validatePackageData(medicinePackage, "new")) {
        var request = new XMLHttpRequest();
        request.open('POST', "MedicineServlet", true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.setRequestHeader('Authorization', 'Basic ');
        request.setRequestHeader('Accept', 'application/json');
        var body = "action=insertPackage&medicineId=" + id + "&capacity=" + medicinePackage.capacity + "&expiryDate=" + medicinePackage.expiryDate;
        request.send(body);
        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200) {
                if (request.getResponseHeader('OPERATION_RESULT')) {
                    //recupero id dalla risposta
                    redirectToMedicineDetails(id);
                } else {
                    //errore creazione paziente
                    showAlertDanger(request.getResponseHeader('ERROR_MESSAGE'));
                }
            }
        };
    }
}

function redirectToMedicineDetails(id) {
    //crea una richiesta alla servlet paziente per reindirizzare
    window.location.replace("MedicineServlet?id=" + id);
}

/**
 * Gestione Autocomplete Medicinali (Green IT Optimization)
 * Utilizza un meccanismo di "Debounce" per evitare di inondare il server
 * di richieste ad ogni singolo tasto premuto.
 */

// Variabile globale per il timer del debounce
let searchTimeout = null;

function fetchMedicineSuggestions(query) {
    // 1. Pulisce il timer precedente (se l'utente sta ancora scrivendo)
    if (searchTimeout) {
        clearTimeout(searchTimeout);
    }

    // 2. Ottimizzazione: Se scrivi meno di 2 caratteri, non disturbare il server
    if (!query || query.trim().length < 2) {
        // Pulisce la lista se cancelli il testo
        const dataList = document.getElementById('medicine-suggestions');
        if (dataList) dataList.innerHTML = '';
        return;
    }

    // 3. Imposta un nuovo timer: la chiamata partirà solo se ti fermi per 300ms
    searchTimeout = setTimeout(() => {

        // Chiamata AJAX asincrona (Micro-fetch)
        fetch('MedicineServlet?action=autocompleteMedicine&q=' + encodeURIComponent(query.trim()))
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                const dataList = document.getElementById('medicine-suggestions');

                // Safety check: se l'elemento non esiste nel DOM, fermati
                if (!dataList) return;

                dataList.innerHTML = ''; // Pulisce i vecchi suggerimenti

                // Aggiunge le nuove opzioni ricevute dal server
                data.forEach(name => {
                    const option = document.createElement('option');
                    option.value = name;
                    dataList.appendChild(option);
                });
            })
            .catch(err => console.error('Errore autocomplete medicinali:', err));

    }, 300); // 300ms di ritardo (Debounce time)
}