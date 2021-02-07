package liquibase.ext.mongodb.precondition;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.BsonUtils;
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement;
import liquibase.precondition.AbstractPrecondition;
import lombok.Getter;
import lombok.Setter;
import org.bson.conversions.Bson;

import static java.lang.String.format;

public class DocumentExistsPrecondition extends AbstractPrecondition {

    @Getter
    @Setter
    private String collectionName;

    @Getter
    @Setter
    private String filter;

    @Override
    public String getName() {
        return "documentExists";
    }

    @Override
    public Warnings warn(final Database database) {
        return new Warnings();
    }

    @Override
    public ValidationErrors validate(final Database database) {
        return new ValidationErrors();
    }

    @Override
    public void check(final Database database, final DatabaseChangeLog changeLog, final ChangeSet changeSet,
                      final ChangeExecListener changeExecListener) throws PreconditionFailedException, PreconditionErrorException {

        try {

            final Bson bsonFilter = BsonUtils.orEmptyDocument(filter);
            final CountDocumentsInCollectionStatement countDocumentsInCollectionStatement = new CountDocumentsInCollectionStatement(collectionName, bsonFilter);

            if (countDocumentsInCollectionStatement.queryForLong((MongoLiquibaseDatabase) database) <= 0L) {
                throw new PreconditionFailedException(format(
                        "Document does not exist in collection %s", collectionName), changeLog, this);

            }
        } catch (final PreconditionFailedException e) {
            throw e;
        } catch (final Exception e) {
            throw new PreconditionErrorException(e, changeLog, this);
        }
    }

    @Override
    public String getSerializedObjectNamespace() {
        return GENERIC_CHANGELOG_EXTENSION_NAMESPACE;
    }
}
