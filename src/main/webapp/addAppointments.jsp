<%--
  Created by IntelliJ IDEA.
  User: anton
  Date: 21/01/2023
  Time: 20:07
--%>
<%@ page contentType="text/html;charset=UTF-8"
         import="usermanagement.application.UserBean"%>
<%@ page import="patientmanagement.application.PatientBean" %>
<%@ page import="java.util.ArrayList" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <title>Chemo Nuove sedute</title>
    <link rel="stylesheet" href="./static/styles/main_css.min.css">
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

    ArrayList<PatientBean> patients = (ArrayList<PatientBean>) request.getAttribute("availablePatients");

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
    <jsp:include page="static/templates/userHeaderLogged.html"/>
</header>
<div id="page-content">
    <div id="alert-box"></div>
    <div id="add-appointments-box" class="box">
        <div id="box-name-row" class="row">
            <h1 class="title">Nuove sedute</h1>
            <jsp:include page="./static/templates/loggedUserButtons.html"/>
        </div>
        <form id="add-appointments-content">
            <div class="title-section">
                <h2 class="title">Selezione pazienti</h2>
            </div>
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
            <div id="patient-list" class="result-box-container">
                <%

                    if (patients.isEmpty()) {
                        //visualizzazione messaggio nessun paziente trovato
                %>
                <div class="result-box-container">
                    <h2 class="no-result">Nessun paziente disponibile</h2>
                </div>
                <%
                } else {
                    for (PatientBean patient:patients) {
                %>
                <div id="patient-box-id" class="box form">
                    <div class="first-row">
                        <div class="column left">
                            <h2 class="result-name"><%=patient.getName()%> <%=patient.getSurname()%></h2>
                            <p><%=patient.getTaxCode()%></p>
                        </div>
                        <div id="patient-check-id" class="column right checkbox-container">
                            <label class="checkbox-label">
                                <input type="checkbox" name="patient-id" value="<%=patient.getPatientId()%>">
                                <span class="checkbox-custom rectangular"></span>
                            </label>
                        </div >
                    </div>
                    <div class="row">
                        <h4 class="left"><%=patient.getCondition()%></h4>
                        <p class="right">Numero sedute: <%=patient.getTherapy().getSessions()%></p>
                    </div>
                </div>
                <%
                        }
                    }
                %>
            </div>
            <input type="button" class="button-primary-m submit-button" value="Crea calendario" onclick="addAppointments()">
        </form>
    </div>
</div>
<%
        }
    }
%>
</body>
</html>
