package liquibase.nosql.snapshot;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.snapshot.SnapshotGeneratorChain;
import liquibase.structure.DatabaseObject;

public abstract class NoSqlSnapshotGenerator implements SnapshotGenerator {

    private final Class<? extends DatabaseObject> defaultFor;
    private Class<? extends DatabaseObject>[] addsTo;
    protected NoSqlSnapshotGenerator (Class<? extends DatabaseObject> defaultFor, Class<? extends DatabaseObject>[] addsTo) {
        this.defaultFor = defaultFor;
        this.addsTo = addsTo;
    }

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof MongoLiquibaseDatabase) {
            if ((defaultFor != null) && defaultFor.isAssignableFrom(objectType)) {
                return PRIORITY_DEFAULT;
            }
            if (addsTo() != null) {
                for (Class<? extends DatabaseObject> type : addsTo()) {
                    if (type.isAssignableFrom(objectType)) {
                        return PRIORITY_ADDITIONAL;
                    }
                }
            }
        }
        return PRIORITY_NONE;
    }

    @Override
    public DatabaseObject snapshot(DatabaseObject example, DatabaseSnapshot snapshot, SnapshotGeneratorChain chain) throws DatabaseException, InvalidExampleException {
        if ((defaultFor != null) && defaultFor.isAssignableFrom(example.getClass())) {
            return snapshotObject(example, snapshot);
        }

        DatabaseObject chainResponse = chain.snapshot(example, snapshot);
        if (chainResponse == null) {
            return null;
        }

        if (shouldAddTo(snapshot)) {
            if (addsTo() != null) {
                for (Class<? extends DatabaseObject> addType : addsTo()) {
                    if (addType.isAssignableFrom(example.getClass())) {
                        addTo(chainResponse, snapshot);
                    }
                }
            }
        }
        return chainResponse;
    }

    protected boolean shouldAddTo(DatabaseSnapshot snapshot) {
        return (defaultFor != null) && snapshot.getSnapshotControl().shouldInclude(defaultFor);
    }

    protected abstract DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException;

    protected abstract void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException;

    @Override
    public Class<? extends DatabaseObject>[] addsTo() {
        return addsTo;
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[0];
    }

    protected Class<? extends DatabaseObject> defaultFor() {
        return defaultFor;
    }
}
