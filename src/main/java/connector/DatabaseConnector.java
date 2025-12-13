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

    private static MongoClient mongoClient = null;

    private DatabaseConnector() {}

    public static synchronized MongoDatabase getDatabase(){
        if (mongoClient == null) {
            logger.log(Level.INFO, "--- CREAZIONE NUOVO POOL DI CONNESSIONI (DOVRESTI VEDERLO 1 VOLTA SOLA) ---");
            mongoClient = new MongoClient("localhost", 27017);
        }

        final CodecRegistry pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                org.bson.codecs.configuration.CodecRegistries.fromProviders(
                        PojoCodecProvider.builder().automatic(true).build()));

        return mongoClient.getDatabase(dbName).withCodecRegistry(pojoCodecRegistry);
    }

    public static void setDbName(String dbName){
        DatabaseConnector.dbName = dbName;
    }
}