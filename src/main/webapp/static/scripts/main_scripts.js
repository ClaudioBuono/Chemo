/*====================================================================
 * FILE: main_scripts.js
 * DESC: Foglio di script principale consolidato.
 * Unisce tutti i file statici javascript locali per l'ottimizzazione (riduzione delle richieste HTTP).
 *====================================================================*/

// alert.js
bootstrap_alert = function() {}
bootstrap_alert.warning = function(message) {
    $('#alert-box').html('<div class="alert alert-warning"><span>' + message + '</span></div>')
}
bootstrap_alert.danger = function(message) {
    $('#alert-box').html('<div class="alert alert-danger"><span>' + message + '</span></div>')
}

function showAlertWarning(error) {
    bootstrap_alert.warning(error);
    window.scrollTo(0, 0);
}
function showAlertDanger(error) {
    bootstrap_alert.danger(error);
    window.scrollTo(0, 0);
}

//buttons.js
function editToSaveButton(newid, parentid, oldbuttonid, funcname) {
    const newbutton = document.createElement("input");
    newbutton.setAttribute("type", "button");
    newbutton.setAttribute("id", newid);
    newbutton.setAttribute("class", "button-primary-m rounded edit-button");
    newbutton.setAttribute("value", "Salva");
    newbutton.setAttribute("onclick", funcname);
    const parent = document.getElementById(parentid);
    const oldbutton = document.getElementById(oldbuttonid);
    parent.replaceChild(newbutton, oldbutton);
}

