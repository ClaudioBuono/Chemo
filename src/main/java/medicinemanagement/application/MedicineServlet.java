package medicinemanagement.application;

import connector.Facade;
import usermanagement.application.UserBean;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/MedicineServlet")
public class MedicineServlet extends HttpServlet {
    private static final Facade facade = new Facade();
    private static final String ACTION = "action";
    private static final String OPERATION_RESULT = "OPERATION_RESULT";
    public static final String EXPIRY_DATE = "expiryDate";
    private final Logger logger = Logger.getLogger(MedicineServlet.class.getName());
    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        // Get user session
        final UserBean user = getSessionUserOrRedirect(request, response);
        if (user == null) return;

        // Routing logic
        final String action = request.getParameter(ACTION);
        final String id = request.getParameter("id");

        try {
            // --- AJAX ENDPOINT: Autocomplete ---
            if ("autocompleteMedicine".equals(action)) {
                handleAutocomplete(request, response);
                return;
            }

            // --- STANDARD VIEWS ---
            if (id != null) {
                redirectToMedicineDetailsPage(request, response, id, user);
            } else {
                handleStandardMedicineListView(request, response, user);
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error in doGet dispatching", e);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request,final HttpServletResponse response) throws ServletException, IOException {
        final String action = request.getParameter(ACTION);
        final UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        try {
            if (action == null) return;

            if (action.equals("insertMedicine")) {// Inserimento medicinale
                final MedicineBean medicine = new MedicineBean(request.getParameter("name"), request.getParameter("ingredients"));

                // Validazione
                if (!medicineValidation(medicine)) {
                    response.addHeader(OPERATION_RESULT, "false");
                    response.addHeader("ERROR_MESSAGE", "Aggiunta medicinale fallita: i dati inseriti non sono validi.");
                } else {
                    facade.insertMedicine(medicine, user);
                    response.addHeader(OPERATION_RESULT, "true");
                    response.addHeader("MEDICINE_ID", medicine.getId());
                }
            } else if (action.equals("insertPackage")) {// Inserimento confezione
                final String medicineId = request.getParameter("medicineId");
                final int capacity = Integer.parseInt(request.getParameter("capacity"));
                final Date expiryDate = dateParser(request.getParameter(EXPIRY_DATE));

                final PackageBean medicinePackage = new PackageBean(true, expiryDate, capacity, "");

                if (!packageValidation(medicinePackage)) {
                    response.addHeader(OPERATION_RESULT, "false");
                    response.addHeader("ERROR_MESSAGE", "Aggiunta confezione fallita: i dati inseriti non sono validi.");
                } else {
                    facade.insertMedicinePackage(medicineId, medicinePackage, user);
                    response.addHeader(OPERATION_RESULT, "true");
                }
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error in doPost: {0}", e.getMessage());
        }
    }

    // ==========================================
    // WORKER METHODS (Business Logic Handlers)
    // ==========================================

    /**
     * Handles the standard "Medicine List" view.
     * Manages pagination, filtering parameters, and view forwarding.
     */
    private void handleStandardMedicineListView(final HttpServletRequest request, final HttpServletResponse response, final UserBean user) {
        // Setup Pagination
        final int page = parsePageParameter(request);

        // Build filters
        final ArrayList<String> keys = new ArrayList<>();
        final ArrayList<Object> values = new ArrayList<>();
        final StringBuilder searchParams = buildParameters(request, keys, values);

        // Retrieve data
        final ArrayList<MedicineBean> paginatedResult = (ArrayList<MedicineBean>) facade.findMedicinesPaginated(keys, values, page, PAGE_SIZE, user);
        final long totalRecords = facade.countMedicinesFiltered(keys, values, user);

        // Setup JSP Attributes
        setupPaginationAttributes(request, page, totalRecords);
        request.setAttribute("medicinesResult", paginatedResult);
        request.setAttribute("searchParams", searchParams.toString());

        // Forward to View
        try {
            request.getRequestDispatcher("medicinesList.jsp").forward(request, response);
        } catch (final ServletException | IOException e) {
            logger.log(Level.SEVERE, "Forwarding to medicine list failed", e);
        }
    }

    // ===============
    // HELPER METHODS
    // ===============

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

    private void setupPaginationAttributes(final HttpServletRequest request, final int page, final long totalRecords) {
        int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
    }

    private void redirectToMedicineDetailsPage(final HttpServletRequest request, final HttpServletResponse response, final String id, final UserBean user) {
        final ArrayList<MedicineBean> medicines = (ArrayList<MedicineBean>) facade.findMedicines("_id", id, user);

        if (!medicines.isEmpty()) {
            request.setAttribute("medicine", medicines.get(0));
            try {
                getServletContext().getRequestDispatcher(response.encodeURL("/medicineDetails.jsp")).forward(request, response);
            } catch (final ServletException | IOException e) {
                logger.log(Level.SEVERE, "Redirect to details failed: ", e);
            }
        } else {
            try {
                response.sendRedirect("error404.jsp");
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "Redirect 404 failed", e);
            }
        }
    }

    /**
     * Handle AJAX autocomplete logic
     */
    private void handleAutocomplete(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String query = request.getParameter("q");

        // If query is too short, don't do it
        if (query == null || query.trim().length() < 2) {
            return;
        }

        // Get suggestions
        final ArrayList<String> suggestions = (ArrayList<String>) facade.findMedicineNamesLike(query.trim());

        // 3. Costruzione manuale del JSON (per evitare dipendenze esterne come GSON)
        final StringBuilder json = new StringBuilder("[");
        final int suggestionsSize = suggestions.size();
        for (int i = 0; i < suggestionsSize; ++i) {
            json.append("\"").append(suggestions.get(i)).append("\""); // Aggiunge virgolette: "Nome"
            if (i < suggestions.size() - 1) {
                json.append(","); // Virgola separatrice
            }
        }
        json.append("]");

        // 4. Invio Risposta
        response.setContentType("application/json"); // Diciamo al browser che è JSON
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }

    private StringBuilder buildParameters(final HttpServletRequest request, final ArrayList<String> keys, final ArrayList<Object> values) {
        final StringBuilder searchParams = new StringBuilder(128);

        // Check if action requested is a filtered search
        final String action = request.getParameter(ACTION);
        if ("searchMedicine".equals(action)) {
            searchParams.append("&action=searchMedicine");

            // --- Name Filter ---
            final String name = request.getParameter("medicineName");
            if (name != null && !name.trim().isEmpty()) {
                keys.add("name");
                values.add(name.trim());
                searchParams.append("&medicineName=").append(name.trim());
            }

            // --- Expiry Date Filter ---
            final String expiryDate = request.getParameter(EXPIRY_DATE);
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                keys.add(EXPIRY_DATE);
                values.add(dateParser(expiryDate.trim()));
                searchParams.append("&expiryDate=").append(expiryDate.trim());
            }

            // --- Status Filter ---
            final String status = request.getParameter("medicineStatus");
            if (status != null && !status.equals("na") && !status.isEmpty()) {
                keys.add("status");
                values.add(Boolean.parseBoolean(status));
                searchParams.append("&medicineStatus=").append(status);
            }
        }

        return searchParams;
    }



