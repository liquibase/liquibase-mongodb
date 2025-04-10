package liquibase.nosql.database

import liquibase.CatalogAndSchema
import liquibase.database.DatabaseConnection
import liquibase.exception.DatabaseException
import liquibase.statement.DatabaseFunction
import liquibase.structure.DatabaseObject
import spock.lang.Specification

class AbstractNoSqlDatabaseTest extends Specification {
    
    def "should return PRIORITY_DATABASE for getPriority"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getPriority() == database.PRIORITY_DATABASE
    }
    
    def "should return false for supportsInitiallyDeferrableColumns"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsInitiallyDeferrableColumns()
    }
    
    def "should return false for supportsSequences"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsSequences()
    }
    
    def "should return false for supportsDropTableCascadeConstraints"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsDropTableCascadeConstraints()
    }
    
    def "should return false for supportsAutoIncrement"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsAutoIncrement()
    }
    
    def "should return '//' for getLineComment"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getLineComment() == "//"
    }
    
    def "should return null for getAutoIncrementClause"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getAutoIncrementClause(null, null, null, null) == null
    }
    
    def "should return false for isSystemObject"() {
        given:
        def database = new TestNoSqlDatabase()
        def example = Mock(DatabaseObject)
        
        expect:
        !database.isSystemObject(example)
    }
    
    def "should return false for isLiquibaseObject"() {
        given:
        def database = new TestNoSqlDatabase()
        def object = Mock(DatabaseObject)
        
        expect:
        !database.isLiquibaseObject(object)
    }
    
    def "should return null for getViewDefinition"() {
        given:
        def database = new TestNoSqlDatabase()
        def schema = new CatalogAndSchema("catalog", "schema")
        
        expect:
        database.getViewDefinition(schema, "name") == null
    }
    
    def "should return null for various escapeObjectName methods"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.escapeObjectName("catalog", "schema", "object", String.class) == null
        database.escapeTableName("catalog", "schema", "table") == null
        database.escapeIndexName("catalog", "schema", "index") == null
        database.escapeObjectName("object", String.class) == null
        database.escapeColumnName("catalog", "schema", "table", "column") == null
        database.escapeColumnNameList("col1, col2") == null
    }
    
    def "should return false for supportsTablespaces"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsTablespaces()
    }
    
    def "should return true for supportsCatalogs"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.supportsCatalogs()
    }
    
    def "should return ORIGINAL_CASE for getSchemaAndCatalogCase"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getSchemaAndCatalogCase() == CatalogAndSchema.CatalogAndSchemaCase.ORIGINAL_CASE
    }
    
    def "should return false for supportsSchemas"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsSchemas()
    }
    
    def "should return false for supportsCatalogInObjectName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsCatalogInObjectName(String.class)
    }
    
    def "should return null for generatePrimaryKeyName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.generatePrimaryKeyName("table") == null
    }
    
    def "should return null for escapeSequenceName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.escapeSequenceName("catalog", "schema", "sequence") == null
    }
    
    def "should return null for escapeViewName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.escapeViewName("catalog", "schema", "view") == null
    }
    
    def "should return null for escapeStringForDatabase"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.escapeStringForDatabase("string") == null
    }
    
    def "should return false for supportsRestrictForeignKeys"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsRestrictForeignKeys()
    }
    
    def "should return null for escapeConstraintName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.escapeConstraintName("constraint") == null
    }
    
    def "should return empty list for getDateFunctions"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getDateFunctions().isEmpty()
    }
    
    def "should return false for supportsForeignKeyDisable"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsForeignKeyDisable()
    }
    
    def "should return false for disableForeignKeyChecks"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.disableForeignKeyChecks()
    }
    
    def "should not throw exception for enableForeignKeyChecks"() {
        given:
        def database = new TestNoSqlDatabase()
        
        when:
        database.enableForeignKeyChecks()
        
        then:
        noExceptionThrown()
    }
    
    def "should return true for isCaseSensitive"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.isCaseSensitive()
    }
    
    def "should return false for isReservedWord"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.isReservedWord("word")
    }
    
    def "should return false for isFunction"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.isFunction("function")
    }
    
    def "should return 0 for getDataTypeMaxParameters"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getDataTypeMaxParameters("dataType") == 0
    }
    
    def "should return false for dataTypeIsNotModifiable"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.dataTypeIsNotModifiable("typeName")
    }
    
    def "should return null for generateDatabaseFunctionValue"() {
        given:
        def database = new TestNoSqlDatabase()
        def function = Mock(DatabaseFunction)
        
        expect:
        database.generateDatabaseFunctionValue(function) == null
    }
    
    def "should return false for createsIndexesForForeignKeys"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.createsIndexesForForeignKeys()
    }
    
    def "should return false for supportsPrimaryKeyNames"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsPrimaryKeyNames()
    }
    
    def "should return false for supportsNotNullConstraintNames"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsNotNullConstraintNames()
    }
    
    def "should return false for supportsBatchUpdates"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsBatchUpdates()
    }
    
    def "should return false for requiresExplicitNullForColumns"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.requiresExplicitNullForColumns()
    }
    
    def "should return null for getSystemSchema"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.getSystemSchema() == null
    }
    
    def "should return null for escapeDataTypeName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.escapeDataTypeName("dataType") == null
    }
    
    def "should return null for unescapeDataTypeName"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.unescapeDataTypeName("dataType") == null
    }
    
    def "should return null for unescapeDataTypeString"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.unescapeDataTypeString("dataTypeString") == null
    }
    
    def "should return null for validate"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        database.validate() == null
    }
    
    def "should return false for requiresUsername"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.requiresUsername()
    }
    
    def "should return false for requiresPassword"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.requiresPassword()
    }
    
    def "should return false for getAutoCommitMode"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.getAutoCommitMode()
    }
    
    def "should return false for supportsDDLInTransaction"() {
        given:
        def database = new TestNoSqlDatabase()
        
        expect:
        !database.supportsDDLInTransaction()
    }
    
    def "should check product name for isCorrectDatabaseImplementation"() {
        given:
        def database = new TestNoSqlDatabase()
        def connection = Mock(DatabaseConnection)
        
        when:
        def result = database.isCorrectDatabaseImplementation(connection)
        
        then:
        1 * connection.getDatabaseProductName() >> "TestDB"
        result == true
    }
    
    def "should format toString properly"() {
        given:
        def database = new TestNoSqlDatabase()
        def connection = Mock(DatabaseConnection)
        
        when:
        def result = database.toString()
        
        then:
        result == "TestDB : NOT CONNECTED"
        
        when:
        database.setConnection(connection)
        result = database.toString()
        
        then:
        1 * connection.getURL() >> "test://localhost/db"
        result == "TestDB : test://localhost/db"
    }
    
    private static class TestNoSqlDatabase extends AbstractNoSqlDatabase {
        @Override
        void dropDatabaseObjects(CatalogAndSchema schemaToDrop) {
            // Do nothing for test
        }

        @Override
        String getDefaultDriver(String url) {
            return "test.driver"
        }

        @Override
        String getDatabaseProductName() {
            return "TestDB"
        }
        
        @Override
        String getShortName() {
            return "testdb"
        }
        
        @Override
        Integer getDefaultPort() {
            return 12345
        }
        
        @Override
        protected String getDefaultDatabaseProductName() {
            return "TestDB"
        }
    }
}