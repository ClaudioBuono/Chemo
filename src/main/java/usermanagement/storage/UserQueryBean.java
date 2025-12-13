package usermanagement.storage;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import connector.DatabaseConnector;
import usermanagement.application.UserBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class UserQueryBean {
    final Logger logger = Logger.getLogger(getClass().getName());
    //Inserimento singolo documento nella collection
    public void insertDocument(final UserBean userBean){
        final MongoCollection<Document> collection = getCollection();
        final Document doc = createDocument(userBean);

        collection.insertOne(doc);
        logger.info("Inserimento documento avvenuto con successo!");
    }

    //Inserimento collezione di documenti nella collection
    public void insertDocuments(final List<UserBean> utenti){
        final ArrayList<Document> documenti = new ArrayList<>();
        for(final UserBean ut : utenti){
            final Document doc = createDocument(ut);
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
    public void updateDocument(final String id, final String valId, final String chiave, final String valoreChiave){
        final MongoCollection<Document> collection = getCollection();
        collection.updateOne(Filters.eq(id, valId), Updates.set(chiave, valoreChiave));

        logger.info("Modifica documento avvenuta con successo!");
    }

    //Ricerca documento nella collection per una data coppia (chiave, valore)
    public List<UserBean> findDocument(final String chiave, final String valore){
        final MongoCollection<Document> collection = getCollection();
        final FindIterable<Document> iterDoc = collection.find(Filters.eq(chiave, valore));
        final Iterator<Document> it = iterDoc.iterator();
        final ArrayList<UserBean> users = new ArrayList<>();

        while(it.hasNext()){
            final Document document = it.next();
            final UserBean user = new UserBean(document.getString("id"), document.getString("name"), document.getString("surname"), document.getDate("birthDate"), document.getString("city"), document.getString("username"), document.getString("password"), document.getString("specialization"), document.getInteger("type"));
            users.add(user);
        }
        return users;
    }

    //Effettuo la connessione con il db, recupero la collection dal db e la restituisco
    private MongoCollection<Document> getCollection(){
        final MongoDatabase db = DatabaseConnector.getDatabase();

        final MongoCollection<Document> coll = db.getCollection("user");
        logger.info("Collection \'utente\' recuperata con successo");
        return coll;
    }

    private Document createDocument(final UserBean userBean){
        return new Document("id", userBean.getId())
                .append("name", userBean.getName())
                .append("surname", userBean.getSurname())
                .append("city", userBean.getBirthplace())
                .append("birthDate", userBean.getBirthDate())
                .append("username", userBean.getUsername())
                .append("password", userBean.getPassword())
                .append("specialization", userBean.getSpecialization())
                .append("type", userBean.getType());
    }
}

