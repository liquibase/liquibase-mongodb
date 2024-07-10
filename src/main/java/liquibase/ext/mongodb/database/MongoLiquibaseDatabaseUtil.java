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
            throw new DatabaseException(String.format("User '%s' doesn't have access to database '%s'", connection.getConnectionUserName(), urlDatabaseName));
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
    }
}
