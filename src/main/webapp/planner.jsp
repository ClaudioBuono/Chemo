<%--
  Created by IntelliJ IDEA.
  User: anton
  Date: 23/01/2023
  Time: 18:45
  REFACTORED: Green Coding Implementation (Map-based lookup)
--%>
<%@ page contentType="text/html;charset=UTF-8"
         import="userManagement.application.UserBean"
         import="plannerManagement.application.PlannerBean"
         import="plannerManagement.application.green.AppointmentRecord"
         import="java.util.Date"
         import="java.text.Format"
         import="java.text.SimpleDateFormat"
         import="java.time.LocalTime"
         import="java.time.LocalDate"
         import="patientmanagement.application.PatientBean"
         import="medicinemanagement.application.MedicineBean"
         import="connector.Facade"
         import="java.util.Map"
         import="java.util.List"
%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <title>Chemo Calendario</title>
    <link rel="stylesheet" href="./static/styles/planner.css">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-GLhlTQ8iRABdZLl6O3oVMWSktQOp6b7In1Zl3/Jr59b6EGGoI1aFkw7cmDA6j6gD" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/js/bootstrap.bundle.min.js" integrity="sha384-w76AqPfDkMBDXo30jS1Sgez6pr3x5MlQ1ZAGC+nuZB+EYdgRZgiwxhTBTkF7CXvN" crossorigin="anonymous"></script>
