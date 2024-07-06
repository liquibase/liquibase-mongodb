package liquibase.nosql.executor;


import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.lockservice.ReplaceChangeLogLockStatement;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;


public class ReplaceChangeLogLockSqlGenerator extends AbstractSqlGenerator<ReplaceChangeLogLockStatement> {

    @Override
    public ValidationErrors validate(ReplaceChangeLogLockStatement statement, Database database, SqlGeneratorChain<ReplaceChangeLogLockStatement> sqlGeneratorChain) {
        return null;
    }

    @Override
    public Sql[] generateSql(ReplaceChangeLogLockStatement statement, Database database, SqlGeneratorChain<ReplaceChangeLogLockStatement> sqlGeneratorChain) {
        // SY-TODO: create sql sentences for lock operations..
        return new Sql[0];
    }

    @Override
    public int getPriority() {
        return SqlGenerator.PRIORITY_DATABASE;
    }

}
