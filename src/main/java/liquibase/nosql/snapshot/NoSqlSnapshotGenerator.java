package liquibase.nosql.snapshot;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.snapshot.SnapshotGeneratorChain;
import liquibase.structure.DatabaseObject;

import java.util.ResourceBundle;

import static liquibase.plugin.Plugin.PRIORITY_SPECIALIZED;

public class NoSqlSnapshotGenerator implements SnapshotGenerator {
    private static final ResourceBundle mongoBundle = ResourceBundle.getBundle("liquibase/i18n/liquibase-mongo");

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof MongoLiquibaseDatabase) {
            return PRIORITY_SPECIALIZED;
        }
        return PRIORITY_NONE;
    }

    @Override
    public <T extends DatabaseObject> T snapshot(T example, DatabaseSnapshot snapshot, SnapshotGeneratorChain chain) throws DatabaseException, InvalidExampleException {
        throw new DatabaseException(String.format(mongoBundle.getString("command.unsupported"), "db-doc, diff*, generate-changelog, and snapshot*"));
    }

    @Override
    public Class<? extends DatabaseObject>[] addsTo() {
        return new Class[0];
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[0];
    }
}
