package liquibase.ext.mongodb.database;

import com.mongodb.MongoException;
import liquibase.exception.DatabaseException;

public class MongoLiquibaseDatabaseUtil {
    private MongoLiquibaseDatabaseUtil() {
    }

    public static void checkDatabaseAccessibility(MongoConnection connection) throws DatabaseException {
        try {
            String urlDatabaseName = connection.getConnectionString().getDatabase();
            for (String dbName : connection.getMongoClient().listDatabaseNames()) {
                if (dbName.equals(urlDatabaseName)) {
                    return;
                }
            }
            throw new DatabaseException();
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
    }
}
