package patientmanagement.storage;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import connector.DatabaseConnector;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import patientmanagement.application.PatientBean;
import patientmanagement.application.TherapyBean;
import patientmanagement.application.TherapyMedicineBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class PatientQueryBean {
    private static final String NOTES = "notes";
    private static final String CONDITION = "condition";
    private static final String STATUS = "status";
    private static final String PHONE_NUMBER = "phoneNumber";
    private static final String CITY = "city";
    private static final String BIRTH_DATE = "birthDate";
    private static final String SURNAME = "surname";
    private static final String NAME = "name";
    private static final String TAX_CODE = "taxCode";
    private static final String SESSIONS = "sessions";
    private static final String DURATION = "duration";
    private static final String FREQUENCY = "frequency";
    private static final String MEDICINES = "medicines";
    private static final String THERAPY = "therapy";
    final Logger logger = Logger.getLogger(getClass().getName());

    //Inserimento singolo documento nella Collection
    public boolean insertDocument(final PatientBean patient) {
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone( "Europe/Rome"),Locale.ITALY);
        final Date today = calendar.getTime();
        final MongoCollection<Document> collection = getCollection();

        if(patient.getName().isEmpty() || patient.getName().length() > 32){
            logger.severe("ERROR: name lenght incorrect!");
            return false;
        }else if(patient.getSurname().isEmpty() || patient.getSurname().length() > 32){
            logger.severe("ERROR: surname lenght incorrect!");
            return false;
        }else if(patient.getBirthDate().compareTo(today) > 0){
            logger.severe("ERROR: date format incorrect!");
            return false;
        }else if(patient.getCity().isEmpty() || patient.getCity().length() > 50){
            logger.severe("ERROR: city lenght incorrect!");
            return false;
        }else if(patient.getTaxCode().length() != 16){
            logger.severe("ERROR: taxcode lenght incorrect!");
            return false;
        }else if(patient.getPhoneNumber().length() > 18){
            logger.severe("ERROR: phoneNumber lenght incorrect!");
            return false;
        }else if(patient.getNotes().length() > 255){
            logger.severe("ERROR: notes lenght incorrect!");
            return false;
        }else{
            final Document document = createDocument(patient);
            collection.insertOne(document);
            logger.severe("Documento inserito con successo nella Collection");
            return true;
        }
    }

    //Inserimento terapia in un paziente
    public boolean insertDocument(final TherapyBean therapy, final String patientId) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il documento da inserire nella Collection
        final Document therapyDocument = createDocument(therapy);

        //Crea il filtro
        final Bson filter = Filters.eq("_id", new ObjectId(patientId));

        //Recupera il documento del paziente
        final Document patientDoc = collection.find(filter).first();
        if(therapy.getSessions() <= 0){
            logger.severe("ERROR: session non valid!");
            return false;
        }else if(therapy.getMedicines().isEmpty()){
            logger.severe("ERROR: not a single medicine found!");
            return false;
        }else if(therapy.getFrequency() <= 0){
            logger.severe("ERROR: frequency non valid!");
            return false;
        }else if(therapy.getDuration() <= 0){
            logger.severe("ERROR: duration non valid!");
            return false;
        }else if(patientDoc == null){
            logger.log(Level.SEVERE, "ERROR: patient with id {0} not found!", patientId);
            return false;
        }

        //Inserisci il documento della terapia nel documento del paziente
        collection.updateOne(patientDoc, new Document("$set", therapyDocument));
        logger.info("Documento inserito con successo nella Collection");
        return true;
    }

    //Inserimento collezione di documenti nella Collection
    public boolean insertDocuments(final ArrayList<PatientBean> patients) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea un documento per ogni medicinale in medicines
        final ArrayList<Document> docs = new ArrayList<>();
        for (final PatientBean patient : patients) {
            final Document doc = createDocument(patient);
            docs.add(doc);
        }

        //Inserisci i documenti nella collection
        collection.insertMany(docs);
        logger.info("Documenti inseriti con successo nella Collection");
        return true;
    }

    //Elimina documento dalla Collection
    public void deleteDocument(final String key, final String value) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        final Bson filter = Filters.eq(key, value);

        //Cancella il documento
        collection.deleteOne(filter);

        logger.info("Documento eliminato con successo nella Collection");
    }

    //Modifica di un documento
    public boolean updateDocument(final String id, final String valId, final String key, final Object valKey) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        final Bson filter = Filters.eq(id, new ObjectId(valId));
        if(key.equalsIgnoreCase(NOTES) && valKey.toString().isEmpty()){
            logger.severe("ERROR: new length notes too short");
            return false;
        }else if(key.equalsIgnoreCase(CONDITION) && valKey.toString().isEmpty()){
            logger.severe("ERROR: condition too short");
            return false;
        }else if(key.equalsIgnoreCase(FREQUENCY)){
            final String fr = String.valueOf(valKey);
            final int frequency = Integer.parseInt(fr);
            if(frequency <= 0) {
                logger.severe("ERROR: frequency cant be less than 1");
                return false;
            }
        }else if(key.equalsIgnoreCase(DURATION)){
            final String dr = String.valueOf(valKey);
            final int duration = Integer.parseInt(dr);
            if(duration <= 0) {
                logger.severe("ERROR: duration cant be less than 1");
                return false;
            }
        }else if(key.equalsIgnoreCase("dose")){
            final String ds = String.valueOf(valKey);
            final int dose = Integer.parseInt(ds);
            if(dose <= 0) {
                logger.severe("ERROR: dose cant be less than 1");
                return false;
            }
        }else if(key.equalsIgnoreCase("numbers")){
            final String ss = String.valueOf(valKey);
            final int session = Integer.parseInt(ss);
            if(session < 0) {
                logger.severe("ERROR: session cant be less than 1");
                return false;
            }
        }
        //Aggiorna il documento
        collection.updateOne(filter, Updates.set(key, valKey));
        logger.info("Documento aggiornato con successo nella Collection");
        return true;
    }

    public boolean updateTherapy(final String id, final String valId, final String key, final TherapyBean therapyBean){
        final MongoCollection<Document> collection = getCollection();
        final Bson filter = Filters.eq(id, new ObjectId(valId));
        collection.updateOne(filter, Updates.set(key, therapyBean));
        logger.info("Documento aggiornato con successo nella Collection");
        return true;
    }

    //Ricerca di un documento nella Collection data una coppia (key, value)
    public ArrayList<PatientBean> findDocument(final String key, final Object value) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        final Bson filter = Filters.eq(key, value);

        //Cerca il documento
        final FindIterable<Document> iterDoc = collection.find(filter);

        final Iterator<Document> it = iterDoc.iterator();
        final ArrayList<PatientBean> patients = new ArrayList<>();

        //Itero su ogni documento restituito dalla query
        while(it.hasNext()) {
            final Document document = it.next();
            //ArrayList<TherapyBean> therapies = convertToArray(document.getList("therapy", TherapyBean.class));
            final PatientBean patient = parsePatient(document);
            patient.setPatientId(document.get("_id").toString());
            patients.add(patient);
        }

        return patients;
    }

    public ArrayList<PatientBean> findDocument(final ArrayList<String> key, final ArrayList<Object> value) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson finalFilter = null;
        Bson filter = null;
        int i = 0;

        //Ciclo sull'array di filtri
        do {
            switch (key.get(i)) {
                case NAME, SURNAME -> { // Nome e Cognome lavorano con le regex

                    final String quotedValue = Pattern.quote((String) value.get(i));
                    final Bson currentFilter = Filters.regex(
                            key.get(i),
                            quotedValue,
                            "i"
                    );

                    if (i == 0) {
                        finalFilter = currentFilter;
                    } else {
                        filter = currentFilter;
                    }
                }

                case STATUS -> { //Stato paziente
                    if (i == 0)
                        finalFilter = Filters.eq(key.get(i), value.get(i));
                    else
                        filter = Filters.eq(key.get(i), value.get(i));
                }

                case "medicine" -> { //Medicinale nella terapia
                    if (i == 0)
                        finalFilter = Document.parse("{'therapy.medicines': {$elemMatch: {medicineId: '"+value.get(i)+"'}}}");
                    else
                        filter = Document.parse("{'therapy.medicines': {$elemMatch: {medicineId: '"+value.get(i)+"'}}}");
                }

                default -> logger.log(Level.SEVERE, "ERROR: illegal value for key {0}", key.get(i));
            }

            if (i > 0)
                finalFilter = Filters.and(finalFilter, filter);

            ++i;
        } while(i < key.size());

        //Cerca il documento
        final FindIterable<Document> iterDoc = collection.find(finalFilter);

        final Iterator<Document> it = iterDoc.iterator();
        final ArrayList<PatientBean> patients = new ArrayList<>();

        //Itero su ogni documento restituito dalla query
        while(it.hasNext()) {
            final Document document = it.next();
            final PatientBean patient = parsePatient(document);
            patient.setPatientId(document.get("_id").toString());
            patients.add(patient);
        }

        return patients;
    }

    // Paginated search, handles both full search and filtered search
    public List<PatientBean> findDocumentsPaginated(final List<String> keys, final List<Object> values, final int page, final int size) {
        final MongoCollection<Document> collection = getCollection();

        // Create filters
        final Bson finalQuery = buildFilter(keys, values);

        // Pagination query
        final int skipCount = (page - 1) * size;
        final ArrayList<PatientBean> patients = new ArrayList<>();
        final FindIterable<Document> result = collection.find(finalQuery)
                .skip(skipCount)
                .limit(size);

        // Create patient documents to return
        for (final Document document : result) {
            final PatientBean p = parsePatient(document);
            p.setPatientId(document.get("_id").toString());
            patients.add(p);
        }

        return patients;
    }

    // Count the number of patients, handles both full search and filtered search
    public long countPatientsFiltered(final List<String> keys, final List<Object> values) {
        final MongoCollection<Document> collection = getCollection();

        // Create filters
        final Bson filter = buildFilter(keys, values);

        // Counts documents in the collection (does not make a query)
        return collection.countDocuments(filter);
    }

    public ArrayList<PatientBean> findAll() {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Cerca il documento
        final FindIterable<Document> iterDoc = collection.find();

        final Iterator<Document> it = iterDoc.iterator();
        final ArrayList<PatientBean> patients = new ArrayList<>();

        //Itero su ogni documento restituito dalla query
        while(it.hasNext()) {
            final Document document = it.next();
            final PatientBean patient = parsePatient(document);
            patient.setPatientId(document.get("_id").toString());
            patients.add(patient);
        }

        return patients;
    }

    //Ricerca di un documento nella Collection in base al suo ObjectId
    public PatientBean findDocumentById(final String value) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        final Bson filter = Filters.eq("_id", new ObjectId(value));

        //Cerca il documento
        final Document document = collection.find(filter).first();

        //ArrayList<TherapyBean> therapies = convertToArray(document.getList("therapy", TherapyBean.class));
        if(document != null) {
            final PatientBean patient = parsePatient(document);
            patient.setPatientId(value);

            return patient;
        }

        return null;
    }


    //Metodi ausiliari
    private MongoCollection<Document> getCollection() {
        final MongoDatabase mongoDatabase = DatabaseConnector.getDatabase();

        final MongoCollection<Document> collection = mongoDatabase.getCollection("patient");
        logger.info("Collection 'patient' recuperata con successo");
        return collection;
    }

    private Document createDocument(final PatientBean patient) {
        final ObjectId objectId = new ObjectId();
        patient.setPatientId(objectId.toString());
        return new Document("_id", objectId)
                .append(TAX_CODE, patient.getTaxCode())
                .append(NAME, patient.getName())
                .append(SURNAME, patient.getSurname())
                .append(BIRTH_DATE, patient.getBirthDate())
                .append(CITY, patient.getCity())
                .append(PHONE_NUMBER, patient.getPhoneNumber())
                .append(STATUS, patient.getStatus())
                .append(NOTES, patient.getNotes());
    }

   private Document createDocument(final TherapyBean therapyBean) {
        //Creo il documento con le informazioni della terapia
       final Document therapyDocument = new Document()
                .append(SESSIONS, therapyBean.getSessions())
                .append(DURATION, therapyBean.getDuration())
                .append(FREQUENCY, therapyBean.getFrequency())
                .append(MEDICINES, therapyBean.getMedicines());

       //Restituisco il documento della terapia
        return new Document(THERAPY, therapyDocument);
    }

    private TherapyBean parseTherapy(final Document document) {
        //Se non c'è una terapia restituisco null
        if(document == null) {
            return null;
        }

        //Se c'è una terapia

        //Recupero i medicinali e li inserisco in un ArrayList
        final List<Document> medicinesDocument = document.getList(MEDICINES, Document.class);
        final ArrayList<TherapyMedicineBean> medicines = new ArrayList<>();

        for (final Document d : medicinesDocument) {
            medicines.add(new TherapyMedicineBean(d.getString("medicineId"), d.getInteger("dose")));
        }


        //Restituisco il documento della terapia
        return new TherapyBean(document.getInteger(SESSIONS), medicines,
                document.getInteger(DURATION), document.getInteger(FREQUENCY));
    }

    // Build filters to use in search
    private Bson buildFilter(final List<String> keys, final List<Object> values) {
        final List<Bson> filtersList = new ArrayList<>();

        final int keysSize = keys.size();
        final int valuesSize = values.size();
        if (keysSize == valuesSize) {
            for (int i = 0; i < keysSize; ++i) {
                final String currentKey = keys.get(i);
                final Object currentValue = values.get(i);

                switch (currentKey) {
                    case NAME, SURNAME -> filtersList.add(Filters.regex(currentKey, Pattern.quote(currentValue.toString()), "i"));

                    case STATUS -> filtersList.add(Filters.eq(currentKey, currentValue));

                    case "medicine" -> filtersList.add(Filters.eq("therapy.medicines.medicineId", currentValue));

                    default -> logger.log(Level.SEVERE, "Invalid filter: {0}", currentValue);
                }
            }
        }

        // Se non ci sono filtri, ritorna un Documento vuoto (trova tutto)
        return filtersList.isEmpty() ? new Document() : Filters.and(filtersList);
    }

    // Parse a PatientBean from a retrieved document
    private PatientBean parsePatient(final Document document) {
        return new PatientBean(
                document.getString(TAX_CODE),
                document.getString(NAME),
                document.getString(SURNAME),
                document.getDate(BIRTH_DATE),
                document.getString(CITY),
                document.getString(PHONE_NUMBER),
                document.getBoolean(STATUS),
                document.getString(CONDITION),
                document.getString(NOTES),
                parseTherapy((Document) document.get(THERAPY))
        );
    }
}