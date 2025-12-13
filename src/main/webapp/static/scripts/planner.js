/* ==========================================
   GESTIONE UI & LOADING (Effetti Visivi)
   ========================================== */

/**
 * Attiva lo stato di caricamento: cursore clessidra e opacità.
 * Da chiamare prima di un redirect o di una operazione lunga.
 */
function startLoading() {
    // 1. Diciamo al browser che sta lavorando (su tutta la pagina)
    document.body.style.cursor = 'wait';
    document.body.style.pointerEvents = "none"; // Nessuno può cliccare nulla (nemmeno l'header) per evitare errori

    // 2. Applichiamo l'effetto visivo SOLO al contenuto del planner
    var plannerContent = document.getElementById("planner-content");
    if (plannerContent) {
        plannerContent.style.transition = "opacity 0.2s ease-out";
        plannerContent.style.opacity = "0.5"; // Diventa semi-trasparente
    }
}

/**
 * Ripristina lo stato normale.
 */
function stopLoading() {
    // 1. Sblocchiamo tutto
    document.body.style.cursor = 'default';
    document.body.style.pointerEvents = "auto";

    // 2. Ripristiniamo la visibilità del planner
    var plannerContent = document.getElementById("planner-content");
    if (plannerContent) {
        plannerContent.style.opacity = "1"; // Torna nitido
    }
}

/* ==========================================
   LOGICA DI BUSINESS (Planner)
   ========================================== */

function addAppointments() {
    var checkedPatients = $('input[name="patient-id"]:checked');
    // var checkedPatients = getCheckedBoxes("patient-id"); // Alternativa vanilla JS

    if (!(checkedPatients.length > 0)) {
        // Errore validazione: Nessun paziente
        showAlertWarning("Seleziona almeno un paziente");
        // Non serve stopLoading() qui perché non l'abbiamo ancora avviato
    } else {
        // Avvia effetto caricamento mentre contattiamo il server
        startLoading();

        var request = new XMLHttpRequest();
        request.open('POST', "PlannerServlet", true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.setRequestHeader('Authorization', 'Basic ');
        request.setRequestHeader('Accept', 'application/json');

        var body = "action=addAppointments&patientsNumber=" + checkedPatients.length;
        for (let i = 0; i < checkedPatients.length; i++) {
            body += "&patient" + i + "=" + checkedPatients[i].value;
        }

        request.send(body);

        request.onreadystatechange = function () {
            if (request.readyState === 4) {
                if (request.status === 200) {
                    if (request.getResponseHeader('OPERATION_RESULT')) {
                        // Successo: il redirect manterrà il loading attivo fino al cambio pagina
                        redirectToPlanner();
                    } else {
                        // Errore logico dal server
                        stopLoading(); // Sblocca la pagina per permettere di riprovare
                        showAlertDanger("Creazione nuova agenda fallita.");
                    }
                } else {
                    // Errore HTTP (es. 500 o 404)
                    stopLoading(); // Sblocca la pagina
                    showAlertDanger("Errore di comunicazione con il server.");
                }
            }
        };
    }
}

function redirectToPlanner(id, buttonPressed) {
    // Attiva subito il feedback visivo perché stiamo per cambiare pagina
    startLoading();

    if (id == null) {
        window.location.replace("PlannerServlet");
    } else {
        // Nota: encodeURIComponent è buona prassi per evitare problemi con caratteri strani negli ID
        window.location.replace("PlannerServlet?id=" + encodeURIComponent(id) + "&buttonPressed=" + encodeURIComponent(buttonPressed));
    }
}

function getCheckedBoxes(checkboxName) {
    const checkboxes = document.getElementsByName(checkboxName);
    var checkboxesChecked = [];

    for (let i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].checked) {
            checkboxesChecked.push(checkboxes[i]);
        }
    }

    return checkboxesChecked.length > 0 ? checkboxesChecked : null;
}

/* ==========================================
   EVENT LISTENERS (Inizializzazione)
   ========================================== */

document.addEventListener("DOMContentLoaded", function() {
    // Aggiunge l'effetto loading anche ai click manuali sui bottoni di navigazione (se presenti nell'HTML)
    // Cerca qualsiasi elemento con classe 'nav-trigger' o bottoni che chiamano redirect
    const navButtons = document.querySelectorAll('.btn-change-planner, .nav-link-planner');

    navButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            startLoading();
        });
    });
});