</head>
<body>
<%
    HttpSession sessione=request.getSession(false);
    if (sessione == null) {
        response.sendRedirect("./error401.jsp");
    } else {
        UserBean user = (UserBean) sessione.getAttribute("currentSessionUser");
        if (user == null ) {
            response.sendRedirect("./error401.jsp");
        } else {
            // RECUPERO DATI DALLA SERVLET
            String nextPlanner = (String) request.getAttribute("afterVisualizedId");
            String previousPlanner = (String) request.getAttribute("beforeVisualizedId");
            String latestPlanner = (String) request.getAttribute("latestPlannerId");
            PlannerBean planner = (PlannerBean) request.getAttribute("plannerToVisualize");
            String weekDate = (String) request.getAttribute("weekDate");

            // Recupero la Mappa Ottimizzata
            Map<String, List<AppointmentRecord>> gridMap = (Map<String, List<AppointmentRecord>>) request.getAttribute("gridMap");

            Facade facade = new Facade();

            // Estrapolazione data inizio planner per gestire le intestazioni colonne
            Date slotFullDate = planner.getStartDate();
            Format formatterYear = new SimpleDateFormat("yyyy");
            Format formatterMonth = new SimpleDateFormat("MM");
            Format formatterDay = new SimpleDateFormat("dd");
            int year = Integer.parseInt(formatterYear.format(slotFullDate));
            int month = Integer.parseInt(formatterMonth.format(slotFullDate));
            int day = Integer.parseInt(formatterDay.format(slotFullDate));
            int calendarDay = day;
%>
<header>
    <jsp:include page="static/templates/userHeaderLogged.html"/>
</header>
<div id="page-content">
    <div id="alert-box"></div>
    <div id="user-box" class="box">
        <div id="box-name-row" class="row">
            <h1 class="title">Calendario sedute</h1>
            <jsp:include page="./static/templates/loggedUserButtons.html"/>
        </div>
        <div id="planner-content">
            <div id="planner-top-bar" class="box-in">
                <input type="button" id="today-button" class="button-secondary-m" value="Oggi" onclick="redirectToPlanner('<%=latestPlanner%>', 'latest')">
                <div id="week-selector">
                    <% if (previousPlanner.equals("")) { %>
                    <div id="previous-week" class="icon chevron-inactive">
                        <svg xmlns="http://www.w3.org/2000/svg" class="bi bi-chevron-left" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/></svg>
                    </div>
                    <% } else { %>
                    <div id="previous-week" class="icon chevron" onclick="redirectToPlanner('<%=previousPlanner%>', 'prev')">
                        <svg xmlns="http://www.w3.org/2000/svg" class="bi bi-chevron-left" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/></svg>
                    </div>
                    <% } %>
                    <h3 id="actual-week"><%=weekDate%></h3>
                    <% if (nextPlanner.equals("")) { %>
                    <div id="next-week" class="icon chevron-inactive">
                        <svg xmlns="http://www.w3.org/2000/svg" class="bi bi-chevron-right" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/></svg>
                    </div>
                    <% } else { %>
                    <div id="next-week" class="icon chevron" onclick="redirectToPlanner('<%=nextPlanner%>', 'next')">
                        <svg xmlns="http://www.w3.org/2000/svg" class="bi bi-chevron-right" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/></svg>
                    </div>
                    <% } %>
                </div>
                <a id="add-new-appointments" class="icon" href="PatientServlet?action=viewAvailablePatients">
                    <svg xmlns="http://www.w3.org/2000/svg" class="bi bi-plus" viewBox="0 0 16 16"><path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/></svg>
                </a>
            </div>

            <div id="planner-appointments-section" class="box-in">
                <div id="planner-first-row" class="planner-row">
                    <div class="planner-column planner-time"></div>
                    <div id="planner-days-row" class="planner-row box">
                        <% String[] daysName = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì"};
                            for(int d=0; d<5; d++) { %>
                        <div class="planner-column">
                            <h5 class="planner-day-name"><%=daysName[d]%></h5>
                            <h5 class="planner-day-number"><%=calendarDay + d%></h5>
                        </div>
                        <% } %>
                    </div>
                    <div class="planner-column planner-scroll"></div>
                </div>

                <div id="planner-appointments-rows">
                    <%
                        int hours = 9;
                        int appointmentsSlotsNumber = 9; // 9 ore lavorative (9-18)

                        // CICLO RIGHE (ORE)
                        for (int i = 0; i < appointmentsSlotsNumber; i++, hours++) {
                            String minutes = "00";
                    %>
                    <div id="planner-<%=i%>-row" class="planner-row">
                        <div class="planner-column planner-time">
                            <h5 class="time"><%=hours%>:<%=minutes%></h5>
                        </div>

                        <div id="planner-<%=hours%><%=minutes%>-row" class="planner-row appointments-row">
                            <%
                                // CICLO COLONNE (GIORNI)
                                for (int j = 0; j < 5; j++) {

                                    // Generiamo la chiave: "yyyy-MM-dd_HH:00"
                                    LocalDate currentSlotDate = LocalDate.of(year, month, day + j);
                                    String keyHour = (hours < 10 ? "0" + hours : String.valueOf(hours));
                                    String mapKey = currentSlotDate.toString() + "_" + keyHour + ":00";

                                    // Lettura diretta dalla mappa
                                    List<AppointmentRecord> slotApps = null;
                                    if(gridMap != null) {
                                        slotApps = gridMap.get(mapKey);
                                    }

                                    int slotNumber = (slotApps != null) ? slotApps.size() : 0;

                                    if (slotNumber == 0) {
                            %>
                            <div class="planner-column empty"></div>
                            <%
                            } else {
                            %>
                            <button type="button" class="planner-column planner-appointment" data-bs-toggle="modal" data-bs-target="#slot-info-<%=i%>-<%=j%>">
                                <div class="row">
                                    <div class="icon">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="bi bi-person-fill" viewBox="0 0 16 16">
                                            <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1H3Zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"/>
                                        </svg>
                                    </div>
                                    <h3 class="appointment-patients-number"><%=slotNumber%></h3>
                                </div>
                            </button>

                            <div class="modal fade" id="slot-info-<%=i%>-<%=j%>" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-scrollable">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <h1 class="modal-title fs-5">Informazioni appuntamento</h1>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body">
                                            <div id="slot-i-j-patients">
                                                <% if (user.getType() == 1) { %>
                                                <h3>Pazienti</h3>
                                                <%
                                                    // Qui iteriamo SOLO sugli appuntamenti di questo slot non su tutti!
                                                    for (AppointmentRecord appRec : slotApps) {
                                                        PatientBean p = facade.findPatients("_id", appRec.idPatient(), user).get(0);
                                                        LocalTime start = appRec.date().toLocalTime();
                                                        LocalTime end = start.plusMinutes(appRec.duration());
                                                %>
                                                <p><%=p.getName()%> <%=p.getSurname()%> - Poltrona <%=appRec.chair()%> - <%=start%>-<%=end%></p>
                                                <%  } } %>
                                            </div>

                                            <div id="slot-i-j-medicines">
                                                <h3>Medicinali</h3>
                                                <%
                                                    for (AppointmentRecord appRec : slotApps) {
                                                        MedicineBean m = facade.findMedicines("_id", appRec.idMedicine(), user).get(0);
                                                %>
                                                <p><%=m.getName()%> - Poltrona <%=appRec.chair()%></p>
                                                <%  } %>
                                            </div>
                                        </div>
                                        <div class="modal-footer">
                                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Chiudi</button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <%
                                    } // end else (slot pieno)
                                } // end for j (colonne)
                            %>
                        </div>
                        <div class="planner-column planner-scroll"></div>
                    </div>
                    <%
                        } // end for i (righe)
                    %>
                    <hr>
                </div>
            </div>
        </div>
    </div>
</div>
<%
        }
    }
%>
</body>
</html>