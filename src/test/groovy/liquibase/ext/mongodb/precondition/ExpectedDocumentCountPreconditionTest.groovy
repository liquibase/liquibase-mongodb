package liquibase.ext.mongodb.precondition

import com.mongodb.client.MongoCollection
import liquibase.Scope
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.changelog.visitor.ChangeExecListener
import liquibase.exception.PreconditionErrorException
import liquibase.exception.PreconditionFailedException
import liquibase.ext.mongodb.database.MongoConnection
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement
import org.bson.Document
import org.bson.conversions.Bson
import spock.lang.Specification

class ExpectedDocumentCountPreconditionTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def changeLog = Mock(DatabaseChangeLog)
    def changeSet = Mock(ChangeSet)
    def changeExecListener = Mock(ChangeExecListener)
    def mongoConnection = Mock(MongoConnection)
    def mongoDatabase = Mock(com.mongodb.client.MongoDatabase)
    def mongoCollection = Mock(MongoCollection)
    def scope = Mock(Scope)
    
    def setup() {
        Scope.getCurrentScope() >> scope
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection("emptyCollection") >> mongoCollection
        mongoDatabase.getCollection("populatedCollection") >> mongoCollection
        mongoDatabase.getCollection("errorCollection") >> mongoCollection
    }
    
    def "should have correct name"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        
        expect:
        precondition.getName() == "expectedDocumentCount"
    }
    
    def "should return empty warnings"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        
        when:
        def warnings = precondition.warn(database)
        
        then:
        warnings.getMessages().isEmpty()
    }
    
    def "should return empty validation errors"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        
        when:
        def validationErrors = precondition.validate(database)
        
        then:
        validationErrors.getErrorMessages().isEmpty()
    }
    
    def "should pass when document count matches expected"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        precondition.setCollectionName("emptyCollection")
        precondition.setExpectedCount(0L)
        mongoCollection.countDocuments(_ as Bson) >> 0L
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        noExceptionThrown()
    }
    
    def "should pass when document count matches expected with filter"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        precondition.setCollectionName("populatedCollection")
        precondition.setFilter('{"status": "active"}')
        precondition.setExpectedCount(10L)
        mongoCollection.countDocuments(_ as Bson) >> 10L
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        noExceptionThrown()
    }
    
    def "should fail when document count does not match expected"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        precondition.setCollectionName("populatedCollection")
        precondition.setExpectedCount(5L)
        mongoCollection.countDocuments(_ as Bson) >> 10L
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionFailedException)
    }
    
    def "should handle exceptions"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        precondition.setCollectionName("errorCollection")
        precondition.setExpectedCount(0L)
        mongoCollection.countDocuments(_ as Bson) >> { throw new Exception("Test exception") }
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionErrorException)
    }
    
    def "should return correct serialized object namespace"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        
        expect:
        precondition.getSerializedObjectNamespace() == precondition.GENERIC_CHANGELOG_EXTENSION_NAMESPACE
    }
    
    def "should set and get collection name"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        def collectionName = "testCollection"
        
        when:
        precondition.setCollectionName(collectionName)
        
        then:
        precondition.getCollectionName() == collectionName
    }
    
    def "should set and get filter"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        def filter = '{"status": "active"}'
        
        when:
        precondition.setFilter(filter)
        
        then:
        precondition.getFilter() == filter
    }
    
    def "should set and get expected count"() {
        given:
        def precondition = new ExpectedDocumentCountPrecondition()
        def expectedCount = 42L
        
        when:
        precondition.setExpectedCount(expectedCount)
        
        then:
        precondition.getExpectedCount() == expectedCount
    }
}