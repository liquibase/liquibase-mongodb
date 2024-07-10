package liquibase.ext.mongodb.database;

import com.mongodb.MongoException;
import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.statement.ListCollectionNamesStatement;

public class MongoLiquibaseDatabaseUtil {
    private MongoLiquibaseDatabaseUtil() {
    }

    public static void checkCollectionsExist(MongoLiquibaseDatabase mongoLiquibaseDatabase) throws DatabaseException {
        Scope.getCurrentScope().getLog(MongoLiquibaseDatabaseUtil.class).severe("--------- inside checkCollectionsExist ----------------------------");

        try {
            if (new ListCollectionNamesStatement().queryForList(mongoLiquibaseDatabase).isEmpty()) {
                throw new DatabaseException("Database does not exist");
            }
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
    }
}
