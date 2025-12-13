package plannermanagement.storage;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import connector.DatabaseConnector;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import plannermanagement.application.AppointmentBean;
import plannermanagement.application.PlannerBean;
import plannermanagement.application.green.PlannerSummary;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class PlannerQueryBean {
    private static final String APPOINTMENTS = "appointments";
    private static final String START = "start";
    final Logger logger = Logger.getLogger(getClass().getName());
    //Inserimento singolo documento nella collection
    public boolean insertDocument(final PlannerBean plannerBean){
        final MongoCollection<Document> collection = getCollection();
        final Document doc = createDocument(plannerBean);
        collection.insertOne(doc);

        if(plannerBean.getAppointments().isEmpty()){
            logger.severe("ERROR: No appointments selected to create the schedule");
            return false;
        }

        logger.info("Inserimento documento avvenuto con successo!");
        return true;
    }

    //Inserimento singolo documento nella collection, nel caso in cui l'agenda non sia popolata
    public boolean insertDocument(final PlannerBean plannerBean, final List<AppointmentBean> appuntamenti){
        final MongoCollection<Document> collection = getCollection();
        for(final AppointmentBean app : appuntamenti){
            plannerBean.getAppointments().add(app);
        }
        final Document doc = createDocument(plannerBean);

        collection.insertOne(doc);
        logger.info("Inserimento documento avvenuto con successo!");
        return true;
    }

    //Inserimento collezione di documenti nella collection
    public void insertDocuments(final List<PlannerBean> agende){
        final ArrayList<Document> documenti = new ArrayList<>();
        for(final PlannerBean ag : agende){
            final Document doc = createDocument(ag);
            documenti.add(doc);
        }

        final MongoCollection<Document> collection = getCollection();
        collection.insertMany(documenti);

        logger.info("Inserimento documenti avvenuto con successo!");
    }

    //Eliminazione documento dalla collection
    public void deleteDocument(final String chiave, final String valore){
        final MongoCollection<Document> collection = getCollection();
        collection.deleteOne(Filters.eq(chiave, valore));

        logger.info("Eliminazione documento avvenuta con successo!");
    }

    //Modifica di un documento
    public void updateDocument(final String id, final String valId, final String chiave, final Object valoreChiave){
        final MongoCollection<Document> collection = getCollection();
        collection.updateOne(Filters.eq(id, new ObjectId(valId)), Updates.set(chiave, valoreChiave));

        logger.info("Modifica documento avvenuta con successo!");
    }

    //Ricerca documento nella collection per una data coppia (chiave, valore)
    public List<PlannerBean> findDocument(final String chiave, final String valore){
        final MongoCollection<Document> collection = getCollection();
        final FindIterable<Document> iterDoc = collection.find(Filters.eq(chiave, valore));
        final Iterator<Document> it = iterDoc.iterator();
        final ArrayList<PlannerBean> p = new ArrayList<>();

        while(it.hasNext()){
            final Document document = it.next();
            final ArrayList<AppointmentBean> appointments = convertToArray(document.getList(APPOINTMENTS, Document.class));
            final PlannerBean planner = new PlannerBean(document.get("_id").toString(), document.getDate("startDate"), document.getDate("endDate"), appointments);
            p.add(planner);
        }
        return p;
    }

    public List<PlannerBean> findAll(){
        final MongoCollection<Document> collection = getCollection();
        final FindIterable<Document> iterDoc = collection.find();
        final Iterator<Document> it = iterDoc.iterator();
        final ArrayList<PlannerBean> p = new ArrayList<>();

        while(it.hasNext()){
            final Document document = it.next();
            final ArrayList<AppointmentBean> appointments = convertToArray(document.getList(APPOINTMENTS, Document.class));
            final PlannerBean planner = new PlannerBean(document.get("_id").toString(), document.getDate(START), document.getDate("end"), appointments);
            p.add(planner);
        }
        return p;
    }

    public List<PlannerSummary> findAllSummaries() {
        final MongoCollection<Document> collection = getCollection();

        // 1. PROJECTION: Chiediamo SOLO id, start, end.
        final var projection = Projections.fields(
                Projections.include("_id", START, "end")
        );

        // 2. QUERY LEGGERA
        final var iterDoc = collection.find()
                .projection(projection)
                .sort(Sorts.ascending(START));

        final List<PlannerSummary> summaries = new ArrayList<>();

        for (final Document doc : iterDoc) {
            // Conversione al volo: Mongo Date -> Java LocalDate
            final LocalDateTime start = doc.getDate(START).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            final LocalDateTime end = doc.getDate("end").toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

            summaries.add(new PlannerSummary(
                    doc.get("_id").toString(),
                    start,
                    end
            ));
        }
        return summaries;
    }

    public PlannerBean findById(final String id) {
        final MongoCollection<Document> collection = getCollection();

        // Qui NON usiamo proiezioni, perché ci servono gli appuntamenti!
        final Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();

        if (doc == null) return null;

        // Qui scatta la conversione pesante, MA LO FAI SOLO PER 1 RECORD!
        final ArrayList<AppointmentBean> appointments = convertToArray(doc.getList(APPOINTMENTS, Document.class));

        return new PlannerBean(
                doc.get("_id").toString(),
                doc.getDate(START),
                doc.getDate("end"),
                appointments
        );
    }

    public PlannerBean findDocumentById(final String id) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        final Bson filter = Filters.eq("_id", new ObjectId(id));

        //Cerca il documento
        final Document document = collection.find(filter).first();


        return new PlannerBean(document.get("_id").toString(), document.getDate(START), document.getDate("end"), convertToArray(document.getList(APPOINTMENTS, Document.class)));
    }

    public PlannerBean findLastDocument() {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Cerca il documento
        final Document document = collection.find().sort(new Document("_id", -1)).first();


        return new PlannerBean(document.get("_id").toString(), document.getDate(START), document.getDate("end"), convertToArray(document.getList(APPOINTMENTS, Document.class)));
    }

    private MongoCollection<Document> getCollection(){
        final MongoDatabase db = DatabaseConnector.getDatabase();

        final MongoCollection<Document> coll = db.getCollection("planner");
        logger.info("Collection \'planner\' recuperata con successo");
        return coll;
    }

    private Document createDocument(final PlannerBean plannerBean){
        final List<AppointmentBean> app = plannerBean.getAppointments();

        final ObjectId objectId = new ObjectId();
        plannerBean.setId(objectId.toString());
        return new Document("_id", objectId)
                .append(START, plannerBean.getStartDate())
                .append("end", plannerBean.getEndDate())
                .append(APPOINTMENTS, app);
    }

    private ArrayList<AppointmentBean> convertToArray(final List<Document> list){
        if(list == null)
            return new ArrayList<>();

        final ArrayList<AppointmentBean> appointments = new ArrayList<>();

        for(final Document d : list) {
            appointments.add(new AppointmentBean(d.getString("idPatient"), d.getString("idMedicine"), d.getDate("date"), d.getString("chair"), d.getInteger("duration")));
        }

        return appointments;
    }

}
