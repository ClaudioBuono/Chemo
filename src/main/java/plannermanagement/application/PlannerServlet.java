package plannermanagement.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import connector.Facade;
import medicinemanagement.application.MedicineBean;
import medicinemanagement.application.PackageBean;
import patientmanagement.application.PatientBean;
import plannermanagement.application.green.AppointmentRecord;
import plannermanagement.application.green.CalendarGridHelper;
import plannermanagement.application.green.PlannerRecord;
import plannermanagement.application.green.PlannerSummary;
import userManagement.application.UserBean;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

@WebServlet("/PlannerServlet")
public class PlannerServlet extends HttpServlet {

    private static final Facade facade = new Facade();
    final Logger logger = Logger.getLogger(getClass().getName());

    //Modificare il valore di questo campo secondo quanto riportato nel manuale di installazione
    private static final String PY_DIR_PATH = "C:\\Users\\anton\\IdeaProjects\\Chemo\\py\\"; //Path assoluto della directory "py"

    private static final File PY_DIR = new File(PY_DIR_PATH); //Directory "py"

    private static final String PYTHON_FILE_PATH = PY_DIR_PATH + "module.py"; //File path del modulo di IA

    private static final String INPUT_FILE = "patients.json"; //Nome file di input

    private static final String MEDICINES_FILE = "medicines.json"; //Nome file medicinali usati dal modulo di IA

    private static final String OUTPUT_FILE = "resultSchedule.json"; //Nome file output

    private static final ZoneId TIMEZONE = ZoneId.systemDefault(); //Fuso orario relativo al sistema

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        // 1. SESSION & USER RETRIEVAL
        final UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        // 2. DATA RETRIEVAL
        // We fetch all planners to calculate navigation indexes
        final List<PlannerSummary> summaries = facade.findAllPlannerSummaries(user);
        if (summaries == null || summaries.isEmpty()) {
            try {
                response.sendRedirect("error.jsp");
            } catch (final IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }

            return;
        }

        // 3. NAVIGATION LOGIC
        // We resolve the correct index based on user action (prev/next/latest)
        // using a thread-safe local variable logic.
        final int currentIndex = resolvePlannerIndex(request, summaries);
        final PlannerSummary currentPlannerSummary = summaries.get(currentIndex);

        // Calculate and set IDs for navigation buttons (Previous/Next arrows)
        setupNavigationAttributes(request, summaries, currentIndex);

        // -----------------------------------------------------------
        // 4. GREEN CODING OPTIMIZATION
        // This section handles:
        // A. Architecture Refactoring (Bean -> Record)
        // B. Algorithm Optimization (O(N^2) -> O(N))
        // C. Database Access Optimization (Deduplication & Caching)
        // -----------------------------------------------------------
        final PlannerBean plannerToVisualize = facade.findPlannerById(currentPlannerSummary.id(), user);
        prepareGreenData(request, plannerToVisualize, user);

        // 5. VIEW RENDERING
        // Set standard attributes for the JSP header/title
        request.setAttribute("plannerToVisualize", plannerToVisualize);
        request.setAttribute("weekDate", getWeekDate(plannerToVisualize.getStartDate()));

