package patientmanagement.application;

import java.util.Objects;

public class TherapyMedicineBean {

    //Parametri
    private String medicineId;
    private String medicineName;
    private int dose;

    //Costruttori

    public TherapyMedicineBean() {
    }

    public TherapyMedicineBean(String medicineId, int dose) {
        this.medicineId = medicineId;
        this.dose = dose;
    }

    //Getters

    public String getMedicineId() {
        return medicineId;
    }

    public int getDose() {
        return dose;
    }

    public String getMedicineName() {return medicineName;}

    //Setters

    public void setMedicineId(final String medicineId) {
        this.medicineId = medicineId;
    }

    public void setDose(final int dose) {
        this.dose = dose;
    }

    public void setMedicineName(final String medicineName) {this.medicineName = medicineName;}

    //Metodi ereditati da Object

    @Override
    public String toString() {
        return "Medicines{" +
                "medicineId='" + medicineId + '\'' +
                ", dose=" + dose +
                ", medicineName='" + medicineName + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final TherapyMedicineBean that = (TherapyMedicineBean) o;
        return dose == that.dose &&
                Objects.equals(medicineId, that.medicineId) &&
                Objects.equals(medicineName, that.medicineName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicineId, medicineName, dose);
    }
}
