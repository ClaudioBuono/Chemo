package plannermanagement.application;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlannerBean {
    private String id;
    private Date startDate;
    private Date endDate;
    private ArrayList<AppointmentBean> appointments;

    public PlannerBean(){}

    //Non prevede l'inserimento diretto degli appuntamenti, istanziando solamente l'agenda senza popolarla
    public PlannerBean(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.appointments = new ArrayList<>();
    }

    //Prevede l'inserimento diretto degli appuntamenti, instanziando e popolando l'agenda

    public PlannerBean(final Date startDate, final Date endDate, final List<AppointmentBean> appointments) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.appointments = (ArrayList<AppointmentBean>) appointments;
    }

    public PlannerBean(final String id, final Date startDate, final Date endDate, final List<AppointmentBean> appointments) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.appointments = (ArrayList<AppointmentBean>) appointments;
    }

    public String getId() {
        return id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getParsedStartDate() {
        return dateParser(startDate);
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getParsedEndDate() {
        return dateParser(endDate);
    }

    public List<AppointmentBean> getAppointments() {
        return appointments;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    public void setAppointments(final List<AppointmentBean> appointments) {
        this.appointments = (ArrayList<AppointmentBean>) appointments;
    }

    @Override
    public String toString() {
        return "Agenda{" +
                "id='" + id + '\'' +
                ", dataInizio=" + startDate +
                ", dataFine=" + endDate +
                ", appuntamenti=" + appointments +
                '}';
    }

    private String dateParser(Date date) {
        final Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}
