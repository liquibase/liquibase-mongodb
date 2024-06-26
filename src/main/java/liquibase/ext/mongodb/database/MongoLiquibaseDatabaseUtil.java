package liquibase.ext.mongodb.database;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.conversions.Bson;

import static liquibase.ext.mongodb.database.MongoLiquibaseDatabase.ADMIN_DATABASE_NAME;

public class MongoLiquibaseDatabaseUtil {
    private MongoLiquibaseDatabaseUtil() {
    }

    public static void sendPingSignal(DatabaseConnection connection) throws DatabaseException {
        try {
            if (connection instanceof MongoConnection) {
                MongoClient mongoClient = ((MongoConnection) connection).getMongoClient();
                Bson command = new BsonDocument("ping", new BsonInt64(1));
                MongoDatabase adminDb = mongoClient.getDatabase(ADMIN_DATABASE_NAME);
                adminDb.runCommand(command);
            }
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
    }
}
