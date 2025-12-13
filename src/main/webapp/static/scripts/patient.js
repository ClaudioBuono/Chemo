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

    // --- Validazione campi generali (Invariata) ---
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

    // --- Validazione Medicinali (MODIFICATA) ---
    for (let i = 0; i < therapy.medicinesNumber; i++) {
        // Nota: therapy.medicines[i].id ora contiene il NOME del farmaco, non l'ID.
        console.log("Medicinale inserito: " + therapy.medicines[i].id);

        // MODIFICA: Controllo solo che non sia vuoto o nullo
        if (!therapy.medicines[i].id || therapy.medicines[i].id.trim().length === 0) {
            document.getElementById("medicine-" + i + "-validity").innerHTML = "Inserire un medicinale";
            validity = false;
        } else {
            // Se c'è scritto qualcosa, lo accettiamo (la validazione vera la farà il backend se il nome non esiste)
            document.getElementById("medicine-" + i + "-validity").innerHTML = "";
        }

        console.log("Dose medicinale: " + therapy.medicines[i].dose);
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

async function addMedicineField(prefixId, number) {
    // prefixId è 'new' o 'saved'
    // number è l'indice progressivo (es. 1, 2, 3...)

    const newmedicine = document.createElement("div");
    newmedicine.setAttribute("id", prefixId + "-medicine-item-" + number);
    newmedicine.setAttribute("class", "input-fields-row");

    // --- COLONNA SINISTRA (Input Medicinale) ---
    const firstfield = document.createElement("div");
    firstfield.setAttribute("class", "field left");

    const label1 = document.createElement("label");
    label1.setAttribute("for", "medicine-name-item-" + number);
    let nextNumber = number + 1;
    label1.innerHTML = nextNumber + "° Medicinale";

    // CAMBIAMENTO QUI: Creiamo un INPUT invece di una SELECT
    var inputAutocomplete = document.createElement("input");
    inputAutocomplete.setAttribute("type", "text");
    inputAutocomplete.setAttribute("id", "search-patient-medicine-" + prefixId + "-" + number); // ID univoco
    inputAutocomplete.setAttribute("class", "input-field");
    inputAutocomplete.setAttribute("name", "medicineName" + number); // IMPORTANTE per il recupero dati
    inputAutocomplete.setAttribute("placeholder", "Cerca farmaco...");
    inputAutocomplete.setAttribute("autocomplete", "off");

    // Colleghiamo la Datalist
    var datalistId = "medicine-suggestions-" + prefixId + "-" + number;
    inputAutocomplete.setAttribute("list", datalistId);

    // Colleghiamo la funzione JS per l'autocomplete
    // Nota: escape dei caratteri per la stringa oninput
    inputAutocomplete.setAttribute("oninput", "fetchMedicineSuggestions(this.value, '" + datalistId + "')");

    // Creiamo la Datalist vuota
    var datalist = document.createElement("datalist");
    datalist.setAttribute("id", datalistId);

    const validity1 = document.createElement("p");
    validity1.setAttribute("id", "medicine-" + number + "-validity");
    validity1.setAttribute("class", "validity-paragraph status-unavailable");

    // Appendiamo tutto
    firstfield.appendChild(label1);
    firstfield.appendChild(inputAutocomplete);
    firstfield.appendChild(datalist); // Aggiungiamo la datalist al DOM
    firstfield.appendChild(validity1);

    // --- COLONNA DESTRA (Dose - Rimane invariata) ---
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

    // --- CHIUSURA ---
    newmedicine.appendChild(firstfield);
    newmedicine.appendChild(secondfield);

    // Aggiungiamo la riga al contenitore corretto (new-medicines o saved-medicines)
    // Nota: nel tuo HTML gli ID dei contenitori sono un po' diversi ('new-medicines' vs 'saved-medicines' probabilmente)
    // Assicurati che l'ID passato alla funzione matchi l'ID del div contenitore nel DOM.
    // Dallo script vedo: document.getElementById(id + "-medicines")
    document.getElementById(prefixId + "-medicines").appendChild(newmedicine);

    document.getElementById("medicines-number").innerHTML = nextNumber;

    // Aggiorniamo il bottone "Aggiungi" per puntare al prossimo numero
    // Nota: 'add-medicine-new' o 'add-medicine-saved'
    var btnId = "add-medicine-" + prefixId;
    var btn = document.getElementById(btnId);
    if(btn) {
        btn.setAttribute("onclick", "addMedicineField('" + prefixId + "'," + nextNumber + ")");
    }
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
