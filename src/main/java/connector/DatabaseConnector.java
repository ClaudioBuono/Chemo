package connector;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnector {
    static Logger logger = Logger.getLogger(DatabaseConnector.class.getName());
    private static String dbName = "Chemo";

    public static synchronized MongoDatabase getDatabase(){
        //Viene creato il client
        MongoClient mongo = new MongoClient("localhost", 27017);
        //Configurazione del CodecRegistry per gestire la traduzione da bson e pojo e viceversa
        CodecRegistry pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                org.bson.codecs.configuration.CodecRegistries.fromProviders(
                        PojoCodecProvider.builder().automatic(true).build()));
        //Accediamo al db
        final MongoDatabase db = mongo.getDatabase(dbName).withCodecRegistry(pojoCodecRegistry);

        logger.log(Level.INFO, "Connessione al db {0} avvenuta con successo", dbName);

        return db;

    }

    public static void setDbName(String dbName){
        DatabaseConnector.dbName = dbName;
    }
}