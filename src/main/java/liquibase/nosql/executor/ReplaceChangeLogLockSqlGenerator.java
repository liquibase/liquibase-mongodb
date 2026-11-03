package liquibase.nosql.executor;


import com.mongodb.client.model.Filters;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.lockservice.MongoChangeLogLock;
import liquibase.ext.mongodb.lockservice.MongoChangeLogLockToDocumentConverter;
import liquibase.ext.mongodb.lockservice.ReplaceChangeLogLockStatement;
import liquibase.ext.mongodb.statement.BsonUtils;
import liquibase.sql.CallableSql;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import lombok.Getter;
import lombok.Setter;
import org.bson.BsonDocument;
import org.bson.Document;

import java.time.Clock;
import java.util.Arrays;
import java.util.Date;

import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.COMMAND_NAME;
import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;


public class ReplaceChangeLogLockSqlGenerator extends AbstractSqlGenerator<ReplaceChangeLogLockStatement> {

    public static final int DEFAULT_LOCK_ID = 1;

    /**
     * Clock field in order to make it testable
     */
    @Getter
    @Setter
    private Clock clock = Clock.systemDefaultZone();


    @Override
    public ValidationErrors validate(
            ReplaceChangeLogLockStatement statement,
            Database database,
            SqlGeneratorChain<ReplaceChangeLogLockStatement> sqlGeneratorChain
    ) {
        return null;
    }

    @Override
    public Sql[] generateSql(
            ReplaceChangeLogLockStatement statement,
            Database database,
            SqlGeneratorChain<ReplaceChangeLogLockStatement> sqlGeneratorChain
    ) {
        Document updateCommand = toUpdateCommand(statement);
        String sqlString = SHELL_DB_PREFIX + COMMAND_NAME
                + "("
                + BsonUtils.toJson(updateCommand)
                + ");";

        CallableSql sql = new CallableSql(sqlString, ";", "1");
        return new Sql[]{sql};
    }

    private Document toUpdateCommand(ReplaceChangeLogLockStatement statement) {
        MongoChangeLogLock lockModel = new MongoChangeLogLock(
                DEFAULT_LOCK_ID,
                new Date(clock.instant().toEpochMilli()),
                MongoChangeLogLock.formLockedBy(),
                statement.isLocked()
        );
        Document lockDocument = new MongoChangeLogLockToDocumentConverter().toDocument(lockModel);

        BsonDocument idFilter = Filters.eq(MongoChangeLogLock.Fields.id, DEFAULT_LOCK_ID).toBsonDocument();

        Document docToUpdate = new Document();
        docToUpdate.put("q", Document.parse(idFilter.toJson()));
        docToUpdate.put("u", lockDocument);
        docToUpdate.put("upsert", true);

        Document updateCommand = new Document();
        updateCommand.put("update", statement.getCollectionName());
        updateCommand.put("updates", Arrays.asList(docToUpdate));
        return updateCommand;
    }

    @Override
    public int getPriority() {
        return SqlGenerator.PRIORITY_DATABASE;
    }

}
