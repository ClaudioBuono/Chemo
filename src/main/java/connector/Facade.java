package connector;

import medicinemanagement.application.MedicineBean;
import medicinemanagement.application.PackageBean;
import medicinemanagement.storage.MedicineQueryBean;
import patientmanagement.application.PatientBean;
import patientmanagement.application.TherapyBean;
import patientmanagement.application.TherapyMedicineBean;
import patientmanagement.storage.PatientQueryBean;
import plannermanagement.application.AppointmentBean;
import plannermanagement.application.PlannerBean;
import plannermanagement.application.green.PlannerSummary;
import plannermanagement.storage.PlannerQueryBean;
import userManagement.application.UserBean;
import userManagement.storage.UserQueryBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Facade {
    final Logger logger = Logger.getLogger(getClass().getName());
    private final UserQueryBean userQueryBean = new UserQueryBean();
    private final PlannerQueryBean plannerQueryBean = new PlannerQueryBean();
    private final MedicineQueryBean medicineQueryBean = new MedicineQueryBean();
    private final PatientQueryBean patientQueryBean = new PatientQueryBean();

    public Facade(){
        // Empty constructor
    }

    /*
    Il metodo verifica se l'utente, una volta individuato, ha i permessi per effettuare determinate operazioni,
    ovvero se si tratta di un membro del personale medico (type = 1) o del gestore dei farmaci (type = 2)
     */
    private boolean isUserAuthorized(final String username, final int type){
        boolean valid = false;
        final ArrayList<UserBean> users = (ArrayList<UserBean>) findUsers("username", username);
        if(users.get(0) != null && users.get(0).getType() == type)
                valid = true;

        return valid;
    }

    /*
    OPERAZIONI CRUD PER ENTITA' USER
     */
    public List<UserBean> findUsers(final String chiave, final String valore){
        return userQueryBean.findDocument(chiave, valore);
    }

    public void updateUser(final String id, final String valId, final String chiave, final String valoreChiave){
        userQueryBean.updateDocument(id, valId, chiave, valoreChiave);
    }

    public void deleteUser(final String chiave, final String valore){
        userQueryBean.deleteDocument(chiave, valore);
    }

    public void insertUsers(final List<UserBean> utenti){
        userQueryBean.insertDocuments(utenti);
    }

    public void insertUser(final UserBean userBean){
        userQueryBean.insertDocument(userBean);
    }

    /*
    OPERAZIONI CRUD PER ENTITA' PLANNER
     */

    public PlannerBean findLatestPlanner(final UserBean user) {
        try {
            if(isUserAuthorized(user.getUsername(), 2) || isUserAuthorized(user.getUsername(), 1))
                return plannerQueryBean.findLastDocument();
        } catch (final Exception e) {
            logger.severe(e.getMessage());
        }


        return null;
    }

    public List<PlannerSummary> findAllPlannerSummaries(final UserBean user) {
        try {
            if(isUserAuthorized(user.getUsername(), 1) || isUserAuthorized(user.getUsername(), 2))
                return plannerQueryBean.findAllSummaries();
            else
                throw new IllegalAccessException("Utente non autorizzato alla modifica dei planner");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public PlannerBean findPlannerById(final String id, final UserBean user) {
        try {
            if(isUserAuthorized(user.getUsername(), 1) || isUserAuthorized(user.getUsername(), 2))
                return plannerQueryBean.findById(id);
            else
                throw new IllegalAccessException("Utente non autorizzato alla modifica dei planner");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }


    public void updatePlanner(final String id, final String valId, final String chiave, final Object valoreChiave, final UserBean user){
        try {
            if(isUserAuthorized(user.getUsername(), 1))
                plannerQueryBean.updateDocument(id, valId, chiave, valoreChiave);
            else
                throw new IllegalAccessException("Utente non autorizzato alla modifica di medicinali");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void deletePlanner(final String chiave, final String valore){
        plannerQueryBean.deleteDocument(chiave, valore);
    }

    public void insertPlanners(final List<PlannerBean> planners, final UserBean user){
        try {
            if (isUserAuthorized(user.getUsername(), 1))
                plannerQueryBean.insertDocuments(planners);
            else
                throw new IllegalAccessException("Utente non autorizzato per la produzione del planner");
        }catch(final Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    public void insertPlanner(final Date startDate, final Date endDate, final List<AppointmentBean> appointments, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 1)) {
                PlannerBean planner = new PlannerBean(startDate, endDate, appointments);
                plannerQueryBean.insertDocument(planner);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di un planner");
        }catch(final Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void insertPlanner(final PlannerBean planner, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 1)) {
                plannerQueryBean.insertDocument(planner);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di un planner");
        }catch(final Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /*
    OPERAZIONI CRUD PER ENTITA' MEDICINE
     */
    public MedicineBean insertMedicine(final String name, final String ingredients, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                MedicineBean medicineBean = new MedicineBean(name, ingredients);
                medicineQueryBean.insertDocument(medicineBean);
                return medicineBean;
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di medicinali");
        }catch(final Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    public void insertMedicine(final MedicineBean medicine, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                medicineQueryBean.insertDocument(medicine);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di medicinali");
        }catch(final Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    public void insertMedicinePackage(final String medicineId, final PackageBean medicinePackage, final UserBean user) {
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                medicineQueryBean.insertDocument(medicinePackage, medicineId);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di medicinali");
        }catch(final Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    public void insertMedicinePackage(final String medicineId, final String boxId, final boolean status, final Date expiryDate, final int capacity, final UserBean user) {
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                PackageBean newPackage = new PackageBean(status, expiryDate, capacity, boxId);
                medicineQueryBean.insertDocument(newPackage, medicineId);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di medicinali");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }


    public void removeMedicinePackage(final String boxId, final UserBean user) {
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                medicineQueryBean.deleteDocument("boxId", boxId);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento di medicinali");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void insertMedicines(final List<MedicineBean> medicines){medicineQueryBean.insertDocuments(medicines);}

    public void deleteMedicine(final String key, final String value, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                medicineQueryBean.deleteDocument(key, value);
            }else
                throw new IllegalAccessException("Utente non autorizzato all'eliminazione di medicinali");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void updateMedicine(final String id, final String valId, final String key, final Object valKey, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 2)){
                medicineQueryBean.updateDocument(id, valId, key, valKey);
            }else
                throw new IllegalAccessException("Utente non autorizzato alla modifica di medicinali");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public List<MedicineBean> findMedicines(final String key, final Object value, final UserBean user){
        ArrayList<MedicineBean> medicines = new ArrayList<>();
        try{
            if(isUserAuthorized(user.getUsername(), 2) || isUserAuthorized(user.getUsername(), 1)){
                if(key.equals("_id"))  {
                    medicines.add(medicineQueryBean.findDocumentById(String.valueOf(value)));
                    return medicines;
                }
                return medicineQueryBean.findDocument(key, value);
            }else
                throw new IllegalAccessException("Utente non autorizzato alla modifica di medicinali");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return medicines;
    }

    public MedicineBean findMedicineByName(final String name) {
        return medicineQueryBean.findMedicineByName(name);
    }

    public List<MedicineBean> findMedicines(final List<String> key, final List<Object> value, final UserBean user){
        ArrayList<MedicineBean> medicines = new ArrayList<>();
        try{
            if(isUserAuthorized(user.getUsername(), 2) || isUserAuthorized(user.getUsername(), 1)){
                return medicineQueryBean.findDocument(key, value);
            }else
                throw new IllegalAccessException("Utente non autorizzato alla modifica di medicinali");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return medicines;
    }

    public List<MedicineBean> findAllMedicines(final UserBean user) {
        try {
            if(isUserAuthorized(user.getUsername(), 1) || isUserAuthorized(user.getUsername(), 2))
                return medicineQueryBean.findAll();
            else
                throw new IllegalAccessException("Utente non autorizzato alla modifica di medicinali");
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * Single method for searching medicines: manages both the complete list (empty lists)
     * and filtered searches, always with pagination
     */
    public List<MedicineBean> findMedicinesPaginated(final List<String> keys, final List<Object> values, final int page, final int size, final UserBean user) {
        try {
            if (isUserAuthorized(user.getUsername(), 1) || isUserAuthorized(user.getUsername(), 2)) {
                return medicineQueryBean.findMedicinesPaginated(keys, values, page, size);
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Errore Facade search: {0}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Method for counting the total results of a search (or the entire database if filters are empty).
     * Used to calculate the number of pages.
     */
    public long countMedicinesFiltered(final List<String> keys, final List<Object> values, final UserBean user) {
        try {
            if (isUserAuthorized(user.getUsername(), 1) || isUserAuthorized(user.getUsername(), 2)) {
                return medicineQueryBean.countMedicinesFiltered(keys, values);
            } else {
                throw new IllegalAccessException("Utente non autorizzato");
            }

        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Errore Facade count: {0}", e.getMessage());
        }
        return 0;
    }

    public List<String> findMedicineNamesLike(final String query) {
        return medicineQueryBean.findMedicineNamesLike(query);
    }

    /*
    OPERAZIONI CRUD PER ENTITA' PATIENT
     */

    public PatientBean insertPatient(PatientBean patient, UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 1)){
                patientQueryBean.insertDocument(patient);
                return patient;
            }
            else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public void insertTherapy(final String patientId, final TherapyBean therapy, final UserBean user) {
        try{
            if(isUserAuthorized(user.getUsername(), 1)) {
                patientQueryBean.insertDocument(therapy, patientId);
            }
            else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void insertTherapy(final int sessions, final List<TherapyMedicineBean> medicines, final int duration, final int frequency, final String patientId, final UserBean user) {
        try{
            TherapyBean therapy = new TherapyBean(sessions, medicines, duration, frequency);
            if(isUserAuthorized(user.getUsername(), 1)) {
                patientQueryBean.insertDocument(therapy, patientId);
            }
            else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void insertPatients(final List<PatientBean> patients, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 1))
                patientQueryBean.insertDocuments(patients);
            else
                throw new IllegalAccessException("Utente non autorizzato all'inserimento dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void deletePatient(final String key, final String value, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 1))
                patientQueryBean.deleteDocument(key, value);
            else
                throw new IllegalAccessException("Utente non autorizzato all'eliminazione dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void updatePatient(final String id, final String valId, final String key, final Object valKey, final UserBean user){
        try{
            if(isUserAuthorized(user.getUsername(), 1))
                patientQueryBean.updateDocument(id, valId, key, valKey);
            else
                throw new IllegalAccessException("Utente non autorizzato alla modifica dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    //modificare il metodo in patientQueryBean in modo che restituisca ArrayList<PatientBean>
    public List<PatientBean> findPatients(final String key, final Object value, final UserBean user){
        ArrayList<PatientBean> patients = new ArrayList<>();
        try{
            if(isUserAuthorized(user.getUsername(), 1)) {
                if (key.equals("_id")) {
                    patients.add(patientQueryBean.findDocumentById(String.valueOf(value)));
                    return patients;
                }
                return patientQueryBean.findDocument(key, value);
            }
            else
                throw new IllegalAccessException("Utente non autorizzato alla visualizzazione dei pazienti");
        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return patients;
    }

    /**
     * Single method for searching patients: manages both the complete list (empty lists)
     * and filtered searches, always with pagination
     */
    public List<PatientBean> findPatientsPaginated(final List<String> keys, final List<Object> values, final int page, final int size, final UserBean user) {
        try {
            if (isUserAuthorized(user.getUsername(), 1)) {
                // Chiama il metodo del Bean
                return patientQueryBean.findDocumentsPaginated(keys, values, page, size);
            } else {
                throw new IllegalAccessException("Utente non autorizzato");
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Errore Facade search: {0}", e.getMessage());
            return new ArrayList<>(); // Ritorna lista vuota per sicurezza
        }
    }

    /**
     * Method for counting the total results of a search (or the entire database if filters are empty).
     * Used to calculate the number of pages.
     */
    public long countPatientsFiltered(final List<String> keys, final List<Object> values, final UserBean user) {
        try {
            if (isUserAuthorized(user.getUsername(), 1)) {
                return patientQueryBean.countPatientsFiltered(keys, values);
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE,"Errore Facade count: {0}", e.getMessage());
        }
        return 0;
    }

}
