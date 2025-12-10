<%--
  Created by IntelliJ IDEA.
  User: anton
  Date: 17/01/2023
  Time: 17:50
--%>
<%@ page contentType="text/html;charset=UTF-8"
         import="userManagement.application.UserBean"%>
<%@ page import="medicinemanagement.application.MedicineBean" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="connector.Facade" %>
<html>
<head>
    <title>Chemo | Medicinali</title>
    <script src="./static/scripts/search.js"></script>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-GLhlTQ8iRABdZLl6O3oVMWSktQOp6b7In1Zl3/Jr59b6EGGoI1aFkw7cmDA6j6gD" crossorigin="anonymous">
</head>
<body>
<%
    HttpSession sessione=request.getSession(false);
    if (sessione == null) {
        //redirect alla pagina di error 401 Unauthorized
        response.sendRedirect("./error401.jsp");
    } else {
        UserBean user = (UserBean) sessione.getAttribute("currentSessionUser");
        if (user == null) {
            //è presente una sessione senza utente
            response.sendRedirect("./error401.jsp");
        } else {
%>
<header>
    <jsp:include page="./static/templates/userHeaderLogged.html"/>
</header>
<div id="page-content">
    <div id="alert-box"></div>
    <div id="medicines-list-box" class="box">
        <div id="box-name-row" class="row">
            <h1 class="title">Medicinali</h1>
            <jsp:include page="./static/templates/loggedUserButtons.html"/>
        </div>
        <form id="search-form" class="form box" action="MedicineServlet" method="get">
            <div class="title-section">
                <h2 class="title">Ricerca</h2>
            </div>
            <input type="text" id="search-medicine-name" class="search-field input-field" name="medicineName" placeholder="Nome medicinale">
            <div id="search-buttons">
                <input type="button" id="search-filters-button" class="button-tertiary-s rounded edit-button" value="Espandi filtri" onclick="expandSearchFilters()">
                <button type="submit" id="search-request-button" class="button-primary-m rounded edit-button" name="action" value="searchMedicine">Cerca</button>
            </div>
            <div id="search-filters" class="hidden">
                <div class="input-fields-row">
                    <div class="field left">
                        <label for="search-medicine-expiry-date">Scadenza</label>
                        <input type="date" id="search-medicine-expiry-date" class="input-field" name="expiryDate">
                    </div>
                    <div class="field right">
                        <label for="search-medicine-status">Stato</label>
                        <select id="search-medicine-status" class="input-field" name="medicineStatus">
                            <option value="na" selected>Seleziona stato</option>
                            <option value="true">Disponibile</option>
                            <option value="false">Esaurito</option>
                        </select>
                    </div>
                </div>
            </div>
        </form>
        <div id="medicines-list">
            <!-- Si itera fino a quando ci sono risultati-->
            <%
                ArrayList<MedicineBean> medicines = (ArrayList<MedicineBean>) request.getAttribute("medicinesResult");

                if (medicines == null) {
                    medicines = new ArrayList<MedicineBean>();
                }

                // Total Pages (già calcolato nel backend usando totalRecords / PAGE_SIZE)
                int totalPages = 1; // Default
                if (request.getAttribute("totalPages") != null) {
                    totalPages = (Integer) request.getAttribute("totalPages");
                }

                // Current Page (già determinato dal backend)
                int currentPage = 1; // Default
                if (request.getAttribute("currentPage") != null) {
                    currentPage = (Integer) request.getAttribute("currentPage");
                }


                // Dimensione fissa del range di pagine da mostrare
                int rangeSize = 8;

                // Calcola la pagina di inizio e fine del range visibile
                int startPage = Math.max(1, currentPage - (rangeSize / 2));
                int endPage = Math.min(totalPages, startPage + rangeSize - 1);

                // Aggiusta startPage se siamo vicini alla fine
                if (endPage - startPage + 1 < rangeSize) {
                    startPage = Math.max(1, endPage - rangeSize + 1);
                }
                %>

            <div class="pagination-container">
                <nav aria-label="Patient pagination">
                    <ul class="pagination">

                        <%-- Bottone per Pagina 1 --%>
                        <% if(currentPage > rangeSize/2 ) { %>
                        <li class="page-item">
                            <a class="page-link"
                               href="MedicineServlet?page=1"
                               aria-label="First">
                                <span aria-hidden="true">1</span>
                            </a>
                        </li>
                        <% } %>

                        <%-- Bottone per Pagina Precedente --%>
                        <% if(currentPage > 1) { %>
                        <li class="page-item">
                            <a class="page-link"
                               href="MedicineServlet?page=<%= currentPage - 1 %>"
                               aria-label="Previous">
                                <span aria-hidden="true">&laquo;</span>
                            </a>
                        </li>
                        <% } %>

                        <%-- Pagine Numeriche (range visibile) --%>
                        <% for(int i = startPage; i <= endPage; i++) { %>
                        <li class="page-item <%= (i == currentPage) ? "active disabled" : "" %>">
                            <a class="page-link"
                               href="MedicineServlet?page=<%= i %>">
                                <%= i %>
                            </a>
                        </li>
                        <% } %>

                        <%-- Bottone per Pagina Successiva --%>
                        <% if(currentPage < totalPages) { %>
                        <li class="page-item">
                            <a class="page-link"
                               href="MedicineServlet?page=<%= currentPage + 1 %>"
                               aria-label="Next">
                                <span aria-hidden="true">&raquo;</span>
                            </a>
                        </li>
                        <% } %>

                        <%-- Bottone per Ultima Pagina --%>
                        <% if(currentPage < totalPages - rangeSize/2) { %>
                        <li class="page-item">
                            <a class="page-link"
                               href="MedicineServlet?page=<%= totalPages %>"
                               aria-label="Last">
                                <span aria-hidden="true"><%= totalPages %></span>
                            </a>
                        </li>
                        <% } %>
                    </ul>
                </nav>
            </div>

            <%

                if (medicines.isEmpty()) {
                    //visualizzazione messaggio nessun medicinale trovato
            %>
            <div class="result-box-container">
                <h2 class="no-result">Nessun medicinale trovato</h2>
            </div>

            <%
            } else {

                String patientStatus;
                for (MedicineBean medicine:medicines) {
                    //visualizzazione box singolo paziente
                    if (medicine.getAmount() > 0)
                        patientStatus = "status-available";
                    else
                        patientStatus = "status-unavailable";
            %>

            <div class="result-box-container">
                <button type="submit" id="medicine-box-id" class="box" onclick="redirectToMedicineDetails('<%=medicine.getId()%>')">
                    <div class="first-row">
                        <div class="column left">
                            <h2 class="result-name"><%=medicine.getName()%></h2>
                        </div>
                        <div class="column icon <%=patientStatus%> right">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="currentColor" class="bi bi-capsule-pill" viewBox="0 0 16 16">
                                <path d="M11.02 5.364a3 3 0 0 0-4.242-4.243L1.121 6.778a3 3 0 1 0 4.243 4.243l5.657-5.657Zm-6.413-.657 2.878-2.879a2 2 0 1 1 2.829 2.829L7.435 7.536 4.607 4.707ZM12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8Zm-.5 1.042a3 3 0 0 0 0 5.917V9.042Zm1 5.917a3 3 0 0 0 0-5.917v5.917Z"/>
                            </svg>
                        </div>
                    </div>
                    <div class="column">
                        <h4 class="result-title left">Ingredienti</h4>
                        <p class="left"><%=medicine.getIngredients()%></p>
                    </div>
                    <div class="row">
                        <p class="left">Confezioni: <%=medicine.getAmount()%></p>
                    </div>
                </button>
            </div>
            <%
                    }
                }
            %>
        </div>
    </div>
</div>
<%
        }
    }
%>
</body>
</html>
