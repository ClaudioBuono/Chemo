package medicinemanagement.storage;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import connector.DatabaseConnector;
import medicinemanagement.application.MedicineBean;
import medicinemanagement.application.PackageBean;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MedicineQueryBean {
    final Logger logger = Logger.getLogger(getClass().getName());

    private static final String NAME = "name";
    private static final String STATUS = "status";
    private static final String EXPIRY_DATE = "expiryDate";
    private static final String PACKAGE = "package";
    private static final String AMOUNT = "amount";
    private static final String INGREDIENTS = "ingredients";

    //Inserimento singolo documento nella Collection
    public boolean insertDocument(MedicineBean medicine) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il documento da inserire nella Collection
        Document document = createDocument(medicine);
        if(medicine.getName().length() < 0 || medicine.getName().length() > 32){
            System.out.println("ERROR: name length incorrect!");
            return false;
        }else if(medicine.getIngredients().length() > 100){
            System.out.println("ERROR: ingredients length out of range!");
            return false;
        }
        //Inserisci il documento nella collection
        collection.insertOne(document);
        System.out.println("Documento inserito con successo nella Collection");
        return true;
    }

    //Inserimento una confezione in un medicinale
    public void insertDocument(PackageBean newPackage, String medicineId) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = Filters.eq("_id", new ObjectId(medicineId));

        //Recupera il documento del medicinale
        Document medicineDocument = collection.find(filter).first();

        //Aggiorna l'amount di package
        final int amount = medicineDocument.getInteger(AMOUNT);
        collection.updateOne(medicineDocument, new Document("$set", new Document(AMOUNT, amount+1)));

        //Aggiorna l'id del package
        newPackage.setPackageId(String.valueOf(amount));

        //Crea il documento da inserire nella Collection
        Document packageDocument = createDocument(newPackage);

        medicineDocument = collection.find(filter).first();

        //Inserisci il documento nella collection
        collection.updateOne(medicineDocument, new Document("$push", packageDocument));

        System.out.println("Documento inserito con successo nella Collection");
    }

    //Inserimento collezione di documenti nella Collection
    public void insertDocuments(ArrayList<MedicineBean> medicines) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea un documento per ogni medicinale in medicines
        ArrayList<Document> docs = new ArrayList<>();
        for(MedicineBean medicine : medicines) {
            Document doc = createDocument(medicine);
            docs.add(doc);
        }

        //Inserisci i documenti nella collection
        collection.insertMany(docs);

        System.out.println("Documenti inseriti con successo nella Collection");
    }

    //Elimina documento dalla Collection
    public void deleteDocument(String key, String value) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = Filters.eq(key, value);

        //Cancella il documento
        collection.deleteOne(filter);

        System.out.println("Documento eliminato con successo nella Collection");
    }

    //Modifica di un documento
    public void updateDocument(String id, String valId, String key, Object valKey) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = Filters.eq(id, valId);

        //Aggiorna il documento
        collection.updateOne(filter, Updates.set(key, valKey));

        System.out.println("Documento aggiornato con successo nella Collection");
    }

    //Ricerca di un documento nella Collection data una coppia (key, value)

    public ArrayList<MedicineBean> findDocument(String key, Object value) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = Filters.eq(key, value);

        //Cerca il documento
        FindIterable<Document> iterDoc = collection.find(filter);

        Iterator<Document> it = iterDoc.iterator();
        ArrayList<MedicineBean> medicines = new ArrayList<>();

        while (it.hasNext()) {
            Document document = it.next();
            final MedicineBean medicine = parseMedicine(document);
            medicines.add(medicine);
        }

        return medicines;
    }

    public ArrayList<MedicineBean> findDocument(ArrayList<String> key, ArrayList<Object> value) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = null;
        Bson finalFilter = null;
        Pattern regex;
        int i = 0;

        do {
            System.out.println("i: " + i + " filter: " + key.get(i));
            //Controllo di che tipo di valore si tratta
            switch (key.get(i)) {
                case NAME -> { //Nome
                    regex = Pattern.compile(Pattern.quote((String) value.get(i)), Pattern.CASE_INSENSITIVE);
                    if (i == 0)
                        finalFilter = Filters.eq(key.get(i), regex);
                    else
                        filter = Filters.eq(key.get(i), regex);
                }

                case STATUS -> { //Stato
                    if((boolean) value.get(i)) { //Disponibile
                        if (i == 0)
                            finalFilter = Filters.gt(AMOUNT, 0);
                        else
                            filter = Filters.gt(AMOUNT, 0);
                    } else { //Esaurito
                        if (i == 0)
                            finalFilter = Filters.eq(AMOUNT, 0);
                        else
                            filter = Filters.eq(AMOUNT, 0);
                    }
                }

                case EXPIRY_DATE -> { //Data scadenza: medicinali con almeno un package in scadenza entro quella data
                    if (i == 0)
                        finalFilter = Document.parse("{'package': {$elemMatch: { expiryDate: { $lt: ISODate('"+value.get(i)+"')}}}}");
                    else
                        filter = Document.parse("{'package': {$elemMatch: { expiryDate: { $lt: ISODate('"+value.get(i)+"')}}}}");
                }
            }

            if (i > 0)
                finalFilter = Filters.and(finalFilter, filter);

            i++;
        } while (i < key.size());

        System.out.println(finalFilter);

        //Cerca il documento
        FindIterable<Document> iterDoc = collection.find(finalFilter);

        Iterator<Document> it = iterDoc.iterator();
        ArrayList<MedicineBean> medicines = new ArrayList<>();

        while (it.hasNext()) {
            Document document = it.next();
            final MedicineBean medicine = parseMedicine(document);
            medicines.add(medicine);
        }

        return medicines;
    }

    public MedicineBean findDocumentById(String value) {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Crea il filtro
        Bson filter = Filters.eq("_id", new ObjectId(value));

        //Cerca il documento
        Document document = collection.find(filter).first();

        //Restituisce il medicinale
        if (document == null) return null;
        return parseMedicine(document);
    }

    public ArrayList<MedicineBean> findAll() {
        //Recupera la Collection
        MongoCollection<Document> collection = getCollection();

        //Cerca il documento
        FindIterable<Document> iterDoc = collection.find();

        Iterator<Document> it = iterDoc.iterator();
        ArrayList<MedicineBean> medicines = new ArrayList<>();

        while (it.hasNext()) {
            Document document = it.next();
            final MedicineBean medicine = parseMedicine(document);
            medicines.add(medicine);
        }

        return medicines;
    }

    /**
     * Retrieves a paginated list of medicines based on dynamic filters.
     */
    public List<MedicineBean> findMedicinesPaginated(final List<String> keys, final List<Object> values, final int page, final int size) {
        final MongoCollection<Document> collection = getCollection();

        // Build the filter
        final Bson filter = buildFilter(keys, values);

        // Pagination query
        final int skipCount = (page - 1) * size;
        final ArrayList<MedicineBean> medicines = new ArrayList<>();
        final FindIterable<Document> result = collection.find(filter)
                .skip(skipCount)
                .limit(size);

        // Create medicine documents to return
        for (final Document document : result) {
            final MedicineBean m = parseMedicine(document);
            m.setId(document.get("_id").toString());
            medicines.add(m);
        }

        return medicines;
    }

    /**
     * Counts the total number of documents matching the filters.
     */
    public long countMedicinesFiltered(final List<String> keys, final List<Object> values) {
        return getCollection().countDocuments(buildFilter(keys, values));
    }


    //Metodi ausiliari
    private MongoCollection<Document> getCollection() {
        MongoDatabase mongoDatabase = DatabaseConnector.getDatabase();

        MongoCollection<Document> collection = mongoDatabase.getCollection("medicine");
        System.out.println("Collection 'medicinale' recuperata con successo");
        return collection;
    }

    private Document createDocument(MedicineBean medicine) {
        ObjectId objectId = new ObjectId();
        medicine.setId(objectId.toString());
        return new Document("_id", objectId)
                .append(NAME, medicine.getName())
                .append(INGREDIENTS, medicine.getIngredients())
                .append(AMOUNT, medicine.getAmount())
                .append(PACKAGE, medicine.getPackages());
    }

    private Document createDocument(PackageBean box) {
        Document document = new Document("packageId", box.getPackageId())
                .append(STATUS, box.getStatus())
                .append("capacity", box.getCapacity())
                .append(EXPIRY_DATE, box.getExpiryDate());

        return new Document(PACKAGE, document);
    }

    private ArrayList<PackageBean> convertToArray(List<Document> packages) {
        //Se non ci sono package restituisco null
        if (packages == null)
            return null;

        //Se ci sono package

        //Inserisco i package in un ArrayList
        ArrayList<PackageBean> packageArrayList = new ArrayList<>();

        for(Document d : packages)
            packageArrayList.add(new PackageBean(d.getBoolean(STATUS), d.getDate(EXPIRY_DATE), d.getInteger("capacity"), d.getString("packageId")));

        //Restituisco l'ArrayList
        return packageArrayList;
    }

    /**
     * Helper method to build the MongoDB Bson filter dynamically.
     */
    private Bson buildFilter(final List<String> keys, final List<Object> values) {
        final List<Bson> filtersList = new ArrayList<>();

        // Safety check: verify lists are not null and have the same size
        final int keysSize = keys.size();
        final int valuesSize = values.size();
        if (keysSize == valuesSize) {
            for (int i = 0; i < keysSize; ++i) {
                final String currentKey = keys.get(i);
                final Object currentValue = values.get(i);

                switch (currentKey) {
                    // Search by name using Regex (Case Insensitive)
                    case NAME -> filtersList.add(Filters.regex(NAME, Pattern.quote(currentValue.toString()), "i"));

                    // Logic based on stock availability:
                    // true  -> Available (amount > 0)
                    // false -> Out of stock (amount == 0)
                    case STATUS -> {
                        final boolean isAvailable = (Boolean) currentValue;
                        if (isAvailable) {
                            filtersList.add(Filters.gt(AMOUNT, 0));
                        } else {
                            filtersList.add(Filters.eq(AMOUNT, 0));
                        }
                    }

                    // TODO: CONTROLLARE SE LA SERVLET PASSA UNA STRINGA O UN DATE
                    case EXPIRY_DATE -> {
                        // Logic: Find medicines with at least one package expiring BEFORE the given date.
                        // Uses $elemMatch to search inside the 'package' array.
                        Date dateLimit = (Date) currentValue;

                        // query: { 'package': { $elemMatch: { expiryDate: { $lt: dateLimit } } } }
                        filtersList.add(Filters.elemMatch(PACKAGE, Filters.lt(EXPIRY_DATE, dateLimit)));
                    }

                    default -> logger.log(Level.SEVERE, "Warning: Unrecognized filter key: {0}", currentKey);
                }
            }
        }

        // If list is empty, return an empty Document (find all), otherwise combine with AND
        logger.log(Level.INFO, "Filters list : {0}", filtersList);
        return filtersList.isEmpty() ? new Document() : Filters.and(filtersList);
    }


    private MedicineBean parseMedicine(final Document document) {
        return new MedicineBean(
                document.get(("_id")).toString(),
                document.getString(NAME),
                document.getString(INGREDIENTS),
                document.getInteger(AMOUNT),
                convertToArray(document.getList(PACKAGE, Document.class))
        );
    }
}