        try {
            getServletContext().getRequestDispatcher(response.encodeURL("/planner.jsp")).forward(request, response);
        } catch (final ServletException | IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Recupero l'action dalla request
        String action = request.getParameter("action");

        //Recupero l'utente dalla sessione
        UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        try {
            if ("addAppointments".equals(action)) {
                //Recupero il numero di pazienti dalla request
                int patientNumber = Integer.parseInt(request.getParameter("patientsNumber"));

                //Inserisco ogni paziente nell'array di PatientJSON
                List<PatientJSON> patientsJson = new ArrayList<>();
                for (int i = 0; i < patientNumber; i++) {
                    //Recupero l'id paziente
                    String patientId = request.getParameter("patient" + i);

                    //Recupero il paziente corrispondente
                    PatientBean patient = facade.findPatients("_id", patientId, user).get(0);

                    //Inserisco i dati del paziente in PatientJSON
                    patientId = patient.getPatientId();
                    String medicineId = patient.getTherapy().getMedicines().get(0).getMedicineId(); //Assumiamo 1 medicinale per seduta, prendo il primo a scopo di esempio
                    int dose = patient.getTherapy().getMedicines().get(0).getDose();
                    PatientJSON patientJSON = new PatientJSON(patientId, medicineId, dose);

                    //Aggiungo il paziente all'array di pazientiJSON
                    patientsJson.add(patientJSON);
                }

                //Inizializzo GSON
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                //Recupero il file di input
                File inputFile = new File(PY_DIR, INPUT_FILE);

                String json = gson.toJson(patientsJson, new TypeToken<List<PatientJSON>>() {}.getType());

                //Scrivo nel file JSON
                try (FileWriter writer = new FileWriter(inputFile)) {
                    writer.append(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                //Recupero tutti i medicinali con le relative quantità disponibili
                List<MedicinesJSON> medicinesJSONList = new ArrayList<>();
                for (MedicineBean medicine : facade.findAllMedicines(user)) {
                    String medicineId = medicine.getId();
                    int quantity = 0;
                    for (PackageBean packageBean : medicine.getPackages()) {
                        quantity += packageBean.getCapacity();
                    }
                    medicinesJSONList.add(new MedicinesJSON(medicineId, quantity));
                }

                //Recupero il file dei medicinali
                File medicinesFile = new File(PY_DIR, MEDICINES_FILE);

                json = gson.toJson(medicinesJSONList, new TypeToken<List<PatientJSON>>() {}.getType());

                //Scrivo nel file JSON
                try (FileWriter writer = new FileWriter(medicinesFile)) {
                    writer.append(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                //Inizializzo la lista dei pazienti di output
                List<String> patientIds = null;

                while (patientIds == null) {
                    //Faccio eseguire il processo del modulo di IA
                    Process pythonProcess = Runtime.getRuntime().exec(new String[]{"py", PYTHON_FILE_PATH});

                    //Attendo che il processo di python abbia finito
                    pythonProcess.waitFor();

                    //Recupero il file di output
                    File outputFile = new File(PY_DIR, OUTPUT_FILE);

                    //Apro il file JSON contenente i risultati del modulo di IA
                    try (FileReader reader = new FileReader(outputFile)) {

                        //Converto l'array di JSON in una lista di String
                        patientIds = gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //Recupero la data in cui è stata fatta la richiesta
                LocalDate now = LocalDate.now(TIMEZONE);

                //Calcolo primo e ultimo giorno della settimana corrente
                LocalDate firstDayOfWeek = now.with(previousOrSame(DayOfWeek.MONDAY));
                LocalDate lastDayOfWeek = now.with(nextOrSame(DayOfWeek.FRIDAY));

                //Recupero l'ultimo planner presente nel db
                PlannerBean latestPlanner = facade.findLatestPlanner(user);

                //Se siamo al venerdì, è necessario schedulare per la settimana successiva
                ZonedDateTime latestPlannerEndDate = latestPlanner.getEndDate().toInstant().atZone(TIMEZONE);
                if(now.atTime(LocalTime.now()).atZone(TIMEZONE).isAfter(latestPlannerEndDate)) {
                    firstDayOfWeek = firstDayOfWeek.plusWeeks(1);
                    lastDayOfWeek = lastDayOfWeek.plusWeeks(1);
                }


                //Popolo la lista di appuntamenti
                int i = 0;
                ArrayList<AppointmentBean> appointments = new ArrayList<>();
                ZonedDateTime appointmentDateTime = firstDayOfWeek.atTime(9, 0).atZone(TIMEZONE);
                for (String patientId : patientIds) {
                    int seat = (i % 5)+1;
                    String medicineId = facade.findPatients("_id", patientId, user).get(0).getTherapy().getMedicines().get(0).getMedicineId();

                    //Si considerano 5 sedute per ora
                    i++;
                    if ((i % 5) == 0)
                        appointmentDateTime = appointmentDateTime.plusHours(1);


                    Date appointmentDate = Date.from(appointmentDateTime.toInstant());
                    int duration = facade.findPatients("_id", patientId, user).get(0).getTherapy().getDuration();
                    appointments.add(new AppointmentBean(patientId, medicineId, appointmentDate, String.valueOf(seat), duration));
                }

                //Converto primo e ultimo giorno della settimana in Date
                Date firstDay = Date.from(firstDayOfWeek.atTime(9, 0).atZone(TIMEZONE).toInstant());
                Date lastDay = Date.from(lastDayOfWeek.atTime(15, 0).atZone(TIMEZONE).toInstant());

                //Creo il planner da inserire nel database
                PlannerBean planner = new PlannerBean(firstDay, lastDay, appointments);

                //Se è già presente un planner per la settimana corrente allora va sovrascritto, altrimenti va inserito normalmente
                if(firstDay.equals(latestPlanner.getStartDate())) {
                    facade.updatePlanner("_id", planner.getId(), "start", planner.getStartDate(), user);
                    facade.updatePlanner("_id", planner.getId(), "end", planner.getEndDate(), user);
                    facade.updatePlanner("_id", planner.getId(), "appointments", planner.getAppointments(), user);
                }
                else
                    facade.insertPlanner(planner, user);

                //Aggiungo l'operation result all'header
                response.addHeader("OPERATION_RESULT", "true");

                //Svuoto il contenuto dei file
                FileWriter pw = new FileWriter(inputFile);
                pw.close();

                pw = new FileWriter(medicinesFile);
                pw.close();

                pw = new FileWriter(new File(PY_DIR, OUTPUT_FILE));
                pw.close();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Interrupted!", e);
            Thread.currentThread().interrupt();
        }

    }

    // ==========================================
    // WORKER METHODS (Business Logic Handlers)
    // ==========================================

    /**
     * Determines which planner index should be displayed based on the request parameters.
     * It handles 'latest', 'prev', and 'next' logic in a thread-safe manner.
     *
     * @param request The HTTP request containing 'id' and 'buttonPressed'
     * @param planners The list of all available planners
     * @return The index of the planner to display
     */
    private int resolvePlannerIndex(final HttpServletRequest request, final List<PlannerSummary> planners) {
        String id = request.getParameter("id");
        String buttonPressed = request.getParameter("buttonPressed");

        // Default: Show the latest available planner
        final int lastIndex = planners.size() - 1;

        // If it's the first load, return the latest
        if (id == null) {
            return lastIndex;
        }

        // Find the index of the planner currently being viewed (before navigation)
        int currentDisplayedIndex = 0;
        final int plannersCount = planners.size();
        for(int i = 0; i < plannersCount; ++i) {
            if(planners.get(i).id().equals(id)) {
                currentDisplayedIndex = i;
                break;
            }
        }

        // Calculate new index based on user action
        if (buttonPressed == null) return currentDisplayedIndex;

        return switch (buttonPressed) {
            case "latest" -> lastIndex;
            case "prev" -> Math.max(0, currentDisplayedIndex - 1);
            case "next" -> Math.min(lastIndex, currentDisplayedIndex + 1);
            default -> currentDisplayedIndex;
        };
    }

    /**
     * Sets the request attributes required by the frontend to render navigation arrows.
     */
    private void setupNavigationAttributes(final HttpServletRequest request, final List<PlannerSummary> planners, final int currentIndex) {
        String beforeId = "";
        String afterId = "";
        final String latestId = planners.get(planners.size() - 1).id();

        if (currentIndex > 0) {
            beforeId = planners.get(currentIndex - 1).id();
        }
        if (currentIndex < planners.size() - 1) {
            afterId = planners.get(currentIndex + 1).id();
        }

        request.setAttribute("latestPlannerId", latestId);
        request.setAttribute("beforeVisualizedId", beforeId);
        request.setAttribute("afterVisualizedId", afterId);
    }

    /**
     * CORE OPTIMIZATION METHOD.
     * It transforms legacy data into optimized structures and pre-fetches database entities
     * to avoid the "N+1 Select Problem" in the view layer.
     */
    private void prepareGreenData(final HttpServletRequest request, final PlannerBean plannerToVisualize, final UserBean user) {
        // A. ADAPTER PATTERN: Convert Mutable Bean to Immutable Record
        final PlannerRecord greenPlanner = convertToRecord(plannerToVisualize);

        // B. ALGORITHMIC OPTIMIZATION: Use the O(N) approach to calculate the grid
        final CalendarGridHelper gridHelper = new CalendarGridHelper();
        final Map<String, List<AppointmentRecord>> optimizedGrid = gridHelper.preCalculateGrid(greenPlanner);
        request.setAttribute("gridMap", optimizedGrid);

        // C. DATABASE OPTIMIZATION (Batch Fetching / Caching)
        // Instead of querying the DB for every single appointment in the JSP (causing Connection Storm),
        // we extract unique IDs and fetch them once.

        // Flatten the map to get all appointments in the current view
        final List<AppointmentRecord> allAppointments = optimizedGrid.values().stream()
                .flatMap(List::stream)
                .toList();

        final Map<String, PatientBean> patientCache = new HashMap<>();
        final Map<String, MedicineBean> medicineCache = new HashMap<>();

        // Loop through appointments and populate caches (Data Deduplication)
        for (final AppointmentRecord app : allAppointments) {
            // Fetch Patient only if not already in cache
            if (!patientCache.containsKey(app.idPatient())) {
                final ArrayList<PatientBean> res = (ArrayList<PatientBean>) facade.findPatients("_id", app.idPatient(), user);
                if (res != null && !res.isEmpty()) {
                    patientCache.put(app.idPatient(), res.get(0));
                }
            }
            // Fetch Medicine only if not already in cache
            if (!medicineCache.containsKey(app.idMedicine())) {
                final ArrayList<MedicineBean> res = (ArrayList<MedicineBean>) facade.findMedicines("_id", app.idMedicine(), user);
                if (res != null && !res.isEmpty()) {
                    medicineCache.put(app.idMedicine(), res.get(0));
                }
            }
        }

        // Pass caches to the View
        request.setAttribute("patientCache", patientCache);
        request.setAttribute("medicineCache", medicineCache);
    }

    /**
     * Helper to convert Legacy Beans to Java Records.
     * Uses LocalDateTime for better time manipulation performance.
     */
    private PlannerRecord convertToRecord(final PlannerBean bean) {
        if (bean == null) return null;
        final List<AppointmentRecord> greenApps = bean.getAppointments().stream()
                .map(app -> new AppointmentRecord(
                        app.getIdPatient(),
                        app.getIdMedicine(),
                        app.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        app.getChair(),
                        app.getDuration()
                ))
                .toList();

        return new PlannerRecord(
                bean.getId(),
                bean.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                bean.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                greenApps
        );
    }

    /**
     * Formats the date range string
     */
    private String getWeekDate(final Date start) {
        // 1. Conversione da Date a LocalDate
        final LocalDate startDate = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // 2. Impostiamo il Formatter in ITALIANO
        // "MMMM" indica il nome completo del mese (es. "gennaio")
        final DateTimeFormatter italianFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ITALIAN);

        // 3. Otteniamo il nome del mese dalla data (uso startDate come facevi tu)
        String monthName = startDate.format(italianFormatter);

        // 4. Mettiamo la prima lettera Maiuscola (Java lo restituisce minuscolo: "gennaio" -> "Gennaio")
        if (!monthName.isEmpty()) {
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        }

        // 5. Restituiamo la stringa formattata: "12 - 16 Gennaio 2026"
        return monthName + " " + startDate.getYear();
    }


    //Inner Class che contiene i dati da passare a patients.json
        private static class PatientJSON {
        String patientId;
        String medicineId;
        int dose;

        //Costruttori

        public PatientJSON() {
        }

        public PatientJSON(String patientId, String medicineId, int dose) {
            this.patientId = patientId;
            this.medicineId = medicineId;
            this.dose = dose;
        }

        @Override
        public String toString() {
            return "PatientJSON{" +
                    "patientId='" + patientId + '\'' +
                    ", medicineId='" + medicineId + '\'' +
                    ", dose=" + dose +
                    '}';
        }
    }


    //Inner Class che contiene i dati da passare a medicines.json
    private static class MedicinesJSON {
        private String medicineId;
        private int quantity;

        //Costruttore

        public MedicinesJSON(String medicineId, int quantity) {
            this.medicineId = medicineId;
            this.quantity = quantity;
        }

        //toString

        @Override
        public String toString() {
            return "MedicinesJSON{" +
                    "medicineId='" + medicineId + '\'' +
                    ", quantity=" + quantity +
                    '}';
        }
    }
}
