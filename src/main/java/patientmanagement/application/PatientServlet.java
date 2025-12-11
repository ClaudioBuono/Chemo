package patientmanagement.application;

import connector.Facade;
import userManagement.application.UserBean;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/PatientServlet")
public class PatientServlet extends HttpServlet {
    private static final String SURNAME = "surname";
    private static final String TAX_CODE = "taxCode";
    private static final String NAME = "name";
    private static final String BIRTH_DATE = "birthDate";
    private static final String CITY = "city";
    private static final String PHONE_NUMBER = "phoneNumber";
    private static final String NOTES = "notes";
    private static final String STATUS = "status";
    private static final String MEDICINE = "medicine";
    private static final String ACTION = "action";
    final Logger logger = Logger.getLogger(getClass().getName());

    static final int PAGE_SIZE = 10;
    static Facade facade = new Facade();
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        // Get current user
        final UserBean user = getSessionUserOrRedirect(request, response);
        if (user == null) return;

        // Routing logic
        final String action = request.getParameter(ACTION);
        final String id = request.getParameter("id");

        try {
            if ("viewAvailablePatients".equals(action)) {
                handleAvailablePatientsView(request, response, user);

            } else if (id != null) {
                redirectToPatientDetailsPage(request, response, id, user);

            } else {
                handleStandardPatientListView(request, response, user);
            }
        } catch (ServletException | IOException e) {
            logger.log(Level.SEVERE, "Error in doGet dispatching", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Recupero l'action dalla request
        String action = request.getParameter(ACTION);

        //Recupero l'utente dalla sessione
        UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        try {
            switch (action) {
                case "createPatientProfile" -> {  //Creazione profilo paziente
                    PatientBean patient = new PatientBean(
                            request.getParameter(TAX_CODE),
                            request.getParameter(NAME),
                            request.getParameter(SURNAME),
                            dateParser(request.getParameter(BIRTH_DATE)),
                            request.getParameter(CITY),
                            request.getParameter(PHONE_NUMBER),
                            false ,
                            request.getParameter(NOTES));

                    if (!patientValidation(patient)) {
                        response.addHeader("OPERATION_RESULT","false");
                        response.addHeader("ERROR_MESSAGE","I dati inseriti non sono validi.");
                    } else {
                        patient = facade.insertPatient(patient,user);

                        //salvataggio id paziente nel response header così da poterlo reindirizzare alla sua pagina
                        response.addHeader("OPERATION_RESULT","true");
                        response.addHeader("PATIENT_ID", patient.getPatientId());
                    }
                }

                case "completePatientProfile" -> {  //Completamento profilo paziente e modifica terapia
                    //Recupero l'id del paziente
                    String patientId = request.getParameter("id");

                    //Recupero i dati della terapia dalla request
                    String condition = request.getParameter("condition");
                    int duration = Integer.parseInt(request.getParameter("duration"));
                    int frequency = Integer.parseInt(request.getParameter("frequency"));
                    int sessions = Integer.parseInt(request.getParameter("sessions"));
                    ArrayList<TherapyMedicineBean> medicines = new ArrayList<>();
                    //Recupero i medicinali dalla request e li aggiungo a medicines
                    int medicinesNumber = Integer.parseInt(request.getParameter("medicinesNumber"));
                    String currentMedicineId, currentMedicineDose;
                    for(int i = 0; i < medicinesNumber; i++) {
                        currentMedicineId = "medicineId" + i;
                        currentMedicineDose = "medicineDose" + i;
                        TherapyMedicineBean medicine = new TherapyMedicineBean(request.getParameter(currentMedicineId), Integer.parseInt(request.getParameter(currentMedicineDose)));
                        medicines.add(medicine);
                    }

                    TherapyBean therapy = new TherapyBean(
                            sessions,
                            medicines,
                            duration,
                            frequency
                    );

                    if (!therapyValidation(condition, therapy)) {
                        response.addHeader("OPERATION_RESULT","false");
                        response.addHeader("ERROR_MESSAGE","I dati inseriti non sono validi.");
                    } else {
                        //Aggiorno il profilo paziente con la malattia
                        facade.updatePatient("_id", patientId, "condition", request.getParameter("condition"), user);
                        //Inserisco la terapia
                        facade.insertTherapy(patientId, therapy, user);

                        //Reindirizzo alla pagina del paziente appena creato
                        response.addHeader("OPERATION_RESULT","true");
                        response.addHeader("PATIENT_ID", request.getParameter("id"));
                    }
                }

                case "editPatientStatus" -> { //Modifica stato paziente
                    String operationResult;
                    String patientId = request.getParameter("id");

                    ArrayList<PatientBean> patients = facade.findPatients("_id", patientId, user);
                    //controlla se esiste una condition e una terapia per quel paziente
                    if (patients.get(0).getTherapy() == null || request.getParameter(STATUS) == null) {
                        //se non esiste c'è un errore e l'operazione fallisce
                        operationResult = "false";
                        response.addHeader("ERROR_MESSAGE","Modifica stato fallita.");
                    } else {
                        final boolean patientStatus = Boolean.parseBoolean(request.getParameter(STATUS));
                        //se esiste viene effettuata la modifica
                        operationResult = "true";
                        if (patients.get(0).getStatus() != patientStatus) {
                            //Aggiorno lo stato del paziente
                            facade.updatePatient("_id", patientId, STATUS, patientStatus, user);
                        }
                    }

                    //Reindirizzo alla pagina del paziente appena creato
                    response.addHeader("OPERATION_RESULT",operationResult);
                }
            }
        }
        catch (Throwable e) {
            System.out.println(e.getMessage());
        }
    }

    // ==========================================
    // WORKER METHODS (Business Logic Handlers)
    // ==========================================

    /**
     * Handles the "New Appointments" view.
     * Fetches only patients with status=true (Available) and forwards to addAppointments.jsp.
     */
    private void handleAvailablePatientsView(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) throws ServletException, IOException {
        // Setup Pagination
        int page = parsePageParameter(request);

        // Fixed Filter: Only "Available" patients
        final ArrayList<String> keys = new ArrayList<>();
        final ArrayList<Object> values = new ArrayList<>();
        keys.add(STATUS);
        values.add(true);

        // Retrieve Data
        final ArrayList<PatientBean> paginatedResult = (ArrayList<PatientBean>) facade.findPatientsPaginated(keys, values, page, PAGE_SIZE, user);
        final long totalRecords = facade.countPatientsFiltered(keys, values, user);

        // Setup JSP Attributes
        setupPaginationAttributes(request, page, totalRecords);
        request.setAttribute("availablePatients", paginatedResult); // Specific attribute name for this view

        // Maintain the action in pagination links
        request.setAttribute("searchParams", "&action=viewAvailablePatients");

        // Forward
        request.getRequestDispatcher("addAppointments.jsp").forward(request, response);
    }

    /**
     * Handles the standard "Patient List" view.
     * Supports dynamic search filters, pagination, and Green IT ETag caching.
     */
    private void handleStandardPatientListView(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) throws ServletException, IOException {
        // Setup Pagination
        final int page = parsePageParameter(request);

        // Build Dynamic Filters
        final ArrayList<String> keys = new ArrayList<>();
        final ArrayList<Object> values = new ArrayList<>();
        final StringBuilder searchParams = buildParameters(request, keys, values);

        // Retrieve Data
        final ArrayList<PatientBean> paginatedResult = (ArrayList<PatientBean>) facade.findPatientsPaginated(keys, values, page, PAGE_SIZE, user);
        long totalRecords = facade.countPatientsFiltered(keys, values, user);

        // ETag Check
        // If the browser already has this data, send 304 and stop execution.
//        if (manageETagCache(request, response, paginatedResult, page, totalRecords)) {
//            return;
//        }

        // Setup JSP Attributes
        setupPaginationAttributes(request, page, totalRecords);
        request.setAttribute("patientsResult", paginatedResult); // Standard attribute name
        request.setAttribute("searchParams", searchParams.toString());

        // Forward
        request.getRequestDispatcher("patientList.jsp").forward(request, response);
    }

    // ===============
    // HELPER METHODS
    // ===============

    /**
     * Retrieves the current user from the session or redirects to error401 if invalid.
     */
    private UserBean getSessionUserOrRedirect(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        UserBean user = (session != null) ? (UserBean) session.getAttribute("currentSessionUser") : null;
        if (user == null) {
            try {
                response.sendRedirect("error401.jsp");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Redirect failed", e);
            }
        }
        return user;
    }

    /**
     * Safely parses the 'page' parameter from the request. Defaults to 1 on error.
     */
    private int parsePageParameter(HttpServletRequest request) {
        if (request.getParameter("page") != null) {
            try {
                return Integer.parseInt(request.getParameter("page"));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid page format, defaulting to 1", e);
            }
        }
        return 1;
    }

    /**
     * Calculates total pages and sets the common pagination attributes.
     */
    private void setupPaginationAttributes(HttpServletRequest request, int page, long totalRecords) {
        int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
    }

    private void redirectToPatientDetailsPage(final HttpServletRequest request, final HttpServletResponse response, final String id, final UserBean user) {
        // Find the patient
        final ArrayList<PatientBean> patients = facade.findPatients("_id", id, user);
        if (!patients.isEmpty()) {
            request.setAttribute("patient", patients.get(0));
            try {
                // Redirect to patient details page
                getServletContext().getRequestDispatcher(response.encodeURL("/patientDetails.jsp")).forward(request, response);
            } catch (final ServletException e) {
                logger.log(Level.SEVERE, "ServletException: ", e);
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "IOException: ", e);
            }
        } else {
            try {
                response.sendRedirect("error404.jsp");
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "IOException: ", e);
            }
        }
    }


    private StringBuilder buildParameters(final HttpServletRequest request, final ArrayList<String> keys, final ArrayList<Object> values) {
        final StringBuilder searchParams = new StringBuilder(128);

        // Check if action requested is a filtered search
        final String action = request.getParameter(ACTION);
        if ("searchPatient".equals(action)) {
            logger.info("Action: searchPatient");
            searchParams.append("&action=searchPatient");

            // --- Name filter ---
            final String name = request.getParameter(NAME);
            if (name != null && !name.trim().isEmpty()) {
                keys.add(NAME);
                values.add(name.trim());
                searchParams.append("&name=").append(name.trim());
            }

            // --- Surname filter ---
            final String surname = request.getParameter(SURNAME);
            if (surname != null && !surname.trim().isEmpty()) {
                keys.add(SURNAME);
                values.add(surname.trim());
                searchParams.append("&surname=").append(surname.trim());
            }

            // --- Medicine filter ---
            final String medicine = request.getParameter("patientMedicine");
            if (medicine != null && !medicine.equals("null") && !medicine.isEmpty()) {
                keys.add(MEDICINE);
                values.add(medicine);
                searchParams.append("&patientMedicine=").append(medicine);
            }

            // --- Patient status filter ---
            final String status = request.getParameter("patientStatus");
            if (status != null && !status.equals("na") && !status.isEmpty()) {
                keys.add(STATUS);
                values.add(Boolean.parseBoolean(status));
                searchParams.append("&patientStatus=").append(status);
            }
        }

        return searchParams;
    }

    /**
     * Implements the Conditional GET logic (ETag).
     * Calculates a unique hash for the current data view. If it matches the client's cache,
     * returns 304 Not Modified to save bandwidth and server CPU (JSP rendering).
     */