    //Metodi di supporto
    private Date dateParser(final String date) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(date);
        }
        catch (final Exception e) {
            return null;
        }
    }

    private boolean medicineValidation(final MedicineBean medicine) {
        if (!medicineNameValidity(medicine.getName())) {
            return false;
        }
        if (!ingredientsValidity(medicine.getIngredients())) {
            return false;
        }
        return true;
    }
    private boolean packageValidation(final PackageBean medicinePackage) {
        if (!capacityValidity(medicinePackage.getCapacity())) {
            return false;
        }
        if (!dateValidity(medicinePackage.getParsedExpiryDate())) {
            return false;
        }
        return true;
    }
    private boolean numberValidity(final String notes) {
        final String format = "^[0-9]+$";
        return notes.matches(format);
    }
    private boolean dateValidity(final String date) {
        final String format = "^(19|20)[0-9]{2}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$";
        return date.matches(format);
    }
    private boolean nameValidity(final String name) {
        final String format = "^[A-Za-z][A-Za-z'-]++([ A-Za-z][A-Za-z'-]++)*+$";
        return name.matches(format);
    }
    private boolean medicineNameValidity(final String name) {
        if (name.length() > 32)
            return false;
        return nameValidity(name);
    }
    private boolean ingredientsValidity(final String ingredients) {
        if (ingredients.length() > 100)
            return false;
        final String format = "^[A-Za-z0-9][A-Za-z0-9'\\-]++([ A-Za-z0-9][A-Za-z0-9'-]++)*+$";
        return ingredients.matches(format);
    }
    private boolean capacityValidity(final Integer capacity) {
        return numberValidity(String.valueOf(capacity));
    }
}
