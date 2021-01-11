package liquibase.ext.mongodb.precondition;

import org.bson.conversions.Bson;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.statement.BsonUtils;
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement;
import liquibase.precondition.AbstractPrecondition;
import lombok.Getter;
import lombok.Setter;

import static java.lang.String.format;

public class ExpectedDocumentCountPrecondition extends AbstractPrecondition{
	@Getter
    @Setter
    private String collectionName;

    @Getter
    @Setter
    private String filter;
    
    @Getter
    @Setter
    private Long expectedCount;

    @Override
    public String getName() {
        return "expectedDocumentCount";
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
            Long actualDocumentCount = countDocumentsInCollectionStatement.queryForLong((MongoConnection) database.getConnection());
            if (!actualDocumentCount.equals(expectedCount)) {
                throw new PreconditionFailedException(format(
                        "ExpectedDocumentCount precondition fails for collection %s, expected: %s, actual: %s", collectionName, expectedCount, actualDocumentCount), changeLog, this);
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
