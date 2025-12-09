package medicinemanagement.application;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/MedicineServlet")
public class MedicineServlet extends HttpServlet {
    private static final Facade facade = new Facade();
    private final Logger logger = Logger.getLogger(MedicineServlet.class.getName());
    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        // Check if user is logged in
        final HttpSession session = request.getSession(false);
        final UserBean user = (session != null) ? (UserBean) session.getAttribute("currentSessionUser") : null;

        if (user == null) {
            try {
                response.sendRedirect("error401.jsp");
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "Redirect failed: ", e);
            }
            return;
        }

        // Check if a single medicine is required
        final String id = request.getParameter("id");
        if (id != null) {
            redirectToMedicineDetailsPage(request, response, id, user);
            return;
        }

        // =======================
        // PAGINATED LIST & SEARCH
        // =======================

        // Pagination parameters
        int page = 1;
        if (request.getParameter("page") != null) {
            try {
                page = Integer.parseInt(request.getParameter("page"));
            } catch (final NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid page number, defaulting to 1", e);
            }
        }

        // Filters & Parameters building
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();

        // Maintain parameters link inside JSP (ex: &medicineName=Paracetamol&page=2)
        final StringBuilder searchParams = buildParameters(request, keys, values);

        // Retrieve Data
        final ArrayList<MedicineBean> paginatedResult = (ArrayList<MedicineBean>) facade.findMedicinesPaginated(keys, values, page, PAGE_SIZE, user);

        // ============================
        // PREPARE JSP RESPONSE
        // ============================

        // Calculate total pages
        final long totalRecords = facade.countMedicinesFiltered(keys, values, user);
        int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        // Send data to JSP
        request.setAttribute("medicinesResult", paginatedResult); // NOTA: Ho uniformato il nome a 'medicinesResult'
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);

        // Pass parameter string to make "next" and "previous" buttons work during search
        request.setAttribute("searchParams", searchParams.toString());

        final RequestDispatcher dispatcher = request.getRequestDispatcher("medicinesList.jsp");
        try {
            dispatcher.forward(request, response);
        } catch (final ServletException | IOException e) {
            logger.log(Level.SEVERE, "Forwarding failed: ", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        UserBean user = (UserBean) request.getSession().getAttribute("currentSessionUser");

        try {
            if (action == null) return;

            if (action.equals("insertMedicine")) {// Inserimento medicinale
                MedicineBean medicine = new MedicineBean(request.getParameter("name"), request.getParameter("ingredients"));

                // Validazione
                if (!medicineValidation(medicine)) {
                    response.addHeader("OPERATION_RESULT", "false");
                    response.addHeader("ERROR_MESSAGE", "Aggiunta medicinale fallita: i dati inseriti non sono validi.");
                } else {
                    facade.insertMedicine(medicine, user);
                    response.addHeader("OPERATION_RESULT", "true");
                    response.addHeader("MEDICINE_ID", medicine.getId());
                }
            } else if (action.equals("insertPackage")) {// Inserimento confezione
                String medicineId = request.getParameter("medicineId");
                int capacity = Integer.parseInt(request.getParameter("capacity"));
                Date expiryDate = dateParser(request.getParameter("expiryDate"));

                PackageBean medicinePackage = new PackageBean(true, expiryDate, capacity, "");

                if (!packageValidation(medicinePackage)) {
                    response.addHeader("OPERATION_RESULT", "false");
                    response.addHeader("ERROR_MESSAGE", "Aggiunta confezione fallita: i dati inseriti non sono validi.");
                } else {
                    facade.insertMedicinePackage(medicineId, medicinePackage, user);
                    response.addHeader("OPERATION_RESULT", "true");
                }
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error in doPost: {0}", e.getMessage());
        }
    }

    // ==============================
    // doGet HELPER METHODS
    // ==============================
    private void redirectToMedicineDetailsPage(final HttpServletRequest request, final HttpServletResponse response, final String id, final UserBean user) {
        // Find the medicine
        final ArrayList<MedicineBean> medicines = facade.findMedicines("_id", id, user);

        if (!medicines.isEmpty()) {
            request.setAttribute("medicine", medicines.get(0));
            try {
                // Redirect alla pagina dettagli
                getServletContext().getRequestDispatcher(response.encodeURL("/medicineDetails.jsp")).forward(request, response);
            } catch (final ServletException | IOException e) {
                logger.log(Level.SEVERE, "Redirect to details failed: ", e);
            }
        } else {
            try {
                response.sendRedirect("error404.jsp"); // O gestisci l'errore come preferisci
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "Redirect 404 failed", e);
            }
        }
    }

    private StringBuilder buildParameters(final HttpServletRequest request, final ArrayList<String> keys, final ArrayList<Object> values) {
        final StringBuilder searchParams = new StringBuilder(128);

        // Check if action requested is a filtered search
        final String action = request.getParameter("action");
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
            final String expiryDate = request.getParameter("expiryDate");
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                keys.add("expiryDate");
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
    private Date dateParser(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(date);
        }
        catch (Exception e) {
            return null;
        }
    }

    private boolean medicineValidation(MedicineBean medicine) {
        if (!medicineNameValidity(medicine.getName())) {
            return false;
        }
        if (!ingredientsValidity(medicine.getIngredients())) {
            return false;
        }
        return true;
    }
    private boolean packageValidation(PackageBean medicinePackage) {
        if (!capacityValidity(medicinePackage.getCapacity())) {
            return false;
        }
        if (!dateValidity(medicinePackage.getParsedExpiryDate())) {
            return false;
        }
        return true;
    }
    private boolean numberValidity(String notes) {
        String format = "^[0-9]+$";
        return notes.matches(format);
    }
    private boolean dateValidity(String date) {
        String format = "^(19|20)[0-9]{2}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$";
        return date.matches(format);
    }
    private boolean nameValidity(String name) {
        String format = "^[A-Za-z][A-Za-z'-]+([ A-Za-z][A-Za-z'-]+)*$";
        return name.matches(format);
    }
    private boolean medicineNameValidity(String name) {
        if (name.length() > 32)
            return false;
        return nameValidity(name);
    }
    private boolean ingredientsValidity(String ingredients) {
        if (ingredients.length() > 100)
            return false;
        String format = "^[A-Za-z0-9][A-Za-z0-9'\\-]+([ A-Za-z0-9][A-Za-z0-9'-]+)*$";
        return ingredients.matches(format);
    }
    private boolean capacityValidity(Integer capacity) {
        return numberValidity(String.valueOf(capacity));
    }
}
