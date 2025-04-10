package liquibase.ext.mongodb.database

import liquibase.CatalogAndSchema
import liquibase.Scope
import liquibase.changelog.ChangeLogHistoryService
import liquibase.changelog.ChangeLogHistoryServiceFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.DatabaseException
import liquibase.exception.LiquibaseException
import liquibase.executor.Executor
import liquibase.executor.ExecutorService
import liquibase.ext.mongodb.statement.DropAllCollectionsStatement
import liquibase.nosql.executor.NoSqlExecutor
import spock.lang.Specification

class MongoLiquibaseDatabaseTest extends Specification {
    def database = new MongoLiquibaseDatabase()
    def mockConnection = Mock(MongoConnection)
    def mockMongoDatabase = Mock(com.mongodb.client.MongoDatabase)
    def mockExecutorService = Mock(ExecutorService)
    def mockExecutor = Mock(Executor)
    def mockHistoryServiceFactory = Mock(ChangeLogHistoryServiceFactory)
    def mockHistoryService = Mock(ChangeLogHistoryService)
    
    def setup() {
        database.setConnection(mockConnection)
        mockConnection.getMongoDatabase() >> mockMongoDatabase
        
        // Mock scope and services
        def mockScope = Mock(Scope)
        Scope.getCurrentScope() >> mockScope
        mockScope.getSingleton(ExecutorService.class) >> mockExecutorService
        mockExecutorService.getExecutor(NoSqlExecutor.EXECUTOR_NAME, database) >> mockExecutor
        mockScope.getSingleton(ChangeLogHistoryServiceFactory.class) >> mockHistoryServiceFactory
        mockHistoryServiceFactory.getChangeLogService(database) >> mockHistoryService
    }
    
    def "should return correct product name and short name"() {
        expect:
        database.getDatabaseProductName() == "MongoDB"
        database.getShortName() == "mongodb"
    }
    
    def "should return correct default port"() {
        expect:
        database.getDefaultPort() == 27017
    }
    
    def "should get MongoDB driver for valid URL"() {
        expect:
        database.getDefaultDriver("mongodb://localhost:27017/test") == "liquibase.ext.mongodb.database.MongoClientDriver"
        database.getDefaultDriver("mongodb+srv://localhost:27017/test") == "liquibase.ext.mongodb.database.MongoClientDriver"
    }
    
    def "should return null for invalid URL"() {
        expect:
        database.getDefaultDriver("jdbc:mysql://localhost:3306/test") == null
    }
    
    def "should drop database objects"() {
        when:
        database.dropDatabaseObjects(new CatalogAndSchema("catalog", "schema"))
        
        then:
        1 * mockExecutor.execute(_ as DropAllCollectionsStatement)
        1 * mockHistoryService.destroy()
        noExceptionThrown()
    }
    
    def "should handle exceptions when dropping database objects"() {
        given:
        mockExecutor.execute(_ as DropAllCollectionsStatement) >> { throw new DatabaseException("Test exception") }
        
        when:
        database.dropDatabaseObjects(new CatalogAndSchema("catalog", "schema"))
        
        then:
        thrown(LiquibaseException)
    }
    
    def "should get database version from buildInfo command"() {
        given:
        def versionDocument = new org.bson.Document("version", "5.0.0")
        mockMongoDatabase.runCommand(new org.bson.Document("buildInfo", 1)) >> versionDocument
        
        expect:
        database.getDatabaseProductVersion() == "5.0.0"
    }
    
    def "should return unknown version when buildInfo command fails"() {
        given:
        mockMongoDatabase.runCommand(_ as org.bson.Document) >> { throw new Exception("Failed to get version") }
        def mockLog = Mock(liquibase.logging.Logger)
        def mockScope = Mock(Scope)
        Scope.getCurrentScope() >> mockScope
        mockScope.getLog(_ as Class) >> mockLog
        
        expect:
        database.getDatabaseProductVersion() == "Unknown"
    }
}