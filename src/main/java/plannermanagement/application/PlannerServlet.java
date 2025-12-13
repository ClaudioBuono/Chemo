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
import usermanagement.application.UserBean;

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
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String action = request.getParameter("action");
        final UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        try {
            if ("addAppointments".equals(action)) {
                handleAppointmentScheduling(request, response, user);
            }
        } catch (final InterruptedException e) {
            logger.log(Level.WARNING, "Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error during scheduling", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Scheduling failed");
            } catch (final IOException e1) {
                logger.log(Level.WARNING, e1.getMessage(), e1);
            }

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
        final String id = request.getParameter("id");
        final String buttonPressed = request.getParameter("buttonPressed");

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

    /**
     * Main orchestration method for the scheduling logic.
     */
    private void handleAppointmentScheduling(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) throws IOException, InterruptedException {
        // 1. Prepare Data for the AI Module
        final List<PatientJSON> patientsList = extractPatientsFromRequest(request, user);
        final List<MedicinesJSON> medicinesList = extractMedicinesFromDB(user);

        // 2. Run the External Python Logic
        final List<String> sortedPatientIds = runOptimizationScript(patientsList, medicinesList);

        if (sortedPatientIds == null || sortedPatientIds.isEmpty()) {
            throw new IllegalStateException("AI Module returned no results.");
        }

        // 3. Calculate Dates and Create Appointments
        final PlannerBean newPlanner = generatePlanner(sortedPatientIds, user);

        // 4. Save to Database
        persistPlanner(newPlanner, user);

        response.addHeader("OPERATION_RESULT", "true");
    }

    // ===================================================================================
    //                               HELPER METHODS
    // ===================================================================================

    private List<PatientJSON> extractPatientsFromRequest(final HttpServletRequest request, final UserBean user) {
        final int patientNumber = Integer.parseInt(request.getParameter("patientsNumber"));
        final List<PatientJSON> patientsJson = new ArrayList<>();

        for (int i = 0; i < patientNumber; ++i) {
            final String patientIdParam = request.getParameter("patient" + i);
            // Assuming findPatients returns a list and we need the first one
            final PatientBean patient = facade.findPatients("_id", patientIdParam, user).get(0);

            final String patientId = patient.getPatientId();
            final String medicineId = patient.getTherapy().getMedicines().get(0).getMedicineId();
            final int dose = patient.getTherapy().getMedicines().get(0).getDose();

            patientsJson.add(new PatientJSON(patientId, medicineId, dose));
        }
        return patientsJson;
    }

    private List<MedicinesJSON> extractMedicinesFromDB(final UserBean user) {
        final List<MedicinesJSON> medicinesJSONList = new ArrayList<>();
        for (final MedicineBean medicine : facade.findAllMedicines(user)) {
            int quantity = 0;
            for (final PackageBean packageBean : medicine.getPackages()) {
                quantity += packageBean.getCapacity();
            }
            medicinesJSONList.add(new MedicinesJSON(medicine.getId(), quantity));
        }
        return medicinesJSONList;
    }

    /**
     * Writes JSON inputs, executes the Python script, and reads the output.
     */
    private List<String> runOptimizationScript(final List<PatientJSON> patients, final List<MedicinesJSON> medicines) throws IOException, InterruptedException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Write Input Files
        writeJsonToFile(new File(PY_DIR, INPUT_FILE), gson.toJson(patients));
        writeJsonToFile(new File(PY_DIR, MEDICINES_FILE), gson.toJson(medicines));

        // Execute Python Process
        // Use ProcessBuilder for better control than Runtime.exec
        final ProcessBuilder pb = new ProcessBuilder("py", PYTHON_FILE_PATH);
        pb.directory(new File(PY_DIR.toURI())); // Set working directory if needed
        final Process process = pb.start();

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.log(Level.WARNING, "Python script exited with error code: {0}", exitCode);
            // Optional: Read process.getErrorStream() here for debugging
        }

        // Read Output
        final File outputFile = new File(PY_DIR, OUTPUT_FILE);
        if (!outputFile.exists() || outputFile.length() == 0) {
            return Collections.emptyList();
        }

        try (final FileReader reader = new FileReader(outputFile)) {
            final List<String> result = gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());

            // Clean up files after reading
            cleanUpFiles(new File(PY_DIR, INPUT_FILE), new File(PY_DIR, MEDICINES_FILE), outputFile);

            return result;
        }
    }

    private PlannerBean generatePlanner(final List<String> patientIds, final UserBean user) {
        final LocalDate now = LocalDate.now(TIMEZONE);

        // Determine the target week (current or next)
        final LocalDate[] weekBounds = determinePlanningWeek(now, user);
        final LocalDate firstDayOfWeek = weekBounds[0];
        final LocalDate lastDayOfWeek = weekBounds[1];

        // Generate Appointments
        final List<AppointmentBean> appointments = new ArrayList<>();
        ZonedDateTime appointmentDateTime = firstDayOfWeek.atTime(9, 0).atZone(TIMEZONE);

        int counter = 0;
        for (final String patientId : patientIds) {
            final int seat = (counter % 5) + 1;

            // Retrieve patient data for the appointment
            final PatientBean p = facade.findPatients("_id", patientId, user).get(0);
            final String medicineId = p.getTherapy().getMedicines().get(0).getMedicineId();
            final int duration = p.getTherapy().getDuration();

            // Increment hour every 5 patients
            ++counter;
            if (counter > 0 && (counter % 5) == 0) {
                appointmentDateTime = appointmentDateTime.plusHours(1);
            }

            final Date appDate = Date.from(appointmentDateTime.toInstant());
            appointments.add(new AppointmentBean(patientId, medicineId, appDate, String.valueOf(seat), duration));
        }

        final Date start = Date.from(firstDayOfWeek.atTime(9, 0).atZone(TIMEZONE).toInstant());
        final Date end = Date.from(lastDayOfWeek.atTime(15, 0).atZone(TIMEZONE).toInstant());

        return new PlannerBean(start, end, appointments);
    }

    private LocalDate[] determinePlanningWeek(final LocalDate now, final UserBean user) {
        LocalDate firstDay = now.with(previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastDay = now.with(nextOrSame(DayOfWeek.FRIDAY));

        final PlannerBean latestPlanner = facade.findLatestPlanner(user);

        if (latestPlanner != null) {
            final ZonedDateTime latestPlannerEndDate = latestPlanner.getEndDate().toInstant().atZone(TIMEZONE);
            // If current time is after the latest planner, move to next week
            if (now.atTime(LocalTime.now()).atZone(TIMEZONE).isAfter(latestPlannerEndDate)) {
                firstDay = firstDay.plusWeeks(1);
                lastDay = lastDay.plusWeeks(1);
            }
        }
        return new LocalDate[]{firstDay, lastDay};
    }

    private void persistPlanner(final PlannerBean planner, final UserBean user) {
        final PlannerBean existingPlanner = facade.findLatestPlanner(user); // Check if we are updating or inserting

        // Logic: if the start date matches the existing planner, we update. Otherwise, insert.
        // Note: You might want a better check than just Start Date equality (e.g., Planner ID)
        if (existingPlanner != null && planner.getStartDate().equals(existingPlanner.getStartDate())) {
            facade.updatePlanner("_id", planner.getId(), "start", planner.getStartDate(), user);
            facade.updatePlanner("_id", planner.getId(), "end", planner.getEndDate(), user);
            facade.updatePlanner("_id", planner.getId(), "appointments", planner.getAppointments(), user);
        } else {
            facade.insertPlanner(planner, user);
        }
    }

    // ===================================================================================
    //                               UTILITIES
    // ===================================================================================

    private void writeJsonToFile(final File file, final String jsonContent) throws IOException {
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(jsonContent);
        }
    }

    private void cleanUpFiles(final File... files) {
        for (final File f : files) {
            try (final FileWriter pw = new FileWriter(f)) {
                // Empties the file
            } catch (final IOException e) {
                logger.warning("Could not clear file: " + f.getName());
            }
        }
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
