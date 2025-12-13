<%--
  Created by IntelliJ IDEA.
  User: anton
  Date: 16/01/2023
  Time: 13:23
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="connector.Facade"
         import="java.util.ArrayList"
         import="usermanagement.application.UserBean"
         import="patientmanagement.application.PatientBean"%>
<%@ page import="medicinemanagement.application.MedicineBean" %>
<html>
<head>
    <title>Chemo | Storico pazienti</title>
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
        if (user == null || user.getType() != 1) {
            //è presente una sessione senza utente o con utente non autorizzato
            if (user == null) {
                response.sendRedirect("./error401.jsp");
            } else {
                response.sendRedirect("./error403.jsp");
            }
        } else {
%>

<%
    // Recupera gli attributi impostati dal Servelt/Controller

    ArrayList<PatientBean> patients = (ArrayList<PatientBean>) request.getAttribute("patientsResult");

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
    int rangeSize = 6;

    // Calcola la pagina di inizio e fine del range visibile
    int startPage = Math.max(1, currentPage - (rangeSize / 2));
    int endPage = Math.min(totalPages, startPage + rangeSize - 1);

    // Aggiusta startPage se siamo vicini alla fine
    if (endPage - startPage + 1 < rangeSize) {
        startPage = Math.max(1, endPage - rangeSize + 1);
    }

    // Parametri di ricerca
    String searchParams = ""; // Default
    if (request.getAttribute("searchParams") != null) {
        searchParams = (String) request.getAttribute("searchParams");
    }


%>
<header>
    <jsp:include page="./static/templates/userHeaderLogged.html"/>
</header>
<div id="page-content">
    <div id="alert-box"></div>
    <div id="patient-list-box" class="box">
        <div id="box-name-row" class="row">
            <h1 class="title">Storico pazienti</h1>
            <jsp:include page="./static/templates/loggedUserButtons.html"/>
        </div>
        <form id="search-form" class="form box" action="PatientServlet" method="get">
            <div class="title-section">
                <h2 class="title">Ricerca</h2>
            </div>
            <div class="input-fields-row">
                <div class="field left">
                    <input type="text" id="search-patient-name" class="search-field input-field" name="name" placeholder="Nome paziente">
                </div>
                <div class="field right">
                    <input type="text" id="search-patient-surname" class="search-field input-field" name="surname" placeholder="Cognome paziente">
                </div>
            </div>
            <div id="search-buttons">
                <input type="button" id="search-filters-button" class="button-tertiary-s rounded edit-button" value="Espandi filtri" onclick="expandSearchFilters()">
                <button type="submit" id="search-request-button" class="button-primary-m rounded edit-button" name="action" value="searchPatient">Cerca</button>
            </div>
            <div id="search-filters" class="hidden">
                <div class="input-fields-row">
                    <div class="field left">
                        <label for="search-patient-medicine">Medicinale</label>
                        <input type="text"
                               id="search-patient-medicine"
                               class="input-field"
                               name="patientMedicine"
                               list="medicine-suggestions-search"
                               autocomplete="off"
                               placeholder="Cerca medicinale..."
                               oninput="fetchMedicineSuggestions(this.value, 'medicine-suggestions-search')">

                        <datalist id="medicine-suggestions-search"></datalist>
                    </div>
                    <div class="field right">
                        <label for="search-patient-status">Stato</label>
                        <select id="search-patient-status" class="input-field" name="patientStatus">
                            <option value="na" selected>Seleziona stato</option>
                            <option value="true">Disponibile</option>
                            <option value="false">Non disponibile</option>
                        </select>
                    </div>
                </div>
            </div>
        </form>
        <div class="pagination-container">
            <nav aria-label="Patient pagination">
                <ul class="pagination">

                    <%-- Bottone per Pagina 1 --%>
                    <% if(currentPage > (rangeSize/2 + 1)) { %>
                    <li class="page-item">
                        <a class="page-link"
                           href="PatientServlet?page=1<%=searchParams%>"
                           aria-label="First">
                            <span aria-hidden="true">1</span>
                        </a>
                    </li>
                    <% } %>

                    <%-- Bottone per Pagina Precedente --%>
                    <% if(currentPage > 1) { %>
                    <li class="page-item">
                        <a class="page-link"
                           href="PatientServlet?page=<%= currentPage - 1 %><%=searchParams%>"
                           aria-label="Previous">
                            <span aria-hidden="true">&laquo;</span>
                        </a>
                    </li>
                    <% } %>

                    <%-- Pagine Numeriche (range visibile) --%>
                    <% for(int i = startPage; i <= endPage; i++) { %>
                    <li class="page-item <%= (i == currentPage) ? "active disabled" : "" %>">
                        <a class="page-link"
                           href="PatientServlet?page=<%= i %><%=searchParams%>">
                            <%= i %>
                        </a>
                    </li>
                    <% } %>

                    <%-- Bottone per Pagina Successiva --%>
                    <% if(currentPage < totalPages) { %>
                    <li class="page-item">
                        <a class="page-link"
                           href="PatientServlet?page=<%= currentPage + 1 %><%=searchParams%>"
                           aria-label="Next">
                            <span aria-hidden="true">&raquo;</span>
                        </a>
                    </li>
                    <% } %>

                    <%-- Bottone per Ultima Pagina --%>
                    <% if(currentPage < totalPages - (rangeSize/2 - 1)) { %>
                    <li class="page-item">
                        <a class="page-link"
                           href="PatientServlet?page=<%= totalPages %><%=searchParams%>"
                           aria-label="Last">
                            <span aria-hidden="true"><%= totalPages %></span>
                        </a>
                    </li>
                    <% } %>
                </ul>
            </nav>
        </div>
        <div id="patient-list">
            <!-- Si itera fino a quando ci sono risultati-->
            <%
                if (patients.isEmpty()) {
                    //visualizzazione messaggio nessun paziente trovato
            %>
            <div class="result-box-container">
                <h2 class="no-result">Nessun paziente trovato</h2>
            </div>
            <%
                    } else {
                        String patientStatus;
                        for (PatientBean patient:patients) {
                            //visualizzazione box singolo paziente
                            if (patient.getStatus())
                                patientStatus = "status-available";
                            else
                                patientStatus = "status-unavailable";
            %>
            <div class="result-box-container">
                <button type="submit" id="patient-box-id" class="box" onclick="redirectToPatientDetails('<%=patient.getPatientId()%>')">
                    <div class="first-row">
                        <div class="column left">
                            <h2 class="result-name"><%=patient.getName()%> <%=patient.getSurname()%></h2>
                            <p><%=patient.getTaxCode()%></p>
                        </div>
                        <div class="column icon <%=patientStatus%> right">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="currentColor" class="bi bi-person-circle" viewBox="0 0 16 16">
                                <path d="M11 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"/>
                                <path fill-rule="evenodd" d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8zm8-7a7 7 0 0 0-5.468 11.37C3.242 11.226 4.805 10 8 10s4.757 1.225 5.468 2.37A7 7 0 0 0 8 1z"/>
                            </svg>
                        </div>
                    </div>
                    <%
                        if (patient.getCondition() != null){
                    %>
                    <div class="row">
                        <h4 class="left"><%=patient.getCondition()%></h4>
                        <p class="right">Appuntamenti: <%=patient.getTherapy().getSessions()%></p>
                    </div>
                    <%
                        }
                    %>
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
