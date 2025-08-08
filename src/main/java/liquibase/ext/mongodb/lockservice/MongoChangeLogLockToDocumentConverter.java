package liquibase.ext.mongodb.lockservice;

import liquibase.nosql.changelog.AbstractNoSqlItemToDocumentConverter;
import org.bson.Document;

import java.util.Date;

public class MongoChangeLogLockToDocumentConverter extends AbstractNoSqlItemToDocumentConverter<MongoChangeLogLock, Document> {

    @Override
    public Document toDocument(final MongoChangeLogLock item) {

        return new Document()
                .append(MongoChangeLogLock.Fields.id, item.getId())
                .append(MongoChangeLogLock.Fields.lockGranted, item.getLockGranted())
                .append(MongoChangeLogLock.Fields.lockedBy, item.getLockedBy())
                .append(MongoChangeLogLock.Fields.locked, item.getLocked());
    }

    @Override
    public MongoChangeLogLock fromDocument(final Document document) {

        return new MongoChangeLogLock(
                document.get(MongoChangeLogLock.Fields.id, Integer.class)
                , document.get(MongoChangeLogLock.Fields.lockGranted, Date.class)
                , document.get(MongoChangeLogLock.Fields.lockedBy, String.class)
                , document.get(MongoChangeLogLock.Fields.locked, Boolean.class)
        );
    }
}