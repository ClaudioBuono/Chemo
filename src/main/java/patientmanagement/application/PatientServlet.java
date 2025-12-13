package patientmanagement.application;

import connector.Facade;
import medicinemanagement.application.MedicineBean;
import userManagement.application.UserBean;

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
    private static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    private static final String OPERATION_RESULT = "OPERATION_RESULT";
    public static final String FALSE = "false";
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
        } catch (final ServletException | IOException e) {
            logger.log(Level.SEVERE, "Error in doGet dispatching", e);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        // Recupero action e user
        final String action = request.getParameter(ACTION);
        final UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        try {
            if (action == null) return;

            switch (action) {
                case "createPatientProfile" -> handleCreatePatientProfile(request, response, user);
                case "completePatientProfile" -> handleCompletePatientProfile(request, response, user);
                case "editPatientStatus" -> handleEditPatientStatus(request, response, user);
                default -> logger.log(Level.WARNING, "Azione sconosciuta: {0}", action);
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Errore nella doPost", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server");
            } catch (final IOException ex) {
                logger.log(Level.SEVERE, "Errore nella doPost", e);
            }
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
        final int page = parsePageParameter(request);

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
        final long totalRecords = facade.countPatientsFiltered(keys, values, user);

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

    /**
     * Handles the creation of a new patient profile.
     * Validates input data and persists the patient if valid.
     */
    private void handleCreatePatientProfile(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) {
        PatientBean patient = new PatientBean(
                request.getParameter(TAX_CODE),
                request.getParameter(NAME),
                request.getParameter(SURNAME),
                dateParser(request.getParameter(BIRTH_DATE)),
                request.getParameter(CITY),
                request.getParameter(PHONE_NUMBER),
                false,
                request.getParameter(NOTES));

        if (!patientValidation(patient)) {
            response.addHeader(OPERATION_RESULT, FALSE);
            response.addHeader(ERROR_MESSAGE, "Invalid input data for patient creation.");
        } else {
            patient = facade.insertPatient(patient, user);
            response.addHeader(OPERATION_RESULT, "true");
            response.addHeader("PATIENT_ID", patient.getPatientId());
        }
    }

    /**
     * Handles the completion of the patient profile by adding/editing a therapy.
     * Orchestrates data parsing, medicine lookup logic, and data persistence.
     */
    private void handleCompletePatientProfile(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) {
        final String patientId = request.getParameter("id");
        final String condition = request.getParameter("condition");

        // Parse numeric parameters
        final int duration = Integer.parseInt(request.getParameter("duration"));
        final int frequency = Integer.parseInt(request.getParameter("frequency"));
        final int sessions = Integer.parseInt(request.getParameter("sessions"));
        final int medicinesNumber = Integer.parseInt(request.getParameter("medicinesNumber"));

        // Extract and validate medicines using the helper method
        final ArrayList<TherapyMedicineBean> medicines;
        try {
            medicines = extractMedicinesFromRequest(request, medicinesNumber);
        } catch (final IllegalArgumentException e) {
            // Handle specific lookup errors (e.g., medicine name not found)
            response.addHeader(OPERATION_RESULT, FALSE);
            response.addHeader(ERROR_MESSAGE, e.getMessage());
            return;
        }

        final TherapyBean therapy = new TherapyBean(sessions, medicines, duration, frequency);

        if (!therapyValidation(condition, therapy)) {
            response.addHeader(OPERATION_RESULT, FALSE);
            response.addHeader(ERROR_MESSAGE, "Invalid therapy data.");
        } else {
            // Update patient condition and insert the therapy
            facade.updatePatient("_id", patientId, "condition", condition, user);
            facade.insertTherapy(patientId, therapy, user);

            response.addHeader(OPERATION_RESULT, "true");
            response.addHeader("PATIENT_ID", patientId);
        }
    }

    /**
     * Helper method per estrarre e validare i medicinali dalla request.
     * Gestisce la logica di Lookup (Nome -> ID).
     */
    private ArrayList<TherapyMedicineBean> extractMedicinesFromRequest(final HttpServletRequest request, final int medicinesNumber) {
        final ArrayList<TherapyMedicineBean> medicines = new ArrayList<>();

        for (int i = 0; i < medicinesNumber; ++i) {
            final String inputVal = request.getParameter("medicineId" + i); // Qui arriva il NOME
            final int doseVal = Integer.parseInt(request.getParameter("medicineDose" + i));
            String realIdToSave = null;

            // A. Retrocompatibilità ID
            if (inputVal != null && inputVal.matches("^[0-9a-fA-F]{24}$")) {
                realIdToSave = inputVal;
            }
            // B. Lookup per Nome
            else if (inputVal != null) {
                final MedicineBean foundMed = facade.findMedicineByName(inputVal.trim());
                if (foundMed != null) {
                    realIdToSave = foundMed.getId();
                } else {
                    // Lanciamo eccezione per interrompere il flusso pulitamente
                    throw new IllegalArgumentException("Errore: Il medicinale '" + inputVal + "' non esiste nel sistema.");
                }
            }

            if (realIdToSave != null) {
                medicines.add(new TherapyMedicineBean(realIdToSave, doseVal));
            }
        }
        return medicines;
    }

    /**
     * Handles the update of the patient's status (Available/Unavailable).
     */
    private void handleEditPatientStatus(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) {
        final String patientId = request.getParameter("id");
        final ArrayList<PatientBean> patients = (ArrayList<PatientBean>) facade.findPatients("_id", patientId, user);
        final String statusParam = request.getParameter(STATUS);

        // Validation check
        if (patients.isEmpty() || patients.get(0).getTherapy() == null || statusParam == null) {
            response.addHeader(OPERATION_RESULT, FALSE);
            response.addHeader(ERROR_MESSAGE, "Failed to update status: Invalid patient or missing therapy.");
        } else {
            final boolean newStatus = Boolean.parseBoolean(statusParam);
            // Only update if the status has actually changed
            if (patients.get(0).getStatus() != newStatus) {
                facade.updatePatient("_id", patientId, STATUS, newStatus, user);
            }
            response.addHeader(OPERATION_RESULT, "true");
        }
    }

    // ===============
    // HELPER METHODS
    // ===============

    /**
     * Retrieves the current user from the session or redirects to error401 if invalid.
     */
    private UserBean getSessionUserOrRedirect(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpSession session = request.getSession(false);
        final UserBean user = (session != null) ? (UserBean) session.getAttribute("currentSessionUser") : null;
        if (user == null) {
            try {
                response.sendRedirect("error401.jsp");
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "Redirect failed", e);
            }
        }
        return user;
    }

    /**
     * Safely parses the 'page' parameter from the request. Defaults to 1 on error.
     */
    private int parsePageParameter(final HttpServletRequest request) {
        if (request.getParameter("page") != null) {
            try {
                return Integer.parseInt(request.getParameter("page"));
            } catch (final NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid page format, defaulting to 1", e);
            }
        }
        return 1;
    }

    /**
     * Calculates total pages and sets the common pagination attributes.
     */
    private void setupPaginationAttributes(final HttpServletRequest request, final int page, final long totalRecords) {
        int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
    }

    private void redirectToPatientDetailsPage(final HttpServletRequest request, final HttpServletResponse response, final String id, final UserBean user) {
        // Find the patient
        final ArrayList<PatientBean> patients = (ArrayList<PatientBean>) facade.findPatients("_id", id, user);
        if (!patients.isEmpty()) {
            enrichPatientsWithMedicineNames(patients, user);
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


    /**
     * Builds the dynamic search parameters for patient filtering.
     */
    private StringBuilder buildParameters(final HttpServletRequest request, final ArrayList<String> keys, final ArrayList<Object> values) {
        final StringBuilder searchParams = new StringBuilder(128);

        // Check if action requested is a filtered search
        final String action = request.getParameter(ACTION);

        // Early return pattern: reduces nesting indentation
        if (!"searchPatient".equals(action)) {
            return searchParams;
        }

        searchParams.append("&action=searchPatient");

        // Delegate logic to helper methods
        addStringFilter(request, NAME, keys, values, searchParams);
        addStringFilter(request, SURNAME, keys, values, searchParams);
        addMedicineFilter(request, keys, values, searchParams);
        addStatusFilter(request, keys, values, searchParams);

        return searchParams;
    }

    /**
     * Helper method to handle standard string filters (Name, Surname).
     */
    private void addStringFilter(final HttpServletRequest request, final String paramName, final ArrayList<String> keys, final ArrayList<Object> values, final StringBuilder searchParams) {
        final String value = request.getParameter(paramName);
        if (value != null && !value.trim().isEmpty()) {
            keys.add(paramName); // Assuming constant name matches param name
            values.add(value.trim());
            searchParams.append("&").append(paramName).append("=").append(value.trim());
        }
    }

    /**
     * Handles the medicine filter logic, including the Name-to-ID lookup strategy.
     * Checks if the input is a raw ID (legacy) or a Name (autocomplete) and resolves the correct DB ID.
     */
    private void addMedicineFilter(final HttpServletRequest request, final ArrayList<String> keys, final ArrayList<Object> values, final StringBuilder searchParams) {
        final String medicineInput = request.getParameter("patientMedicine");

        // Robustness check: not null, not string "null", not empty
        if (medicineInput != null && !medicineInput.equals("null") && !medicineInput.trim().isEmpty()) {

            // CASE 1: Input is already a valid MongoDB ObjectId (Retro-compatibility)
            if (medicineInput.matches("^[0-9a-fA-F]{24}$")) {
                keys.add(MEDICINE);
                values.add(medicineInput);
            }
            // CASE 2: Input is a NAME (Autocomplete context) -> Perform DB Lookup
            else {
                final MedicineBean med = facade.findMedicineByName(medicineInput.trim());

                keys.add(MEDICINE);
                if (med != null) {
                    values.add(med.getId());
                } else {
                    // Force 0 results instead of ignoring the filter
                    values.add("000000000000000000000000");
                }
            }

            // Append the original NAME to the URL for UI consistency during pagination
            searchParams.append("&patientMedicine=").append(medicineInput.trim());
        }
    }

    /**
     * Helper method to handle the patient status filter (boolean parsing).
     */
    private void addStatusFilter(final HttpServletRequest request, final ArrayList<String> keys, final ArrayList<Object> values, final StringBuilder searchParams) {
        final String status = request.getParameter("patientStatus");
        if (status != null && !status.equals("na") && !status.isEmpty()) {
            keys.add(STATUS);
            values.add(Boolean.parseBoolean(status));
            searchParams.append("&patientStatus=").append(status);
        }
    }

    /**
     * Enrich TherapyBeans on-the-fly.
     * Iterates through the patient list and delegates the enrichment logic for each patient.
     */
    private void enrichPatientsWithMedicineNames(final ArrayList<PatientBean> patients, final UserBean user) {
        // Safety check
        if (patients == null || patients.isEmpty()) return;

        // Iterate over patients and delegate to helper method
        for (final PatientBean patient : patients) {
            enrichSinglePatientTherapy(patient, user);
        }
    }

    /**
     * Helper method to process a single patient's therapy.
     * Checks for therapy existence and iterates through medicines.
     */
    private void enrichSinglePatientTherapy(final PatientBean patient, final UserBean user) {
        // Skip processing if patient has no therapy or no medicines assigned
        if (patient.getTherapy() == null || patient.getTherapy().getMedicines() == null) {
            return;
        }

        // Iterate over therapy medicines and delegate the lookup
        for (final TherapyMedicineBean therapyMed : patient.getTherapy().getMedicines()) {
            performMedicineLookup(therapyMed, user);
        }
    }

    /**
     * Performs the actual DB lookup to find the medicine name from its ID.
     * Implements the "Green IT" optimization by skipping lookup if name is already present.
     */
    private void performMedicineLookup(final TherapyMedicineBean therapyMed, final UserBean user) {
        // Optimization: If the name is already present (e.g., from cache), skip the query
        if (therapyMed.getMedicineName() != null && !therapyMed.getMedicineName().isEmpty()) {
            return;
        }

        final String medId = therapyMed.getMedicineId();

        // Proceed only if we have a valid ID
        if (medId != null && !medId.isEmpty()) {
            // QUERY LOOKUP: Retrieve the actual medicine object using the ID
            // Note: findMedicines returns a list. We retrieve the first match.
            final ArrayList<MedicineBean> meds = (ArrayList<MedicineBean>) facade.findMedicines("_id", medId, user);

            if (!meds.isEmpty()) {
                // DATA ENRICHMENT: Set the name in the "volatile" field
                therapyMed.setMedicineName(meds.get(0).getName());
            } else {
                // Fallback for data consistency issues
                therapyMed.setMedicineName("Medicine not found (ID: " + medId + ")");
            }
        }
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
    private Date dateParser(final String date) {
        final SimpleDateFormat pattern = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return pattern.parse(date);
        }
        catch (final Exception e) {
            return null;
        }
    }
    private String dateReverseParser(final Date date) {
        final DateFormat pattern = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return pattern.format(date);
        }
        catch (final Exception e) {
            return null;
        }
    }

    private boolean patientValidation(final PatientBean patient){
        // Validazione Nome
        if (!namesValidity(patient.getName())) {
            return false;
        }
        // Validazione Cognome
        if (!namesValidity(patient.getSurname())) {
            return false;
        }

        // Validazione Data
        final String processedBirthDate = dateReverseParser(patient.getBirthDate());

        if (processedBirthDate == null || !dateValidity(processedBirthDate)) {
            return false;
        }
        // Validazione Città
        if (!cityValidity(patient.getCity())) {
            return false;
        }
        // Validazione Codice Fiscale
        if (!taxCodeValidity(patient.getTaxCode())) {
            return false;
        }
        // Validazione Numero di Telefono
        if (!phoneNumberValidity(patient.getPhoneNumber())) {
            return false;
        }
        // Validazione Note
        if (!notesValidity(patient.getNotes())) {
            return false;
        }

        return true;
    }
    private boolean therapyValidation(final String condition, final TherapyBean therapy){
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
        for (final TherapyMedicineBean medicine: therapy.getMedicines()) {
            if (!idValidity(medicine.getMedicineId())) {
                return false;
            }
            if (!doseValidity(medicine.getDose())) {
                return false;
            }
        }

        return true;
    }
    private boolean idValidity(final String id) {
        final String format = "^[a-f\\d]{24}$";
        return id.matches(format);
    }
    private boolean numberValidity(final String notes) {
        final String format = "^[0-9]+$";
        return notes.matches(format);
    }
    private boolean dateValidity(final String date) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final LocalDate newDate = LocalDate.parse(date, formatter );
        final LocalDate currentDate = LocalDate.now();
        if (newDate.isAfter(currentDate))
            return false;
        final String format = "^(19|20)[0-9]{2}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$";
        return date.matches(format);
    }
    private boolean nameValidity(final String name) {
        final String format = "^[A-Za-z][A-Za-z'-]++([ A-Za-z][A-Za-z'-]++)*+$";
        return name.matches(format);
    }
    private boolean namesValidity(final String name) {
        if (name.length() > 32)
            return false;
        return nameValidity(name);
    }
    private boolean cityValidity(final String city) {
        if (city.length() > 32)
            return false;
        return nameValidity(city);
    }
    private boolean taxCodeValidity(final String taxCode) {
        final String format = "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$";
        return taxCode.matches(format);
    }
    private boolean phoneNumberValidity(final String phoneNumber) {
        final String format = "^[+]?[(]?[0-9]{2,3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,7}$";
        return phoneNumber.matches(format);
    }
    private boolean notesValidity(final String notes) {
        if (notes.length() > 255)
            return false;
        final String format = "^[A-Za-z0-9][A-Za-z0-9'.,\\n-]++([ A-Za-z0-9][A-Za-z0-9'.,\\n-]++)*+$";
        return notes.matches(format);
    }
    private boolean conditionValidity(final String condition) {
        return nameValidity(condition);
    }
    private boolean sessionsValidity(final Integer sessions) {
        return numberValidity(String.valueOf(sessions));
    }
    private boolean frequencyValidity(final Integer frequency) {
        return numberValidity(String.valueOf(frequency));
    }
    private boolean durationValidity(final Integer duration) {
        return numberValidity(String.valueOf(duration));
    }
    private boolean doseValidity(final Integer dose) {
        return numberValidity(String.valueOf(dose));
    }
}