//    private boolean manageETagCache(final HttpServletRequest request, final HttpServletResponse response,
//                                    final ArrayList<PatientBean> results, final int page, final long totalRecords) {
//
//        // Calculate the unique signature (Hash) of the visible data.
//        final String contentSignature = results.toString() + page + totalRecords;
//
//        // Generate the ETag (W/ indicates a "Weak ETag", sufficient for semantic comparison)
//        final String eTag = "W/\"" + contentSignature.hashCode() + "\"";
//
//        // Check the header sent by the browser
//        final String incomingETag = request.getHeader("If-None-Match");
//
//        // Compare the ETags
//        if (incomingETag != null && incomingETag.equals(eTag)) {
//            // MATCH. he client has the latest version.
//            // Return 304. The server will NOT send the JSP body.
//            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
//            return true; // Stop execution
//        }
//
//        // No MATCH. Set the ETag for the next visit and proceed.
//        response.setHeader("ETag", eTag);
//        return false;
//    }

    // ==============================
    // PARSING AND VALIDATION METHODS
    // ==============================
    private Date dateParser(String date) {
        SimpleDateFormat pattern = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return pattern.parse(date);
        }
        catch (Exception e) {
            return null;
        }
    }
    private String dateReverseParser(Date date) {
        DateFormat pattern = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return pattern.format(date);
        }
        catch (Exception e) {
            return null;
        }
    }

    private boolean patientValidation(PatientBean patient){
        if (!namesValidity(patient.getName())) {
            return false;
        }
        if (!namesValidity(patient.getSurname())) {
            return false;
        }
        if (!dateValidity(dateReverseParser(patient.getBirthDate()))) {
            return false;
        }
        if (!cityValidity(patient.getCity())) {
            return false;
        }
        if (!taxCodeValidity(patient.getTaxCode())) {
            return false;
        }
        if (!phoneNumberValidity(patient.getPhoneNumber())) {
            return false;
        }
        if (!notesValidity(patient.getNotes())) {
            return false;
        }
        return true;
    }
    private boolean therapyValidation(String condition, TherapyBean therapy){
        if (!conditionValidity(condition)) {
            return false;
        }
        if (!sessionsValidity(therapy.getSessions())) {
            return false;
        }
        if (!frequencyValidity(therapy.getFrequency())) {
            return false;
        }
        if (!durationValidity(therapy.getDuration())) {
            return false;
        }
        for (TherapyMedicineBean medicine: therapy.getMedicines()) {
            if (!idValidity(medicine.getMedicineId())) {
                return false;
            }
            if (!doseValidity(medicine.getDose())) {
                return false;
            }
        }

        return true;
    }
    private boolean idValidity(String id) {
        String format = "^[a-f\\d]{24}$";
        return id.matches(format);
    }
    private boolean numberValidity(String notes) {
        String format = "^[0-9]+$";
        return notes.matches(format);
    }
    private boolean dateValidity(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate newDate = LocalDate.parse(date, formatter );
        LocalDate currentDate = LocalDate.now();
        if (newDate.isAfter(currentDate))
            return false;
        String format = "^(19|20)[0-9]{2}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$";
        return date.matches(format);
    }
    private boolean nameValidity(String name) {
        String format = "^[A-Za-z][A-Za-z'-]+([ A-Za-z][A-Za-z'-]+)*$";
        return name.matches(format);
    }
    private boolean namesValidity(String name) {
        if (name.length() > 32)
            return false;
        return nameValidity(name);
    }
    private boolean cityValidity(String city) {
        if (city.length() > 32)
            return false;
        return nameValidity(city);
    }
    private boolean taxCodeValidity(String taxCode) {
        String format = "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$";
        return taxCode.matches(format);
    }
    private boolean phoneNumberValidity(String phoneNumber) {
        String format = "^[+]?[(]?[0-9]{2,3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,7}$";
        return phoneNumber.matches(format);
    }
    private boolean notesValidity(String notes) {
        if (notes.length() > 255)
            return false;
        String format = "^[A-Za-z0-9][A-Za-z0-9'.,\\n-]+([ A-Za-z0-9][A-Za-z0-9'.,\\n-]+)*$";
        return notes.matches(format);
    }
    private boolean conditionValidity(String condition) {
        return nameValidity(condition);
    }
    private boolean sessionsValidity(Integer sessions) {
        return numberValidity(String.valueOf(sessions));
    }
    private boolean frequencyValidity(Integer frequency) {
        return numberValidity(String.valueOf(frequency));
    }
    private boolean durationValidity(Integer duration) {
        return numberValidity(String.valueOf(duration));
    }
    private boolean doseValidity(Integer dose) {
        return numberValidity(String.valueOf(dose));
    }
}
