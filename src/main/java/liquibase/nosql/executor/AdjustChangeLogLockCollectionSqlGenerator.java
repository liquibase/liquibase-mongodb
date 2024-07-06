package liquibase.nosql.executor;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.lockservice.AdjustChangeLogLockCollectionStatement;
import liquibase.ext.mongodb.statement.BsonUtils;
import liquibase.sql.CallableSql;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;

import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.COMMAND_NAME;
import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;


public class AdjustChangeLogLockCollectionSqlGenerator extends AbstractSqlGenerator<AdjustChangeLogLockCollectionStatement> {

    @Override
    public ValidationErrors validate(AdjustChangeLogLockCollectionStatement statement, Database database, SqlGeneratorChain<AdjustChangeLogLockCollectionStatement> sqlGeneratorChain) {
        return null;
    }

    @Override
    public Sql[] generateSql(AdjustChangeLogLockCollectionStatement statement, Database database, SqlGeneratorChain<AdjustChangeLogLockCollectionStatement> sqlGeneratorChain) {
        String sqlString = SHELL_DB_PREFIX + COMMAND_NAME
                + "("
                + BsonUtils.toJson(statement.getCommand())
                + ");";

        CallableSql sql = new CallableSql(sqlString, ";", "1");
        return new Sql[]{sql};
    }

    @Override
    public int getPriority() {
        return SqlGenerator.PRIORITY_DATABASE;
    }

}
