package patientmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TherapyBean {

    //Parametri
    private int sessions;
    private ArrayList<TherapyMedicineBean> medicines;

    private int duration;
    private int frequency;

    //Costruttori

    public TherapyBean() {
    }

    public TherapyBean(final int sessions, final List<TherapyMedicineBean> medicines, final int duration, final int frequency) {
        this.sessions = sessions;
        this.medicines = (ArrayList<TherapyMedicineBean>) medicines;
        this.duration = duration;
        this.frequency = frequency;
    }

    //Getters

    public int getSessions() {
        return sessions;
    }

    public ArrayList<TherapyMedicineBean> getMedicines() {
        return medicines;
    }

    public int getDuration() {
        return duration;
    }

    public int getFrequency() {
        return frequency;
    }

    //Setters

    public void setSessions(int sessions) {
        this.sessions = sessions;
    }

    public void setMedicines(ArrayList<TherapyMedicineBean> medicines) {
        this.medicines = medicines;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    //Metodi ereditati da Object

    @Override
    public String toString() {
        return "TherapyBean{" +
                "sessions=" + sessions +
                ", medicines=" + medicines +
                ", duration=" + duration +
                ", frequency=" + frequency +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final TherapyBean that = (TherapyBean) o;
        return sessions == that.sessions;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sessions);
    }
}
