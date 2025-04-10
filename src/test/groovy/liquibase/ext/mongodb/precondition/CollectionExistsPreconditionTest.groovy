package liquibase.ext.mongodb.precondition

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import liquibase.Scope
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.changelog.visitor.ChangeExecListener
import liquibase.exception.PreconditionErrorException
import liquibase.exception.PreconditionFailedException
import liquibase.ext.mongodb.database.MongoConnection
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement
import liquibase.nosql.executor.NoSqlExecutor
import org.bson.Document
import spock.lang.Specification

class CollectionExistsPreconditionTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def changeLog = Mock(DatabaseChangeLog)
    def changeSet = Mock(ChangeSet)
    def changeExecListener = Mock(ChangeExecListener)
    def mongoConnection = Mock(MongoConnection)
    def mongoDatabase = Mock(MongoDatabase)
    def mongoCollection = Mock(MongoCollection)
    def scope = Mock(Scope)
    
    def setup() {
        Scope.getCurrentScope() >> scope
        database.getMongoConnection() >> mongoConnection
        mongoConnection.getDatabase() >> mongoDatabase
        database.execute(_ as CountCollectionByNameStatement) >> { CountCollectionByNameStatement statement ->
            if (statement.collectionName == "existingCollection") {
                return 1L
            } else if (statement.collectionName == "nonExistingCollection") {
                return 0L
            } else if (statement.collectionName == "errorCollection") {
                throw new Exception("Test exception")
            }
            return 0L
        }
    }
    
    def "should have correct name"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        expect:
        precondition.getName() == "collectionExists"
    }
    
    def "should return empty warnings"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        when:
        def warnings = precondition.warn(database)
        
        then:
        warnings.getMessages().isEmpty()
    }
    
    def "should return empty validation errors"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        when:
        def validationErrors = precondition.validate(database)
        
        then:
        validationErrors.getErrorMessages().isEmpty()
    }
    
    def "should pass when collection exists"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        precondition.setCollectionName("existingCollection")
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        noExceptionThrown()
    }
    
    def "should fail when collection does not exist"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        precondition.setCollectionName("nonExistingCollection")
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionFailedException)
    }
    
    def "should handle exceptions"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        precondition.setCollectionName("errorCollection")
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionErrorException)
    }
    
    def "should return correct serialized object namespace"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        expect:
        precondition.getSerializedObjectNamespace() == precondition.GENERIC_CHANGELOG_EXTENSION_NAMESPACE
    }
    
    def "should set and get collection name"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        def collectionName = "testCollection"
        
        when:
        precondition.setCollectionName(collectionName)
        
        then:
        precondition.getCollectionName() == collectionName
    }
}