function addDeleteButton(text, newid,parentid, nextbuttonid, funcname) {
    const newbutton = document.createElement("input");
    newbutton.setAttribute("type", "button");
    newbutton.setAttribute("id", newid);
    newbutton.setAttribute("class", "button-tertiary-m rounded edit-button");
    newbutton.setAttribute("value", text);
    newbutton.setAttribute("onclick", funcname);
    const parent = document.getElementById(parentid);
    const nextbutton = document.getElementById(nextbuttonid);
    parent.insertBefore(newbutton, nextbutton);
}
//medicine.js
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
//patients.js
function validatePatientData(patient){
    let validity = true;
    //validazione del formato
    if (!namesValidity(patient.name)) {
        document.getElementById("name-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("name-validity").innerHTML = "";
    }
    if (!namesValidity(patient.surname)) {
        document.getElementById("surname-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("surname-validity").innerHTML = "";
    }
    if (!dateValidity(patient.birthDate)) {
        document.getElementById("birthdate-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("birthdate-validity").innerHTML = "";
    }
    if (!cityValidity(patient.city)) {
        document.getElementById("city-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("city-validity").innerHTML = "";
    }
    if (!taxCodeValidity(patient.taxCode)) {
        document.getElementById("tax-code-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("tax-code-validity").innerHTML = "";
    }
    if (!phoneNumberValidity(patient.phoneNumber)) {
        document.getElementById("phone-number-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("phone-number-validity").innerHTML = "";
    }
    if (!notesValidity(patient.notes)) {
        document.getElementById("notes-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("notes-validity").innerHTML = "";
    }
    return validity;
}

function validateTherapyData(therapy){
    var validity = true;
    //validazione del formato
    if (!conditionValidity(therapy.condition)) {
        document.getElementById("condition-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("condition-validity").innerHTML = "";
    }
    if (!sessionsValidity(therapy.sessions)) {
        document.getElementById("sessions-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("sessions-validity").innerHTML = "";
    }
    if (!frequencyValidity(therapy.frequency)) {
        document.getElementById("frequency-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("frequency-validity").innerHTML = "";
    }
    if (!durationValidity(therapy.duration)) {
        document.getElementById("duration-validity").innerHTML = "Formato errato";
        validity = false;
    } else {
        document.getElementById("duration-validity").innerHTML = "";
    }
    for (let i = 0; i < therapy.medicinesNumber; i++) {
        console.log("Id medicinale selezionato: " + therapy.medicines[i].id);
        if (!idValidity(therapy.medicines[i].id)) {
            document.getElementById("medicine-" + i + "-validity").innerHTML = "Formato errato";
            validity = false;
        } else {
            document.getElementById("medicine-" + i + "-validity").innerHTML = "";
        }
        console.log("Dose medicinale selezionato: " + therapy.medicines[i].dose);
        if (!doseValidity(therapy.medicines[i].dose)) {
            document.getElementById("dose-" + i + "-validity").innerHTML = "Formato errato";
            validity = false;
        } else {
            document.getElementById("dose-" + i + "-validity").innerHTML = "";
        }
    }

    return validity;
}

/* Funzioni per i dettagli del paziente*/

function addTherapyForm() {
    document.getElementById("new-therapy-button").className = "hidden";
    document.getElementById("new-therapy-form").className = "box form";
}

async function addMedicineField(id, number) {
    const newmedicine = document.createElement("div");
    newmedicine.setAttribute("id", id + "-medicine-item-" + number);
    newmedicine.setAttribute("class", "input-fields-row");

    const firstfield = document.createElement("div");
    firstfield.setAttribute("class", "field left");
    const label1 = document.createElement("label");
    label1.setAttribute("for", "medicine-name-item-" + number);
    let nextNumber = number + 1;
    label1.innerHTML = nextNumber + "° Medicinale";
    var select1 = document.createElement("select");
    select1.setAttribute("id", "medicine-name-item-" + number);
    select1.setAttribute("class", "input-field");
    select1.setAttribute("name", "medicineName" + number);
    //chiamata servlet per ottenere i medicinali
    select1 = findAllMedicines(select1);
    console.log("Valore della select: " + select1);
    const validity1 = document.createElement("p");
    validity1.setAttribute("id", "medicine-" + number + "-validity");
    validity1.setAttribute("class", "validity-paragraph status-unavailable");
    firstfield.appendChild(label1);
    firstfield.appendChild(select1);
    firstfield.appendChild(validity1);

    const secondfield = document.createElement("div");
    secondfield.setAttribute("class", "field right");
    const label2 = document.createElement("label");
    label2.setAttribute("for", "medicine-dose-item-" + number);
    label2.innerHTML = "Dose (in ml)";
    const input2 = document.createElement("input");
    input2.setAttribute("id", "medicine-dose-item-" + number);
    input2.setAttribute("class", "input-field");
    input2.setAttribute("type", "text");
    input2.setAttribute("name", "medicineDose" + number);
    const validity2 = document.createElement("p");
    validity2.setAttribute("id", "dose-" + number + "-validity");
    validity2.setAttribute("class", "validity-paragraph status-unavailable");
    secondfield.appendChild(label2);
    secondfield.appendChild(input2);
    secondfield.appendChild(validity2);

    newmedicine.appendChild(firstfield);
    newmedicine.appendChild(secondfield);
    document.getElementById(id + "-medicines").appendChild(newmedicine);

    document.getElementById("medicines-number").innerHTML = nextNumber;
    document.getElementById("add-medicine-" + id).setAttribute("onclick", "addMedicineField('" + id + "'," + nextNumber + ")");
}

function editStatusButton(id) {
    editToSaveButton("save-patient-status-button", "patient-status-button", "edit-patient-status-button", "submitUpdatedStatus('" + id + "')");
    document.getElementById("status").className = "input-field";
}

function editTherapyButtons(id, medicines) {
    editToSaveButton("save-therapy-button", "therapy-buttons", "edit-therapy-button", "submitUpdatedTherapy('" + id + "')");
    addDeleteButton("Elimina","delete-therapy-button","therapy-buttons", "save-therapy-button", "deleteTherapy('" + id + "')");
    document.getElementById("condition").className = "input-field";
    document.getElementById("sessions-number").className = "input-field";
    document.getElementById("sessions-frequency").className = "input-field";
    document.getElementById("sessions-duration").className = "input-field";

    for (let i = 0; i < medicines; i++) {
        document.getElementById("medicine-name-item-" + i ).className = "input-field";
        document.getElementById("medicine-dose-item-" + i ).className = "input-field";
    }

    const addMedicine = document.createElement("input");
    addMedicine.setAttribute("type", "button");
    addMedicine.setAttribute("id", "add-medicine-saved");
    addMedicine.setAttribute("class", "button-secondary-s rounded edit-button");
    addMedicine.setAttribute("value", "Aggiungi medicinale");
    addMedicine.setAttribute("onclick", "addMedicineField('saved'," + medicines + ")");
    document.getElementById("therapy-section").appendChild(addMedicine);
}


/* Funzioni per le chiamata alle servlet per il paziente*/

function redirectToPatientDetails(id) {
    //crea una richiesta alla servlet paziente per reindirizzare
    window.location.replace("PatientServlet?id=" + id);
}

function addPatient() {
    //recupero dei parametri dalla pagina
    var name = document.getElementById("name").value;
    var surname = document.getElementById("surname").value;
    var birthDate = document.getElementById("birthdate").value;
    var city = document.getElementById("city").value;
    var taxCode = document.getElementById("tax-code").value;
    var phoneNumber = document.getElementById("phone-number").value;
    var notes = document.getElementById("notes").value;

    const patient = {
        name: name,
        surname: surname,
        birthDate: birthDate,
        city: city,
        taxCode: taxCode,
        phoneNumber: phoneNumber,
        notes: notes
    };
    console.log("Risultato controllo campi: " + validatePatientData(patient));
    if (validatePatientData(patient)) {
        var request = new XMLHttpRequest();
        request.open('POST', "PatientServlet", true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.setRequestHeader('Authorization', 'Basic ');
        request.setRequestHeader('Accept', 'application/json');
        var body = "action=createPatientProfile&name=" + name + "&surname=" + surname + "&birthDate=" +
            birthDate + "&city=" + city + "&taxCode=" + taxCode +
            "&phoneNumber=" + phoneNumber + "&notes=" + notes;
        request.send(body);
        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200) {
                //redirectToPage("patientDetails.jsp");
                //alert(request.responseText);
                if (request.getResponseHeader('OPERATION_RESULT')) {
                    //window.location = "patientDetails.jsp";
                    const patientID = request.getResponseHeader('PATIENT_ID');
                    //recupero id dalla risposta
                    redirectToPatientDetails(patientID);
                } else {
                    //errore creazione paziente
                    showAlertDanger(request.getResponseHeader('ERROR_MESSAGE'));
                }
            }
        };
    }
}

function addTherapy(id) {
    //recupero dei parametri dalla pagina
    const condition = document.getElementById("new-condition").value;
    const frequency = document.getElementById("new-sessions-frequency").value;
    const duration = document.getElementById("new-sessions-duration").value;
    const sessions = document.getElementById("new-sessions-number").value;
    const medicinesNumber = document.getElementById("medicines-number").innerHTML;
    let medicines = [];
    for (let i = 0; i < medicinesNumber; i++) {
        var medicine = {
            id: "",
            dose: ""
        };
        medicine.id = document.getElementById("medicine-name-item-" + i).value;
        medicine.dose = document.getElementById("medicine-dose-item-" + i).value;
        medicines[i] = medicine;
        console.log("Aggiunto medicinale " + medicines[i].id + " - " + medicines[i].dose);
    }

    const therapy = {
        condition: condition,
        sessions: sessions,
        frequency: frequency,
        duration: duration,
        medicinesNumber: medicinesNumber,
        medicines: medicines
    };

    if (validateTherapyData(therapy)) {
        //i campi hanno tutti il formato corretto
        var body = "action=completePatientProfile&id=" + id +"&condition=" + condition + "&frequency=" +
            frequency + "&duration=" + duration + "&sessions=" + sessions +"&medicinesNumber=" + medicinesNumber;
        for (let i = 0; i < medicinesNumber; i++) {
            body += "&medicineId" + i + "=" + medicines[i].id;
            body += "&medicineDose" + i + "=" + medicines[i].dose;
        }
        sendTherapyData(body);
    }
}
function sendTherapyData(body){
    var request = new XMLHttpRequest();
    request.open('POST', "PatientServlet", true);
    request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    request.setRequestHeader('Authorization', 'Basic ');
    request.setRequestHeader('Accept', 'application/json');
    request.send(body);

    request.onreadystatechange = function () {
        if (request.readyState === 4 && request.status === 200) {
            //redirectToPage("patientDetails.jsp");
            //alert(request.responseText);
            if (request.getResponseHeader('OPERATION_RESULT')){
                const patientID = request.getResponseHeader('PATIENT_ID');
                //recupero id dalla risposta
                redirectToPatientDetails(patientID);
            } else {
                //errore aggiunta terapia
                showAlertDanger(request.getResponseHeader('ERROR_MESSAGE'));
            }
        }
    };
}
function submitUpdatedTherapy(id) {
    //recupero dei parametri dalla pagina
    const condition = document.getElementById("condition").value;
    const frequency = document.getElementById("sessions-frequency").value;
    const duration = document.getElementById("sessions-duration").value;
    const sessions = document.getElementById("sessions-number").value;
    const medicinesNumber = document.getElementById("medicines-number").innerHTML;

    let medicines = [];
    for (let i = 0; i < medicinesNumber; i++) {
        var medicine = {
            id: "",
            dose: ""
        };
        medicine.id = document.getElementById("medicine-name-item-" + i).value;
        medicine.dose = document.getElementById("medicine-dose-item-" + i).value;
        medicines[i] = medicine;
        console.log("Modificato medicinale " + medicines[i].id + " - " + medicines[i].dose);
    }

    const therapy = {
        condition: condition,
        sessions: sessions,
        frequency: frequency,
        duration: duration,
        medicinesNumber: medicinesNumber,
        medicines: medicines
    };
    console.log("Medicines: "+ medicines[0].id + " " + medicines[0].dose);

    if (validateTherapyData(therapy)) {
        //i campi hanno tutti il formato corretto
        var body = "action=completePatientProfile&id=" + id +"&condition=" + condition + "&frequency=" +
            frequency + "&duration=" + duration + "&sessions=" + sessions +"&medicinesNumber=" + medicinesNumber;
        for (let i = 0; i < medicinesNumber; i++) {
            body += "&medicineId" + i + "=" + medicines[i].id;
            body += "&medicineDose" + i + "=" + medicines[i].dose;
        }
        sendTherapyData(body)
    }
}


function submitUpdatedStatus(id){
    //recupero dei parametri dalla pagina
    const status = document.getElementById("status").value;
    console.log(status);
    const therapy = document.getElementById("therapy").innerHTML;
    console.log(therapy);
    console.log(id);

    var validity = true;
    //validazione del formato
    if (!therapy) {
        validity = false;
    }
    if (validity) {
        //i campi hanno tutti il formato corretto
        var request = new XMLHttpRequest();
        request.open('POST', "PatientServlet", true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.setRequestHeader('Authorization', 'Basic ');
        request.setRequestHeader('Accept', 'application/json');
        const body = "action=editPatientStatus&id=" + id +"&status=" + status;
        request.send(body);

        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200) {
                //redirectToPage("patientDetails.jsp");
                //alert(request.responseText);
                if (request.getResponseHeader('OPERATION_RESULT')){
                    //recupero id dalla risposta
                    redirectToPatientDetails(id);
                } else {
                    //errore aggiunta terapia
                    showAlertDanger(request.getResponseHeader('ERROR_MESSAGE'));
                }
            }
        };
    }
}
//planner.js
function addAppointments() {
    var checkedPatients = $('input[name="patient-id"]:checked');
    //var checkedPatients = getCheckedBoxes("patient-id");

    if (!(checkedPatients.length > 0)) {
        // errore: deve essere selezionato almeno un paziente
        showAlertWarning("Seleziona almeno un paziente");

    } else {
        var request = new XMLHttpRequest();
        request.open('POST', "PlannerServlet", true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.setRequestHeader('Authorization', 'Basic ');
        request.setRequestHeader('Accept', 'application/json');
        var body = "action=addAppointments&patientsNumber=" + checkedPatients.length;
        for (let i=0; i < checkedPatients.length; i++) {
            // vengono aggiunti i pazienti al body
            body += "&patient" + i + "=" + checkedPatients[i].value;
        }
        request.send(body);
        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200) {
                if (request.getResponseHeader('OPERATION_RESULT')) {
                    // redirect al calendario
                    redirectToPlanner();
                } else {
                    // errore creazione agenda
                    showAlertDanger("Creazione nuova agenda fallita.");
                }
            }
        };
    }
}

function redirectToPlanner(id, buttonPressed) {
    if (id == null) {
        window.location.replace("PlannerServlet");
    } else {
        window.location.replace("PlannerServlet?id="+id+"&buttonPressed="+buttonPressed);
    }
    // genera un redirect alla servlet paziente creando una richiesta get
}

function getCheckedBoxes(checkboxName) {
    const checkboxes = document.getElementsByName(checkboxName);
    var checkboxesChecked = [];
    // ciclo per scorrere tutti i checkbox
    for (let i=0; i < checkboxes.length; i++) {
        // vengono aggiunti al nuovo array quelli selezionati
        if (checkboxes[i].checked) {
            checkboxesChecked.push(checkboxes[i]);
        }
    }
    // restituisce array di valori selezionati o null se non ci sono valori
    return checkboxesChecked.length > 0 ? checkboxesChecked : null;
}
//search.js
function expandSearchFilters() {
    document.getElementById("search-filters").className = "";
    document.getElementById("search-filters-button").setAttribute( "value", "Riduci filtri");
    document.getElementById("search-filters-button").onclick = reduceSearchFilters;
}
function reduceSearchFilters() {
    document.getElementById("search-filters").className = "hidden";
    document.getElementById("search-filters-button").setAttribute( "value", "Espandi filtri");
    document.getElementById("search-filters-button").onclick = expandSearchFilters;
}
//user.js
function editUserCredentials(id) {
    editToSaveButton("save-user-credentials-button", "user-credentials-button", "edit-user-credentials-button", "submitUpdatedCredentials('" + id + " ')");
    document.getElementById("username").className = "input-field";
    document.getElementById("password").className = "input-field";
}