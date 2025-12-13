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

public class PlannerQueryBean {
    //Inserimento singolo documento nella collection
    public boolean insertDocument(PlannerBean plannerBean){
        MongoCollection<Document> collection = getCollection();
        Document doc = createDocument(plannerBean);
        collection.insertOne(doc);

        if(plannerBean.getAppointments().size() == 0){
            System.out.println("ERROR: No appointments selected to create the schedule");
            return false;
        }

        System.out.println("Inserimento documento avvenuto con successo!");
        return true;
    }

    //Inserimento singolo documento nella collection, nel caso in cui l'agenda non sia popolata
    public boolean insertDocument(PlannerBean plannerBean, ArrayList<AppointmentBean> appuntamenti){
        MongoCollection<Document> collection = getCollection();
        for(AppointmentBean app : appuntamenti){
            plannerBean.getAppointments().add(app);
        }
        Document doc = createDocument(plannerBean);

        collection.insertOne(doc);
        System.out.println("Inserimento documento avvenuto con successo!");
        return true;
    }

    //Inserimento collezione di documenti nella collection
    public void insertDocuments(ArrayList<PlannerBean> agende){
        ArrayList<Document> documenti = new ArrayList<>();
        for(PlannerBean ag : agende){
            Document doc = createDocument(ag);
            documenti.add(doc);
        }

        MongoCollection<Document> collection = getCollection();
        collection.insertMany(documenti);

        System.out.println("Inserimento documenti avvenuto con successo!");
    }

    //Eliminazione documento dalla collection
    public void deleteDocument(String chiave, String valore){
        MongoCollection<Document> collection = getCollection();
        collection.deleteOne(Filters.eq(chiave, valore));

        System.out.println("Eliminazione documento avvenuta con successo!");
    }

    //Modifica di un documento
    public void updateDocument(String id, String valId, String chiave, Object valoreChiave){
        MongoCollection<Document> collection = getCollection();
        collection.updateOne(Filters.eq(id, new ObjectId(valId)), Updates.set(chiave, valoreChiave));

        System.out.println("Modifica documento avvenuta con successo!");
    }

    //Ricerca documento nella collection per una data coppia (chiave, valore)
    public ArrayList<PlannerBean> findDocument(String chiave, String valore){
        MongoCollection<Document> collection = getCollection();
        FindIterable<Document> iterDoc = collection.find(Filters.eq(chiave, valore));
        Iterator<Document> it = iterDoc.iterator();
        ArrayList<PlannerBean> p = new ArrayList<>();

        while(it.hasNext()){
            Document document = (Document) it.next();
            ArrayList<AppointmentBean> appointments = convertToArray(document.getList("appointments", Document.class));
            PlannerBean planner = new PlannerBean(document.get("_id").toString(), document.getDate("startDate"), document.getDate("endDate"), appointments);
            p.add(planner);
        }
        return p;
    }

    public ArrayList<PlannerBean> findAll(){
        MongoCollection<Document> collection = getCollection();
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        ArrayList<PlannerBean> p = new ArrayList<>();

        while(it.hasNext()){
            Document document = (Document) it.next();
            ArrayList<AppointmentBean> appointments = convertToArray(document.getList("appointments", Document.class));
            PlannerBean planner = new PlannerBean(document.get("_id").toString(), document.getDate("start"), document.getDate("end"), appointments);
            p.add(planner);
        }
        return p;
    }

    public List<PlannerSummary> findAllSummaries() {
        final MongoCollection<Document> collection = getCollection();

        // 1. PROJECTION: Chiediamo SOLO id, start, end.
        final var projection = Projections.fields(
                Projections.include("_id", "start", "end")
        );

        // 2. QUERY LEGGERA
        final var iterDoc = collection.find()
                .projection(projection)
                .sort(Sorts.ascending("start"));

        final List<PlannerSummary> summaries = new ArrayList<>();

        for (final Document doc : iterDoc) {
            // Conversione al volo: Mongo Date -> Java LocalDate
            final LocalDateTime start = doc.getDate("start").toInstant()
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
        final ArrayList<AppointmentBean> appointments = convertToArray(doc.getList("appointments", Document.class));

        return new PlannerBean(
                doc.get("_id").toString(),
                doc.getDate("start"),
                doc.getDate("end"),
                appointments
        );
    }

    public PlannerBean findDocumentById(String id) {
        //Recupera la Collection
        final MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = Filters.eq("_id", new ObjectId(id));

        //Cerca il documento
        Document document = collection.find(filter).first();


        return new PlannerBean(document.get("_id").toString(), document.getDate("start"), document.getDate("end"), convertToArray(document.getList("appointments", Document.class)));
    }

    public PlannerBean findLastDocument() {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Cerca il documento
        Document document = collection.find().sort(new Document("_id", -1)).first();


        return new PlannerBean(document.get("_id").toString(), document.getDate("start"), document.getDate("end"), convertToArray(document.getList("appointments", Document.class)));
    }

    private MongoCollection<Document> getCollection(){
        final MongoDatabase db = DatabaseConnector.getDatabase();

        MongoCollection<Document> coll = db.getCollection("planner");
        System.out.println("Collection \'agenda\' recuperata con successo");
        return coll;
    }

    private Document createDocument(PlannerBean plannerBean){
        List<AppointmentBean> app = plannerBean.getAppointments();

        ObjectId objectId = new ObjectId();
        plannerBean.setId(objectId.toString());
        return new Document("_id", objectId)
                .append("start", plannerBean.getStartDate())
                .append("end", plannerBean.getEndDate())
                .append("appointments", app);
    }

    private ArrayList<AppointmentBean> convertToArray(List<Document> list){
        if(list == null)
            return null;

        ArrayList<AppointmentBean> appointments = new ArrayList<>();

        for(Document d : list) {
            appointments.add(new AppointmentBean(d.getString("idPatient"), d.getString("idMedicine"), d.getDate("date"), d.getString("chair"), d.getInteger("duration")));
        }

        return appointments;
    }

}
